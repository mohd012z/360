package com.msa.playback360

import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import kotlin.math.ln

data class MqlBinaryReport360(
 val fileName:String, val kind:MqlBinaryKind, val size:Long, val sha256:String,
 val entropy:Double, val evidence:List<MqlEvidence360>
) {
 val strings:Int get()=evidence.count{it.kind==MqlObjectKind.STRING}
 val urls:Int get()=evidence.count{it.kind==MqlObjectKind.URL}
 val dlls:Int get()=evidence.count{it.kind==MqlObjectKind.DLL}
 val apis:Int get()=evidence.count{it.kind==MqlObjectKind.TRADING_API || it.kind==MqlObjectKind.MQL_EVENT || it.kind==MqlObjectKind.INDICATOR}
 val asciiStrings:Int get()=evidence.count{it.details["encoding"]=="ASCII"}
 val utf16LeStrings:Int get()=evidence.count{it.details["encoding"]=="UTF-16LE"}
 val utf16BeStrings:Int get()=evidence.count{it.details["encoding"]=="UTF-16BE"}
 val unicodeStrings:Int get()=utf16LeStrings+utf16BeStrings
}

object MqlBinaryScanner360 {
 private const val BUFFER=64*1024
 private const val MIN_STRING=4
 private const val MAX_STRING=4096
 private const val MAX_EVIDENCE=20_000

 fun scan(file:File):MqlBinaryReport360 {
  require(file.isFile){"Target is not a file"}
  val digest=MessageDigest.getInstance("SHA-256")
  val counts=LongArray(256)
  var total=0L
  val evidence=ArrayList<MqlEvidence360>()
  val ascii=StringBuilder()
  val utf16=StringBuilder()
  val utf16be=StringBuilder()
  var asciiStart=0L
  var utf16Start=0L
  var utf16beStart=0L
  var absolute=0L
  var pendingUtf16Low:Int?=null
  var pendingUtf16Offset=0L
  var pendingBeZeroOffset:Long?=null

  FileInputStream(file).use { input ->
   val buffer=ByteArray(BUFFER)
   while(true){
    val n=input.read(buffer)
    if(n<=0) break
    digest.update(buffer,0,n)
    for(i in 0 until n){
     val u=buffer[i].toInt() and 0xff
     counts[u]++; total++

     if(u in 32..126){
      if(ascii.isEmpty()) asciiStart=absolute
      if(ascii.length<MAX_STRING) ascii.append(u.toChar())
     } else flush(ascii,asciiStart,evidence,"ASCII")

     val low=pendingUtf16Low
     if(low==null){
      if(u in 32..126){ pendingUtf16Low=u; pendingUtf16Offset=absolute }
      else flush(utf16,utf16Start,evidence,"UTF-16LE")
     } else {
      if(u==0){
       if(utf16.isEmpty()) utf16Start=pendingUtf16Offset
       if(utf16.length<MAX_STRING) utf16.append(low.toChar())
      } else {
       flush(utf16,utf16Start,evidence,"UTF-16LE")
      }
      pendingUtf16Low=null
     }
     val beZero=pendingBeZeroOffset
     if(beZero==null){
      if(u==0) pendingBeZeroOffset=absolute
      else flush(utf16be,utf16beStart,evidence,"UTF-16BE")
     } else {
      if(u in 32..126){
       if(utf16be.isEmpty()) utf16beStart=beZero
       if(utf16be.length<MAX_STRING) utf16be.append(u.toChar())
      } else flush(utf16be,utf16beStart,evidence,"UTF-16BE")
      pendingBeZeroOffset=null
     }
     absolute++
    }
   }
  }
  flush(ascii,asciiStart,evidence,"ASCII")
  flush(utf16,utf16Start,evidence,"UTF-16LE")
  flush(utf16be,utf16beStart,evidence,"UTF-16BE")

  val hash=digest.digest().joinToString(""){"%02x".format(it)}
  return MqlBinaryReport360(file.name,Mql360.kindFor(file.name),file.length(),hash,entropy(counts,total),evidence)
 }

 private fun flush(text:StringBuilder,start:Long,out:MutableList<MqlEvidence360>,encoding:String){
  if(text.length>=MIN_STRING && out.size<MAX_EVIDENCE){
   val value=text.toString()
   val matches=MqlCodeLibrary360.lookup(value)
   val base=Mql360.classifyString(value,start,if(encoding=="ASCII") "binary-string" else "binary-unicode-"+encoding.lowercase())
   val details=mutableMapOf("encoding" to encoding,"byteOffset" to start.toString())
   if(matches.isNotEmpty()){
    details["symbols"]=matches.joinToString(","){it.name}
    details["domains"]=matches.map{it.domain.name}.distinct().joinToString(",")
    details["categories"]=matches.map{it.category}.distinct().joinToString(",")
   }
   out += base.copy(details=details)
  }
  text.setLength(0)
 }

 private fun entropy(counts:LongArray,total:Long):Double {
  if(total==0L) return 0.0
  var h=0.0
  for(c in counts) if(c>0){
   val p=c.toDouble()/total.toDouble()
   h -= p*(ln(p)/ln(2.0))
  }
  return h
 }

 fun byEncoding(report:MqlBinaryReport360,encoding:String):List<MqlEvidence360> {
  val wanted=encoding.uppercase().replace("_","-")
  return report.evidence.filter { e ->
   val enc=e.details["encoding"]?.uppercase() ?: return@filter false
   when(wanted){
    "UTF","UTF*" -> enc.startsWith("UTF-")
    "UTF16","UTF-16" -> enc=="UTF-16LE" || enc=="UTF-16BE"
    else -> enc==wanted
   }
  }
 }

 fun grep(report:MqlBinaryReport360,query:String):List<MqlEvidence360> =
  report.evidence.filter { it.value.contains(query,true) || it.details.values.any{v->v.contains(query,true)} }

 fun atOffset(report:MqlBinaryReport360,offset:Long,radius:Long=64):List<MqlEvidence360> =
  report.evidence.filter { e -> e.offset?.let { kotlin.math.abs(it-offset)<=radius } ?: false }.sortedBy{it.offset}

 fun related(report:MqlBinaryReport360,selected:MqlEvidence360):List<MqlEvidence360> {
  val selectedDomains=selected.details["domains"]?.split(",")?.filter{it.isNotBlank()}?.toSet().orEmpty()
  return report.evidence.asSequence().filter{it!==selected}.map{candidate->
   val domains=candidate.details["domains"]?.split(",")?.filter{it.isNotBlank()}?.toSet().orEmpty()
   val distance=if(selected.offset!=null && candidate.offset!=null) kotlin.math.abs(selected.offset-candidate.offset) else Long.MAX_VALUE
   Triple(candidate,selectedDomains.intersect(domains).size,distance)
  }.filter{it.second>0 || it.third<=256}.sortedWith(compareByDescending<Triple<MqlEvidence360,Int,Long>>{it.second}.thenBy{it.third}).take(100).map{it.first}.toList()
 }

 fun summary(report:MqlBinaryReport360):String =
  "MQL360 "+report.kind+" | "+report.fileName+
   "\nSize: "+report.size+" bytes"+
   "\nSHA-256: "+report.sha256+
   "\nEntropy: "+"%.3f".format(report.entropy)+
   "\nEvidence: "+report.evidence.size+
   "\nASCII: "+report.asciiStrings+
   "\nUTF-16LE: "+report.utf16LeStrings+
   "\nUTF-16BE: "+report.utf16BeStrings+
   "\nUnicode: "+report.unicodeStrings+
   "\nMQL/API/Indicator: "+report.apis+
   "\nDLL: "+report.dlls+
   "\nURL: "+report.urls
}
