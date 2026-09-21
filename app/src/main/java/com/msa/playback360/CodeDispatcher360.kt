package com.msa.playback360

import java.io.File

data class CodeCommandResult360(val command:String,val title:String,val message:String,val evidence:List<MqlEvidence360> = emptyList(),val warnings:List<String> = emptyList())

object CodeDispatcher360 {
 fun execute(input:String,file:File,report:MqlBinaryReport360?=null):CodeCommandResult360 {
  val parts=input.trim().split(Regex("\\s+"),limit=2)
  val command=parts.firstOrNull()?.lowercase().orEmpty()
  val arg=parts.getOrNull(1)?.trim().orEmpty()
  val r=report ?: MqlBinaryScanner360.scan(file)
  return when(command){
   "/code","/codeall","/code360","/codecode" -> result(command,"All evidence",MqlBinaryScanner360.summary(r),r.evidence)
   "/codebase" -> result(command,"Evidence database",CodeDeepDive360.renderModel(CodeDeepDive360.model(file,r)),r.evidence)
   "/codeextract","/codeextractor","/codeextraction" -> result(command,"Layered extractor",CodeDeepDive360.extract(file,r),r.evidence)
   "/codemodel" -> result(command,"Reconstruction model",CodeDeepDive360.renderModel(CodeDeepDive360.model(file,r)))
   "/codeevidence" -> result(command,"Verified evidence","Evidence sorted by confidence and offset.",CodeDeepDive360.evidence(r))
   "/codeverify" -> result(command,"Evidence verification",CodeDeepDive360.verify(file,r))
   "/codecli" -> result(command,"MQL360 CLI",CodeDeepDive360.cli())
   "/codepy","/phython","/python" -> result(command,"Python evidence",CodeLanguageDecompiler360.language(file,r,"python"))
   "/codebash","/bash" -> result(command,"Bash / shell evidence",CodeLanguageDecompiler360.language(file,r,"bash"))
   "/codec++" -> result(command,"C/C++ evidence",CodeLanguageDecompiler360.language(file,r,"cpp"))
   "/codedecompiler","/decompiler" -> result(command,"Evidence decompiler",CodeLanguageDecompiler360.decompile(file,r),warnings=listOf("Reconstruction is evidence-based and is not represented as original source."))
   "/codehidden" -> result(command,"Hidden / non-obvious evidence",CodeLanguageDecompiler360.hidden(file,r))
   "/codeassembly" -> result(command,"Assembly-like evidence view",CodeRead360.assembly(file,r))
   "/codestart" -> result(command,"Entry/event evidence",CodeRead360.start(file,r))
   "/codefunction" -> result(command,"Function/method evidence",CodeRead360.functions(file,r))
   "/coderead","/codex" -> result(command,"Deep code reader",CodeRead360.read(file,r))
   "/codeask" -> result(command,"Evidence query",CodeRead360.ask(file,r,arg))
   "/codeprocess" -> result(command,"Analysis process",CodeRead360.process(file,r))
   "/codelog" -> result(command,"Analysis log",CodeRead360.log(file,r))
   "/codecopy" -> result(command,"Evidence copy",CodeRead360.copy(r))
   "/codewrite" -> result(command,"Safe write boundary",CodeRead360.writeBoundary())
   "/codedecryptor" -> result(command,"Decode/decrypt boundary",CodeRead360.decryptBoundary())
   "/codesign" -> result(command,"Signature / hash identity","SHA-256: "+r.sha256+"\nFile: "+file.name+"\nSize: "+file.length()+" bytes\nThis is file identity evidence, not a claim of publisher/authenticode signing.")
   "/learnintelligentmodel" -> {
    val learned=IntelligentEvidenceModel360.learn(r)
    result(command,"Intelligent evidence learning",
     "Learned session patterns: "+learned.size+"\n"+
     "High confidence: "+learned.count{it.confidence>=80}+"\n"+
     "Learning uses observed evidence and USER_LABEL confirmations only.")
   }
   "/modelcopy" -> {
    val copy=IntelligentEvidenceModel360.modelCopy()
    result(command,"Model evidence copy",
     if(copy.isEmpty()) "No learned patterns yet. Run /learnintelligentmodel first."
     else copy.take(500).joinToString("\n"){it.confidence.toString()+"% ["+it.kind+"] "+it.token+" obs="+it.observations+" confirmed="+it.confirmations})
   }
   "/improvemodel" -> result(command,"Improve evidence model",IntelligentEvidenceModel360.improve(r))
   "/method","/codemethod","/deepmethod","/hiddenmethod" -> {
    val methods=MqlMethodCluster360.build(file,r)
    result(command,"EX4/EX5 method reconstruction",MqlMethodCluster360.render(methods))
   }
   "/codemap","/codestack","/coderelationships" -> {
    val g=CodeRelationGraph360.build(file,r)
    result(command,"Evidence relationship graph",CodeRelationGraph360.render(g))
   }
   "/codecatalog","/codelist","/code*" -> result(command,"MQL360 command catalog",CodeDeepDive360.catalog())
   "/codesummary","/codemetadata","/metadata","/codebinary" -> result(command,"Target summary",MqlBinaryScanner360.summary(r))
   "/structure","/codestructure","/coderegions" -> {
    val s=ExStructure360.inspect(file)
    result(command,"EX4/EX5 structure map",ExStructure360.summary(s))
   }
   "/codestring" -> result(command,"String evidence","Normalized string findings.",r.evidence.filter{it.kind==MqlObjectKind.STRING})
   "/codeascii" -> encoded(command,r,"ASCII")
   "/codeutf","/codeunicode" -> encoded(command,r,"UTF")
   "/codeutf16" -> encoded(command,r,"UTF-16")
   "/codeutf16le" -> encoded(command,r,"UTF-16LE")
   "/codeutf16be" -> encoded(command,r,"UTF-16BE")
   "/codeutf8","/codeutf32" -> CodeCommandResult360(command,"Encoding not fully implemented","Validated streaming decoding for this encoding is not implemented yet.",warnings=listOf("No inferred text is returned."))
   "/codeconstants","/codenumeric" -> {
    val n=NumericEvidenceScanner360.scan(file)
    result(command,"Numeric constant candidates",NumericEvidenceScanner360.summary(n))
   }
   "/codebom" -> {
    val bom=CodeEncoding360.detectBom(file)
    result(command,"BOM evidence",if(bom.isEmpty()) "No supported BOM observed in the scan window." else bom.joinToString("\n"){it.encoding+" @ 0x"+it.offset.toString(16).uppercase()})
   }
   "/codeencode" -> result(command,"Encoding summary",CodeEncoding360.summary(r,CodeEncoding360.detectBom(file)))
   "/codevalidate" -> result(command,"Encoding validation",CodeEncoding360.validate(r).joinToString("\n"){it.encoding+": "+it.valid+"/"+it.count+" valid; suspicious="+it.suspicious})
   "/codegrep" -> if(arg.isBlank()) usage(command,"Usage: /codegrep <text>") else result(command,"Search: "+arg,"Evidence matching query.",MqlBinaryScanner360.grep(r,arg))
   "/codeoffset" -> {
    val off=parseOffset(arg)
    if(off==null) usage(command,"Usage: /codeoffset 0x18A20") else result(command,"Offset 0x"+off.toString(16).uppercase(),"Nearby evidence +/-64 bytes.",MqlBinaryScanner360.atOffset(r,off))
   }
   "/codedll" -> byKind(command,r,MqlObjectKind.DLL,"DLL/native references")
   "/codec","/codec+","/codegz","/codegzip" -> {
    result(command,"Native / compression extractor",NativeCompressionExtractor360.render(file))
   }
   "/coderegionreconstruct","/codedeepreconstruct" -> {
    val target=when(arg.lowercase()){ "mq4"->MqlBinaryKind.MQ4; "mq5"->MqlBinaryKind.MQ5; else->if(r.kind==MqlBinaryKind.EX4)MqlBinaryKind.MQ4 else MqlBinaryKind.MQ5 }
    val src=MqlRegionReconstruction360.render(file,r,target)
    CodeCommandResult360(command,"Region-aware reconstructed "+target,src,warnings=listOf("Relationship proximity is evidence, not proof of original control flow."))
   }
   "/codereconstruct","/codereverse" -> {
    val target=when(arg.lowercase()){ "mq4"->MqlBinaryKind.MQ4; "mq5"->MqlBinaryKind.MQ5; else->if(r.kind==MqlBinaryKind.EX4)MqlBinaryKind.MQ4 else MqlBinaryKind.MQ5 }
    val rec=MqlReconstructionEngine360.reconstruct(r,target)
    CodeCommandResult360(command,"Reconstructed "+target+" source",rec.source,warnings=rec.warnings+("Confidence: "+rec.confidence+"%"))
   }
   "/codecompare","/codediff" -> if(arg.isBlank()) usage(command,"Usage: "+command+" <path-to-second EX4/EX5>") else {
    val other=File(arg)
    if(!other.isFile || !other.canRead()) usage(command,"Second file is not readable: "+arg)
    else {
     val rr=MqlBinaryScanner360.scan(other)
     val diff=MqlCompare360.compare(r,rr)
     CodeCommandResult360(command,"MQL360 comparison",MqlCompare360.render(diff))
    }
   }
   "/codeindicator" -> byKind(command,r,MqlObjectKind.INDICATOR,"Indicator evidence")
   "/codeapi","/codetrade" -> result(command,"MQL/trade API evidence","Observed API/event evidence.",r.evidence.filter{it.kind==MqlObjectKind.TRADING_API || it.kind==MqlObjectKind.MQL_EVENT})
   "/codeurl","/codenmap" -> byKind(command,r,MqlObjectKind.URL,"URL/network-reference evidence")
   "/coderisk" -> semantic(command,r,setOf(BuiltInCodeDomain.RISK),"Risk terminology evidence")
   "/codesignal" -> semantic(command,r,setOf(BuiltInCodeDomain.SIGNAL),"Signal terminology evidence")
   "/codepanel","/codechart" -> semantic(command,r,setOf(BuiltInCodeDomain.PANEL,BuiltInCodeDomain.CHART),"Panel/chart evidence")
   "/codeexpert","/codeexpertadvisor" -> semantic(command,r,setOf(BuiltInCodeDomain.EXPERT_ADVISOR,BuiltInCodeDomain.TRADE),"EA evidence")
   "/coderelated" -> {
    val selected=if(arg.isBlank()) null else MqlBinaryScanner360.grep(r,arg).firstOrNull()
    if(selected==null) usage(command,"Usage: /coderelated <evidence text>") else result(command,"Related to "+selected.value,"Semantic/proximity relationships.",MqlBinaryScanner360.related(r,selected))
   }
   else -> {
    val known=MqlCodeLibrary360.command(command) ?: Mql360.commands.firstOrNull{it.command.equals(command,true)}
    if(known!=null) CodeCommandResult360(command,"Command registered","Known command; dedicated dispatcher engine is not wired yet.")
    else CodeCommandResult360(command.ifBlank{"/"},"Unknown command","Use /codecatalog or /codelist to inspect supported commands.")
   }
  }
 }
 private fun encoded(c:String,r:MqlBinaryReport360,e:String)=result(c,e+" evidence","Encoding-filtered evidence with exact byte offsets.",MqlBinaryScanner360.byEncoding(r,e))
 private fun byKind(c:String,r:MqlBinaryReport360,k:MqlObjectKind,t:String)=result(c,t,"Observed/classified evidence only.",r.evidence.filter{it.kind==k})
 private fun semantic(c:String,r:MqlBinaryReport360,d:Set<BuiltInCodeDomain>,t:String):CodeCommandResult360 {
  val rows=r.evidence.filter{e->e.details["domains"]?.split(",")?.any{x->d.any{it.name.equals(x.trim(),true)}}==true}
  return result(c,t,"Semantic library matches with provenance.",rows)
 }
 private fun result(c:String,t:String,m:String,e:List<MqlEvidence360> = emptyList())=CodeCommandResult360(c,t,m,e)
 private fun usage(c:String,m:String)=CodeCommandResult360(c,"Command usage",m)
 private fun parseOffset(v:String):Long?=runCatching{if(v.startsWith("0x",true))v.substring(2).toLong(16) else v.toLong()}.getOrNull()
}
