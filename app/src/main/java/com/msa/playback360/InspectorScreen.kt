package com.msa.playback360
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable fun InspectorScreen(onBack:()->Unit){
 val context=LocalContext.current;val engine=remember{CopyExtractEngine(context)}
 var query by remember{mutableStateOf("")}
 var selected by remember{mutableStateOf(InspectObject("CLASS","Select/index an object",null,"classes*.dex","DEX -> package -> class",null,null,EvidenceSource.DEX,mapOf("Status" to "Prototype provenance + copy/extract workspace")))}
 Column(Modifier.fillMaxSize().padding(12.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)){
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("360 CODEBRAIN",style=MaterialTheme.typography.headlineSmall);TextButton(onClick=onBack){Text("PLAYER")}}
  OutlinedTextField(query,{query=it},Modifier.fillMaxWidth(),label={Text("/grep /target /class /file")},singleLine=true)
  Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){
   Button(onClick={selected=selected.copy(name=query.ifBlank{"Selected object"},realName=query.ifBlank{null})}){Text("TARGET")}
   OutlinedButton(onClick={engine.copy("360 details",selected.copyDetails());Toast.makeText(context,"Copied details",Toast.LENGTH_SHORT).show()}){Text("COPY")}
   OutlinedButton(onClick={val f=engine.extract(selected);Toast.makeText(context,"Extracted: "+f.name,Toast.LENGTH_SHORT).show()}){Text("EXTRACT")}
  }
  Card(Modifier.fillMaxWidth()){Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){
   Text("/detail",style=MaterialTheme.typography.titleMedium);Text("Type: "+selected.type);Text("Name: "+selected.name)
   Text("Real/resolved: "+(selected.realName?:"Unknown / not claimed"));Text("File: "+selected.sourceFile);Text("Location: "+selected.location)
   Text("Offset: "+(selected.offset?.let{v->"0x"+v.toString(16).uppercase()}?:"N/A"));Text("Evidence: "+selected.source)
   Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){TextButton(onClick={engine.copy("name",selected.name)}){Text("COPY NAME")};TextButton(onClick={engine.copy("location",selected.location)}){Text("COPY LOCATION")}}
  }}
  Text("Which file supplies the data",style=MaterialTheme.typography.titleMedium)
  FileSourceGuide.entries.forEach{entry->Card(Modifier.fillMaxWidth()){Row(Modifier.fillMaxWidth().padding(10.dp),horizontalArrangement=Arrangement.SpaceBetween){Column(Modifier.weight(1f)){Text(entry.first);Text(entry.second,style=MaterialTheme.typography.bodySmall)};TextButton(onClick={engine.copy(entry.first,entry.second)}){Text("COPY")}}}}
  Card(Modifier.fillMaxWidth()){Column(Modifier.padding(12.dp)){Text("/filesdata  /compare  /showoffset  /realname",style=MaterialTheme.typography.titleSmall);Text("Workspace: "+engine.workspacePath());Text("Extract creates details.txt + details.json + source-map.json without modifying the original APK.")}}
 }
}