package com.msa.playback360

enum class EvidenceSource { DEX, MANIFEST, RESOURCE, XML, ASSET, NATIVE, RUNTIME, INFERRED }

data class InspectObject(
 val type:String, val name:String, val realName:String?=null, val sourceFile:String,
 val location:String, val offset:Long?=null, val size:Long?=null,
 val source:EvidenceSource, val details:Map<String,String> = emptyMap()
){
 fun copyValue()=realName?:name
 fun copyDetails():String=buildString{
  appendLine("[360]"); appendLine("Type: $type"); appendLine("Name: $name")
  realName?.let { n -> appendLine("Resolved name: $n") }
  appendLine("Source file: $sourceFile"); appendLine("Location: $location")
  offset?.let { o -> appendLine("Offset: 0x"+o.toString(16).uppercase()) }
  size?.let { s -> appendLine("Size: $s bytes") }
  appendLine("Evidence: $source")
  details.forEach { e -> appendLine(e.key+": "+e.value) }
 }
}
object FileSourceGuide {
 val entries=listOf(
  "Class / method / field" to "classes*.dex",
  "Activity / Service / Provider" to "AndroidManifest.xml + DEX",
  "Resource name / ID" to "resources.arsc",
  "Layout / UI" to "res/*.xml + resources.arsc",
  "JavaScript" to "assets/*.js / WebView assets",
  "Flutter" to "flutter_assets / native metadata",
  "Native / JNI" to "lib/<ABI>/*.so + DEX",
  "Signing identity" to "APK signing metadata",
  "Runtime API / URL" to "instrumented runtime observation"
 )
}