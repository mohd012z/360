package com.msa.playback360

data class ServerLink360(
 val name:String,
 val scheme:String,
 val host:String,
 val port:Int?,
 val path:String,
 val source:String,
 val route:String?=null,
 val runtimeObserved:Boolean=false,
 val status:String?=null
)

object ServerRouteLibrary360 {
 val commands=listOf(
  "/server","/link","/routes","/map","/viewer","/name",
  "/hidden","/hiddenlayer","/hiddenstack","/hiddenlog","/hiddencache"
 )

 fun sanitizeUrl(value:String):String{
  val sensitive=Regex("""(?i)(token|key|secret|password|auth|signature|sig|session)=([^&]+)""")
  return sensitive.replace(value){"${it.groupValues[1]}=<redacted>"}
 }

 fun viewerSections()=listOf(
  "SERVER" to "Hosts, schemes, ports and evidence source",
  "LINK" to "Literal, constructed and runtime-observed links",
  "ROUTES" to "Screen/navigation/API relationships",
  "MAP" to "Target dependency graph",
  "NAME" to "Stored, mapped and inferred names with confidence",
  "HIDDEN" to "Non-obvious authorized-target artifacts with evidence",
  "HIDDEN LAYER" to "Static/runtime/cache/log/resource layers",
  "HIDDEN STACK" to "Observed callback/call-stack relationships",
  "HIDDEN LOG" to "App-owned logs found in the selected target/workspace",
  "HIDDEN CACHE" to "App-owned cache metadata and files"
 )
}
