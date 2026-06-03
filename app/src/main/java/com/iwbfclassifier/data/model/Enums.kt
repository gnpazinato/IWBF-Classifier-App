package com.iwbfclassifier.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Sport Class values (docs/09). Serialized by their code, e.g. "1.0", "NE". */
@Serializable
enum class SportClass(val code: String) {
    @SerialName("1.0") C1_0("1.0"),
    @SerialName("1.5") C1_5("1.5"),
    @SerialName("2.0") C2_0("2.0"),
    @SerialName("2.5") C2_5("2.5"),
    @SerialName("3.0") C3_0("3.0"),
    @SerialName("3.5") C3_5("3.5"),
    @SerialName("4.0") C4_0("4.0"),
    @SerialName("4.5") C4_5("4.5"),
    @SerialName("NE") NE("NE");

    companion object {
        // NE removed as a selectable class (user request); kept in the enum so any
        // previously-saved "NE" still deserializes.
        val selectable: List<SportClass> = entries.filter { it != NE }

        /** Lenient parse for imported data: handles "4,0" and stray whitespace/case. */
        fun fromCode(raw: String?): SportClass? {
            if (raw.isNullOrBlank()) return null
            val norm = raw.trim().replace(',', '.').uppercase()
            return entries.firstOrNull { it.code.equals(norm, ignoreCase = true) }
        }
    }
}

/** Sport Class Status — the only four the app uses (user request): N, C, R-NÃO, R-FRD. */
@Serializable
enum class SportClassStatus(val code: String, val label: String) {
    @SerialName("N") N("N", "New"),
    @SerialName("C") C("C", "Confirmed"),
    @SerialName("R-NÃO") RNAO("R-NÃO", "Review"),
    @SerialName("R-FRD") RFRD("R-FRD", "Review (Fixed Date)");

    companion object {
        val selectable: List<SportClassStatus> = entries.toList()

        /** Lenient parse for imported data; maps legacy "R"/"FRD" to the new codes. */
        fun fromCode(raw: String?): SportClassStatus? {
            if (raw.isNullOrBlank()) return null
            val n = raw.trim().uppercase().replace('Ã', 'A').replace('–', '-')
            return when {
                n == "N" -> N
                n == "C" -> C
                n == "R-FRD" || n == "FRD" || n == "RFRD" -> RFRD
                n == "R-NAO" || n == "RNAO" || n.startsWith("R") -> RNAO
                else -> null
            }
        }
    }
}

/** App-specific workflow status (docs/01, docs/09) — not an official IWBF status. */
@Serializable
enum class ObservationStatus(val label: String) {
    @SerialName("Not Observed") NotObserved("Not Observed"),
    @SerialName("Quick Check") QuickCheck("Quick Check"),
    @SerialName("Observe") Observe("Observe"),
    @SerialName("Discuss") Discuss("Discuss"),
    @SerialName("Finalized") Finalized("Finalized");

    companion object {
        val selectable: List<ObservationStatus> = entries.toList()
    }
}
