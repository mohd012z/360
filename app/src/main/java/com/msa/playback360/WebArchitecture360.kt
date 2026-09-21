package com.msa.playback360

data class WebMethod360(
 val name:String,
 val file:String,
 val route:String?,
 val layer:WebLayer360,
 val callers:List<String> = emptyList(),
 val callees:List<String> = emptyList(),
 val evidence:String
)

data class WebStackFrame360(
 val depth:Int,
 val label:String,
 val url:String?,
 val method:String?,
 val layer:WebLayer360,
 val evidence:String
)

object WebArchitecture360 {
 fun methodView(m:WebMethod360)=buildString{
  appendLine("[360 WEB METHOD]")
  appendLine("Name: "+m.name);appendLine("File: "+m.file)
  appendLine("Layer: "+m.layer);m.route?.let{appendLine("Route: "+it)}
  appendLine("Callers: "+m.callers.joinToString());appendLine("Callees: "+m.callees.joinToString())
  appendLine("Evidence: "+m.evidence)
 }

 fun cloneLabel(url:String)="workspace-web-clone:"+CodeCrypto360.fingerprint(url.toByteArray()).take(12)

 fun stack(targets:List<WebTarget360>)=targets.mapIndexed{i,t->
  WebStackFrame360(i,t.fileName?:t.host,t.url,null,t.layer,t.evidence)
 }
}
