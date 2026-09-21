package com.msa.playback360

import java.io.File
import java.io.RandomAccessFile
import java.util.zip.GZIPInputStream

data class NativeMarker360(val offset:Long,val family:String,val marker:String,val confidence:Int)
data class GzipMember360(val offset:Long,val decodedBytes:Int,val preview:String,val status:String)

object NativeCompressionExtractor360 {
 private const val LIMIT=8*1024*1024

 fun nativeMarkers(file:File):List<NativeMarker360>{
  val bytes=readPrefix(file,LIMIT)
  val patterns=listOf(
   "C/C++" to listOf("std::","__cdecl","__stdcall","__thiscall","operator new","operator delete","type_info","vftable","__cxa_","libstdc++"),
   "C" to listOf("malloc","calloc","realloc","free","memcpy","memset","strlen","strcmp","printf","sprintf")
  )
  val out=mutableListOf<NativeMarker360>()
  patterns.forEach{(family,markers)->markers.forEach{m->
   findAscii(bytes,m).take(128).forEach{off->out+=NativeMarker360(off.toLong(),family,m,if(family=="C/C++")78 else 70)}
  }}
  return out.distinctBy{Triple(it.offset,it.family,it.marker)}.sortedBy{it.offset}.take(2000)
 }

 fun gzipMembers(file:File):List<GzipMember360>{
  val bytes=readPrefix(file,LIMIT)
  val hits=mutableListOf<Int>()
  for(i in 0 until bytes.size-2) if((bytes[i].toInt()and 255)==0x1f&&(bytes[i+1].toInt()and 255)==0x8b&&(bytes[i+2].toInt()and 255)==8) hits+=i
  return hits.take(64).map{off->
   runCatching{
    val decoded=GZIPInputStream(bytes.copyOfRange(off,bytes.size).inputStream()).use{gz->
     val out=java.io.ByteArrayOutputStream()
     val buf=ByteArray(8192)
     while(out.size()<1024*1024){val n=gz.read(buf);if(n<=0)break;out.write(buf,0,minOf(n,1024*1024-out.size()))}
     out.toByteArray()
    }
    val preview=decoded.take(160).map{b->val v=b.toInt()and 255;if(v in 32..126)v.toChar() else '.'}.joinToString("")
    GzipMember360(off.toLong(),decoded.size,preview,"validated gzip member")
   }.getOrElse{GzipMember360(off.toLong(),0,"","gzip signature observed; stream did not validate")}
  }
 }

 fun render(file:File):String=buildString{
  val native=nativeMarkers(file);val gz=gzipMembers(file)
  appendLine("NATIVE + COMPRESSION EXTRACTOR")
  appendLine("C/C++ markers: "+native.size)
  native.take(200).forEach{appendLine("0x"+it.offset.toString(16).uppercase()+" ["+it.family+" "+it.confidence+"%] "+it.marker)}
  appendLine();appendLine("GZIP candidates: "+gz.size)
  gz.forEach{appendLine("0x"+it.offset.toString(16).uppercase()+" decoded="+it.decodedBytes+" • "+it.status+(if(it.preview.isNotBlank())+" • "+it.preview else ""))}
  appendLine();append("Markers are static evidence only. GZIP data is decoded only when a standard member validates; the imported target is never modified.")
 }

 private fun readPrefix(file:File,max:Int):ByteArray{
  val n=minOf(file.length(),max.toLong()).toInt();val b=ByteArray(n)
  RandomAccessFile(file,"r").use{if(n>0)it.readFully(b)}
  return b
 }
 private fun findAscii(data:ByteArray,text:String):List<Int>{
  val needle=text.toByteArray(Charsets.US_ASCII);if(needle.isEmpty()||data.size<needle.size)return emptyList()
  val out=mutableListOf<Int>()
  outer@ for(i in 0..data.size-needle.size){for(j in needle.indices)if(data[i+j]!=needle[j])continue@outer;out+=i}
  return out
 }
}
