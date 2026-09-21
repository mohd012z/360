package com.msa.playback360

import java.io.File
import java.security.MessageDigest
import kotlin.math.ln

enum class BinaryKind { APK, EX4, EX5, DEX, ELF, XML, JSON, UNKNOWN }

data class BinaryFinding(
 val category:String,
 val value:String,
 val offset:Long,
 val evidence:String
)

data class BinaryReport(
 val fileName:String,
 val kind:BinaryKind,
 val size:Long,
 val sha256:String,
 val entropy:Double,
 val findings:List<BinaryFinding>
)

/**
 * Read-only binary inspection for user-owned/authorized files.
 * EX4/EX5 support intentionally inventories metadata, strings, URLs,
 * MQL API markers, DLL names, trading markers and offsets. It does NOT
 * claim to reconstruct MQ4/MQ5 source or bypass Market/Cloud protection.
 */
object BinaryInspector360 {
 private val url=Regex("""(?:https?|wss?)://[A-Za-z0-9._~:/?#\[\]@!$&'()*+,;=%-]+""")
 private val dll=Regex("""[A-Za-z0-9_.-]+\.dll""",RegexOption.IGNORE_CASE)
 private val mqlApi=Regex("""\b(?:OrderSend|OrderClose|OrderModify|OrderDelete|OrderSelect|WebRequest|FileOpen|FileWrite|FileRead|Alert|Print|SendNotification|iCustom|OnTick|OnInit|OnDeinit|OnTimer|OnChartEvent|OnTrade|OnTradeTransaction)\b""")
 private val trading=Regex("""\b(?:magic|stoploss|takeprofit|trailing|grid|martingale|hedg|scalp|RSI|MACD|Bollinger|Stochastic|Ichimoku|ZigZag|Fibonacci|Pivot|Support|Resistance|Breakout|Reversal)\b""",RegexOption.IGNORE_CASE)

 fun inspect(file:File):BinaryReport{
  val bytes=file.readBytes()
  val kind=when(file.extension.lowercase()){
   "apk"->BinaryKind.APK;"ex4"->BinaryKind.EX4;"ex5"->BinaryKind.EX5;"dex"->BinaryKind.DEX
   "so"->BinaryKind.ELF;"xml"->BinaryKind.XML;"json"->BinaryKind.JSON;else->BinaryKind.UNKNOWN
  }
  val textRuns=asciiRuns(bytes,4)
  val out=mutableListOf<BinaryFinding>()
  fun addMatches(category:String,re:Regex){
   textRuns.forEach{r->re.findAll(r.second).forEach{m->
    out+=BinaryFinding(category,m.value,r.first+m.range.first,"STATIC_STRING")
   }}
  }
  addMatches("URL",url);addMatches("DLL",dll);addMatches("MQL_API",mqlApi);addMatches("TRADING_MARKER",trading)
  textRuns.filter{it.second.startsWith("#property",true)}.forEach{out+=BinaryFinding("PROPERTY",it.second,it.first,"STATIC_STRING")}
  return BinaryReport(file.name,kind,bytes.size.toLong(),sha256(bytes),entropy(bytes),out.distinctBy{Triple(it.category,it.value,it.offset)})
 }

 private fun asciiRuns(data:ByteArray,min:Int):List<Pair<Long,String>>{
  val out=mutableListOf<Pair<Long,String>>();var start=-1
  for(i in data.indices){
   val ok=(data[i].toInt() and 0xff) in 32..126
   if(ok && start<0) start=i
   if((!ok || i==data.lastIndex) && start>=0){
    val end=if(ok && i==data.lastIndex)i+1 else i
    if(end-start>=min) out+=start.toLong() to data.copyOfRange(start,end).toString(Charsets.US_ASCII)
    start=-1
   }
  }
  return out
 }
 private fun sha256(b:ByteArray)=MessageDigest.getInstance("SHA-256").digest(b).joinToString(""){"%02x".format(it)}
 private fun entropy(b:ByteArray):Double{
  if(b.isEmpty())return 0.0
  val f=IntArray(256);b.forEach{f[it.toInt() and 255]++}
  return f.filter{it>0}.sumOf{c->val p=c.toDouble()/b.size; -p*(ln(p)/ln(2.0))}
 }
}
