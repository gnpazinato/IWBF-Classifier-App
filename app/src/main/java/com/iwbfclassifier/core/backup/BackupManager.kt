package com.iwbfclassifier.core.backup

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Local-first backup: zips/unzips the app's whole data folder (the human-inspectable
 * competitions/ tree) so a classifier can save a `.zip` anywhere — including Google Drive
 * via the Android file picker — and restore it on another tablet. No backend, no login
 * (CLAUDE.md). The `.zip` is a plain archive: open it on any computer to inspect the JSON.
 */
object BackupManager {

    /** Zip every file under [root] (recursively) into [out], keeping relative paths. */
    fun export(root: File, out: OutputStream) {
        ZipOutputStream(BufferedOutputStream(out)).use { zip ->
            val base = root.canonicalFile
            base.walkTopDown()
                .filter { it.isFile }
                .forEach { file ->
                    val rel = file.relativeTo(base).path.replace(File.separatorChar, '/')
                    zip.putNextEntry(ZipEntry(rel))
                    file.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                }
        }
    }

    /**
     * Extract a backup [input] into [root], overwriting files with the same path and adding
     * any new ones. Existing data not present in the archive is left untouched (a restore
     * never silently deletes). Guards against Zip-Slip path traversal.
     */
    fun import(root: File, input: InputStream) {
        val base = root.canonicalFile
        ZipInputStream(BufferedInputStream(input)).use { zip ->
            var entry: ZipEntry? = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val target = resolveSafely(base, entry.name)
                    target.parentFile?.mkdirs()
                    target.outputStream().use { zip.copyTo(it) }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
    }

    /** Resolve [name] under [base], rejecting entries that would escape the data folder. */
    private fun resolveSafely(base: File, name: String): File {
        val target = File(base, name).canonicalFile
        val prefix = base.path + File.separator
        if (target != base && !target.path.startsWith(prefix)) {
            throw SecurityException("Unsafe backup entry: $name")
        }
        return target
    }
}
