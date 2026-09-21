package com.msa.playback360

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File

object TargetImporter360 {
 fun import(context:Context,uri:Uri):File{
  val name=queryName(context,uri).replace(Regex("[^A-Za-z0-9._-]"),"_").ifBlank{"target.bin"}
  val dir=File(context.filesDir,"360workspace/imports").apply{mkdirs()}
  val out=File(dir,name)
  context.contentResolver.openInputStream(uri).use{input->
   requireNotNull(input){"Unable to open selected target"}
   out.outputStream().buffered().use{output->input.copyTo(output,64*1024)}
  }
  return out
 }
 private fun queryName(context:Context,uri:Uri):String{
  context.contentResolver.query(uri,arrayOf(OpenableColumns.DISPLAY_NAME),null,null,null)?.use{c->
   if(c.moveToFirst())return c.getString(0)?:"target.bin"
  }
  return uri.lastPathSegment?:"target.bin"
 }
}
