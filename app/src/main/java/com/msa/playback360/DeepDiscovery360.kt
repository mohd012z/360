package com.msa.playback360

enum class DiscoveryLayer { STATIC, RUNTIME, CACHE, LOG, ROUTE, NETWORK, RESOURCE, INFERRED }

data class DiscoveryNode360(
 val id:String,
 val kind:String,
 val name:String,
 val source:String,
 val location:String,
 val layer:DiscoveryLayer,
 val hidden:Boolean=false,
 val evidence:String,
 val parentId:String?=null,
 val metadata:Map<String,String> = emptyMap()
)

data class DiscoveryEdge360(
 val from:String,
 val to:String,
 val relation:String,
 val evidence:String
)

data class DiscoveryMap360(
 val nodes:List<DiscoveryNode360>,
 val edges:List<DiscoveryEdge360>
){
 fun byKind(kind:String)=nodes.filter{it.kind.equals(kind,true)}
 fun hidden()=nodes.filter{it.hidden}
 fun layers()=nodes.groupBy{it.layer}
 fun related(id:String):List<DiscoveryNode360>{
  val ids=edges.filter{it.from==id}.map{it.to}+edges.filter{it.to==id}.map{it.from}
  return nodes.filter{it.id in ids}
 }
}

/**
 * Evidence-first map for authorized targets.
 * "Hidden" means not obvious from the normal UI, not bypassed or concealed.
 */
object DeepDiscovery360 {
 fun classifyName(path:String):String=when{
  path.contains("cache",true)->"CACHE"
  path.contains("log",true)->"LOG"
  path.contains("route",true)->"ROUTE"
  path.contains("server",true)||path.contains("host",true)->"SERVER"
  path.contains("url",true)||path.contains("link",true)->"LINK"
  else->"ITEM"
 }

 fun safeHiddenReason(node:DiscoveryNode360):String=when(node.layer){
  DiscoveryLayer.CACHE->"App-owned cache artifact"
  DiscoveryLayer.LOG->"App-owned diagnostic/log artifact"
  DiscoveryLayer.RUNTIME->"Observed only during authorized runtime instrumentation"
  DiscoveryLayer.INFERRED->"Inferred from references; not directly observed"
  else->"Not directly exposed by the normal UI"
 }
}
