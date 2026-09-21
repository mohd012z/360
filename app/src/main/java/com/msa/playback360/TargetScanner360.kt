package com.msa.playback360

import java.io.File
import java.util.zip.ZipFile

data class TargetScan360(
 val file:File,
 val binary:BinaryReport,
 val libraries:List<CodeLibraryEntry>,
 val objects:List<InspectObject>,
 val archiveEntries:Int=0,
 val notes:List<String> = emptyList()
)

object TargetScanner360 {
 fun scan(file:File):TargetScan360{
  val binary=BinaryInspector360.inspect(file)
  val libs=CodeLibrary360.byExtension(file.name).toMutableList()
  val objects=mutableListOf<InspectObject>()
  val notes=mutableListOf<String>()
  binary.findings.take(1000).forEachIndexed{i,f->
   objects+=InspectObject(
    type=f.category,name=f.value,sourceFile=file.name,
    location=file.name+" @ 0x"+f.offset.toString(16).uppercase(),
    offset=f.offset,source=EvidenceSource.INFERRED,
    details=mapOf("Evidence" to f.evidence,"Index" to i.toString())
   )
  }
  var count=0
  if(file.extension.equals("apk",true)||file.extension.equals("zip",true)){
   runCatching{
    ZipFile(file).use{zip->
     val e=zip.entries()
     while(e.hasMoreElements()){
      val z=e.nextElement();count++
      val name=z.name
      CodeLibrary360.byExtension(name).forEach{if(it !in libs)libs+=it}
      val src=when{
       name.startsWith("lib/")->EvidenceSource.NATIVE
       name.startsWith("res/")->EvidenceSource.RESOURCE
       name.startsWith("assets/")->EvidenceSource.ASSET
       name.endsWith(".xml",true)->EvidenceSource.XML
       name.endsWith(".dex",true)->EvidenceSource.DEX
       else->EvidenceSource.INFERRED
      }
      objects+=InspectObject(
       type="ARCHIVE_ENTRY",name=name,sourceFile=file.name,location=name,
       size=z.size.takeIf{it>=0},source=src,
       details=mapOf("Compressed" to z.compressedSize.toString(),"CRC" to z.crc.toString())
      )
     }
    }
   }.onFailure{notes+="Archive inventory failed: "+(it.message?:"unknown error")}
  }
  if(libs.isEmpty())notes+="No exact extension library matched; binary evidence is still available."
  return TargetScan360(file,binary,libs.distinctBy{it.family},objects,count,notes)
 }
}
