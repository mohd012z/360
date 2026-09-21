package com.msa.playback360
import java.io.File
import kotlin.math.abs

data class CodeRelation360(val left:String,val right:String,val leftOffset:Long,val rightOffset:Long,val distance:Long,val score:Int,val reason:String)
object CodeRelationGraph360 {
 fun build(file:File,r:MqlBinaryReport360,radius:Long=4096):List<CodeRelation360>{
  val semantic=r.evidence.filter{it.offset!=null && it.kind!=MqlObjectKind.STRING}.take(4000)
  val numeric=NumericEvidenceScanner360.scan(file).take(4000)
  val out=mutableListOf<CodeRelation360>()
  for(e in semantic){
   val eo=e.offset?:continue
   numeric.asSequence().map{n->n to abs(n.offset-eo)}.filter{it.second<=radius}.sortedBy{it.second}.take(8).forEach{(n,d)->
    val proximity=(100-(d*70/radius)).toInt().coerceIn(30,100)
    val score=((proximity+n.confidence)/2).coerceAtMost(90)
    out+=CodeRelation360(e.kind.name+":"+e.value,"NUM:"+n.value,eo,n.offset,d,score,"offset proximity; relationship not proven")
   }
  }
  val ev=semantic.sortedBy{it.offset}
  for(i in ev.indices)for(j in i+1 until minOf(ev.size,i+16)){
   val a=ev[i];val b=ev[j];val ao=a.offset?:continue;val bo=b.offset?:continue;val d=abs(bo-ao);if(d>radius)break
   val shared=a.details["domains"]?.split(",")?.intersect(b.details["domains"]?.split(",")?.toSet().orEmpty()).orEmpty()
   val score=(45+(radius-d)*35/radius+(if(shared.isNotEmpty())10 else 0)).toInt().coerceAtMost(90)
   out+=CodeRelation360(a.kind.name+":"+a.value,b.kind.name+":"+b.value,ao,bo,d,score,if(shared.isNotEmpty())"proximity + shared semantic domain" else "offset proximity")
  }
  return out.distinctBy{listOf(it.left,it.right,it.leftOffset.toString(),it.rightOffset.toString())}.sortedByDescending{it.score}.take(5000)
 }
 fun render(rows:List<CodeRelation360>)=buildString{
  append("RELATIONSHIP GRAPH • ").append(rows.size).append(" edges\n")
  rows.take(500).forEach{append(it.score).append("%  0x").append(it.leftOffset.toString(16).uppercase()).append(" ").append(it.left).append("  <->  0x").append(it.rightOffset.toString(16).uppercase()).append(" ").append(it.right).append("  d=").append(it.distance).append("  ").append(it.reason).append("\n")}
  append("\nEdges are reconstruction candidates. Proximity does not prove original control flow or variable ownership.")
 }
}
