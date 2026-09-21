package com.msa.playback360
import java.io.File
import java.io.RandomAccessFile
import java.security.MessageDigest

object CodeRead360 {
 fun read(file:File,r:MqlBinaryReport360):String=buildString{
  appendLine("CODE READ 360")
  appendLine(MqlBinaryScanner360.summary(r))
  appendLine();appendLine(ExStructure360.summary(ExStructure360.inspect(file)))
  appendLine();appendLine(MqlMethodCluster360.render(MqlMethodCluster360.build(file,r)))
 }
 fun assembly(file:File,r:MqlBinaryReport360):String=buildString{
  appendLine("ASSEMBLY-LIKE BINARY VIEW")
  appendLine("EX4/EX5 is not assumed to be native CPU assembly. Bytes are shown with evidence annotations.")
  val ev=r.evidence.filter{it.offset!=null}.sortedBy{it.offset}.take(200)
  RandomAccessFile(file,"r").use{raf->
   ev.forEach{e->
    val off=e.offset?:return@forEach
    if(off>=0&&off<raf.length()){
     raf.seek(off);val n=minOf(16L,raf.length()-off).toInt();val b=ByteArray(n);raf.readFully(b)
     append("0x").append(off.toString(16).uppercase()).append("  ")
     append(b.joinToString(" "){"%02X".format(it.toInt()and 255)})
     append("  ; ").append(e.kind).append(" ").append(e.value).appendLine()
    }
   }
  }
 }
 fun start(file:File,r:MqlBinaryReport360):String=buildString{
  appendLine("CODE START / ENTRY EVIDENCE")
  val candidates=r.evidence.filter{it.kind==MqlObjectKind.MQL_EVENT}.sortedBy{it.offset}
  if(candidates.isEmpty())appendLine("No direct MQL event/entry evidence observed.")
  candidates.forEach{appendLine((it.offset?.let{o->"0x"+o.toString(16).uppercase()}?:"N/A")+" "+it.value+" ["+it.confidence+"%]")}
  append("These are event-entry candidates, not proven VM instruction entry addresses.")
 }
 fun functions(file:File,r:MqlBinaryReport360)=MqlMethodCluster360.render(MqlMethodCluster360.build(file,r))
 fun process(file:File,r:MqlBinaryReport360):String=buildString{
  appendLine("ANALYSIS PROCESS")
  appendLine("1 LOAD • "+file.name+" • "+file.length()+" bytes")
  appendLine("2 HASH • "+r.sha256)
  appendLine("3 STRUCTURE • "+ExStructure360.inspect(file).regions.size+" sampled windows")
  appendLine("4 EVIDENCE • "+r.evidence.size+" findings")
  appendLine("5 METHODS • "+MqlMethodCluster360.build(file,r).size+" candidate clusters")
  appendLine("6 RELATIONSHIPS • "+CodeRelationGraph360.build(file,r).size+" evidence edges")
  append("Read-only analysis; original target is unchanged.")
 }
 fun log(file:File,r:MqlBinaryReport360):String=buildString{
  appendLine("MQL360 ANALYSIS LOG")
  appendLine("file="+file.name);appendLine("size="+file.length());appendLine("sha256="+r.sha256)
  appendLine("kind="+r.kind);appendLine("entropy="+"%.4f".format(r.entropy));appendLine("evidence="+r.evidence.size)
  r.evidence.groupingBy{it.kind}.eachCount().toList().sortedByDescending{it.second}.forEach{appendLine(it.first.name+"="+it.second)}
 }
 fun copy(r:MqlBinaryReport360):String="Evidence copy prepared in memory: "+r.evidence.size+" findings. Original binary is not modified."
 fun writeBoundary():String="Code write is restricted to generated reconstruction/report workspace output. Imported EX4/EX5 targets remain read-only."
 fun decryptBoundary():String="No generic EX4/EX5 protection decryption is performed. /codedecryptor may only decode standard formats or data for which the user supplies the required key/password."
 fun ask(file:File,r:MqlBinaryReport360,q:String):String{
  if(q.isBlank())return "Usage: /codeask <term or question>"
  val hits=MqlBinaryScanner360.grep(r,q).take(30)
  return if(hits.isEmpty())"No direct evidence matched: "+q else buildString{appendLine("Evidence matching '"+q+"':");hits.forEach{appendLine(it.kind.name+" "+it.value+" @ "+(it.offset?.let{"0x"+it.toString(16).uppercase()}?:"N/A")+" ["+it.confidence+"%]")}}
 }
}
