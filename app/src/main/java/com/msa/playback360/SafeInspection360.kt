package com.msa.playback360

/**
 * Safe inspection profile. "Safe" means low-impact, transparent and
 * permission-respecting. It is NOT anti-detection or stealth behavior.
 */
data class SafeInspectionProfile(
 val readOnly:Boolean=true,
 val rateLimitPerSecond:Int=20,
 val maxPreviewBytes:Int=256*1024,
 val redactSecrets:Boolean=true,
 val preserveOriginal:Boolean=true,
 val visibleAuditLog:Boolean=true,
 val requireAuthorization:Boolean=true
)

object SafeInspection360 {
 private val secretKeys=Regex(
  "(authorization|cookie|set-cookie|password|passwd|token|secret|private[_-]?key|session)",
  RegexOption.IGNORE_CASE
 )
 fun redact(key:String,value:String,enabled:Boolean=true):String =
  if(enabled && secretKeys.containsMatchIn(key)) "<redacted>" else value

 fun canInspect(auth:TargetAuthorization, profile:SafeInspectionProfile):Boolean =
  (!profile.requireAuthorization || auth.decision()!=AccessDecision.BLOCK) && profile.readOnly

 fun explain(profile:SafeInspectionProfile)=listOf(
  "Read-only: "+profile.readOnly,
  "Preserve original: "+profile.preserveOriginal,
  "Visible audit log: "+profile.visibleAuditLog,
  "Redact secrets: "+profile.redactSecrets,
  "Rate limit: "+profile.rateLimitPerSecond+"/s",
  "Preview limit: "+profile.maxPreviewBytes+" bytes"
 )
}
