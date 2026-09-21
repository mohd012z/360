package com.msa.playback360

enum class CodeLayer360 {
 SOURCE, RECONSTRUCTED, BYTECODE, NATIVE, RESOURCE, CONFIG, RUNTIME, STORAGE, CRYPTO, INFERRED
}

data class CodeNode360(
 val id:String,
 val name:String,
 val kind:String,
 val file:String,
 val location:String,
 val layer:CodeLayer360,
 val offset:Long?=null,
 val parentId:String?=null,
 val evidence:String,
 val confidence:Int=100,
 val metadata:Map<String,String> = emptyMap()
)

data class CodeEdge360(
 val from:String,
 val to:String,
 val relation:String,
 val evidence:String
)

data class CodeStackFrame360(
 val depth:Int,
 val nodeId:String,
 val display:String,
 val runtimeObserved:Boolean,
 val durationMs:Long?=null
)

data class CodeStoreRecord360(
 val node:CodeNode360,
 val sha256:String?=null,
 val extractedCopy:String?=null,
 val sessionId:String?=null
)

object CodeArchitecture360 {
 val commands=listOf(
  "/code","/codestack","/codelayer","/codestore","/codeview",
  "/codeencrypt","/codeencypt","/codedecrypt","/codemethod","/codemethode","/codeclone"
 )

 fun layers(nodes:List<CodeNode360>)=nodes.groupBy{it.layer}

 fun stack(start:String,edges:List<CodeEdge360>,nodes:List<CodeNode360>):List<CodeNode360>{
  val byId=nodes.associateBy{it.id};val seen=mutableSetOf<String>();val out=mutableListOf<CodeNode360>()
  fun walk(id:String){
   if(!seen.add(id))return
   byId[id]?.let{out+=it}
   edges.filter{it.from==id}.forEach{walk(it.to)}
  }
  walk(start);return out
 }

 fun view(node:CodeNode360)=buildString{
  appendLine("[360 CODE]")
  appendLine("Name: "+node.name)
  appendLine("Kind: "+node.kind)
  appendLine("Layer: "+node.layer)
  appendLine("File: "+node.file)
  appendLine("Location: "+node.location)
  node.offset?.let{appendLine("Offset: 0x"+it.toString(16).uppercase())}
  appendLine("Evidence: "+node.evidence)
  appendLine("Confidence: "+node.confidence+"%")
 }

 fun cloneForWorkspace(node:CodeNode360,newId:String)=
  node.copy(id=newId,metadata=node.metadata+("clone" to "workspace-copy"))
}
