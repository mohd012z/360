package com.msa.playback360

enum class SecurityEvidenceKind360 {
 TARGET, URL, DNS, ROUTE, SCRIPT, API, MEDIA, PROCESS, FILE, NETWORK,
 PHISHING, MALWARE, CONFIG, CERTIFICATE, RUNTIME
}

data class SecurityEvidenceNode360(
 val id:String,
 val kind:SecurityEvidenceKind360,
 val title:String,
 val value:String,
 val source:String,
 val location:String,
 val evidence:String,
 val severity:FindingSeverity360=FindingSeverity360.INFO,
 val metadata:Map<String,String> = emptyMap()
)

data class SecurityEvidenceEdge360(
 val from:String,
 val to:String,
 val relation:String,
 val evidence:String
)

data class Security360Report(
 val nodes:List<SecurityEvidenceNode360>,
 val edges:List<SecurityEvidenceEdge360>
){
 fun related(id:String):List<SecurityEvidenceNode360>{
  val ids=edges.filter{it.from==id}.map{it.to}+edges.filter{it.to==id}.map{it.from}
  return nodes.filter{it.id in ids}
 }
 fun findings()=nodes.filter{it.severity!=FindingSeverity360.INFO}
 fun byKind(kind:SecurityEvidenceKind360)=nodes.filter{it.kind==kind}
}

object Security360 {
 val commands=listOf(
  "/security360","/evidence","/evidencemap","/securitymap",
  "/networkevidence","/processevidence","/fileevidence","/finding"
 )

 fun fromWeb(url:String):Security360Report{
  val web=WebConnection360.fromInput(url)?:return Security360Report(emptyList(),emptyList())
  val nodes=mutableListOf<SecurityEvidenceNode360>()
  val edges=mutableListOf<SecurityEvidenceEdge360>()
  val targetId="target:"+web.host
  val urlId="url:"+CodeCrypto360.fingerprint(web.url.toByteArray()).take(16)
  nodes+=SecurityEvidenceNode360(targetId,SecurityEvidenceKind360.TARGET,web.host,web.host,"URL_INPUT",web.host,"Selected web target")
  nodes+=SecurityEvidenceNode360(urlId,SecurityEvidenceKind360.URL,"URL",web.url,"URL_INPUT",web.url,web.evidence)
  edges+=SecurityEvidenceEdge360(targetId,urlId,"HAS_URL","User-supplied target")
  ConnectionEvidence360.resolve(web.host)?.let{dns->
   dns.addresses.forEachIndexed{i,ip->
    val id="dns:$i:$ip"
    nodes+=SecurityEvidenceNode360(id,SecurityEvidenceKind360.DNS,"DNS address",ip,dns.source,web.host,"Ordinary resolver evidence")
    edges+=SecurityEvidenceEdge360(urlId,id,"RESOLVES_TO","System DNS")
   }
  }
  DefensiveWebTriage360.phishing(web.url).forEachIndexed{i,f->
   val id="phish:$i"
   nodes+=SecurityEvidenceNode360(id,SecurityEvidenceKind360.PHISHING,f.type,f.value,"DEFENSIVE_TRIAGE",web.url,f.reason,f.severity)
   edges+=SecurityEvidenceEdge360(urlId,id,"HAS_INDICATOR",f.reason)
  }
  return Security360Report(nodes,edges)
 }
}
