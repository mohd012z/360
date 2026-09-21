package com.msa.playback360

import java.io.File
import java.io.RandomAccessFile

data class PeImportFunction360(val dll:String,val name:String?,val ordinal:Long?,val thunkRva:Long)
data class PeImportModule360(val dll:String,val functions:List<PeImportFunction360>)
data class PeExport360(val name:String?,val ordinal:Long,val rva:Long,val forwarder:String?)
data class PeSymbolsReport360(val imports:List<PeImportModule360>,val exports:List<PeExport360>,val warnings:List<String>)

object PeSymbolInspector360 {
 private const val MAX_MODULES=1024
 private const val MAX_FUNCS=8192
 private const val MAX_STRING=512

 fun inspect(file:File,pe:PeReport360=PeInspector360.inspect(file)):PeSymbolsReport360 {
  val imports=mutableListOf<PeImportModule360>();val exports=mutableListOf<PeExport360>();val warnings=mutableListOf<String>()
  RandomAccessFile(file,"r").use{r->
   val imp=pe.directories.firstOrNull{it.name=="IMPORT"}
   if(imp!=null) runCatching{parseImports(r,pe,imp,imports)}.onFailure{warnings+="Import parse stopped: "+(it.message?:"invalid structure")}
   val exp=pe.directories.firstOrNull{it.name=="EXPORT"}
   if(exp!=null) runCatching{parseExports(r,pe,exp,exports)}.onFailure{warnings+="Export parse stopped: "+(it.message?:"invalid structure")}
  }
  return PeSymbolsReport360(imports,exports,warnings)
 }

 private fun parseImports(r:RandomAccessFile,pe:PeReport360,d:PeDirectory360,out:MutableList<PeImportModule360>){
  val base=PeInspector360.rvaToFileOffset(pe,d.rva)?:return
  for(i in 0 until MAX_MODULES){
   val p=base+i*20L;if(!range(r,p,20))break;r.seek(p)
   val oft=u32(r);u32(r);u32(r);val nameRva=u32(r);val ft=u32(r)
   if(oft==0L&&nameRva==0L&&ft==0L)break
   val dll=cstrRva(r,pe,nameRva)?:continue
   val thunk=if(oft!=0L)oft else ft
   val funcs=mutableListOf<PeImportFunction360>();val is64=pe.optionalMagic==0x20b;val step=if(is64)8 else 4
   for(n in 0 until MAX_FUNCS){
    val tp=PeInspector360.rvaToFileOffset(pe,thunk+n*step.toLong())?:break
    if(!range(r,tp,step.toLong()))break;r.seek(tp);val v=if(is64)u64(r) else u32(r);if(v==0L)break
    val ordinalFlag=if(is64)Long.MIN_VALUE else 0x80000000L
    if((v and ordinalFlag)!=0L) funcs+=PeImportFunction360(dll,null,v and 0xffff,thunk+n*step)
    else {
     val np=PeInspector360.rvaToFileOffset(pe,v)?:break
     if(!range(r,np,3L))break;r.seek(np);u16(r);val name=cstr(r)
     funcs+=PeImportFunction360(dll,name,null,thunk+n*step)
    }
   }
   out+=PeImportModule360(dll,funcs)
  }
 }

 private fun parseExports(r:RandomAccessFile,pe:PeReport360,d:PeDirectory360,out:MutableList<PeExport360>){
  val p=PeInspector360.rvaToFileOffset(pe,d.rva)?:return;if(!range(r,p,40L))return;r.seek(p)
  r.skipBytes(16);val ordinalBase=u32(r);val functionCount=u32(r).coerceAtMost(MAX_FUNCS.toLong()).toInt();val nameCount=u32(r).coerceAtMost(MAX_FUNCS.toLong()).toInt()
  val functionsRva=u32(r);val namesRva=u32(r);val ordinalsRva=u32(r)
  val names=mutableMapOf<Int,String>()
  for(i in 0 until nameCount){
   val no=PeInspector360.rvaToFileOffset(pe,namesRva+i*4L)?:break;val oo=PeInspector360.rvaToFileOffset(pe,ordinalsRva+i*2L)?:break
   if(!range(r,no,4L)||!range(r,oo,2L))break;r.seek(no);val nr=u32(r);r.seek(oo);val idx=u16(r);val name=cstrRva(r,pe,nr);if(name!=null)names[idx]=name
  }
  for(i in 0 until functionCount){
   val fo=PeInspector360.rvaToFileOffset(pe,functionsRva+i*4L)?:break;if(!range(r,fo,4L))break;r.seek(fo);val frva=u32(r);if(frva==0L)continue
   val forward=if(frva>=d.rva&&frva<d.rva+d.size)cstrRva(r,pe,frva) else null
   out+=PeExport360(names[i],ordinalBase+i,frva,forward)
  }
 }

 private fun cstrRva(r:RandomAccessFile,pe:PeReport360,rva:Long)=PeInspector360.rvaToFileOffset(pe,rva)?.let{p->if(range(r,p,1L)){r.seek(p);cstr(r)}else null}
 private fun cstr(r:RandomAccessFile):String{val b=ArrayList<Byte>();repeat(MAX_STRING){if(r.filePointer>=r.length())return@repeat;val x=r.read();if(x<=0)return b.toByteArray().toString(Charsets.US_ASCII);b+=x.toByte()};return b.toByteArray().toString(Charsets.US_ASCII)}
 private fun range(r:RandomAccessFile,p:Long,n:Long)=p>=0&&n>=0&&p<=r.length()&&n<=r.length()-p
 private fun u16(r:RandomAccessFile):Int{val a=r.readUnsignedByte();val b=r.readUnsignedByte();return a or(b shl 8)}
 private fun u32(r:RandomAccessFile):Long{var v=0L;for(i in 0..3)v=v or(r.readUnsignedByte().toLong() shl(8*i));return v}
 private fun u64(r:RandomAccessFile):Long{var v=0L;for(i in 0..7)v=v or(r.readUnsignedByte().toLong() shl(8*i));return v}
}
