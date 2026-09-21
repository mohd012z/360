package com.msa.playback360

enum class CodeFamily {
 DART, HERMES, JAVASCRIPT, MQL4_SOURCE, MQL5_SOURCE, EX4_BINARY, EX5_BINARY, M3U, M3U8, HLS_TAGS, JIAGGU_MARKER,
 ARM_NATIVE, CSS, CPP, C, JAVA, KOTLIN, SMALI, DEX, ELF, UNKNOWN
}

data class CodeLibraryEntry(
 val family:CodeFamily,
 val extensions:Set<String>,
 val signatures:List<String>,
 val viewer:String,
 val capabilities:List<String>,
 val note:String=""
)

object CodeLibrary360 {
 val entries=listOf(
  CodeLibraryEntry(CodeFamily.DART,setOf("dart"),listOf("import 'dart:","void main("),"Dart","text",listOf("strings","imports","classes","functions","references","copy","extract")),
  CodeLibraryEntry(CodeFamily.HERMES,setOf("hbc","bundle"),listOf("Hermes","HBC"),"hermes-bytecode",listOf("header","strings","modules","functions","offsets","hex"),"Compiled Hermes is shown as bytecode/reconstructed evidence, not original JS."),
  CodeLibraryEntry(CodeFamily.JAVASCRIPT,setOf("js","mjs","cjs","jsx"),listOf("function ","=>","require(","import "),"javascript",listOf("source","modules","functions","urls","json","callbacks","format")),
  CodeLibraryEntry(CodeFamily.MQL4_SOURCE,setOf("mq4","mqh"),listOf("#property","OnTick","OrderSend"),"mql4",listOf("source","functions","inputs","includes","trading-api","urls")),
  CodeLibraryEntry(CodeFamily.MQL5_SOURCE,setOf("mq5","mqh"),listOf("#property","OnTradeTransaction","MqlTradeRequest"),"mql5",listOf("source","functions","inputs","includes","trading-api","urls")),
  CodeLibraryEntry(CodeFamily.EX4_BINARY,setOf("ex4"),emptyList(),"mql360",listOf("metadata","hash","entropy","strings","offsets","hex","mql-api","dll","urls","objects","map","compare","sources","summary"),"Compiled MQL4 evidence analysis only; original MQ4 source is not claimed or protection bypassed."),
  CodeLibraryEntry(CodeFamily.EX5_BINARY,setOf("ex5"),emptyList(),"mql360",listOf("metadata","hash","entropy","strings","offsets","hex","mql-api","dll","urls","objects","map","compare","sources","summary"),"Compiled MQL5 evidence analysis only; original MQ5 source is not claimed or protection bypassed."),
  CodeLibraryEntry(CodeFamily.M3U,setOf("m3u"),listOf("#EXTM3U","#EXTINF"),"playlist",listOf("entries","groups","names","logos","tvg-id","tvg-name","urls","headers","duplicates","copy","extract"),"Playlist inventory; credentials and sensitive query values should be redacted in reports."),
  CodeLibraryEntry(CodeFamily.M3U8,setOf("m3u8"),listOf("#EXTM3U","#EXT-X-"),"hls",listOf("master-playlist","media-playlist","variants","bandwidth","resolution","codecs","audio","subtitles","segments","keys-metadata","target-duration","sequence","live-vod","urls"),"HLS structure inspection; do not expose protected key material."),
  CodeLibraryEntry(CodeFamily.HLS_TAGS,setOf("m3u","m3u8"),listOf("#EXTINF","#EXT-X-STREAM-INF","#EXT-X-MEDIA","#EXT-X-KEY","#EXT-X-MAP","#EXT-X-TARGETDURATION","#EXT-X-MEDIA-SEQUENCE","#EXT-X-ENDLIST"),"hls-tags",listOf("tag-index","attributes","line-number","url-relation","timeline","validation"),"Indexes #EXT and #EXT-X metadata with source-line provenance."),
  CodeLibraryEntry(CodeFamily.JIAGGU_MARKER,setOf("so","dex","dat"),listOf("jiagu","360jiagu","libjiagu"),"protected-marker",listOf("identify","inventory","strings","metadata","offsets"),"Identification/inventory only; no protection bypass."),
  CodeLibraryEntry(CodeFamily.ARM_NATIVE,setOf("so","elf","bin"),listOf("ELF","arm64","aarch64","armeabi"),"arm",listOf("elf-header","abi","symbols","imports","exports","jni","strings","hex")),
  CodeLibraryEntry(CodeFamily.CSS,setOf("css","scss"),listOf("@media","display:","color:"),"css",listOf("source","selectors","variables","media-queries","urls")),
  CodeLibraryEntry(CodeFamily.CPP,setOf("cpp","cc","cxx","hpp","hh","hxx"),listOf("#include","std::","namespace "),"cpp",listOf("source","classes","functions","includes","symbols")),
  CodeLibraryEntry(CodeFamily.C,setOf("c","h"),listOf("#include","typedef ","struct "),"c",listOf("source","functions","includes","symbols")),
  CodeLibraryEntry(CodeFamily.JAVA,setOf("java"),listOf("package ","public class ","interface "),"java",listOf("source","classes","methods","imports")),
  CodeLibraryEntry(CodeFamily.KOTLIN,setOf("kt","kts"),listOf("package ","fun ","data class "),"kotlin",listOf("source","classes","functions","imports")),
  CodeLibraryEntry(CodeFamily.SMALI,setOf("smali"),listOf(".class ",".method ","invoke-"),"smali",listOf("methods","calls","registers","strings","offsets")),
  CodeLibraryEntry(CodeFamily.DEX,setOf("dex"),listOf("dex\n"),"dex",listOf("header","classes","methods","strings","references","offsets")),
  CodeLibraryEntry(CodeFamily.ELF,setOf("so","elf"),listOf("ELF"),"native",listOf("header","abi","symbols","imports","exports","jni","strings"))
 )

 fun byExtension(name:String):List<CodeLibraryEntry>{
  val ext=name.substringAfterLast('.', "").lowercase()
  return entries.filter{ext in it.extensions}
 }

 fun commandIndex()=mapOf(
  "/dart" to CodeFamily.DART,
  "/hermes" to CodeFamily.HERMES,
  "/js" to CodeFamily.JAVASCRIPT,
  "/mq4" to CodeFamily.MQL4_SOURCE,
  "/mq5" to CodeFamily.MQL5_SOURCE,
  "/ex4" to CodeFamily.EX4_BINARY,
  "/ex5" to CodeFamily.EX5_BINARY,
  "/mql360" to CodeFamily.EX5_BINARY,
  "/m3u" to CodeFamily.M3U,
  "/m3u8" to CodeFamily.M3U8,
  "/#" to CodeFamily.HLS_TAGS,
  "/extinf" to CodeFamily.HLS_TAGS,
  "/ext-x" to CodeFamily.HLS_TAGS,
  "/jiaggu" to CodeFamily.JIAGGU_MARKER,
  "/arm" to CodeFamily.ARM_NATIVE,
  "/css" to CodeFamily.CSS,
  "/c++" to CodeFamily.CPP,
  "/cpp" to CodeFamily.CPP
 )
}
