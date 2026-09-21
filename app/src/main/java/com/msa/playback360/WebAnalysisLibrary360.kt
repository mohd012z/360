package com.msa.playback360

enum class WebAnalysisTool360 {
 SEMGREP, BIN, GREP, STRINGS, PROCMON_IMPORT, WIRESHARK_IMPORT, PHISHING, MALWARE
}

enum class FindingSeverity360 { INFO, LOW, MEDIUM, HIGH }

data class WebAnalysisFinding360(
 val tool:WebAnalysisTool360,
 val category:String,
 val value:String,
 val location:String,
 val evidence:String,
 val severity:FindingSeverity360=FindingSeverity360.INFO
)

object WebAnalysisLibrary360 {
 val commands=mapOf(
  "/semgrep" to WebAnalysisTool360.SEMGREP,
  "/bin" to WebAnalysisTool360.BIN,
  "/grep" to WebAnalysisTool360.GREP,
  "/string" to WebAnalysisTool360.STRINGS,
  "/strings" to WebAnalysisTool360.STRINGS,
  "/procmon" to WebAnalysisTool360.PROCMON_IMPORT,
  "/wireshark" to WebAnalysisTool360.WIRESHARK_IMPORT,
  "/phishing" to WebAnalysisTool360.PHISHING,
  "/malware" to WebAnalysisTool360.MALWARE
 )

 fun policy(tool:WebAnalysisTool360)=when(tool){
  WebAnalysisTool360.SEMGREP->"Static rules over selected/authorized source and extracted workspace files."
  WebAnalysisTool360.BIN->"Read-only metadata, strings, hashes, entropy and known-format evidence."
  WebAnalysisTool360.GREP->"Search selected workspace text/code with file and line provenance."
  WebAnalysisTool360.STRINGS->"Extract printable strings with offsets; redact likely secrets in reports."
  WebAnalysisTool360.PROCMON_IMPORT->"Import and correlate user-provided Procmon logs; no hidden monitoring."
  WebAnalysisTool360.WIRESHARK_IMPORT->"Import and summarize user-provided packet captures for authorized traffic."
  WebAnalysisTool360.PHISHING->"Defensive indicators: deceptive hostnames, suspicious redirects/forms and credential-request patterns."
  WebAnalysisTool360.MALWARE->"Defensive triage: hashes, suspicious indicators, permissions, persistence/network clues; no payload execution."
 }
}
