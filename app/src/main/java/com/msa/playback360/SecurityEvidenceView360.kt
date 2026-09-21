package com.msa.playback360

data class EvidenceTimelineItem360(
 val order:Int,
 val kind:SecurityEvidenceKind360,
 val title:String,
 val location:String,
 val severity:FindingSeverity360,
 val evidence:String
)

object SecurityEvidenceView360 {
 fun timeline(report:Security360Report)=report.nodes.mapIndexed{i,n->
  EvidenceTimelineItem360(i,n.kind,n.title,n.location,n.severity,n.evidence)
 }

 fun summary(report:Security360Report)=buildString{
  appendLine("[SECURITY360]")
  appendLine("Evidence nodes: "+report.nodes.size)
  appendLine("Relationships: "+report.edges.size)
  appendLine("Findings: "+report.findings().size)
  SecurityEvidenceKind360.entries.forEach{k->
   val n=report.byKind(k).size
   if(n>0)appendLine(k.name+": "+n)
  }
 }

 fun detail(report:Security360Report,id:String):String{
  val n=report.nodes.firstOrNull{it.id==id}?:return "Evidence not found"
  return buildString{
   appendLine(n.kind.name+" • "+n.title)
   appendLine("Value: "+n.value)
   appendLine("Source: "+n.source)
   appendLine("Location: "+n.location)
   appendLine("Evidence: "+n.evidence)
   appendLine("Severity: "+n.severity)
   val related=report.related(id)
   if(related.isNotEmpty())appendLine("Related: "+related.joinToString{it.title})
  }
 }
}
