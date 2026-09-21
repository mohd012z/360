package com.msa.playback360

import java.io.File
import java.security.MessageDigest

data class SystemBinaryEvidence360(
 val name:String,
 val path:String,
 val extension:String,
 val size:Long,
 val sha256:String,
 val category:String,
 val architectureHint:String?,
 val readable:Boolean
)

/**
 * Read-only inventory for a user-selected Windows system folder or copied DLL/EXE set.
 * It does not bypass Windows permissions, load DLLs, execute binaries, or modify System32.
 */
object CodeSystem32_360 {
 private val allowed=setOf("dll","exe","sys","ocx","cpl","drv","mui")
 private val categories=mapOf(
  "dll" to "Dynamic library",
  "exe" to "Executable",
  "sys" to "Driver",
  "ocx" to "COM/ActiveX library",
  "cpl" to "Control Panel component",
  "drv" to "Driver component",
  "mui" to "Language/resource file"
 )

 fun inventory(root:File,maxFiles:Int=10_000):List<SystemBinaryEvidence360>{
  require(root.exists()){"Selected path does not exist"}
  val files=if(root.isFile) sequenceOf(root) else root.walkTopDown().filter{it.isFile}
  return files.filter{it.extension.lowercase() in allowed}.take(maxFiles).map{file->
   SystemBinaryEvidence360(
    file.name,file.absolutePath,file.extension.lowercase(),file.length(),
    sha256(file),categories[file.extension.lowercase()]?:"Binary",
    architectureHint(file),file.canRead()
   )
  }.toList()
 }

 fun relatedToMql(rows:List<SystemBinaryEvidence360>,report:MqlBinaryReport360):List<SystemBinaryEvidence360>{
  val refs=report.evidence.filter{it.kind==MqlObjectKind.DLL || it.kind==MqlObjectKind.NATIVE_REFERENCE}
   .flatMap{Regex("""[A-Za-z0-9_.-]+\.(?:dll|exe|sys)""",RegexOption.IGNORE_CASE).findAll(it.value).map{m->m.value.lowercase()}.toList()}
   .toSet()
  return rows.filter{it.name.lowercase() in refs}
 }

 private fun architectureHint(file:File):String? {
  if(!file.canRead() || file.length()<64) return null
  return runCatching{
   file.inputStream().use{input->
    val h=ByteArray(64); if(input.read(h)<64) return@use null
    if(h[0]!=0x4d.toByte() || h[1]!=0x5a.toByte()) return@use null
    val pe=(h[0x3c].toInt() and 0xff) or ((h[0x3d].toInt() and 0xff) shl 8) or ((h[0x3e].toInt() and 0xff) shl 16) or ((h[0x3f].toInt() and 0xff) shl 24)
    file.inputStream().use{p->
     var remain=pe.toLong(); while(remain>0){val skipped=p.skip(remain); if(skipped<=0)return@use null;remain-=skipped}
     val b=ByteArray(6); if(p.read(b)<6)return@use null
     val machine=(b[4].toInt() and 0xff) or ((b[5].toInt() and 0xff) shl 8)
     when(machine){0x14c->"x86";0x8664->"x64";0xAA64->"ARM64";else->"PE machine 0x"+machine.toString(16)}
    }
   }
  }.getOrNull()
 }

 private fun sha256(file:File):String{
  val d=MessageDigest.getInstance("SHA-256")
  file.inputStream().use{input->val b=ByteArray(64*1024);while(true){val n=input.read(b);if(n<=0)break;d.update(b,0,n)}}
  return d.digest().joinToString(""){"%02x".format(it)}
 }
}
