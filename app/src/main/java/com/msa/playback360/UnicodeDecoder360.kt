package com.msa.playback360

import java.io.File
import java.io.FileInputStream

data class UnicodeString360(val encoding:String,val offset:Long,val value:String,val valid:Boolean)

object UnicodeDecoder360 {
 private const val MIN=4
 private const val MAX=4096

 fun scanUtf8(file:File,maxResults:Int=5000):List<UnicodeString360>{
  val out=mutableListOf<UnicodeString360>()
  val bytes=FileInputStream(file).use{it.readBytes()}
  var i=0
  while(i<bytes.size && out.size<maxResults){
   val start=i; val sb=StringBuilder(); var multibyte=false
   while(i<bytes.size && sb.length<MAX){
    val d=decodeUtf8(bytes,i) ?: break
    if(!printable(d.first)) break
    if(d.second>1) multibyte=true
    sb.appendCodePoint(d.first); i+=d.second
   }
   if(sb.length>=MIN && multibyte) out+=UnicodeString360("UTF-8",start.toLong(),sb.toString(),true)
   if(i==start)i++
  }
  return out
 }

 fun scanUtf32(file:File,little:Boolean,maxResults:Int=5000):List<UnicodeString360>{
  val out=mutableListOf<UnicodeString360>(); val bytes=FileInputStream(file).use{it.readBytes()}
  var i=0
  while(i+3<bytes.size && out.size<maxResults){
   val start=i; val sb=StringBuilder()
   while(i+3<bytes.size && sb.length<MAX){
    val cp=if(little) u32le(bytes,i) else u32be(bytes,i)
    if(!validCodePoint(cp) || !printable(cp)) break
    sb.appendCodePoint(cp);i+=4
   }
   if(sb.length>=MIN) out+=UnicodeString360(if(little)"UTF-32LE" else "UTF-32BE",start.toLong(),sb.toString(),true)
   if(i==start)i+=4
  }
  return out
 }

 private fun decodeUtf8(b:ByteArray,i:Int):Pair<Int,Int>?{
  val a=b[i].toInt() and 255
  if(a<0x80)return a to 1
  val len=when(a){in 0xC2..0xDF->2;in 0xE0..0xEF->3;in 0xF0..0xF4->4;else->return null}
  if(i+len>b.size)return null
  var cp=a and (0x7F shr len)
  for(k in 1 until len){val x=b[i+k].toInt() and 255;if(x !in 0x80..0xBF)return null;cp=(cp shl 6) or (x and 0x3F)}
  if((len==3 && cp<0x800)||(len==4 && cp<0x10000)||!validCodePoint(cp))return null
  return cp to len
 }
 private fun u32le(b:ByteArray,i:Int):Int=(b[i].toInt() and 255) or ((b[i+1].toInt() and 255) shl 8) or ((b[i+2].toInt() and 255) shl 16) or ((b[i+3].toInt() and 255) shl 24)
 private fun u32be(b:ByteArray,i:Int):Int=((b[i].toInt() and 255) shl 24) or ((b[i+1].toInt() and 255) shl 16) or ((b[i+2].toInt() and 255) shl 8) or (b[i+3].toInt() and 255)
 private fun validCodePoint(cp:Int)=cp in 0..0x10FFFF && cp !in 0xD800..0xDFFF
 private fun printable(cp:Int)=validCodePoint(cp) && (cp>=0x20 && cp!=0x7F)
}
