package com.msa.playback360

import java.io.File

data class ReferenceMetric360(
    val category:String,
    val sourceCount:Int,
    val compiledCount:Int,
    val matched:Int,
    val confidence:Int
)

data class ReferencePairReport360(
    val sourceFile:String,
    val compiledFile:String,
    val sourceKind:MqlBinaryKind,
    val compiledKind:MqlBinaryKind,
    val validPair:Boolean,
    val metrics:List<ReferenceMetric360>,
    val overallConfidence:Int,
    val notes:List<String>
)

object MqlReferencePair360 {
    fun isValid(source:MqlBinaryKind, compiled:MqlBinaryKind):Boolean =
        (source==MqlBinaryKind.MQ4 && compiled==MqlBinaryKind.EX4) ||
        (source==MqlBinaryKind.MQ5 && compiled==MqlBinaryKind.EX5)

    fun compare(source:MqlBinaryReport360, compiled:MqlBinaryReport360):ReferencePairReport360 {
        val valid=isValid(source.kind,compiled.kind)
        val kinds=listOf(
            "Events" to MqlObjectKind.MQL_EVENT,
            "Trading API" to MqlObjectKind.TRADING_API,
            "Indicators" to MqlObjectKind.INDICATOR,
            "DLL" to MqlObjectKind.DLL,
            "URLs" to MqlObjectKind.URL
        )
        val metrics=kinds.map { (label,kind) ->
            val left=normalized(source,kind)
            val right=normalized(compiled,kind)
            val matched=(left intersect right).size
            val denominator=maxOf(1,left.size)
            val confidence=((matched*100.0)/denominator).toInt().coerceIn(0,100)
            ReferenceMetric360(label,left.size,right.size,matched,confidence)
        }
        val usable=metrics.filter{it.sourceCount>0}
        val overall=if(!valid || usable.isEmpty()) 0 else usable.map{it.confidence}.average().toInt().coerceIn(0,100)
        val notes=buildList {
            if(!valid) add("Pair mismatch: use MQ4 with EX4 or MQ5 with EX5.")
            if(valid) add("Confidence measures overlap of indexed evidence, not recovery of unavailable original statements/control flow.")
            if(usable.isEmpty()) add("No comparable source-side semantic evidence was indexed.")
        }
        return ReferencePairReport360(source.fileName,compiled.fileName,source.kind,compiled.kind,valid,metrics,overall,notes)
    }

    fun render(report:ReferencePairReport360):String=buildString {
        appendLine("REFERENCE PAIR VERIFICATION")
        appendLine(report.sourceFile+" <-> "+report.compiledFile)
        appendLine("Pair: "+report.sourceKind+" <-> "+report.compiledKind+"  Valid: "+report.validPair)
        appendLine("Reconstruction confidence: "+report.overallConfidence+"%")
        report.metrics.forEach {
            appendLine(it.category+": "+it.matched+"/"+it.sourceCount+" source evidence matched; compiled="+it.compiledCount+"; confidence="+it.confidence+"%")
        }
        report.notes.forEach{appendLine("Note: "+it)}
    }

    private fun normalized(r:MqlBinaryReport360,k:MqlObjectKind):Set<String> =
        r.evidence.filter{it.kind==k}.map{it.value.trim().lowercase()}.filter{it.isNotBlank()}.toSet()
}
