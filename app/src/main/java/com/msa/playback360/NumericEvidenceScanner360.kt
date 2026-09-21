package com.msa.playback360

import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs

data class NumericEvidence360(val offset:Long,val type:String,val value:String,val confidence:Int,val reason:String)

object NumericEvidenceScanner360 {
 private const val WINDOW=4*1024*1024L
 private const val MAX=5000
 fun scan(file:File):List<NumericEvidence360>{
  val out=mutableListOf<NumericEvidence360>()
  RandomAccessFile(file,"r").use{r->
   val limit=minOf(r.length(),WINDOW);val b=ByteArray(limit.toInt());r.readFully(b)
   var i=0
   while(i+8<=b.size&&out.size<MAX){
    val d=ByteBuffer.wrap(b,i,8).order(ByteOrder.LITTLE_ENDIAN).double
    if(d.isFinite()&&interesting(d))out+=NumericEvidence360(i.toLong(),"DOUBLE_LE",format(d),score(d),reason(d))
    i+=8
   }
   i=0
   while(i+4<=b.size&&out.size<MAX){
    val f=ByteBuffer.wrap(b,i,4).order(ByteOrder.LITTLE_ENDIAN).float
    if(f.isFinite()&&interesting(f.toDouble()))out+=NumericEvidence360(i.toLong(),"FLOAT_LE",format(f.toDouble()),score(f.toDouble()),reason(f.toDouble()))
    i+=4
   }
  }
  return out.distinctBy{Triple(it.offset,it.type,it.value)}
 }
 fun summary(rows:List<NumericEvidence360>)=buildString{
  append("Numeric/constant candidates: ").append(rows.size).append("\n")
  rows.sortedByDescending{it.confidence}.take(500).forEach{append("0x").append(it.offset.toString(16).uppercase()).append("  ").append(it.type).append("  ").append(it.value).append("  ").append(it.confidence).append("%  ").append(it.reason).append("\n")}
  append("\nCandidates are byte interpretations, not proof of original MQL variables or parameters.")
 }
 private fun interesting(v:Double):Boolean{
  val a=abs(v);if(a==0.0||a<1e-9||a>1e9)return false
  val common=listOf(0.1,0.2,0.5,1.0,2.0,3.0,5.0,10.0,14.0,20.0,50.0,100.0,200.0,500.0,1000.0)
  return common.any{abs(v-it)<1e-7} || (v==v.toLong().toDouble()&&a<=100000)
 }
 private fun score(v:Double)=when{listOf(0.1,0.2,0.5,1.0,2.0,5.0,10.0,14.0,20.0,50.0,100.0).any{abs(v-it)<1e-7}->70;v==v.toLong().toDouble()->45;else->30}
 private fun reason(v:Double)=if(v==v.toLong().toDouble())"integer-like numeric candidate" else "common parameter-like numeric candidate"
 private fun format(v:Double)=if(v==v.toLong().toDouble())v.toLong().toString() else "%.8g".format(v)
}
