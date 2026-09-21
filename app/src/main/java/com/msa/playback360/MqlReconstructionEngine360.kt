package com.msa.playback360

/**
 * Evidence-to-MQL source sketcher.
 * Output is intentionally reconstructed pseudocode/source skeleton, never claimed
 * to be the unavailable original MQ4/MQ5 program.
 */
data class MqlReconstruction360(
 val target:MqlBinaryKind,
 val source:String,
 val confidence:Int,
 val evidenceUsed:Int,
 val warnings:List<String>
)

object MqlReconstructionEngine360 {
 fun reconstruct(report:MqlBinaryReport360,target:MqlBinaryKind=preferredTarget(report.kind)):MqlReconstruction360 {
  require(target==MqlBinaryKind.MQ4 || target==MqlBinaryKind.MQ5){"Target must be MQ4 or MQ5"}
  val ev=report.evidence
  fun has(s:String)=ev.any{it.value.contains(s,true) || it.details["symbols"]?.contains(s,true)==true}
  val events=linkedMapOf(
   "OnInit" to has("OnInit"), "OnDeinit" to has("OnDeinit"), "OnTick" to has("OnTick"),
   "OnTimer" to has("OnTimer"), "OnChartEvent" to has("OnChartEvent"), "OnCalculate" to has("OnCalculate")
  ).filterValues{it}.keys
  val apis=listOf("OrderSend","OrderModify","OrderClose","CTrade","MqlTradeRequest","iMA","iRSI","iMACD","iBands","iStochastic","iIchimoku","iCustom","WebRequest")
   .filter{has(it)}
  val dlls=ev.filter{it.kind==MqlObjectKind.DLL}.map{it.value}.distinct().take(50)
  val urls=ev.filter{it.kind==MqlObjectKind.URL}.map{it.value}.distinct().take(50)
  val out=StringBuilder()
  out.append("// MQL360 RECONSTRUCTED SOURCE SKELETON\n")
  out.append("// Not the original source. Generated only from observed/extracted evidence.\n")
  out.append("// Target: ").append(target).append(" | Input: ").append(report.kind).append("\n\n")
  if(dlls.isNotEmpty()){
   out.append("// Observed native-library evidence:\n")
   dlls.forEach{out.append("//   ").append(safeComment(it)).append("\n")}
   out.append("\n")
  }
  if(apis.isNotEmpty()) out.append("// Observed APIs: ").append(apis.joinToString(", ")).append("\n")
  if(urls.isNotEmpty()) out.append("// Observed network references: ").append(urls.size).append("\n\n")
  if(events.isEmpty()){
   out.append("void Mql360_ReconstructedEntry() {\n")
   appendEvidence(out,apis)
   out.append("}\n")
  } else events.forEach{event->
   out.append(eventSignature(event,target)).append(" {\n")
   appendEvidence(out,apis)
   out.append("}\n\n")
  }
  val strong=ev.count{it.kind==MqlObjectKind.MQL_EVENT || it.kind==MqlObjectKind.TRADING_API || it.kind==MqlObjectKind.INDICATOR}
  val confidence=(25 + minOf(60,strong*3) + if(events.isNotEmpty()) 10 else 0).coerceAtMost(90)
  return MqlReconstruction360(target,out.toString(),confidence,ev.size,
   listOf("Original variable names, comments, expressions and control flow are not recoverable unless directly present as evidence.",
    "API proximity does not prove an original call relationship."))
 }

 private fun appendEvidence(out:StringBuilder,apis:List<String>){
  if(apis.isEmpty()) out.append("   // No high-confidence MQL API evidence mapped to this block.\n")
  else apis.forEach{out.append("   // OBSERVED API: ").append(it).append("\n")}
 }

 private fun eventSignature(e:String,target:MqlBinaryKind)=when(e){
  "OnInit" -> "int OnInit()"
  "OnDeinit" -> "void OnDeinit(const int reason)"
  "OnTick" -> "void OnTick()"
  "OnTimer" -> "void OnTimer()"
  "OnChartEvent" -> "void OnChartEvent(const int id,const long &lparam,const double &dparam,const string &sparam)"
  "OnCalculate" -> if(target==MqlBinaryKind.MQ5) "int OnCalculate(/* reconstructed signature unavailable */)" else "int OnCalculate(/* reconstructed signature unavailable */)"
  else -> "void $e()"
 }
 private fun safeComment(s:String)=s.replace("\r"," ").replace("\n"," ").take(240)
 private fun preferredTarget(k:MqlBinaryKind)=if(k==MqlBinaryKind.EX4)MqlBinaryKind.MQ4 else MqlBinaryKind.MQ5
}
