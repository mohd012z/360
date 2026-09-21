package com.msa.playback360
import java.io.File

object CodeLanguageDecompiler360 {
 fun language(file:File,r:MqlBinaryReport360,mode:String):String=buildString{
  val domain=when(mode.lowercase()){
   "python","phython"->BuiltInCodeDomain.PYTHON
   "bash"->BuiltInCodeDomain.TEXT
   "cpp"->BuiltInCodeDomain.CPP
   else->BuiltInCodeDomain.GENERIC
  }
  val rows=r.evidence.filter{e->
   e.details["domains"]?.split(",")?.any{it.trim().equals(domain.name,true)}==true ||
   when(domain){
    BuiltInCodeDomain.PYTHON->e.value.contains("python",true)||e.value.contains("import ",true)||e.value.contains("def ",true)
    BuiltInCodeDomain.CPP->e.value.contains("std::",true)||e.value.contains("__cdecl",true)||e.value.contains("__cxa_",true)
    BuiltInCodeDomain.TEXT->e.value.contains("#!/bin/",true)||e.value.contains("/bin/sh",true)||e.value.contains("/bin/bash",true)
    else->false
   }
  }
  appendLine(mode.uppercase()+" EVIDENCE")
  appendLine("Target: "+file.name)
  if(rows.isEmpty())appendLine("No direct "+mode+" source/runtime markers observed.")
  rows.take(300).forEach{appendLine((it.offset?.let{o->"0x"+o.toString(16).uppercase()}?:"N/A")+" "+it.value+" ["+it.confidence+"%]")}
  if(mode.equals("cpp",true))appendLine(NativeCompressionExtractor360.render(file))
  append("Language markers classify observable evidence; they do not prove the EX4/EX5 was authored in that language.")
 }

 fun decompile(file:File,r:MqlBinaryReport360):String=buildString{
  appendLine("EVIDENCE DECOMPILER")
  appendLine("Target: "+file.name+" • "+r.kind)
  appendLine("This produces a reconstruction scaffold, not the unavailable original source.")
  appendLine()
  appendLine(MqlMethodCluster360.render(MqlMethodCluster360.build(file,r)))
  appendLine()
  val target=if(r.kind==MqlBinaryKind.EX4)MqlBinaryKind.MQ4 else MqlBinaryKind.MQ5
  appendLine(MqlRegionReconstruction360.render(file,r,target))
 }

 fun hidden(file:File,r:MqlBinaryReport360):String=buildString{
  val structure=ExStructure360.inspect(file)
  val high=structure.regions.filter{it.label=="HIGH_ENTROPY"}
  val sparse=structure.regions.filter{it.label=="SPARSE_DATA"||it.label=="LOW_ENTROPY_DATA"}
  val gz=NativeCompressionExtractor360.gzipMembers(file)
  val native=NativeCompressionExtractor360.nativeMarkers(file)
  return buildString{
   appendLine("HIDDEN / NON-OBVIOUS EVIDENCE")
   appendLine("High-entropy windows: "+high.size)
   appendLine("Sparse/low-entropy windows: "+sparse.size)
   appendLine("Validated/signature gzip candidates: "+gz.size)
   appendLine("Native C/C++ markers: "+native.size)
   appendLine()
   high.take(80).forEach{appendLine("0x"+it.offset.toString(16).uppercase()+" HIGH_ENTROPY H="+"%.3f".format(it.entropy))}
   sparse.take(80).forEach{appendLine("0x"+it.offset.toString(16).uppercase()+" "+it.label+" zero="+"%.1f%%".format(it.zeroRatio*100))}
   appendLine()
   append("Hidden means non-obvious static evidence. It does not decrypt protected EX4/EX5 content or reveal unavailable source.")
  }
 }
}
