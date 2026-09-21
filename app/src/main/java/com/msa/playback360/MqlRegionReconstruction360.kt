package com.msa.playback360

import java.io.File

data class ReconstructionBlock360(
    val event: String,
    val evidence: List<String>,
    val constants: List<String>,
    val confidence: Int
)

object MqlRegionReconstruction360 {
    fun blocks(file: File, report: MqlBinaryReport360, radius: Long = 4096L): List<ReconstructionBlock360> {
        val graph = CodeRelationGraph360.build(file, report, radius)
        val events = report.evidence.filter {
            it.kind == MqlObjectKind.MQL_EVENT && it.offset != null
        }
        if (events.isEmpty()) return emptyList()

        return events.map { event ->
            val offset = event.offset ?: 0L
            val edges = graph.filter {
                (it.leftOffset == offset || it.rightOffset == offset) && it.score >= 50
            }
            val labels = edges
                .flatMap { listOf(it.left, it.right) }
                .filterNot { it.startsWith("MQL_EVENT:") || it.startsWith("NUM:") }
                .distinct()
                .take(40)
            val numbers = edges
                .flatMap { listOf(it.left, it.right) }
                .filter { it.startsWith("NUM:") }
                .map { it.removePrefix("NUM:") }
                .distinct()
                .take(24)
            val edgeScore = if (edges.isEmpty()) 0 else edges.take(10).sumOf { it.score } / minOf(10, edges.size)
            ReconstructionBlock360(
                event = eventName(event),
                evidence = labels,
                constants = numbers,
                confidence = (50 + edgeScore / 2).coerceAtMost(90)
            )
        }.distinctBy { it.event }
    }

    fun render(file: File, report: MqlBinaryReport360, target: MqlBinaryKind): String {
        val reconstructed = blocks(file, report)
        return buildString {
            appendLine("// MQL360 REGION-AWARE RECONSTRUCTION")
            appendLine("// Evidence-backed scaffold; not the original source.")
            appendLine()

            if (reconstructed.isEmpty()) {
                appendLine("// No event boundary could be established from direct evidence.")
                appendLine("void Mql360_ReconstructedEntry()")
                appendLine("{")
                appendLine("   // UNKNOWN: event/control-flow boundary unavailable")
                appendLine("}")
            } else {
                reconstructed.forEach { block ->
                    appendLine(signature(block.event, target))
                    appendLine("{")
                    appendLine("   // RECONSTRUCTED BLOCK CONFIDENCE: ${block.confidence}%")
                    block.evidence.forEach {
                        appendLine("   // RELATED EVIDENCE: ${safeComment(it)}")
                    }
                    if (block.constants.isNotEmpty()) {
                        appendLine("   // NEARBY NUMERIC CANDIDATES: ${block.constants.joinToString(", ")}")
                    }
                    appendLine("   // UNKNOWN: exact statements/control flow unavailable")
                    when (block.event) {
                        "OnInit" -> appendLine("   return(INIT_SUCCEEDED);")
                        "OnCalculate" -> appendLine("   return(rates_total); // conventional placeholder, not recovered")
                    }
                    appendLine("}")
                    appendLine()
                }
            }
        }
    }

    private fun eventName(evidence: MqlEvidence360): String {
        val known = listOf("OnInit", "OnDeinit", "OnTick", "OnTimer", "OnChartEvent", "OnCalculate")
        return known.firstOrNull {
            evidence.value.contains(it, ignoreCase = true) ||
                evidence.details["symbols"]?.contains(it, ignoreCase = true) == true
        } ?: "Mql360_ReconstructedBlock"
    }

    private fun signature(event: String, target: MqlBinaryKind): String = when (event) {
        "OnInit" -> "int OnInit()"
        "OnDeinit" -> "void OnDeinit(const int reason)"
        "OnTick" -> "void OnTick()"
        "OnTimer" -> "void OnTimer()"
        "OnChartEvent" -> "void OnChartEvent(const int id,const long &lparam,const double &dparam,const string &sparam)"
        "OnCalculate" -> if (target == MqlBinaryKind.MQ4) {
            "int OnCalculate(const int rates_total,const int prev_calculated,const datetime &time[],const double &open[],const double &high[],const double &low[],const double &close[],const long &tick_volume[],const long &volume[],const int &spread[])"
        } else {
            "int OnCalculate(const int rates_total,const int prev_calculated,const datetime &time[],const double &open[],const double &high[],const double &low[],const double &close[],const long &tick_volume[],const long &volume[],const int &spread[])"
        }
        else -> "void Mql360_ReconstructedBlock()"
    }

    private fun safeComment(value: String): String =
        value.replace("\r", " ").replace("\n", " ").take(220)
}
