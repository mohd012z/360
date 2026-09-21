package com.msa.playback360

import java.io.File
import java.io.FileInputStream

data class EncodingEvidence360(val encoding:String,val offset:Long,val marker:String,val status:String,val confidence:Int)
data class EncodingValidation360(val encoding:String,val count:Int,val valid:Int,val suspicious:Int,val notes:String)

object CodeEncoding360 {
 private val boms=listOf(
  byteArrayOf(0xEF.toByte(),0xBB.toByte(),0xBF.toByte()) to "UTF-8",
  byteArrayOf(0xFF.toByte(),0xFE.toByte(),0x00,0x00) to "UTF-32LE",
  byteArrayOf(0x00,0x00,0xFE.toByte(),0xFF.toByte()) to "UTF-32BE",
  byteArrayOf(0xFF.toByte(),0xFE.toByte()) to "UTF-16LE",
  byteArrayOf(0xFE.toByte(),0xFF.toByte()) to "UTF-16BE"
 )

 fun detectBom(file:File,maxScanBytes:Int=1024*1024):List<EncodingEvidence360>{
  require(file.isFile){"Target is not a file"}
  val limit=minOf(file.length(),maxScanBytes.toLong()).toInt()
  val data=ByteArray(limit)
  val n=FileInputStream(file).use{it.read(data)}
  if(n<=0) return emptyList()
  val out=mutableListOf<EncodingEvidence360>()
  for((sig,name) in boms){
   var i=0
   while(i<=n-sig.size){
    var ok=true
    for(j in sig.indices) if(data[i+j]!=sig[j]){ok=false;break}
    if(ok) out += EncodingEvidence360(name,i.toLong(),"BOM","OBSERVED",100)
    i++
   }
  }
  return out
 }

 fun validate(report:MqlBinaryReport360):List<EncodingValidation360>{
  val encodings=listOf("ASCII","UTF-16LE","UTF-16BE")
  return encodings.map{enc->
   val rows=MqlBinaryScanner360.byEncoding(report,enc)
   val valid=rows.count{e->e.value.length>=4 && e.value.none{it=='\uFFFD'}}
   EncodingValidation360(enc,rows.size,valid,rows.size-valid,
    if(rows.isEmpty()) "No evidence observed" else "Checks decoded length, replacement characters and scanner provenance.")
  }
 }

 fun summary(report:MqlBinaryReport360,bom:List<EncodingEvidence360>):String {
  val bomText=if(bom.isEmpty()) "none observed in scan window" else bom.joinToString{it.encoding+"@0x"+it.offset.toString(16).uppercase()}
  return "ENCODING360\nASCII: "+report.asciiStrings+
   "\nUTF-16LE: "+report.utf16LeStrings+
   "\nUTF-16BE: "+report.utf16BeStrings+
   "\nBOM: "+bomText
 }
}
