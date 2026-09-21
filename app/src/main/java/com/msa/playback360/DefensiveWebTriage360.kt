package com.msa.playback360

data class DefensiveIndicator360(
 val type:String,
 val value:String,
 val reason:String,
 val severity:FindingSeverity360
)

object DefensiveWebTriage360 {
 private val suspiciousWords=Regex("""(?i)\b(?:verify|urgent|suspended|unlock|password|credential|wallet|seed phrase|gift card)\b""")
 private val executableExt=Regex("""(?i)\.(?:exe|dll|scr|bat|cmd|ps1|js|vbs|apk|jar)$""")

 fun phishing(url:String,html:String=""):List<DefensiveIndicator360>{
  val out=mutableListOf<DefensiveIndicator360>()
  val target=WebConnection360.fromInput(url)
  if(target==null)out+=DefensiveIndicator360("URL",url,"Malformed or unsupported URL",FindingSeverity360.MEDIUM)
  if(suspiciousWords.containsMatchIn(url+" "+html))
   out+=DefensiveIndicator360("SOCIAL_ENGINEERING","keyword-pattern","Credential/urgency language detected; review context manually.",FindingSeverity360.MEDIUM)
  if(Regex("""(?i)<form[^>]+(?:password|login|signin)""").containsMatchIn(html))
   out+=DefensiveIndicator360("FORM","credential-form","Credential-related form markup detected.",FindingSeverity360.MEDIUM)
  return out
 }

 fun malwareHints(strings:List<String>):List<DefensiveIndicator360>{
  val out=mutableListOf<DefensiveIndicator360>()
  strings.forEach{s->
   if(executableExt.containsMatchIn(s))out+=DefensiveIndicator360("EXECUTABLE_REFERENCE",s,"Executable/script reference found.",FindingSeverity360.LOW)
   if(Regex("""(?i)\b(?:powershell|cmd\.exe|rundll32|regsvr32)\b""").containsMatchIn(s))
    out+=DefensiveIndicator360("PROCESS_REFERENCE",s,"Process/tool reference found; requires context.",FindingSeverity360.MEDIUM)
  }
  return out.distinctBy{it.type to it.value}.take(500)
 }
}
