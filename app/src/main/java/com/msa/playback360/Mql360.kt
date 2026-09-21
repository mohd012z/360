package com.msa.playback360

enum class MqlBinaryKind { EX4, EX5, MQ4, MQ5, UNKNOWN }
enum class MqlEvidenceStatus { OBSERVED, EXTRACTED, RECONSTRUCTED, INFERRED, USER_LABEL }
enum class MqlObjectKind { METADATA, BINARY_REGION, STRING, MQL_EVENT, TRADING_API, INDICATOR, DLL, URL, RESOURCE, NATIVE_REFERENCE, INFERRED_FUNCTION, UNKNOWN }

data class MqlEvidence360(
 val kind:MqlObjectKind, val value:String, val offset:Long?=null, val source:String,
 val status:MqlEvidenceStatus=MqlEvidenceStatus.OBSERVED, val confidence:Int=100,
 val details:Map<String,String> = emptyMap()
)

data class MqlCommand360(val command:String,val group:String,val purpose:String,val safeMeaning:String)

object Mql360 {
 val commands=listOf(
  MqlCommand360("/codestart","Core","Start EX4/EX5 evidence analysis","Read-only identification, hash and evidence scan."),
  MqlCommand360("/structure","Binary","Map binary regions and evidence","Offset/structure inventory; no compiler-protection bypass."),
  MqlCommand360("/codebase","Core","Open target evidence database","Normalized MQL360 objects with provenance."),
  MqlCommand360("/codelibrary","Core","Open MQL4/MQL5 semantic library","Known events, APIs, indicators and trading concepts."),
  MqlCommand360("/codeextraction","Evidence","Extract readable evidence","Separate workspace; original stays untouched."),
  MqlCommand360("/codecompress","Workspace","Compress analysis package","Compresses MQL360 output, never the original target."),
  MqlCommand360("/codeview","Viewer","Hex + strings + evidence viewer","Read-only viewer with source and offset."),
  MqlCommand360("/codehex","Viewer","Hex evidence","Shows bytes and nearby evidence without modification."),
  MqlCommand360("/codeoffset","Viewer","Jump to known offset","Only evidence-backed offsets are shown."),
  MqlCommand360("/coderealnameoffset","Evidence","Resolve semantic label at offset","Suggested names are marked inferred, never original."),
  MqlCommand360("/codedll","Evidence","DLL/library references","Inventory of observable DLL/native references."),
  MqlCommand360("/codetransparent","Evidence","Explain provenance","Source, offset, status, confidence and limitations."),
  MqlCommand360("/codehelp","Core","Explain selected finding","Uses MQL library context without inventing source."),
  MqlCommand360("/codeshell","Workspace","Controlled workspace actions","Allow-listed MQL360 workspace operations only."),
  MqlCommand360("/codestack","Map","Relationship/call evidence","Reconstructed relationships are explicitly labeled."),
  MqlCommand360("/codelayer","Map","Evidence layers","Binary -> strings/API -> behavior -> external references."),
  MqlCommand360("/codeapi","Evidence","MQL/API references","Classifies observable MQL4/MQL5 API/event references."),
  MqlCommand360("/codescreen","Viewer","MQL360 main screen","Structure, strings, APIs, DLLs, map and compare."),
  MqlCommand360("/codemirror","Workspace","Create read-only working copy","Preserves original hash and provenance."),
  MqlCommand360("/codeide","Viewer","Evidence-oriented IDE","Navigates evidence, not fake recovered source."),
  MqlCommand360("/ide","Viewer","Alias for code IDE","Evidence-oriented read-only navigation."),
  MqlCommand360("/codejava","Evidence","Java references","Only when Java evidence is present."),
  MqlCommand360("/codepython","Evidence","Python references","Only when Python evidence is present."),
  MqlCommand360("/codec++","Evidence","C/C++ native references","Classifies native/DLL evidence where observable."),
  MqlCommand360("/codedecrypt","Workspace","Decrypt owned workspace data","Supplied key/password only; no EX4/EX5 protection bypass or brute force."),
  MqlCommand360("/codeencrypt","Workspace","Encrypt evidence package","Protects exported workspace/report data."),
  MqlCommand360("/codeconverter","Export","Convert evidence formats","Binary->hex/strings and findings->JSON/report; not EX5->original MQ5."),
  MqlCommand360("/codemetadata","Binary","Binary metadata","Format, size, hash, entropy and available metadata."),
  MqlCommand360("/metadata","Binary","Alias for metadata","Read-only metadata view."),
  MqlCommand360("/code","Evidence","Code evidence view","Observed/reconstructed evidence with labels."),
  MqlCommand360("/codestring","Evidence","String evidence","Printable strings with offsets and classifications."),
  MqlCommand360("/codelabel","Evidence","Assign semantic labels","Automatic labels confidence-scored; manual labels USER_LABEL."),
  MqlCommand360("/codepush","Bridge","Push evidence to Evidence360","Normalized findings only, not executable code injection."),
  MqlCommand360("/codealert","Compare","Change alerts","New/changed/removed evidence between authorized versions."),
  MqlCommand360("/codeobjects","Map","Normalized evidence objects","Metadata, regions, strings, APIs, indicators, DLLs, URLs and resources."),
  MqlCommand360("/codevoid","Evidence","Unknown/low-evidence regions","Marks areas where semantic reconstruction is unsupported."),
  MqlCommand360("/codesummary","Report","Target summary","Evidence counts, notable findings and limitations."),
  MqlCommand360("/codesources","Evidence","Finding provenance","Shows exactly which evidence supports a conclusion."),
  MqlCommand360("/codebinary","Binary","Binary identity","Magic/signature, size, SHA-256, entropy and format evidence."),
  MqlCommand360("/codegrep","Search","Search target evidence","Strings/metadata/API/DLL/URL/labels with source and offset."),
  MqlCommand360("/codemap","Map","Build evidence relationship map","Observed edges remain distinct from inferred edges."),
  MqlCommand360("/map","Map","Alias for evidence map","Unified MQL360 evidence map.")
 )

 private val events=setOf("init","start","deinit","OnInit","OnDeinit","OnTick","OnTimer","OnTrade","OnTradeTransaction","OnBookEvent","OnChartEvent")
 private val trading=setOf("OrderSend","OrderModify","OrderClose","OrderSelect","OrdersTotal","MqlTradeRequest","MqlTradeResult","OrderSendAsync","PositionSelect","PositionGetDouble","CTrade","Buy","Sell","StopLoss","TakeProfit","TrailingStop","MagicNumber","Lots")
 private val indicators=setOf("iCustom","iMA","iRSI","iMACD","iBands","iStochastic","iIchimoku","iATR","ZigZag","MovingAverage","RSI","MACD","Bollinger","Fibonacci")

 fun kindFor(name:String):MqlBinaryKind = when(name.substringAfterLast('.', "").lowercase()){
  "ex4" -> MqlBinaryKind.EX4; "ex5" -> MqlBinaryKind.EX5; "mq4" -> MqlBinaryKind.MQ4; "mq5" -> MqlBinaryKind.MQ5; else -> MqlBinaryKind.UNKNOWN
 }

 fun classifyString(value:String, offset:Long?=null, source:String="binary-string"):MqlEvidence360 {
  val kind=when {
   events.any{value.contains(it,true)} -> MqlObjectKind.MQL_EVENT
   trading.any{value.contains(it,true)} -> MqlObjectKind.TRADING_API
   indicators.any{value.contains(it,true)} -> MqlObjectKind.INDICATOR
   value.contains(".dll",true) || value.contains(".so",true) -> MqlObjectKind.DLL
   value.contains("http://",true) || value.contains("https://",true) || value.contains("ws://",true) || value.contains("wss://",true) || value.contains("WebRequest",true) -> MqlObjectKind.URL
   else -> MqlObjectKind.STRING
  }
  return MqlEvidence360(kind,value,offset,source,MqlEvidenceStatus.OBSERVED,100)
 }

 fun transparent(e:MqlEvidence360):String {
  val off=e.offset?.let { "0x"+it.toString(16).uppercase() } ?: "unknown"
  return "Finding: "+e.value+"\nStatus: "+e.status+"\nSource: "+e.source+"\nOffset: "+off+
   "\nConfidence: "+e.confidence+"%\nInterpretation is limited to available evidence; original MQ4/MQ5 names, comments and statements are not claimed unless directly present."
 }
}
