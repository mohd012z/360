package com.msa.playback360

enum class CodeFamily {
 DART, HERMES, JAVASCRIPT, MQL4_SOURCE, MQL5_SOURCE, JIAGGU_MARKER,
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
  "/jiaggu" to CodeFamily.JIAGGU_MARKER,
  "/arm" to CodeFamily.ARM_NATIVE,
  "/css" to CodeFamily.CSS,
  "/c++" to CodeFamily.CPP,
  "/cpp" to CodeFamily.CPP
 )
}
