package com.msa.playback360

data class WebRuntimeMetric360(
 val target:String,
 val phase:String,
 val elapsedMs:Long?,
 val status:String,
 val evidence:String
)

data class WebBufferView360(
 val contentLength:Long?,
 val receivedBytes:Long,
 val bufferedBytes:Long?,
 val contentType:String?,
 val statusCode:Int?,
 val redirectCount:Int=0
)

object WebRuntimeLibrary360 {
 val commands=listOf(
  "/security","/mirror","/json","/callback","/release","/buffer",
  "/connection","/realip","/ping","/config","/.ini","/.html"
 )

 fun mirrorLabel(url:String)=
  "workspace-mirror:"+CodeCrypto360.fingerprint(ServerRouteLibrary360.sanitizeUrl(url).toByteArray()).take(12)

 fun releaseView(headers:Map<String,String>)=mapOf(
  "server" to (headers["server"]?:""),
  "etag" to (headers["etag"]?:""),
  "last-modified" to (headers["last-modified"]?:""),
  "content-type" to (headers["content-type"]?:""),
  "content-length" to (headers["content-length"]?:"")
 ).filterValues{it.isNotBlank()}

 fun realIpPolicy()="Display only IP addresses returned by normal DNS/connection evidence for the authorized target; do not attempt origin-IP discovery behind privacy/CDN protections."

 fun pingPolicy()="Use ordinary reachability/latency checks only; no flooding, scanning or bypass."
}
