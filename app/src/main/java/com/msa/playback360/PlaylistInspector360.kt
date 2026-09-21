package com.msa.playback360

data class PlaylistEntry360(
 val line:Int,
 val name:String?,
 val group:String?,
 val tvgId:String?,
 val tvgName:String?,
 val logo:String?,
 val url:String,
 val headers:Map<String,String>,
 val tags:List<String>
)

data class PlaylistReport360(
 val kind:String,
 val entries:List<PlaylistEntry360>,
 val tags:Map<String,Int>,
 val urls:Int,
 val groups:Int
)

object PlaylistInspector360 {
 fun inspect(text:String):PlaylistReport360{
  val lines=text.lineSequence().map{it.trim()}.toList()
  val tagCounts=linkedMapOf<String,Int>()
  val out=mutableListOf<PlaylistEntry360>()
  var info:String?=null
  var infoLine=0
  val pendingTags=mutableListOf<String>()
  lines.forEachIndexed{i,line->
   if(line.startsWith("#")){
    val tag=line.substringBefore(':').substringBefore(' ')
    tagCounts[tag]=(tagCounts[tag]?:0)+1
    pendingTags+=line
    if(line.startsWith("#EXTINF",true)){info=line;infoLine=i+1}
   } else if(line.isNotBlank() && (line.contains("://") || line.startsWith("/"))){
    val attrs=parseAttrs(info.orEmpty())
    val rawName=info?.substringAfterLast(',',"")?.trim()?.ifBlank{null}
    val parts=line.split("|",limit=2)
    val headers=if(parts.size==2) parts[1].split("&").mapNotNull{
     val p=it.split("=",limit=2);if(p.size==2)p[0] to redactHeader(p[0],p[1]) else null
    }.toMap() else emptyMap()
    out+=PlaylistEntry360(
     line=if(infoLine>0) infoLine else i+1,
     name=rawName,group=attrs["group-title"],tvgId=attrs["tvg-id"],
     tvgName=attrs["tvg-name"],logo=attrs["tvg-logo"],
     url=redactUrl(parts[0]),headers=headers,tags=pendingTags.toList()
    )
    info=null;infoLine=0;pendingTags.clear()
   }
  }
  val kind=if(lines.any{it.startsWith("#EXT-X-")})"HLS/M3U8" else "M3U"
  return PlaylistReport360(kind,out,tagCounts,out.size,out.mapNotNull{it.group}.distinct().size)
 }

 private fun parseAttrs(s:String):Map<String,String>{
  val r=Regex("""([A-Za-z0-9_-]+)="([^"]*)"""")
  return r.findAll(s).associate{it.groupValues[1] to it.groupValues[2]}
 }
 private fun redactHeader(k:String,v:String)=
  if(Regex("authorization|cookie|token|secret|password|key",RegexOption.IGNORE_CASE).containsMatchIn(k))"<redacted>" else v
 private fun redactUrl(u:String):String{
  val sensitive=Regex("""(?i)(token|key|secret|password|auth|signature|sig|session)=([^&]+)""")
  return sensitive.replace(u){"${it.groupValues[1]}=<redacted>"}
 }
}
