package com.msa.playback360

import java.io.File

data class MqlDllCorrelation360(
 val observedReference:String,
 val matchedFile:String?,
 val imports:List<String>,
 val exports:List<String>,
 val matchedSymbols:List<String>,
 val confidence:Int,
 val notes:String
)

object MqlDllCorrelationEngine360 {
 fun correlate(report:MqlBinaryReport360,dllFiles:List<File>):List<MqlDllCorrelation360>{
  val refs=report.evidence.filter{it.kind==MqlObjectKind.DLL}.map{dllName(it.value)}.filter{it.isNotBlank()}.distinct()
  val observed=report.evidence.map{it.value}.joinToString("\n")
  return refs.map{ref->
   val file=dllFiles.firstOrNull{it.name.equals(ref,true)}
   if(file==null) MqlDllCorrelation360(ref,null,emptyList(),emptyList(),emptyList(),20,"DLL name observed in EX4/EX5 evidence; matching DLL file was not supplied.")
   else runCatching{
    val pe=PeInspector360.inspect(file)
    val symbols=PeSymbolInspector360.inspect(file,pe)
    val imports=symbols.imports.flatMap{m->m.functions.mapNotNull{f->f.name?.let{m.dll+"!"+it}}}.distinct().take(2000)
    val exports=symbols.exports.mapNotNull{it.name}.distinct().take(2000)
    val matches=exports.filter{observed.contains(it,true)}.take(500)
    MqlDllCorrelation360(ref,file.name,imports,exports,matches,
     if(matches.isNotEmpty())85 else 55,
     if(matches.isNotEmpty())"DLL file and exported symbol names are also observed in EX4/EX5 evidence."
     else "DLL file matched by name; exports are inventory evidence only and are not claimed as calls.")
   }.getOrElse{MqlDllCorrelation360(ref,file.name,emptyList(),emptyList(),emptyList(),25,"PE parsing failed: "+(it.message?:"unknown error"))}
  }
 }

 fun reconstructedImports(rows:List<MqlDllCorrelation360>):String=buildString{
  rows.filter{it.matchedFile!=null}.forEach{row->
   append("#import \"").append(safe(row.matchedFile!!)).append("\"\n")
   if(row.matchedSymbols.isEmpty()) append("   // No exported function is directly corroborated by EX4/EX5 string evidence.\n")
   else row.matchedSymbols.forEach{append("   // OBSERVED SYMBOL: ").append(safe(it)).append("\n")}
   append("#import\n\n")
  }
 }

 private fun dllName(v:String):String {
  val m=Regex("""(?i)([A-Za-z0-9_. -]+\.dll)""").find(v)
  return m?.groupValues?.get(1)?.trim()?.substringAfterLast('\\')?.substringAfterLast('/') ?: ""
 }
 private fun safe(v:String)=v.replace("\r"," ").replace("\n"," ").replace("\"","'").take(240)
}
