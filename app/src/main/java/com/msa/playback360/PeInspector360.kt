package com.msa.playback360

import java.io.File
import java.io.RandomAccessFile

data class PeSection360(val name:String,val virtualSize:Long,val virtualAddress:Long,val rawSize:Long,val rawOffset:Long,val characteristics:Long)
data class PeDirectory360(val name:String,val rva:Long,val size:Long)
data class PeReport360(
 val fileName:String,val machine:String,val sections:Int,val timestamp:Long,
 val characteristics:Int,val optionalMagic:Int,val imageBase:Long?,
 val entryPointRva:Long?,val sectionTable:List<PeSection360>,val directories:List<PeDirectory360>,
 val evidence:List<String>
)

/** Read-only bounded PE metadata parser for selected/copied Windows binaries. */
object PeInspector360 {
 private const val MAX_SECTIONS=96
 private val directoryNames=listOf("EXPORT","IMPORT","RESOURCE","EXCEPTION","SECURITY","BASERELOC","DEBUG","ARCHITECTURE","GLOBALPTR","TLS","LOAD_CONFIG","BOUND_IMPORT","IAT","DELAY_IMPORT","CLR","RESERVED")

 fun inspect(file:File):PeReport360 {
  require(file.isFile && file.canRead()){"PE target is not readable"}
  RandomAccessFile(file,"r").use{r->
   require(r.length()>=64){"File too small"}
   require(r.readUnsignedByte()==0x4d && r.readUnsignedByte()==0x5a){"Missing MZ signature"}
   r.seek(0x3c); val pe=readU32(r)
   require(pe in 0..(r.length()-24)){"Invalid PE header offset"}
   r.seek(pe)
   require(readU32(r)==0x00004550L){"Missing PE signature"}
   val machineId=readU16(r); val sectionCount=readU16(r).coerceAtMost(MAX_SECTIONS)
   val timestamp=readU32(r)
   r.skipBytes(8)
   val optionalSize=readU16(r); val characteristics=readU16(r)
   val optionalStart=r.filePointer
   var magic=0; var imageBase:Long?=null; var entry:Long?=null
   val dirs=mutableListOf<PeDirectory360>()
   if(optionalSize>=2 && optionalStart+optionalSize<=r.length()){
    magic=readU16(r)
    if(optionalSize>=20){ r.seek(optionalStart+16); entry=readU32(r) }
    when(magic){
     0x10b -> if(optionalSize>=32){r.seek(optionalStart+28);imageBase=readU32(r)}
     0x20b -> if(optionalSize>=32){r.seek(optionalStart+24);imageBase=readU64(r)}
    }
    val dirBase=when(magic){0x10b->optionalStart+96;0x20b->optionalStart+112;else->-1}
    if(dirBase>=0){
     for(i in directoryNames.indices){
      val pos=dirBase+i*8L
      if(pos+8>optionalStart+optionalSize) break
      r.seek(pos); val rva=readU32(r); val size=readU32(r)
      if(rva!=0L || size!=0L) dirs += PeDirectory360(directoryNames[i],rva,size)
     }
    }
   }
   val sectionBase=optionalStart+optionalSize
   val sections=mutableListOf<PeSection360>()
   for(i in 0 until sectionCount){
    val pos=sectionBase+i*40L
    if(pos+40>r.length()) break
    r.seek(pos)
    val nameBytes=ByteArray(8);r.readFully(nameBytes)
    val name=nameBytes.takeWhile{it.toInt()!=0}.toByteArray().toString(Charsets.US_ASCII)
    val virtualSize=readU32(r);val virtualAddress=readU32(r);val rawSize=readU32(r);val rawOffset=readU32(r)
    r.skipBytes(12);val flags=readU32(r)
    sections += PeSection360(name,virtualSize,virtualAddress,rawSize,rawOffset,flags)
   }
   val machine=when(machineId){0x14c->"x86";0x8664->"x64";0xAA64->"ARM64";else->"0x"+machineId.toString(16)}
   val evidence=buildList{
    if(dirs.any{it.name=="IMPORT"}) add("Import directory present")
    if(dirs.any{it.name=="EXPORT"}) add("Export directory present")
    if(dirs.any{it.name=="RESOURCE"}) add("Resource directory present")
    if(dirs.any{it.name=="SECURITY"}) add("Certificate/security directory present")
    if(dirs.any{it.name=="DEBUG"}) add("Debug directory present")
    if(dirs.any{it.name=="CLR"}) add(".NET CLR directory present")
   }
   return PeReport360(file.name,machine,sectionCount,timestamp,characteristics,magic,imageBase,entry,sections,dirs,evidence)
  }
 }

 fun sectionForRva(report:PeReport360,rva:Long):PeSection360? =
  report.sectionTable.firstOrNull{rva>=it.virtualAddress && rva<it.virtualAddress+maxOf(it.virtualSize,it.rawSize)}

 fun rvaToFileOffset(report:PeReport360,rva:Long):Long? {
  val s=sectionForRva(report,rva)?:return null
  return s.rawOffset+(rva-s.virtualAddress)
 }

 private fun readU16(r:RandomAccessFile):Int {
  val a=r.readUnsignedByte();val b=r.readUnsignedByte();return a or (b shl 8)
 }
 private fun readU32(r:RandomAccessFile):Long {
  var v=0L;for(i in 0..3)v=v or (r.readUnsignedByte().toLong() shl (8*i));return v
 }
 private fun readU64(r:RandomAccessFile):Long {
  var v=0L;for(i in 0..7)v=v or (r.readUnsignedByte().toLong() shl (8*i));return v
 }
}
