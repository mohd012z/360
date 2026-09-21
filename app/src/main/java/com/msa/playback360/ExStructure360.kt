package com.msa.playback360

import java.io.File
import java.io.RandomAccessFile
import kotlin.math.ln

data class ExRegion360(val offset:Long,val size:Int,val entropy:Double,val zeroRatio:Double,val printableRatio:Double,val label:String)
data class ExStructureReport360(val fileName:String,val kind:MqlBinaryKind,val size:Long,val headerHex:String,val regions:List<ExRegion360>,val notes:List<String>)

/** Read-only EX4/EX5 structural mapper. It does not decrypt or bypass compiler protection. */
object ExStructure360 {
 private const val WINDOW=4096
 private const val MAX_REGIONS=8192
 fun inspect(file:File):ExStructureReport360{
  require(file.isFile&&file.canRead()){"Target is not readable"}
  val kind=Mql360.kindFor(file.name)
  require(kind==MqlBinaryKind.EX4||kind==MqlBinaryKind.EX5){"Target must be EX4 or EX5"}
  val regions=mutableListOf<ExRegion360>()
  val head=ByteArray(minOf(128L,file.length()).toInt())
  RandomAccessFile(file,"r").use{r->
   r.readFully(head)
   var off=0L
   val buf=ByteArray(WINDOW)
   while(off<r.length()&&regions.size<MAX_REGIONS){
    r.seek(off);val n=r.read(buf,0,minOf(WINDOW, minOf(Int.MAX_VALUE.toLong(), r.length()-off).toInt()));if(n<=0)break
    val counts=IntArray(256);var zero=0;var printable=0
    for(i in 0 until n){val v=buf[i].toInt()and 255;counts[v]++;if(v==0)zero++;if(v in 32..126)printable++}
    val h=entropy(counts,n)
    val zr=zero.toDouble()/n;val pr=printable.toDouble()/n
    val label=when{off==0L->"HEADER";h>=7.7->"HIGH_ENTROPY";zr>=0.50->"SPARSE_DATA";pr>=0.55->"TEXT_LIKE";h<4.5->"LOW_ENTROPY_DATA";else->"MIXED_BINARY"}
    regions+=ExRegion360(off,n,h,zr,pr,label);off+=n
   }
  }
  val high=regions.count{it.label=="HIGH_ENTROPY"}
  val notes=buildList{
   add("Region labels are statistical evidence, not recovered source-code boundaries.")
   if(high>regions.size/2)add("Most sampled regions have high entropy; plain string/API recovery may be limited.")
   add("No decryption, protection bypass, or execution is performed.")
  }
  return ExStructureReport360(file.name,kind,file.length(),head.joinToString(""){"%02X".format(it)},regions,notes)
 }
 fun summary(x:ExStructureReport360):String=buildString{
  append(x.kind).append(" STRUCTURE • ").append(x.fileName).append("\nSize: ").append(x.size).append(" bytes\n")
  x.regions.groupingBy{it.label}.eachCount().toList().sortedByDescending{it.second}.forEach{append(it.first).append(": ").append(it.second).append("\n")}
  append("\nRegions:\n")
  x.regions.take(256).forEach{append("0x").append(it.offset.toString(16).uppercase()).append(" +").append(it.size).append("  ").append(it.label).append("  H=").append("%.3f".format(it.entropy)).append("  text=").append("%.1f%%".format(it.printableRatio*100)).append("\n")}
  append("\n").append(x.notes.joinToString("\n"))
 }
 private fun entropy(c:IntArray,n:Int):Double{var h=0.0;for(x in c)if(x>0){val p=x.toDouble()/n;h-=p*(ln(p)/ln(2.0))};return h}
}
