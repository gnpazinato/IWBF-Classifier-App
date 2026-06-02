package com.iwbfclassifier.core.importer

import android.util.Xml
import com.iwbfclassifier.data.model.SportClass
import com.iwbfclassifier.data.model.SportClassStatus
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayInputStream
import java.util.zip.Inflater
import java.util.zip.ZipInputStream

/**
 * Parses IWBF entry-list / MIC documents into a [ParsedRoster] without any
 * third-party library (docs/05). Word (.docx) and Excel (.xlsx) are OOXML — zipped
 * XML — so we read them directly. A .zip may bundle several documents (one per
 * team). PDF is best-effort: we recover raw text for manual entry only.
 */
object RosterParser {

    fun parse(fileName: String, bytes: ByteArray): ParsedRoster {
        val lower = fileName.lowercase()
        return when {
            lower.endsWith(".zip") -> parseZip(fileName, bytes)
            lower.endsWith(".docx") -> singleFileRoster(fileName) { parseDocx(fileName, bytes) }
            lower.endsWith(".xlsx") -> singleFileRoster(fileName) { parseXlsx(fileName, bytes) }
            lower.endsWith(".pdf") -> parsePdf(fileName, bytes)
            else -> ParsedRoster(
                filesFound = listOf(fileName),
                filesFailed = listOf(fileName),
                warnings = listOf("Unsupported file type: $fileName. Use ZIP, DOCX, XLSX or PDF."),
            )
        }
    }

    private inline fun singleFileRoster(fileName: String, block: () -> ParsedTeam?): ParsedRoster {
        val team = runCatching { block() }.getOrNull()
        return if (team == null || team.players.isEmpty()) {
            ParsedRoster(
                teams = team?.let { listOf(it) } ?: emptyList(),
                filesFound = listOf(fileName),
                filesFailed = if (team == null) listOf(fileName) else emptyList(),
                warnings = if (team != null && team.players.isEmpty()) {
                    listOf("No players detected in $fileName — check the file or add them manually.")
                } else {
                    listOf("Could not read $fileName.")
                },
            )
        } else {
            ParsedRoster(teams = mergeMic(listOf(team)), filesFound = listOf(fileName))
        }
    }

    // --- ZIP (a bundle of docx/xlsx/pdf) ---

    private fun parseZip(zipName: String, bytes: ByteArray): ParsedRoster {
        val found = mutableListOf<String>()
        val failed = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        val teams = mutableListOf<ParsedTeam>()
        val rawText = mutableMapOf<String, String>()

        ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val name = entry.name.substringAfterLast('/')
                val l = name.lowercase()
                if (!entry.isDirectory && (l.endsWith(".docx") || l.endsWith(".xlsx") || l.endsWith(".pdf"))) {
                    found += name
                    val entryBytes = zis.readBytes()
                    runCatching {
                        when {
                            l.endsWith(".docx") -> parseDocx(name, entryBytes)?.let { teams += it }
                            l.endsWith(".xlsx") -> parseXlsx(name, entryBytes)?.let { teams += it }
                            l.endsWith(".pdf") -> {
                                val text = extractPdfText(entryBytes)
                                if (text.isNotBlank()) rawText[name] = text
                                warnings += "$name is a PDF — automatic table reading is limited; review/add players manually."
                            }
                        }
                    }.onFailure { failed += name; warnings += "Could not read $name." }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        teams.forEach { t -> if (t.players.isEmpty()) warnings += "No players detected in ${t.sourceFile}." }

        return ParsedRoster(
            teams = mergeMic(teams),
            filesFound = found,
            filesFailed = failed,
            warnings = warnings,
            rawTextByFile = rawText,
        )
    }

    // --- DOCX ---

    private fun parseDocx(fileName: String, bytes: ByteArray): ParsedTeam? {
        val documentXml = readZipEntry(bytes, "word/document.xml") ?: return null
        val tables = parseOoxmlTables(documentXml)
        val players = tables.flatMap { playersFromTable(it, fileName) }
        return ParsedTeam(
            name = teamNameFromFile(fileName),
            code = teamCodeFromFile(fileName),
            sourceFile = fileName,
            players = players,
        )
    }

    // --- XLSX ---

    private fun parseXlsx(fileName: String, bytes: ByteArray): ParsedTeam? {
        val sharedStrings = readZipEntry(bytes, "xl/sharedStrings.xml")?.let { parseSharedStrings(it) } ?: emptyList()
        // First worksheet (sheet1.xml by convention).
        val sheetXml = readZipEntry(bytes, "xl/worksheets/sheet1.xml")
            ?: readFirstZipEntryMatching(bytes) { it.startsWith("xl/worksheets/") && it.endsWith(".xml") }
            ?: return null
        val rows = parseSheetRows(sheetXml, sharedStrings)
        val players = playersFromTable(rows, fileName)
        return ParsedTeam(
            name = teamNameFromFile(fileName),
            code = teamCodeFromFile(fileName),
            sourceFile = fileName,
            players = players,
        )
    }

    // --- PDF (best effort: raw text only) ---

    private fun parsePdf(fileName: String, bytes: ByteArray): ParsedRoster {
        val text = extractPdfText(bytes)
        return ParsedRoster(
            filesFound = listOf(fileName),
            warnings = listOf(
                "$fileName is a PDF — automatic table reading is limited. " +
                    "Recovered raw text is available; please create players manually.",
            ),
            rawTextByFile = if (text.isNotBlank()) mapOf(fileName to text) else emptyMap(),
        )
    }

    // === Column mapping / player extraction (shared by DOCX + XLSX) ===

    private data class ColMap(
        val number: Int?,
        val name: Int?,
        val klass: Int?,
        val scs: Int?,
        val iwbfId: Int?,
        val dob: Int?,
        val health: Int?,
        val impairment: Int?,
        val notes: Int?,
        val panel: Int?,
    )

    private fun mapColumns(header: List<String>): ColMap? {
        val lc = header.map { it.lowercase().trim() }
        fun find(vararg keys: String): Int? =
            lc.indexOfFirst { cell -> keys.any { cell.contains(it) } }.takeIf { it >= 0 }

        val name = find("player", "family name", "given", "name")
        // Require at least a name-like column to treat this as a player header.
        if (name == null) return null

        return ColMap(
            number = find("uniform", "number", "#", "no."),
            name = name,
            klass = find("class"),
            scs = find("scs", "status"),
            iwbfId = find("iwbf", "id"),
            dob = find("dd/mm", "date", "birth", "dob"),
            health = find("health"),
            impairment = find("impair"),
            notes = find("note"),
            panel = find("panel"),
        )
    }

    private fun playersFromTable(table: List<List<String>>, sourceFile: String): List<ParsedPlayer> {
        if (table.isEmpty()) return emptyList()
        val headerIdx = table.indexOfFirst { mapColumns(it) != null }
        if (headerIdx < 0) return emptyList()
        val cols = mapColumns(table[headerIdx]) ?: return emptyList()

        val players = mutableListOf<ParsedPlayer>()
        for (i in (headerIdx + 1) until table.size) {
            val row = table[i]
            fun cell(idx: Int?): String? = idx?.let { row.getOrNull(it) }?.trim()?.ifBlank { null }

            val name = cell(cols.name)
            val number = cell(cols.number)?.let { Regex("""\d+""").find(it)?.value }
            if (name == null && number == null) continue
            // Skip rows that are clearly section labels / repeated headers.
            if (name != null && mapColumns(row) != null && name.lowercase().contains("player")) continue

            players += ParsedPlayer(
                number = number,
                name = name,
                importedClass = SportClass.fromCode(cell(cols.klass)),
                scs = SportClassStatus.fromCode(cell(cols.scs)),
                iwbfId = cell(cols.iwbfId),
                dob = cell(cols.dob),
                healthCondition = cell(cols.health),
                impairment = cell(cols.impairment),
                notes = cell(cols.notes),
                panel = cell(cols.panel),
                sourceFile = sourceFile,
            )
        }
        return players
    }

    // === MIC merge ===

    private fun mergeMic(teams: List<ParsedTeam>): List<ParsedTeam> {
        if (teams.size <= 1) return teams
        val (mic, entry) = teams.partition { (it.sourceFile ?: "").lowercase().contains("mic") }
        if (mic.isEmpty()) return teams

        val base = entry.toMutableList()
        val leftover = mutableListOf<ParsedTeam>()

        for (micTeam in mic) {
            val targetIdx = base.indexOfFirst { it.code != null && it.code.equals(micTeam.code, ignoreCase = true) }
            if (targetIdx < 0) {
                leftover += micTeam
                continue
            }
            val target = base[targetIdx]
            val mergedPlayers = target.players.toMutableList()
            for (mp in micTeam.players) {
                val match = mergedPlayers.indexOfFirst { ep ->
                    (mp.iwbfId != null && ep.iwbfId != null && ep.iwbfId.equals(mp.iwbfId, ignoreCase = true)) ||
                        sameName(ep.name, mp.name)
                }
                if (match >= 0) {
                    val ep = mergedPlayers[match]
                    mergedPlayers[match] = ep.copy(
                        healthCondition = ep.healthCondition ?: mp.healthCondition,
                        impairment = ep.impairment ?: mp.impairment,
                        notes = ep.notes ?: mp.notes,
                        panel = ep.panel ?: mp.panel,
                        importedClass = ep.importedClass ?: mp.importedClass,
                        scs = ep.scs ?: mp.scs,
                        dob = ep.dob ?: mp.dob,
                    )
                } else {
                    mergedPlayers += mp
                }
            }
            base[targetIdx] = target.copy(players = mergedPlayers)
        }
        return base + leftover
    }

    private fun sameName(a: String?, b: String?): Boolean {
        if (a == null || b == null) return false
        fun norm(s: String) = s.lowercase().replace(Regex("""[^a-z0-9]"""), "")
        val na = norm(a)
        val nb = norm(b)
        if (na.isBlank() || nb.isBlank()) return false
        return na == nb || na.contains(nb) || nb.contains(na)
    }

    // === File-name helpers ===

    private fun teamNameFromFile(fileName: String): String {
        val base = fileName.substringAfterLast('/').substringBeforeLast('.')
        return base.substringBefore(" - ").trim().ifBlank { base.trim() }
    }

    private fun teamCodeFromFile(fileName: String): String? =
        teamNameFromFile(fileName).split(Regex("""\s+""")).firstOrNull()?.trim()?.ifBlank { null }

    // === Low-level zip / xml helpers ===

    private fun readZipEntry(bytes: ByteArray, path: String): ByteArray? {
        ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (entry.name == path) return zis.readBytes()
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
        return null
    }

    private fun readFirstZipEntryMatching(bytes: ByteArray, predicate: (String) -> Boolean): ByteArray? {
        ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory && predicate(entry.name)) return zis.readBytes()
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
        return null
    }

    /** Extract every (possibly nested) table from a Word document as rows of cell text. */
    private fun parseOoxmlTables(documentXml: ByteArray): List<List<List<String>>> {
        val parser = Xml.newPullParser().apply { setInput(ByteArrayInputStream(documentXml), "UTF-8") }
        val tables = mutableListOf<List<List<String>>>()
        val tableStack = ArrayDeque<MutableList<MutableList<String>>>()
        val rowStack = ArrayDeque<MutableList<String>>()
        val cellStack = ArrayDeque<StringBuilder>()
        var inText = false

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            val local = if (event == XmlPullParser.START_TAG || event == XmlPullParser.END_TAG) {
                parser.name.substringAfterLast(':')
            } else {
                ""
            }
            when (event) {
                XmlPullParser.START_TAG -> when (local) {
                    "tbl" -> tableStack.addLast(mutableListOf())
                    "tr" -> rowStack.addLast(mutableListOf())
                    "tc" -> cellStack.addLast(StringBuilder())
                    "t" -> inText = true
                    "tab" -> cellStack.lastOrNull()?.append(' ')
                    "br", "cr" -> cellStack.lastOrNull()?.append('\n')
                }
                XmlPullParser.TEXT -> if (inText) cellStack.lastOrNull()?.append(parser.text)
                XmlPullParser.END_TAG -> when (local) {
                    "t" -> inText = false
                    "tc" -> {
                        val c = cellStack.removeLastOrNull()
                        rowStack.lastOrNull()?.add(c?.toString()?.trim() ?: "")
                    }
                    "tr" -> rowStack.removeLastOrNull()?.let { tableStack.lastOrNull()?.add(it) }
                    "tbl" -> tableStack.removeLastOrNull()?.let { tables.add(it) }
                }
            }
            event = parser.next()
        }
        return tables
    }

    private fun parseSharedStrings(xml: ByteArray): List<String> {
        val parser = Xml.newPullParser().apply { setInput(ByteArrayInputStream(xml), "UTF-8") }
        val result = mutableListOf<String>()
        var current: StringBuilder? = null
        var inT = false
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            val local = if (event == XmlPullParser.START_TAG || event == XmlPullParser.END_TAG) {
                parser.name.substringAfterLast(':')
            } else {
                ""
            }
            when (event) {
                XmlPullParser.START_TAG -> when (local) {
                    "si" -> current = StringBuilder()
                    "t" -> inT = true
                }
                XmlPullParser.TEXT -> if (inT) current?.append(parser.text)
                XmlPullParser.END_TAG -> when (local) {
                    "t" -> inT = false
                    "si" -> { result += (current?.toString() ?: ""); current = null }
                }
            }
            event = parser.next()
        }
        return result
    }

    private fun parseSheetRows(xml: ByteArray, shared: List<String>): List<List<String>> {
        val parser = Xml.newPullParser().apply { setInput(ByteArrayInputStream(xml), "UTF-8") }
        val rows = mutableListOf<List<String>>()
        var rowCells: MutableMap<Int, String>? = null
        var cellRef: String? = null
        var cellType: String? = null
        var inV = false
        var inIsT = false
        var valueBuf: StringBuilder? = null
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            val local = if (event == XmlPullParser.START_TAG || event == XmlPullParser.END_TAG) {
                parser.name.substringAfterLast(':')
            } else {
                ""
            }
            when (event) {
                XmlPullParser.START_TAG -> when (local) {
                    "row" -> rowCells = mutableMapOf()
                    "c" -> {
                        cellRef = parser.getAttributeValue(null, "r")
                        cellType = parser.getAttributeValue(null, "t")
                        valueBuf = StringBuilder()
                    }
                    "v" -> inV = true
                    "t" -> inIsT = true
                }
                XmlPullParser.TEXT -> if (inV || inIsT) valueBuf?.append(parser.text)
                XmlPullParser.END_TAG -> when (local) {
                    "v" -> inV = false
                    "t" -> inIsT = false
                    "c" -> {
                        val raw = valueBuf?.toString() ?: ""
                        val text = if (cellType == "s") shared.getOrNull(raw.toIntOrNull() ?: -1) ?: "" else raw
                        val colIdx = columnIndex(cellRef)
                        if (colIdx >= 0) rowCells?.put(colIdx, text)
                        valueBuf = null
                    }
                    "row" -> {
                        val cells = rowCells
                        if (cells != null) {
                            val max = (cells.keys.maxOrNull() ?: -1)
                            rows += (0..max).map { cells[it]?.trim() ?: "" }
                        }
                        rowCells = null
                    }
                }
            }
            event = parser.next()
        }
        return rows
    }

    /** "A1" -> 0, "B2" -> 1, "AA1" -> 26. */
    private fun columnIndex(ref: String?): Int {
        if (ref == null) return -1
        val letters = ref.takeWhile { it.isLetter() }.uppercase()
        if (letters.isEmpty()) return -1
        var idx = 0
        for (ch in letters) idx = idx * 26 + (ch - 'A' + 1)
        return idx - 1
    }

    // === PDF best-effort text recovery ===

    private fun extractPdfText(bytes: ByteArray): String {
        val data = String(bytes, Charsets.ISO_8859_1)
        val out = StringBuilder()
        var idx = 0
        var guard = 0
        while (guard++ < 5000) {
            val s = data.indexOf("stream", idx)
            if (s < 0) break
            var start = s + "stream".length
            if (data.startsWith("\r\n", start)) start += 2 else if (start < data.length && data[start] == '\n') start += 1
            val e = data.indexOf("endstream", start)
            if (e < 0) break
            val streamBytes = bytes.copyOfRange(start, e.coerceAtMost(bytes.size))
            val text = tryInflate(streamBytes)?.toString(Charsets.ISO_8859_1)
                ?: String(streamBytes, Charsets.ISO_8859_1)
            extractTextOperators(text, out)
            idx = e + "endstream".length
            if (out.length > 200_000) break
        }
        return out.toString().trim()
    }

    private fun extractTextOperators(content: String, out: StringBuilder) {
        // (literal) Tj  and  [ (a) (b) ] TJ
        Regex("""\((?:\\.|[^\\()])*\)\s*Tj""").findAll(content).forEach {
            out.append(unescapePdf(it.value.substringBeforeLast(')').substringAfter('('))).append('\n')
        }
        Regex("""\[(.*?)\]\s*TJ""", RegexOption.DOT_MATCHES_ALL).findAll(content).forEach { m ->
            Regex("""\((?:\\.|[^\\()])*\)""").findAll(m.groupValues[1]).forEach { piece ->
                out.append(unescapePdf(piece.value.removePrefix("(").removeSuffix(")")))
            }
            out.append('\n')
        }
    }

    private fun unescapePdf(s: String): String =
        s.replace("\\(", "(").replace("\\)", ")").replace("\\\\", "\\")

    private fun tryInflate(data: ByteArray): ByteArray? = runCatching {
        val inflater = Inflater()
        inflater.setInput(data)
        val buffer = ByteArray(16 * 1024)
        val out = java.io.ByteArrayOutputStream()
        while (!inflater.finished()) {
            val n = inflater.inflate(buffer)
            if (n == 0) {
                if (inflater.needsInput() || inflater.needsDictionary()) break
            }
            out.write(buffer, 0, n)
        }
        inflater.end()
        out.toByteArray().takeIf { it.isNotEmpty() }
    }.getOrNull()
}
