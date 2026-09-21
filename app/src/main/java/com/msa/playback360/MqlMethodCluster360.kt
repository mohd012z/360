package com.msa.playback360
import java.io.File
import kotlin.math.abs

data class MethodEvidence360(val label:String,val offset:Long,val kind:MqlObjectKind,val confidence:Int)
data class MethodCluster360(val name:String,val anchorOffset:Long,val startOffset:Long,val endOffset:Long,val evidence:List<MethodEvidence360>,val confidence:Int,val status:MqlEvidenceStatus)

object MqlMethodCluster360 {
 fun build(file:File,report:MqlBinaryReport360,radius:Long=8192):List<MethodCluster360>{
  val located=report.evidence.filter{it.offset!=null && it.value.isNotBlank()}
  val anchors=located.filter{it.kind==MqlObjectKind.MQL_EVENT}.ifEmpty{
   located.filter{it.kind==MqlObjectKind.TRADING_API||it.kind==MqlObjectKind.INDICATOR||it.kind==MqlObjectKind.INFERRED_FUNCTION}.sortedByDescending{it.confidence}.take(32)
  }
  return anchors.mapIndexed{index,anchor->
   val at=anchor.offset?:0L
   val nearby=located.filter{val off=it.offset?:return@filter false;abs(off-at)<=radius}.sortedBy{it.offset}.take(80)
   val semantic=nearby.count{it.kind==MqlObjectKind.TRADING_API||it.kind==MqlObjectKind.INDICATOR||it.kind==MqlObjectKind.DLL||it.kind==MqlObjectKind.URL}
   val corroboration=nearby.count{it.confidence>=70}
   val confidence=((anchor.confidence+minOf(20,semantic*3)+minOf(15,corroboration))/2).coerceIn(10,95)
   val name=if(anchor.kind==MqlObjectKind.MQL_EVENT)anchor.value else "CandidateMethod_"+(index+1)
   MethodCluster360(name,at,maxOf(0L,at-radius),minOf(file.length(),at+radius),nearby.map{MethodEvidence360(it.value,it.offset?:0L,it.kind,it.confidence)},confidence,if(anchor.kind==MqlObjectKind.MQL_EVENT)MqlEvidenceStatus.RECONSTRUCTED else MqlEvidenceStatus.INFERRED)
  }.distinctBy{it.name.lowercase() to it.anchorOffset}.sortedByDescending{it.confidence}
 }
 fun render(rows:List<MethodCluster360>):String=buildString{
  appendLine("MQL METHOD CLUSTERS")
  if(rows.isEmpty())appendLine("No evidence-supported method anchors were observed.")
  rows.forEach{m->
   appendLine();appendLine("["+m.status+"] "+m.name+" • "+m.confidence+"%")
   appendLine("region 0x"+m.startOffset.toString(16).uppercase()+"..0x"+m.endOffset.toString(16).uppercase())
   m.evidence.take(20).forEach{e->appendLine("  "+e.kind+" 0x"+e.offset.toString(16).uppercase()+" "+e.label+" ["+e.confidence+"%]")}
  }
  appendLine();append("Clusters are offset/semantic reconstruction candidates; they do not prove original function boundaries or control flow.")
 }
}
