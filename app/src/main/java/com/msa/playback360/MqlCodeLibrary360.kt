package com.msa.playback360

/**
 * Built-in semantic library used by MQL360.
 * It classifies observable evidence; it does not bypass EX4/EX5 protections
 * or claim recovery of unavailable original source.
 */
enum class BuiltInCodeDomain {
 MQL4, MQL5, EXPERT_ADVISOR, INDICATOR, SCRIPT, CHART, TRADE, DLL_NATIVE,
 CPP, PYTHON, JAVASCRIPT, VBSCRIPT, JAVA, CSHARP, TEXT, INCLUDE, PANEL, RISK, SIGNAL, NETWORK_REFERENCE, GENERIC
}

data class BuiltInCodeSymbol(
 val name:String,
 val domain:BuiltInCodeDomain,
 val category:String,
 val aliases:Set<String> = emptySet(),
 val description:String
)

data class BuiltInCodeCommand(
 val command:String,
 val domain:String,
 val action:String,
 val boundary:String
)

object MqlCodeLibrary360 {
 val symbols=listOf(
  BuiltInCodeSymbol("OnInit",BuiltInCodeDomain.MQL5,"event",setOf("init"),"Initialization event evidence."),
  BuiltInCodeSymbol("OnDeinit",BuiltInCodeDomain.MQL5,"event",setOf("deinit"),"Deinitialization event evidence."),
  BuiltInCodeSymbol("OnTick",BuiltInCodeDomain.EXPERT_ADVISOR,"event",emptySet(),"New-tick event commonly used by EAs."),
  BuiltInCodeSymbol("OnTimer",BuiltInCodeDomain.MQL5,"event",emptySet(),"Timer event evidence."),
  BuiltInCodeSymbol("OnChartEvent",BuiltInCodeDomain.CHART,"event",emptySet(),"Chart/UI event evidence."),
  BuiltInCodeSymbol("OnCalculate",BuiltInCodeDomain.INDICATOR,"event",emptySet(),"Custom-indicator calculation event evidence."),
  BuiltInCodeSymbol("OnTradeTransaction",BuiltInCodeDomain.MQL5,"event",emptySet(),"Trade transaction event evidence."),
  BuiltInCodeSymbol("OrderSend",BuiltInCodeDomain.TRADE,"trade-api",emptySet(),"Order submission reference."),
  BuiltInCodeSymbol("OrderModify",BuiltInCodeDomain.TRADE,"trade-api",emptySet(),"Order modification reference."),
  BuiltInCodeSymbol("OrderClose",BuiltInCodeDomain.TRADE,"trade-api",emptySet(),"Order close reference."),
  BuiltInCodeSymbol("MqlTradeRequest",BuiltInCodeDomain.MQL5,"trade-structure",emptySet(),"MT5 trade request structure reference."),
  BuiltInCodeSymbol("MqlTradeResult",BuiltInCodeDomain.MQL5,"trade-structure",emptySet(),"MT5 trade result structure reference."),
  BuiltInCodeSymbol("CTrade",BuiltInCodeDomain.MQL5,"trade-class",emptySet(),"MT5 standard trade class reference."),
  BuiltInCodeSymbol("iCustom",BuiltInCodeDomain.INDICATOR,"indicator-api",emptySet(),"Custom indicator reference."),
  BuiltInCodeSymbol("iMA",BuiltInCodeDomain.INDICATOR,"indicator-api",setOf("MovingAverage"),"Moving-average indicator evidence."),
  BuiltInCodeSymbol("iRSI",BuiltInCodeDomain.INDICATOR,"indicator-api",setOf("RSI"),"RSI indicator evidence."),
  BuiltInCodeSymbol("iMACD",BuiltInCodeDomain.INDICATOR,"indicator-api",setOf("MACD"),"MACD indicator evidence."),
  BuiltInCodeSymbol("iBands",BuiltInCodeDomain.INDICATOR,"indicator-api",setOf("Bollinger"),"Bollinger Bands evidence."),
  BuiltInCodeSymbol("iStochastic",BuiltInCodeDomain.INDICATOR,"indicator-api",setOf("Stochastic"),"Stochastic indicator evidence."),
  BuiltInCodeSymbol("iIchimoku",BuiltInCodeDomain.INDICATOR,"indicator-api",setOf("Ichimoku"),"Ichimoku indicator evidence."),
  BuiltInCodeSymbol("WebRequest",BuiltInCodeDomain.NETWORK_REFERENCE,"network-api",emptySet(),"Observable outbound web-request API reference."),
  BuiltInCodeSymbol("LoadLibrary",BuiltInCodeDomain.DLL_NATIVE,"native-api",emptySet(),"Native library loading reference."),
  BuiltInCodeSymbol("ShellExecute",BuiltInCodeDomain.DLL_NATIVE,"native-api",emptySet(),"Shell invocation reference; inventory only."),
  BuiltInCodeSymbol("CreateObject",BuiltInCodeDomain.VBSCRIPT,"script-api",emptySet(),"VBScript/COM object construction evidence."),
  BuiltInCodeSymbol("require",BuiltInCodeDomain.JAVASCRIPT,"module-api",emptySet(),"JavaScript module-loading evidence."),
  BuiltInCodeSymbol("import",BuiltInCodeDomain.PYTHON,"module-api",emptySet(),"Python/import-style source evidence."),
  BuiltInCodeSymbol("std::",BuiltInCodeDomain.CPP,"native-source",emptySet(),"C++ standard-library source marker."),
  BuiltInCodeSymbol("#include",BuiltInCodeDomain.INCLUDE,"include",setOf(".mqh"),"Source/include dependency evidence."),
  BuiltInCodeSymbol("ObjectCreate",BuiltInCodeDomain.PANEL,"panel-chart",setOf("OBJ_BUTTON","OBJ_LABEL","OBJ_EDIT"),"Chart panel/object evidence."),
  BuiltInCodeSymbol("RiskPercent",BuiltInCodeDomain.RISK,"risk",setOf("MaxRisk","Lots","LotSize","StopLoss"),"Risk/position-sizing terminology evidence."),
  BuiltInCodeSymbol("Signal",BuiltInCodeDomain.SIGNAL,"signal",setOf("BuySignal","SellSignal","EntrySignal"),"Trading signal terminology evidence."),
  BuiltInCodeSymbol("java.",BuiltInCodeDomain.JAVA,"java",setOf("public class","import java"),"Java source/reference evidence."),
  BuiltInCodeSymbol("System.",BuiltInCodeDomain.CSHARP,"csharp",setOf("using System","namespace"),"C#/.NET source/reference evidence.")
 )

 val commands=listOf(
  BuiltInCodeCommand("/codeextract","Evidence","Extract strings, metadata and readable evidence with offsets.","Separate workspace; original remains unchanged."),
  BuiltInCodeCommand("/codeview","Viewer","Open evidence/hex/string view.","Read-only."),
  BuiltInCodeCommand("/codec++","Language","Classify C/C++/native evidence.","No native protection bypass."),
  BuiltInCodeCommand("/codevbscript","Language","Classify VBScript/COM evidence.","Evidence only; no script execution."),
  BuiltInCodeCommand("/codepy","Language","Classify Python evidence.","Evidence only; no imported target execution."),
  BuiltInCodeCommand("/codemq4","MQL","Classify MQ4/EX4 concepts.","Compiled targets remain evidence-based."),
  BuiltInCodeCommand("/codemq5","MQL","Classify MQ5/EX5 concepts.","Compiled targets remain evidence-based."),
  BuiltInCodeCommand("/codeide","Viewer","Open evidence-oriented IDE.","Never presents inferred text as original source."),
  BuiltInCodeCommand("/codejs","Language","Classify JavaScript evidence.","Static evidence by default."),
  BuiltInCodeCommand("/codeall","Search","Show all normalized evidence categories.","Includes provenance/status/confidence."),
  BuiltInCodeCommand("/codedll","Native","Inventory DLL/native references.","Does not load or execute referenced DLLs."),
  BuiltInCodeCommand("/codestring","Evidence","Extract/classify printable strings.","Retains source offsets."),
  BuiltInCodeCommand("/codegrep","Search","Search normalized evidence.","Search is local to selected/authorized target evidence."),
  BuiltInCodeCommand("/codenmap","Network map","Map extracted hosts, URLs and relationships.","Evidence map only; no port scanning or network probing."),
  BuiltInCodeCommand("/codeconverter","Export","Convert evidence to hex/strings/JSON/report.","Does not claim EX4/EX5-to-original-source conversion."),
  BuiltInCodeCommand("/codecompress","Workspace","Compress evidence/report package.","Does not rewrite the imported binary."),
  BuiltInCodeCommand("/codereverse","Analysis","Reconstruct supported semantic evidence.","No compiler/protection bypass; reconstruction is labeled."),
  BuiltInCodeCommand("/codeai","Analysis","Explain and correlate evidence.","AI conclusions are INFERRED unless directly supported."),
  BuiltInCodeCommand("/codechart","MQL","Classify chart/object/event evidence.","Evidence-based."),
  BuiltInCodeCommand("/codeindicator","MQL","Classify indicator APIs and parameters.","Does not invent unavailable strategy logic."),
  BuiltInCodeCommand("/codescript","MQL","Classify script/event evidence.","Evidence-based."),
  BuiltInCodeCommand("/codeexpertadvisor","MQL","Classify EA/trading/event evidence.","Does not claim exact original EA source."),
  BuiltInCodeCommand("/codeexpert","MQL","Alias for Expert Advisor evidence.","Evidence-based; no source recovery claim."),
  BuiltInCodeCommand("/codepanel","MQL","Classify chart panels, labels, buttons and objects.","Static/source evidence only."),
  BuiltInCodeCommand("/coderisk","MQL","Classify risk and position-sizing evidence.","Does not recommend trades or infer missing parameters."),
  BuiltInCodeCommand("/codesignal","MQL","Classify observable signal logic terminology.","Does not generate trading signals from missing logic."),
  BuiltInCodeCommand("/codeinclude","MQL","Map include/MQH dependencies when source evidence exists.","Does not invent missing includes."),
  BuiltInCodeCommand("/codetrade","MQL","Collect trading-operation evidence.","Observed APIs and terminology only."),
  BuiltInCodeCommand("/codelist","Catalog","List evidence objects, symbols and commands.","Read-only catalog."),
  BuiltInCodeCommand("/code360","Core","Open unified MQL360 evidence workspace.","Read-only analysis workspace."),
  BuiltInCodeCommand("/codecatalog","Catalog","Browse built-in symbol/command catalog.","Reference library only."),
  BuiltInCodeCommand("/coderelated","Map","Show evidence related to the selected object.","Relationships include provenance and status."),
  BuiltInCodeCommand("/codejava","Language","Classify Java evidence.","Static evidence; no execution."),
  BuiltInCodeCommand("/codevb","Language","Classify VB/VBScript evidence.","Static evidence; no execution."),
  BuiltInCodeCommand("/codetxt","Language","Inspect text/config/log evidence.","Read-only text evidence."),
  BuiltInCodeCommand("/codecs","Language","Classify C#/.NET evidence.","Static evidence; no execution."),
  BuiltInCodeCommand("/codeoffset","Binary","Navigate evidence by offset.","Offset navigation only."),
  BuiltInCodeCommand("/codeunicode","Evidence","Extract/classify Unicode strings.","Retains byte offsets and provenance."),
  BuiltInCodeCommand("/codeascii","Evidence","Show ASCII string evidence.","Printable ASCII with exact byte offsets."),
  BuiltInCodeCommand("/codeutf","Evidence","Show all supported UTF string evidence.","Encoding-aware filter; does not guess unavailable text."),
  BuiltInCodeCommand("/codeutf8","Evidence","Show UTF-8-compatible evidence.","Separates decoded text from binary bytes."),
  BuiltInCodeCommand("/codeutf16","Evidence","Show UTF-16 evidence.","Includes endian-specific provenance where detected."),
  BuiltInCodeCommand("/codeutf16le","Evidence","Show UTF-16LE evidence.","Exact byte offsets retained."),
  BuiltInCodeCommand("/codeutf16be","Evidence","Show UTF-16BE evidence.","Exact byte offsets retained."),
  BuiltInCodeCommand("/codeutf32","Evidence","Catalog UTF-32 evidence when supported.","Read-only decoding with provenance."),
  BuiltInCodeCommand("/codeencode","Encoding","Summarize observed text encodings.","Detection/reporting only; original bytes stay unchanged."),
  BuiltInCodeCommand("/codevalidate","Validation","Validate decoded evidence against byte structure.","Marks valid, suspicious or unsupported decoding; never invents text."),
  BuiltInCodeCommand("/codebom","Encoding","Detect Unicode byte-order marks.","Reports BOM type and byte offset when present."),
  BuiltInCodeCommand("/codecode","Core","Unified code/evidence dispatcher.","Routes to safe read-only viewers, search, encoding and semantic analysis.")
 )

 private fun containsSymbol(text:String,s:BuiltInCodeSymbol):Boolean =
  text.contains(s.name,true) || s.aliases.any{text.contains(it,true)}

 fun lookup(text:String):List<BuiltInCodeSymbol> = symbols.filter { containsSymbol(text,it) }

 fun domains(text:String):Set<BuiltInCodeDomain> = lookup(text).map{it.domain}.toSet()

 fun command(name:String):BuiltInCodeCommand? =
  commands.firstOrNull { it.command.equals(name,true) }

 fun summary():Map<String,Int> = commands.groupingBy{it.domain}.eachCount()
}
