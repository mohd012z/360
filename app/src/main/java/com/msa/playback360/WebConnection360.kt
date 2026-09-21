package com.msa.playback360

import java.net.URI

enum class WebConnectionKind360 { HTTP, HTTPS, WS, WSS, MEDIA, CALLBACK, UNKNOWN }

data class WebConnectionEvidence360(
 val url:String,
 val host:String,
 val port:Int?,
 val kind:WebConnectionKind360,
 val source:String,
 val callback:Boolean=false,
 val config:Boolean=false,
 val evidence:String
)

data class WebSecurityView360(
 val https:Boolean,
 val scheme:String,
 val explicitPort:Int?,
 val credentialsInUrl:Boolean,
 val redactedUrl:String,
 val notes:List<String>
)

object WebConnection360 {
 private val urlRegex=Regex("""(?:https?|wss?)://[^\s"'<>]+""",RegexOption.IGNORE_CASE)

 fun fromInput(raw:String):WebConnectionEvidence360?{
  val safe=ServerRouteLibrary360.sanitizeUrl(raw.trim())
  val u=runCatching{URI(safe)}.getOrNull()?:return null
  if(u.scheme !in listOf("http","https","ws","wss"))return null
  val kind=when(u.scheme.lowercase()){
   "http"->WebConnectionKind360.HTTP;"https"->WebConnectionKind360.HTTPS
   "ws"->WebConnectionKind360.WS;"wss"->WebConnectionKind360.WSS
   else->WebConnectionKind360.UNKNOWN
  }
  return WebConnectionEvidence360(safe,u.host.orEmpty(),u.port.takeIf{it>=0},kind,"URL_INPUT",evidence="User-supplied URL")
 }

 fun security(raw:String):WebSecurityView360?{
  val safe=ServerRouteLibrary360.sanitizeUrl(raw.trim())
  val u=runCatching{URI(safe)}.getOrNull()?:return null
  val notes=mutableListOf<String>()
  if(u.scheme=="http"||u.scheme=="ws")notes+="Transport is not TLS-protected."
  if(u.userInfo!=null)notes+="URL contains user-info; avoid exposing credentials."
  return WebSecurityView360(u.scheme=="https"||u.scheme=="wss",u.scheme.orEmpty(),u.port.takeIf{it>=0},u.userInfo!=null,safe,notes)
 }

 fun extractUrls(text:String)=urlRegex.findAll(text).map{ServerRouteLibrary360.sanitizeUrl(it.value)}.distinct().toList()

 fun classifyConfig(name:String)=when(name.substringAfterLast('.', "").lowercase()){
  "json"->"JSON";"ini"->"INI";"html","htm"->"HTML";"conf","config"->"CONFIG";else->null
 }
}
