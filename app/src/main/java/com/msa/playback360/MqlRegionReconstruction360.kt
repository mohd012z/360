package com.msa.playback360
import java.io.File

data class ReconstructionBlock360(val event:String,val evidence:List<String>,val constants:List<String>,val confidence:Int)

object MqlRegionReconstruction360 {
 fun blocks(file:File,r:MqlBinaryReport360,radius:Long=4096):List<ReconstructionBlock360>{
  val graph=CodeRelationGraph360.build(file,r,radius)
  val events=r.evidence.filter{it.kind==MqlObjectKind.MQL_EVENT&&it.offset!=null}
  if(events.isEmpty())return emptyList()
  return events.map{e->
   val off=e.offset!!
   val edges=graph.filter{(it.leftOffset==off||it.rightOffset==off)&&it.score>=50}
   val labels=edges.flatMap{listOf(it.left,it.right)}.filterNot{it.startsWith("MQL_EVENT:")||it.startsWith("NUM:")}.distinct().take(40)
   val nums=edges.flatMap{listOf(it.left,it.right)}.filter{it.startsWith("NUM:")}.map{it.removePrefix("NUM:")}.distinct().take(24)
   ReconstructionBlock360(eventName(e),labels,nums,(55+edges.take(10).sumOf{it.score}/20).coerceAtMost(90))
  }.distinctBy{it.event}
 }
 fun render(file:File,r:MqlBinaryReport360,target:MqlBinaryKind):String{
  val blocks=blocks(file,r)
  return buildString{
   append("// MQL360 REGION-AWARE RECONSTRUCTION\n// Evidence-backed scaffold; not the original source.\n\n")
   if(blocks.isEmpty()){append("// No event boundary could be established from direct evidence.\nvoid Mql360_ReconstructedEntry(){\n   // UNKNOWN: event/control-flow boundary unavailable\n}\n");return@buildString}
   blocks.forEach{b->
    append(signature(b.event,target)).append("{\n")
    append("   // RECONSTRUCTED BLOCK CONFIDENCE: ").append(b.confidence).append("%\n")
    b.evidence.forEach{append("   // RELATED EVIDENCE: ").append(comment(it)).append("\n")}
    if(b.constants.isNotEmpty())append("   // NEARBY NUMERIC CANDIDATES: ").append(b.constants.joinToString(", ")).append("\n")
    append("   // UNKNOWN: exact statements/control flow unavailable\n")
    if(b.event=="OnInit")append("   return(INIT_SUCCEEDED);\n")
    if(b.event=="OnCalculate")append("   return(rates_total); // conventional placeholder, not recovered\n")
    append("}\n\n")
   }
  }
 }
 private fun eventName(e:MqlEvidence360)=listOf("OnInit","OnDeinit","OnTick","OnTimer","OnChartEvent","OnCalculate").firstOrNull{e.value.contains(it,true)||e.details["symbols"]?.contains(it,true)==true}?:e.value.take(80)
 private fun signature(e:String,t:MqlBinaryKind)=when(e){"OnInit"->"int OnInit()\n";"OnDeinit"->"void OnDeinit(const int reason)\n";"OnTick"->"void OnTick()\n";"OnTimer"->"void OnTimer()\n";"OnChartEvent"->"void OnChartEvent(const int id,const long &lparam,const double &dparam,const string &sparam)\n";"OnCalculate"->"int OnCalculate(const int rates_total,const int prev_calculated,const datetime &time[],const double &open[],const double &high[],const double &low[],const double &close[],const long &tick_volume[],const long &volume[],const int &spread[])\n";else->"void Mql360_ReconstructedBlock()\n"}
 private fun comment(s:String)=s.replace("\r"," ").replace("\n"," ").take(220)
}
