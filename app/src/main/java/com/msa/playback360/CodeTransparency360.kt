package com.msa.playback360

enum class AccessDecision { ALLOW, READ_ONLY, BLOCK }
data class TargetAuthorization(
 val ownedOrAuthorized:Boolean=false,
 val approvedForInspection:Boolean=false,
 val readOnly:Boolean=true,
 val note:String=""
){
 fun decision():AccessDecision =
  if(!ownedOrAuthorized || !approvedForInspection) AccessDecision.BLOCK
  else if(readOnly) AccessDecision.READ_ONLY else AccessDecision.ALLOW
}

data class CodeEvidence(
 val target:String,
 val file:String,
 val kind:String,
 val location:String,
 val offset:Long?=null,
 val representation:String,
 val method:String?=null,
 val function:String?=null,
 val style:String?=null,
 val runtimeObserved:Boolean=false,
 val confidence:Int=100
)

object CodeTransparency360 {
 fun describe(e:CodeEvidence):String=buildString{
  appendLine("[360 CODE EVIDENCE]")
  appendLine("Target: "+e.target)
  appendLine("File: "+e.file)
  appendLine("Kind: "+e.kind)
  appendLine("Location: "+e.location)
  e.offset?.let{appendLine("Offset: 0x"+it.toString(16).uppercase())}
  appendLine("Representation: "+e.representation)
  e.method?.let{appendLine("Method: "+it)}
  e.function?.let{appendLine("Function: "+it)}
  e.style?.let{appendLine("Style: "+it)}
  appendLine("Runtime observed: "+e.runtimeObserved)
  appendLine("Confidence: "+e.confidence+"%")
 }
 fun stressLabel(callCount:Long,errorCount:Long,totalMs:Long):String{
  if(callCount<=0)return "NOT OBSERVED"
  val errorRate=errorCount.toDouble()/callCount
  val avg=totalMs/callCount
  return when {
   errorRate>=0.10 -> "ERROR PRESSURE"
   avg>=1000 -> "SLOW PATH"
   callCount>=1000 -> "HOT PATH"
   else -> "NORMAL"
  }
 }
}
