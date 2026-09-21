package com.msa.playback360

import java.io.File

object CodeDeepOrchestrator360 {
 fun run(file:File,r:MqlBinaryReport360):String=buildString {
  val structure=ExStructure360.inspect(file)
  val methods=MqlMethodCluster360.build(file,r)
  val relations=CodeRelationGraph360.build(file,r)
  val numeric=NumericEvidenceScanner360.scan(file)
  val native=NativeCompressionExtractor360.nativeMarkers(file)
  val gzip=NativeCompressionExtractor360.gzipMembers(file)
  val weakEvidence=r.evidence.count{it.confidence<50}
  val weakMethods=methods.count{it.confidence<50}

  appendLine("MQL360 CODEDEEP")
  appendLine("Target: "+file.name+" • "+r.kind+" • "+file.length()+" bytes")
  appendLine("SHA-256: "+r.sha256)
  appendLine("Entropy: "+"%.5f".format(r.entropy))
  appendLine()
  appendLine("1 STRUCTURE")
  appendLine("  windows="+structure.regions.size+
   " highEntropy="+structure.regions.count{it.label=="HIGH_ENTROPY"}+
   " textLike="+structure.regions.count{it.label=="TEXT_LIKE"}+
   " sparse="+structure.regions.count{it.label=="SPARSE_DATA"})
  appendLine("2 EXTRACTION")
  appendLine("  semantic="+r.evidence.size+" numeric="+numeric.size+" native="+native.size+" gzipCandidates="+gzip.size)
  appendLine("3 METHOD MODEL")
  appendLine("  clusters="+methods.size+" lowConfidence="+weakMethods)
  methods.take(24).forEach{appendLine("  "+it.name+" @0x"+it.anchorOffset.toString(16).uppercase()+" "+it.confidence+"% "+it.status)}
  appendLine("4 RELATION GRAPH")
  appendLine("  edges="+relations.size)
  relations.take(24).forEach{appendLine("  "+it.left+" -> "+it.right+" score="+it.score)}
  appendLine("5 WEAK POINTS")
  appendLine("  lowConfidenceEvidence="+weakEvidence)
  if(r.evidence.none{it.kind==MqlObjectKind.MQL_EVENT})appendLine("  missing direct MQL event anchors")
  if(methods.isEmpty())appendLine("  no evidence-supported method clusters")
  appendLine("6 RECONSTRUCTION READINESS")
  val readiness=when{
   r.evidence.isEmpty()->"LOW • structure/reference-pair work required"
   methods.isEmpty()->"LIMITED • relationship/method clustering required"
   weakMethods>methods.size/2->"LIMITED • most method clusters are low confidence"
   else->"EVIDENCE AVAILABLE • reconstruct with provenance/confidence labels"
  }
  appendLine("  "+readiness)
  appendLine()
  append("No protection bypass is performed. Unknown or optimized-away source remains unknown rather than fabricated.")
 }
}
