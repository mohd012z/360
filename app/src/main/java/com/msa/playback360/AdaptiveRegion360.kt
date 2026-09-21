package com.msa.playback360

import kotlin.math.abs

data class AdaptiveRegion360(
 val offset:Long,
 val size:Long,
 val label:String,
 val windowCount:Int,
 val entropy:Double,
 val zeroRatio:Double,
 val printableRatio:Double,
 val transitionScore:Int
)

object AdaptiveRegion360 {
 fun merge(report:ExStructureReport360):List<AdaptiveRegion360>{
  if(report.regions.isEmpty())return emptyList()
  val out=mutableListOf<AdaptiveRegion360>()
  var group=mutableListOf(report.regions.first())
  fun flush(){
   if(group.isEmpty())return
   val size=group.sumOf{it.size.toLong()}
   fun weighted(f:(ExRegion360)->Double)=if(size==0L)0.0 else group.sumOf{f(it)*it.size}/size
   val labels=group.groupingBy{it.label}.eachCount()
   val label=labels.maxByOrNull{it.value}?.key?:"MIXED_BINARY"
   val first=group.first()
   val last=group.last()
   val transition=((abs(first.entropy-last.entropy)*12.0)+
    abs(first.printableRatio-last.printableRatio)*40.0+
    abs(first.zeroRatio-last.zeroRatio)*40.0).toInt().coerceIn(0,100)
   out+=AdaptiveRegion360(first.offset,size,label,group.size,weighted{it.entropy},weighted{it.zeroRatio},weighted{it.printableRatio},transition)
   group=mutableListOf()
  }
  report.regions.drop(1).forEach{next->
   val prev=group.last()
   val compatible=prev.label==next.label ||
    (prev.label!="HEADER"&&next.label!="HEADER"&&
     abs(prev.entropy-next.entropy)<=0.45&&
     abs(prev.printableRatio-next.printableRatio)<=0.18&&
     abs(prev.zeroRatio-next.zeroRatio)<=0.18)
   val contiguous=prev.offset+prev.size==next.offset
   val groupBytes=group.sumOf{it.size}
   if(compatible&&contiguous&&groupBytes<16384)group+=next else{flush();group+=next}
  }
  flush()
  return out
 }
 fun render(rows:List<AdaptiveRegion360>):String=buildString{
  appendLine("ADAPTIVE STRUCTURAL REGIONS")
  appendLine("Adjacent statistical windows are merged by label/entropy/text/zero similarity; these remain inferred regions, not compiler sections.")
  rows.take(256).forEach{
   appendLine("0x"+it.offset.toString(16).uppercase()+"..0x"+(it.offset+it.size).toString(16).uppercase()+
    " "+it.label+" windows="+it.windowCount+" H="+"%.3f".format(it.entropy)+
    " text="+"%.1f%%".format(it.printableRatio*100)+" transition="+it.transitionScore)
  }
 }
}
