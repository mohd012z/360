package com.msa.playback360

import java.io.File

object CodeDiagnosticVisual360 {
 fun seeThrough(file:File,r:MqlBinaryReport360):String=buildString{
  val s=ExStructure360.inspect(file)
  val methods=MqlMethodCluster360.build(file,r)
  appendLine("SEE-THROUGH / TRANSPARENT EVIDENCE VIEW")
  appendLine("Target remains read-only; layers are presentation overlays.")
  s.regions.take(256).forEach{x->
   val inside=r.evidence.filter{e->val o=e.offset?:return@filter false;o>=x.offset&&o<x.offset+x.size}
   appendLine("0x"+x.offset.toString(16).uppercase()+" "+x.label+" H="+"%.3f".format(x.entropy)+" • evidence="+inside.size)
   inside.take(5).forEach{appendLine("  ↳ "+it.kind+" "+it.value+" ["+it.confidence+"%]")}
  }
  appendLine("Method overlays: "+methods.size)
 }

 fun repair(file:File,r:MqlBinaryReport360):String=buildString{
  val s=ExStructure360.inspect(file);val methods=MqlMethodCluster360.build(file,r)
  appendLine("REPAIR GUIDE • ANALYZER / RECONSTRUCTION QUALITY")
  appendLine("This diagnoses the MQL360 reading workflow; it does not patch the imported EX4/EX5.")
  if(r.evidence.isEmpty())appendLine("• No semantic evidence → inspect encoding, structure and reference-pair coverage.")
  if(s.regions.count{it.label=="HIGH_ENTROPY"}>s.regions.size/2)appendLine("• Mostly high-entropy sample → prioritize structural/reference-pair analysis over plain strings.")
  if(methods.isEmpty())appendLine("• No method anchors → correlate APIs, constants and reference pairs before reconstruction.")
  if(r.evidence.none{it.kind==MqlObjectKind.MQL_EVENT})appendLine("• No direct MQL event evidence → do not invent OnInit/OnTick/OnCalculate.")
  appendLine("• Verify offsets → correlate regions → inspect relationships → reconstruct only supported statements.")
 }

 fun troubleshoot(file:File,r:MqlBinaryReport360):String=buildString{
  val s=ExStructure360.inspect(file);val methods=MqlMethodCluster360.build(file,r)
  appendLine("TROUBLESHOOT CHART")
  appendLine("Readable target? -> "+file.canRead())
  appendLine("Known type? -> "+r.kind)
  appendLine("Structure windows? -> "+s.regions.size)
  appendLine("Evidence found? -> "+r.evidence.size)
  appendLine("Event anchors? -> "+r.evidence.count{it.kind==MqlObjectKind.MQL_EVENT})
  appendLine("Method candidates? -> "+methods.size)
  appendLine("Native markers? -> "+NativeCompressionExtractor360.nativeMarkers(file).size)
  appendLine("GZIP candidates? -> "+NativeCompressionExtractor360.gzipMembers(file).size)
  appendLine()
  append("Decision: "+when{
   r.evidence.isEmpty()->"STRUCTURE/ENCODING/REFERENCE-PAIR"
   methods.isEmpty()->"RELATIONSHIP + METHOD CLUSTERING"
   else->"RECONSTRUCTION + VERIFICATION"
  })
 }

 fun timeWarp(file:File,r:MqlBinaryReport360):String=buildString{
  appendLine("TIMEWARP • OFFSET-ORDER EVIDENCE TIMELINE")
  appendLine("This is binary-position order, not execution time or historical time.")
  r.evidence.filter{it.offset!=null}.sortedBy{it.offset}.take(1000).forEach{
   appendLine((it.offset?.let{o->"0x"+o.toString(16).uppercase()}?:"N/A")+" | "+it.kind+" | "+it.value+" | "+it.confidence+"%")
  }
 }
}
