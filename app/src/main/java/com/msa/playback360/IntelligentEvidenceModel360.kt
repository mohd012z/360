package com.msa.playback360

/**
 * Evidence-learning layer for MQL360.
 *
 * It learns from repeated observed evidence and user-confirmed labels inside the
 * analysis session. It does not copy proprietary model weights or claim to
 * recover unavailable original source.
 */
data class LearnedPattern360(
    val token: String,
    val kind: MqlObjectKind,
    val observations: Int,
    val confirmations: Int,
    val contradictions: Int,
    val confidence: Int
)

data class ModelAssessment360(
    val evidence: MqlEvidence360,
    val suggestedKind: MqlObjectKind,
    val confidence: Int,
    val reason: String
)

object IntelligentEvidenceModel360 {
    private val patterns = linkedMapOf<String, LearnedPattern360>()

    fun learn(report: MqlBinaryReport360): List<LearnedPattern360> {
        report.evidence
            .filter { it.value.isNotBlank() && it.kind != MqlObjectKind.UNKNOWN }
            .forEach { observe(it.value, it.kind, confirmed = it.status == MqlEvidenceStatus.USER_LABEL) }
        return snapshot()
    }

    fun learnReferencePair(source:MqlBinaryReport360, compiled:MqlBinaryReport360):String {
        val deep=MqlDeepReference360.compare(source,compiled)
        deep.items.forEach { item ->
            val kind=kindForCategory(item.category) ?: return@forEach
            when(item.state) {
                ReferenceState360.MATCHED -> {
                    observe(item.value,kind,confirmed=true)
                    observe(item.value,kind,confirmed=true)
                }
                ReferenceState360.UNCERTAIN -> observe(item.value,kind,confirmed=false)
                ReferenceState360.MISSING -> contradict(item.value,kind)
                ReferenceState360.EXTRA -> Unit
            }
        }
        return "Reference feedback learned: matched="+deep.matched+
            ", missing="+deep.missing+", uncertain="+deep.uncertain+
            ", model patterns="+patterns.size
    }

    fun confirm(value: String, kind: MqlObjectKind) {
        observe(value, kind, confirmed = true)
    }

    fun contradict(value: String, kind: MqlObjectKind) {
        val key = key(value, kind)
        val old = patterns[key] ?: LearnedPattern360(normalize(value), kind, 0, 0, 0, 50)
        patterns[key] = old.copy(
            contradictions = old.contradictions + 1,
            confidence = score(old.observations, old.confirmations, old.contradictions + 1)
        )
    }

    fun assess(evidence: MqlEvidence360): ModelAssessment360 {
        val normalized = normalize(evidence.value)
        val candidates = patterns.values.filter {
            normalized.contains(it.token, ignoreCase = true) ||
                it.token.contains(normalized, ignoreCase = true)
        }
        val best = candidates.maxByOrNull { it.confidence }
        return if (best == null) {
            ModelAssessment360(evidence, evidence.kind, evidence.confidence, "Direct scanner evidence; no learned corroboration.")
        } else {
            val combined = ((evidence.confidence + best.confidence) / 2).coerceIn(0, 100)
            ModelAssessment360(
                evidence,
                if (best.confidence >= 70) best.kind else evidence.kind,
                combined,
                "Corroborated by ${best.observations} observations and ${best.confirmations} user confirmations."
            )
        }
    }

    fun snapshot(): List<LearnedPattern360> =
        patterns.values.sortedWith(compareByDescending<LearnedPattern360> { it.confidence }.thenByDescending { it.observations })

    fun modelCopy(): List<LearnedPattern360> =
        snapshot().map { it.copy() }

    fun improve(report: MqlBinaryReport360): String {
        learn(report)
        val assessed = report.evidence.map { assess(it) }
        val corroborated = assessed.count { it.reason.startsWith("Corroborated") }
        val changed = assessed.count { it.suggestedKind != it.evidence.kind }
        return buildString {
            appendLine("INTELLIGENT EVIDENCE MODEL")
            appendLine("Patterns: ${patterns.size}")
            appendLine("Evidence assessed: ${assessed.size}")
            appendLine("Corroborated: $corroborated")
            appendLine("Suggested reclassifications: $changed")
            appendLine("Model copy: session evidence patterns only")
            append("No external model weights, hidden source, or unavailable original MQ4/MQ5 code are copied.")
        }
    }

    private fun observe(value: String, kind: MqlObjectKind, confirmed: Boolean) {
        val token = normalize(value)
        if (token.length < 3) return
        val k = key(token, kind)
        val old = patterns[k] ?: LearnedPattern360(token, kind, 0, 0, 0, 50)
        val observations = old.observations + 1
        val confirmations = old.confirmations + if (confirmed) 1 else 0
        patterns[k] = old.copy(
            observations = observations,
            confirmations = confirmations,
            confidence = score(observations, confirmations, old.contradictions)
        )
    }

    private fun score(observations: Int, confirmations: Int, contradictions: Int): Int {
        val base = 50 + minOf(25, observations * 2) + minOf(20, confirmations * 5) - minOf(40, contradictions * 10)
        return base.coerceIn(10, 95)
    }

    private fun normalize(value: String): String =
        value.trim().replace(Regex("\\s+"), " ").take(160)

    private fun kindForCategory(category:String):MqlObjectKind? = when(category) {
        "EVENT" -> MqlObjectKind.MQL_EVENT
        "TRADING_API" -> MqlObjectKind.TRADING_API
        "INDICATOR" -> MqlObjectKind.INDICATOR
        "DLL" -> MqlObjectKind.DLL
        "URL" -> MqlObjectKind.URL
        "CONSTANT" -> MqlObjectKind.CONSTANT
        "FUNCTION" -> MqlObjectKind.INFERRED_FUNCTION
        else -> null
    }

    private fun key(value: String, kind: MqlObjectKind): String =
        kind.name + "|" + normalize(value).lowercase()
}
