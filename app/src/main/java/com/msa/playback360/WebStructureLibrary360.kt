package com.msa.playback360

enum class WebStructureKind360 {
 CONNECTION, DNS, VPN_CONTEXT, MIRROR, CLONE, LAYER, FOLDER, INDEX,
 CHROME_HINT, EXTENSION_HINT, PLUGIN_HINT, ADS_HINT, DOCUMENT, SCRIPT,
 STYLE, API, MEDIA, ASSET, UNKNOWN
}

data class WebStructureNode360(
 val id:String,
 val kind:WebStructureKind360,
 val name:String,
 val location:String,
 val evidence:String,
 val parentId:String?=null,
 val metadata:Map<String,String> = emptyMap()
)

data class WebStructure360(
 val nodes:List<WebStructureNode360>,
 val edges:List<WebEdge360>
){
 fun children(id:String)=nodes.filter{it.parentId==id}
 fun byKind(kind:WebStructureKind360)=nodes.filter{it.kind==kind}
}

object WebStructureLibrary360 {
 val commands=listOf(
  "/connection","/vpn","/dns","/mirror","/clone","/layerstack",
  "/folderstack","/indexof","/chrome","/ext","/plugin","/ads","/structure"
 )

 fun folderStack(path:String):List<String>{
  val parts=path.trim('/').split('/').filter{it.isNotBlank()}
  val out=mutableListOf("/")
  var current=""
  parts.forEach{p->current+="/"+p;out+=current}
  return out
 }

 fun classifyAsset(name:String):WebStructureKind360=when(name.substringAfterLast('.', "").lowercase()){
  "html","htm"->WebStructureKind360.DOCUMENT
  "js","mjs","cjs"->WebStructureKind360.SCRIPT
  "css","scss"->WebStructureKind360.STYLE
  "json","xml"->WebStructureKind360.API
  "m3u","m3u8","mpd","mp4","webm","ts","m4s"->WebStructureKind360.MEDIA
  else->WebStructureKind360.ASSET
 }

 fun indexOf(text:String,needle:String):List<Int>{
  if(needle.isEmpty())return emptyList()
  val out=mutableListOf<Int>();var p=text.indexOf(needle)
  while(p>=0){out+=p;p=text.indexOf(needle,p+needle.length)}
  return out
 }

 fun vpnPolicy()="Show only device/app VPN context exposed through normal Android network APIs; no VPN bypass."
 fun dnsPolicy()="Resolve the selected host using ordinary DNS evidence; no origin discovery behind CDN/privacy controls."
 fun clonePolicy()="Clone means an isolated workspace copy of authorized content, preserving provenance."
 fun chromePolicy()="Inspect exported/selected web artifacts and normal browser metadata only; do not access private Chrome profiles, cookies or credentials."
 fun extensionPolicy()="Inventory extension/plugin references present in selected content; do not install, inject or bypass browser controls."
 fun adsPolicy()="Identify ad-related scripts, hosts, tags and requests as evidence; do not bypass access controls or covertly manipulate third-party services."
}
