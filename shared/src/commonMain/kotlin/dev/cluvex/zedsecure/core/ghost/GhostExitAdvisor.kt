package dev.cluvex.zedsecure.core.ghost

/**
 * Suggests preferred exit regions from country probe results for Ghost Mode.
 * Pure scoring logic — does not talk to cores or change routing by itself.
 * Ghost Mode only.
 */
object GhostExitAdvisor {

    data class Advice(
        val preferredCountryCodes: List<String>,
        val avoidCountryCodes: List<String>,
        val scoreByCountry: Map<String, Double>,
        val reasonFa: String
    )

    fun advise(report: GhostCountryNetProbe.CountryReport): Advice {
        val scores = mutableMapOf<String, Double>()
        val avoid = mutableListOf<String>()

        report.samples.groupBy { it.countryCode }.forEach { (code, list) ->
            val best = list.filter { it.reachable }.minByOrNull { it.latencyMs }
            if (best == null) {
                avoid += code
                scores[code] = 0.0
            } else {
                val latencyScore = (1000.0 / (best.latencyMs + 1)).coerceAtMost(50.0)
                val bwScore = (best.downloadKbps / 100.0).coerceAtMost(40.0)
                scores[code] = latencyScore + bwScore + 10.0
            }
        }

        val preferred = scores.entries
            .filter { it.value > 0 }
            .sortedByDescending { it.value }
            .map { it.key }
            .distinct()
            .take(5)

        val reason = buildString {
            if (preferred.isEmpty()) append("هیچ مسیر سالمی از اسکن کشور پیدا نشد")
            else {
                append("پیشنهاد خروج: ")
                append(preferred.joinToString("، "))
                report.bestCountryCode?.let {
                    append(" • بهترین تأخیر: ")
                    append(it)
                }
            }
        }

        return Advice(
            preferredCountryCodes = preferred,
            avoidCountryCodes = avoid.distinct(),
            scoreByCountry = scores,
            reasonFa = reason
        )
    }
}
