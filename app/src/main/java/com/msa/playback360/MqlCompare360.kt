package com.msa.playback360

enum class MqlDiffState360 { SAME, ADDED, REMOVED, CHANGED, UNCERTAIN }
data class MqlDiff360(val category:String,val key:String,val state:MqlDiffState360,val left:String?,val right:String?)
data class MqlCompareReport360(val left:String,val right:String,val differences:List<MqlDiff360>)

object MqlCompare360 {
 fun compare(a:MqlBinaryReport360,b:MqlBinaryReport360):MqlCompareReport360 {
  val out=mutableListOf<MqlDiff360>()
  compareSet("EVENT", values(a,MqlObjectKind.MQL_EVENT),values(b,MqlObjectKind.MQL_EVENT),out)
  compareSet("TRADE_API",values(a,MqlObjectKind.TRADING_API),values(b,MqlObjectKind.TRADING_API),out)
  compareSet("INDICATOR",values(a,MqlObjectKind.INDICATOR),values(b,MqlObjectKind.INDICATOR),out)
  compareSet("DLL",values(a,MqlObjectKind.DLL),values(b,MqlObjectKind.DLL),out)
  compareSet("URL",values(a,MqlObjectKind.URL),values(b,MqlObjectKind.URL),out)
  compareSet("SYMBOL",symbols(a),symbols(b),out)
  out += MqlDiff360("BINARY","SHA-256",if(a.sha256==b.sha256)MqlDiffState360.SAME else MqlDiffState360.CHANGED,a.sha256,b.sha256)
  out += MqlDiff360("BINARY","SIZE",if(a.size==b.size)MqlDiffState360.SAME else MqlDiffState360.CHANGED,a.size.toString(),b.size.toString())
  return MqlCompareReport360(a.fileName,b.fileName,out)
 }

 fun render(r:MqlCompareReport360):String=buildString {
  append("MQL360 COMPARE\n").append(r.left).append(" <-> ").append(r.right).append("\n\n")
  r.differences.filter{it.state!=MqlDiffState360.SAME}.forEach{
   append(it.state).append(" [").append(it.category).append("] ").append(it.key)
   if(it.left!=null||it.right!=null)append(" : ").append(it.left?:"-").append(" -> ").append(it.right?:"-")
   append("\n")
  }
  if(r.differences.all{it.state==MqlDiffState360.SAME})append("No differences observed in indexed evidence.\n")
  append("\nEvidence comparison does not prove equivalence of unavailable original source/control flow.")
 }

 private fun values(r:MqlBinaryReport360,k:MqlObjectKind)=r.evidence.filter{it.kind==k}.map{it.value.trim()}.filter{it.isNotEmpty()}.toSet()
 private fun symbols(r:MqlBinaryReport360)=r.evidence.flatMap{it.details["symbols"]?.split(",").orEmpty()}.map{it.trim()}.filter{it.isNotEmpty()}.toSet()
 private fun compareSet(cat:String,a:Set<String>,b:Set<String>,o:MutableList<MqlDiff360>){
  (a union b).sorted().forEach{k->o+=when{ k in a&&k in b->MqlDiff360(cat,k,MqlDiffState360.SAME,k,k);k in a->MqlDiff360(cat,k,MqlDiffState360.REMOVED,k,null);else->MqlDiff360(cat,k,MqlDiffState360.ADDED,null,k)}}
 }
}
