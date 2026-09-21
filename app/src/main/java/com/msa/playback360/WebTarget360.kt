package com.msa.playback360

import java.net.URI

enum class WebLayer360 { DOCUMENT, SCRIPT, STYLE, MEDIA, ROUTE, API, ASSET, STORAGE, PLAYER, INFERRED }

data class WebTarget360(
 val url:String,
 val scheme:String,
 val host:String,
 val port:Int?,
 val path:String,
 val fileName:String?,
 val extension:String?,
 val layer:WebLayer360,
 val source:String,
 val evidence:String,
 val runtimeObserved:Boolean=false
)

data class WebEdge360(val from:String,val to:String,val relation:String,val evidence:String)

data class WebMap360(val targets:List<WebTarget360>,val edges:List<WebEdge360>){
 fun hosts()=targets.map{it.host}.filter{it.isNotBlank()}.distinct()
 fun routes()=targets.filter{it.layer==WebLayer360.ROUTE}
 fun players()=targets.filter{it.layer==WebLayer360.PLAYER||it.layer==WebLayer360.MEDIA}
}

object WebTargetLibrary360 {
 val commands=listOf(
  "/web","/webtarget","/deep-dive","/method","/methode","/weblayer",
  "/webplayer","/webstack","/webclone","/webencrypt","/webdecrypt",
  "/webroutes","/realurls","/map","/servername","/webhost","/webdir",
  "/webindex","/index"
 )

 fun classify(raw:String,source:String="selected target"):WebTarget360?{
  val clean=ServerRouteLibrary360.sanitizeUrl(raw.trim())
  val uri=runCatching{URI(clean)}.getOrNull()?:return null
  val path=uri.path.orEmpty()
  val ext=path.substringAfterLast('.', "").lowercase().ifBlank{null}
  val layer=when(ext){
   "js","mjs","cjs"->WebLayer360.SCRIPT
   "css","scss"->WebLayer360.STYLE
   "m3u","m3u8","mpd","mp4","webm","ts","m4s"->WebLayer360.MEDIA
   "html","htm"->WebLayer360.DOCUMENT
   "json","xml"->WebLayer360.API
   else->if(path.contains("/api/",true))WebLayer360.API else WebLayer360.ROUTE
  }
  return WebTarget360(clean,uri.scheme.orEmpty(),uri.host.orEmpty(),uri.port.takeIf{it>=0},path,
   path.substringAfterLast('/').ifBlank{null},ext,layer,source,"URL/route evidence")
 }

 fun directory(path:String):String=path.substringBeforeLast('/',"/").ifBlank{"/"}

 fun indexName(path:String):String?{
  val n=path.substringAfterLast('/')
  return n.takeIf{it.equals("index.html",true)||it.equals("index.htm",true)||it.equals("index.js",true)}
 }
}
