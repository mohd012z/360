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


 fun replaceable(file:File,r:MqlBinaryReport360):String=buildString{
  val methods=MqlMethodCluster360.build(file,r)
  appendLine("REPLACEABLE • RECONSTRUCTION COMPONENT MAP")
  appendLine("Candidates are generated-workspace components only; the imported EX4/EX5 remains read-only.")
  val groups=r.evidence.groupBy{it.kind}.toList().sortedByDescending{it.second.size}
  groups.forEach{(kind,rows)->
   val avg=if(rows.isEmpty())0 else rows.sumOf{it.confidence}/rows.size
   appendLine(kind.name+" • "+rows.size+" findings • avg-confidence="+avg+"%")
  }
  appendLine("Method clusters • "+methods.size)
  methods.take(64).forEach{m->
   appendLine("  "+m.name+" @ 0x"+m.anchorOffset.toString(16).uppercase()+" • "+m.confidence+"% • "+m.status)
  }
  append("Use this map to decide which inferred reconstruction blocks can be regenerated or manually replaced; it does not identify patchable protected-binary code.")
 }

 fun weakPoints(file:File,r:MqlBinaryReport360):String=buildString{
  val s=ExStructure360.inspect(file)
  val methods=MqlMethodCluster360.build(file,r)
  val weakEvidence=r.evidence.filter{it.confidence<50}
  val weakMethods=methods.filter{it.confidence<50}
  appendLine("WEAK POINTS • EVIDENCE QUALITY")
  appendLine("Low-confidence evidence: "+weakEvidence.size)
  appendLine("Low-confidence method clusters: "+weakMethods.size)
  appendLine("High-entropy windows: "+s.regions.count{it.label=="HIGH_ENTROPY"}+"/"+s.regions.size)
  if(r.evidence.none{it.kind==MqlObjectKind.MQL_EVENT})appendLine("• Missing direct event anchors")
  if(r.evidence.none{it.kind==MqlObjectKind.TRADING_API})appendLine("• No direct trading API evidence")
  if(methods.isEmpty())appendLine("• No evidence-supported method cluster")
  weakMethods.take(40).forEach{appendLine("• method "+it.name+" @ 0x"+it.anchorOffset.toString(16).uppercase()+" confidence="+it.confidence+"%")}
  weakEvidence.sortedBy{it.confidence}.take(80).forEach{
   appendLine("• "+it.kind+" "+(it.offset?.let{o->"0x"+o.toString(16).uppercase()}?:"N/A")+" "+it.value+" ["+it.confidence+"%]")
  }
  append("Weak points describe uncertainty in analysis/reconstruction, not exploitable security vulnerabilities.")
 }

 fun timeWarp(file:File,r:MqlBinaryReport360):String=buildString{
  appendLine("TIMEWARP • OFFSET-ORDER EVIDENCE TIMELINE")
  appendLine("This is binary-position order, not execution time or historical time.")
  r.evidence.filter{it.offset!=null}.sortedBy{it.offset}.take(1000).forEach{
   appendLine((it.offset?.let{o->"0x"+o.toString(16).uppercase()}?:"N/A")+" | "+it.kind+" | "+it.value+" | "+it.confidence+"%")
  }
 }
}
