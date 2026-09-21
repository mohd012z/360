package com.msa.playback360

import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import kotlin.math.ln

data class MqlBinaryReport360(
 val fileName:String,
 val kind:MqlBinaryKind,
 val size:Long,
 val sha256:String,
 val entropy:Double,
 val evidence:List<MqlEvidence360>
) {
 val strings:Int get()=evidence.count{it.kind==MqlObjectKind.STRING}
 val urls:Int get()=evidence.count{it.kind==MqlObjectKind.URL}
 val dlls:Int get()=evidence.count{it.kind==MqlObjectKind.DLL}
 val apis:Int get()=evidence.count{it.kind==MqlObjectKind.TRADING_API || it.kind==MqlObjectKind.MQL_EVENT || it.kind==MqlObjectKind.INDICATOR}
}

/**
 * Streaming read-only scanner for EX4/EX5/MQ4/MQ5 evidence.
 * Keeps memory bounded and never modifies or executes the selected target.
 */
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
  var asciiStart=0L
  var absolute=0L

  FileInputStream(file).use { input ->
   val buffer=ByteArray(BUFFER)
   while(true){
    val n=input.read(buffer)
    if(n<=0) break
    digest.update(buffer,0,n)
    for(i in 0 until n){
     val u=buffer[i].toInt() and 0xff
     counts[u]++; total++
     val printable=u in 32..126
     if(printable){
      if(ascii.isEmpty()) asciiStart=absolute
      if(ascii.length<MAX_STRING) ascii.append(u.toChar())
     } else {
      flush(ascii,asciiStart,evidence)
     }
     absolute++
    }
   }
  }
  flush(ascii,asciiStart,evidence)

  val hash=digest.digest().joinToString(""){"%02x".format(it)}
  return MqlBinaryReport360(file.name,Mql360.kindFor(file.name),file.length(),hash,entropy(counts,total),evidence)
 }

 private fun flush(text:StringBuilder,start:Long,out:MutableList<MqlEvidence360>){
  if(text.length>=MIN_STRING && out.size<MAX_EVIDENCE){
   val value=text.toString()
   val matches=MqlCodeLibrary360.lookup(value)
   if(matches.isEmpty()) {
    out += Mql360.classifyString(value,start)
   } else {
    val base=Mql360.classifyString(value,start)
    out += base.copy(details=mapOf(
     "symbols" to matches.joinToString(","){it.name},
     "domains" to matches.map{it.domain.name}.distinct().joinToString(","),
     "categories" to matches.map{it.category}.distinct().joinToString(",")
    ))
   }
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

 fun grep(report:MqlBinaryReport360,query:String):List<MqlEvidence360> =
  report.evidence.filter {
   it.value.contains(query,true) ||
    it.details.values.any{v->v.contains(query,true)}
  }

 fun summary(report:MqlBinaryReport360):String =
  "MQL360 "+report.kind+" | "+report.fileName+
   "\nSize: "+report.size+" bytes"+
   "\nSHA-256: "+report.sha256+
   "\nEntropy: "+"%.3f".format(report.entropy)+
   "\nEvidence: "+report.evidence.size+
   "\nMQL/API/Indicator: "+report.apis+
   "\nDLL: "+report.dlls+
   "\nURL: "+report.urls
}
