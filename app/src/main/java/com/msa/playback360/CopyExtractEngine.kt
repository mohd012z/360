package com.msa.playback360
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class CopyExtractEngine(private val context:Context){
 private val root=File(context.filesDir,"360workspace").apply{mkdirs()}
 fun copy(label:String,text:String){
  val cb=context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
  cb.setPrimaryClip(ClipData.newPlainText(label,text))
 }
 fun extract(obj:InspectObject):File{
  val safe=obj.name.replace(Regex("[^A-Za-z0-9._-]"),"_").take(80)
  val dir=File(root,safe+"_"+System.currentTimeMillis()).apply{mkdirs()}
  File(dir,"details.txt").writeText(obj.copyDetails())
  File(dir,"details.json").writeText(toJson(obj).toString(2))
  File(dir,"source-map.json").writeText(JSONObject().apply{
   put("sourceFile",obj.sourceFile);put("location",obj.location)
   put("offset",obj.offset?:JSONObject.NULL);put("evidence",obj.source.name)
  }.toString(2))
  return dir
 }
 fun toJson(obj:InspectObject)=JSONObject().apply{
  put("type",obj.type);put("name",obj.name);put("realName",obj.realName?:JSONObject.NULL)
  put("sourceFile",obj.sourceFile);put("location",obj.location);put("offset",obj.offset?:JSONObject.NULL)
  put("size",obj.size?:JSONObject.NULL);put("evidence",obj.source.name);put("details",JSONObject(obj.details))
 }
 fun compare(a:InspectObject,b:InspectObject):String{
  val rows=JSONArray()
  fun row(k:String,av:Any?,bv:Any?){rows.put(JSONObject().put("field",k).put("a",av).put("b",bv).put("changed",av!=bv))}
  row("name",a.name,b.name);row("realName",a.realName,b.realName);row("sourceFile",a.sourceFile,b.sourceFile)
  row("location",a.location,b.location);row("offset",a.offset,b.offset);row("size",a.size,b.size);row("evidence",a.source.name,b.source.name)
  return JSONObject().put("compare",rows).toString(2)
 }
 fun workspacePath()=root.absolutePath
}