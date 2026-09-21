package com.msa.playback360

enum class ReferenceState360 { MATCHED, MISSING, EXTRA, UNCERTAIN }

data class ReferenceItem360(
    val category:String,
    val value:String,
    val state:ReferenceState360,
    val sourceConfidence:Int,
    val compiledConfidence:Int,
    val confidence:Int
)

data class DeepReferenceReport360(
    val items:List<ReferenceItem360>,
    val matched:Int,
    val missing:Int,
    val extra:Int,
    val uncertain:Int,
    val confidence:Int
)

object MqlDeepReference360 {
    private val categories=listOf(
        "EVENT" to MqlObjectKind.MQL_EVENT,
        "TRADING_API" to MqlObjectKind.TRADING_API,
        "INDICATOR" to MqlObjectKind.INDICATOR,
        "DLL" to MqlObjectKind.DLL,
        "URL" to MqlObjectKind.URL,
        "CONSTANT" to MqlObjectKind.CONSTANT,
        "FUNCTION" to MqlObjectKind.INFERRED_FUNCTION
    )

    fun compare(source:MqlBinaryReport360,compiled:MqlBinaryReport360):DeepReferenceReport360 {
        val out=mutableListOf<ReferenceItem360>()
        categories.forEach { (label,kind) ->
            val a=values(source,kind)
            val b=values(compiled,kind)
            val keys=(a.keys union b.keys).sorted()
            keys.forEach { key ->
                val left=a[key]
                val right=b[key]
                val state=when {
                    left!=null && right!=null -> if(minOf(left,right)>=50) ReferenceState360.MATCHED else ReferenceState360.UNCERTAIN
                    left!=null -> ReferenceState360.MISSING
                    else -> ReferenceState360.EXTRA
                }
                val score=when(state) {
                    ReferenceState360.MATCHED -> ((left!!+right!!)/2).coerceIn(0,100)
                    ReferenceState360.UNCERTAIN -> ((left?:0)+(right?:0))/2
                    ReferenceState360.MISSING,ReferenceState360.EXTRA -> 0
                }
                out+=ReferenceItem360(label,key,state,left?:0,right?:0,score)
            }
        }
        val matched=out.count{it.state==ReferenceState360.MATCHED}
        val missing=out.count{it.state==ReferenceState360.MISSING}
        val extra=out.count{it.state==ReferenceState360.EXTRA}
        val uncertain=out.count{it.state==ReferenceState360.UNCERTAIN}
        val sourceComparable=matched+missing+uncertain
        val confidence=if(sourceComparable==0) 0 else ((matched*100.0)/sourceComparable).toInt().coerceIn(0,100)
        return DeepReferenceReport360(out,matched,missing,extra,uncertain,confidence)
    }

    private fun values(r:MqlBinaryReport360,kind:MqlObjectKind):Map<String,Int> =
        r.evidence.filter{it.kind==kind && it.value.isNotBlank()}
            .groupBy{normalize(it.value)}
            .mapValues{(_,items)->items.maxOf{it.confidence}}

    private fun normalize(value:String):String =
        value.trim().replace(Regex("\\s+")," ").lowercase().take(240)
}
