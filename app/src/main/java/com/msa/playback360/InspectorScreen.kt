package com.msa.playback360

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import androidx.compose.ui.platform.LocalConfiguration

private enum class InspectorTab(val title:String){ OVERVIEW("Overview"), CODE("Code"), FILES("Files"), LIVE("Live"), SECURITY("Security360") }

@Composable fun InspectorScreen(onBack:()->Unit){
 val context=LocalContext.current
 val config=LocalConfiguration.current
 val width=config.screenWidthDp
 val height=config.screenHeightDp
 val compact=width<600
 val landscape=width>height
 val sidePad=when{width>=1200->24.dp;width>=840->18.dp;else->10.dp}
 val engine=remember{CopyExtractEngine(context)}
 var query by remember{mutableStateOf("")}
 var tab by remember{mutableStateOf(InspectorTab.OVERVIEW)}
 var more by remember{mutableStateOf(false)}
 var toolMenu by remember{mutableStateOf(false)}
 var bubbleX by remember{mutableFloatStateOf(0f)}
 var bubbleY by remember{mutableFloatStateOf(0f)}
 var scan by remember{mutableStateOf<TargetScan360?>(null)}
 var securityReport by remember{mutableStateOf<Security360Report?>(null)}
 val openTarget=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->
  uri?.let{runCatching{TargetScanner360.scan(TargetImporter360.import(context,it))}.onSuccess{s->scan=s;selected=InspectObject("TARGET",s.file.name,s.file.name,s.file.name,"360workspace/imports/"+s.file.name,null,s.file.length(),EvidenceSource.INFERRED,mapOf("Kind" to s.binary.kind.name,"SHA-256" to s.binary.sha256,"Libraries" to s.libraries.joinToString{it.family.name},"Findings" to s.binary.findings.size.toString()))}.onFailure{Toast.makeText(context,it.message?:"Scan failed",Toast.LENGTH_LONG).show()}}
 }
 var selected by remember{mutableStateOf(InspectObject(
  "CLASS","Select/index an object",null,"classes*.dex","DEX -> package -> class",
  null,null,EvidenceSource.DEX,mapOf("Status" to "Ready for target indexing")
 ))}

 Scaffold(
  topBar={Surface(tonalElevation=2.dp){Row(Modifier.fillMaxWidth().padding(horizontal=10.dp,vertical=6.dp),horizontalArrangement=Arrangement.SpaceBetween){
   Column(Modifier.weight(1f)){Text("360",style=MaterialTheme.typography.titleLarge);Text(selected.name,maxLines=1,overflow=TextOverflow.Ellipsis,style=MaterialTheme.typography.labelSmall)}
   Row{TextButton(onClick=onBack){Text("PLAYER")}}
  }}},
  bottomBar={}
 ){pad->
  Column(Modifier.padding(pad).fillMaxSize().padding(horizontal=sidePad,vertical=if(landscape)4.dp else 6.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
   if(scan==null) Button(onClick={openTarget.launch(arrayOf("*/*"))},Modifier.fillMaxWidth()){Text("OPEN TARGET")}
   else Text("Target: "+scan!!.file.name+" • "+scan!!.binary.kind+" • "+scan!!.objects.size+" indexed",style=MaterialTheme.typography.labelMedium)
   OutlinedTextField(query,{query=it},Modifier.fillMaxWidth(),placeholder={Text("Search, file, or https:// target…")},singleLine=true)
   if(query.startsWith("http://")||query.startsWith("https://")||query.startsWith("ws://")||query.startsWith("wss://")){
    val web=remember(query){WebConnection360.fromInput(query)}
    web?.let{w->
     Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
      Button(onClick={
       val sec=Security360.fromWeb(w.url)
       securityReport=sec
       selected=InspectObject("WEB_TARGET",w.host,w.host,w.url,w.url,null,null,EvidenceSource.INFERRED,
        mapOf("Scheme" to w.kind.name,"Host" to w.host,"Port" to (w.port?.toString()?:"default"),
         "Evidence" to w.evidence,"Security360" to SecurityEvidenceView360.summary(sec)))
       tab=InspectorTab.OVERVIEW
      }){Text("WEB TARGET")}
      OutlinedButton(onClick={engine.copy("Web URL",w.url)}){Text("COPY URL")}
     }
    }
   }
   Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)){
    Button(onClick={selected=selected.copy(name=query.ifBlank{"Selected object"},realName=query.ifBlank{null})}){Text("TARGET")}
    OutlinedButton(onClick={engine.copy("360",selected.copyDetails());Toast.makeText(context,"Copied",Toast.LENGTH_SHORT).show()}){Text("COPY")}
    OutlinedButton(onClick={val f=engine.extract(selected);Toast.makeText(context,"Extracted: "+f.name,Toast.LENGTH_SHORT).show()}){Text("EXTRACT")}
    OutlinedButton(onClick={tab=InspectorTab.CODE}){Text("VIEW")}
   }
   when(tab){
    InspectorTab.OVERVIEW->CompactOverview(selected,engine)
    InspectorTab.CODE->CompactCode(selected,engine)
    InspectorTab.FILES->CompactFiles(engine)
    InspectorTab.LIVE->CompactLive(selected)
    InspectorTab.SECURITY->securityReport?.let{Security360Screen(it){tab=InspectorTab.OVERVIEW}} ?: Text("Open a web target to build Security360 evidence.")
   }
   if(more) MoreSheet(engine,{more=false})
  }
  Box(Modifier.fillMaxSize()){
   FloatingActionButton(
    onClick={toolMenu=!toolMenu},
    modifier=Modifier.align(Alignment.BottomEnd).offset{IntOffset(bubbleX.roundToInt(),bubbleY.roundToInt())}
     .padding(16.dp).pointerInput(Unit){detectDragGestures{change,drag->change.consume();bubbleX+=drag.x;bubbleY+=drag.y}}
   ){Text("360")}
   DropdownMenu(expanded=toolMenu,onDismissRequest={toolMenu=false},modifier=Modifier.align(Alignment.BottomEnd)){
    DropdownMenuItem(text={Text("Open Target")},onClick={openTarget.launch(arrayOf("*/*"));toolMenu=false})
    DropdownMenuItem(text={Text("Overview")},onClick={tab=InspectorTab.OVERVIEW;toolMenu=false})
    DropdownMenuItem(text={Text("Code / Read / View")},onClick={tab=InspectorTab.CODE;toolMenu=false})
    DropdownMenuItem(text={Text("Files / Extract")},onClick={tab=InspectorTab.FILES;toolMenu=false})
    DropdownMenuItem(text={Text("Live / Trace")},onClick={tab=InspectorTab.LIVE;toolMenu=false})
    DropdownMenuItem(text={Text("Security360")},onClick={tab=InspectorTab.SECURITY;toolMenu=false})
    HorizontalDivider()
    DropdownMenuItem(text={Text("All Functions")},onClick={more=true;toolMenu=false})
    DropdownMenuItem(text={Text("Copy Current")},onClick={engine.copy("360",selected.copyDetails());toolMenu=false})
    DropdownMenuItem(text={Text("Extract Current")},onClick={engine.extract(selected);toolMenu=false})
   }
  }
 }
}

@Composable private fun CompactOverview(o:InspectObject,e:CopyExtractEngine){
 Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(6.dp)){
  CompactCard("DETAIL",listOf("Type" to o.type,"Name" to o.name,"Real name" to (o.realName?:"Unknown"),"File" to o.sourceFile,"Location" to o.location,"Offset" to (o.offset?.let{"0x"+it.toString(16).uppercase()}?:"N/A"),"Evidence" to o.source.name),e)
  Card(Modifier.fillMaxWidth()){Column(Modifier.padding(10.dp)){Text("SAFE",style=MaterialTheme.typography.titleSmall);Text("Read-only • originals preserved • secrets redacted • audit visible",style=MaterialTheme.typography.bodySmall)}}
 }
}
@Composable private fun CompactCode(o:InspectObject,e:CopyExtractEngine){
 Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(6.dp)){
  CompactCard("CODE",listOf("Target" to o.name,"Source" to o.sourceFile,"Location" to o.location,"Representation" to "Evidence-based / reconstructed when applicable"),e)
  Text("readcode • viewcode • method • function • transparency • stress",style=MaterialTheme.typography.labelSmall)
 }
}
@Composable private fun CompactFiles(e:CopyExtractEngine){
 Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(4.dp)){
  FileSourceGuide.entries.forEach{entry->Card(Modifier.fillMaxWidth()){Row(Modifier.fillMaxWidth().padding(8.dp),horizontalArrangement=Arrangement.SpaceBetween){Column(Modifier.weight(1f)){Text(entry.first,style=MaterialTheme.typography.bodyMedium);Text(entry.second,style=MaterialTheme.typography.labelSmall)};TextButton(onClick={e.copy(entry.first,entry.second)}){Text("COPY")}}}}
 }
}
@Composable private fun CompactLive(o:InspectObject){
 Card(Modifier.fillMaxWidth()){Column(Modifier.padding(10.dp)){Text("LIVE / SESSION",style=MaterialTheme.typography.titleSmall);Text("Target: "+o.name);Text("Runtime observed: "+(o.source==EvidenceSource.RUNTIME));Text("Follow Live • Trace • Snapshot • Compare",style=MaterialTheme.typography.labelSmall)}}
}
@Composable private fun CompactCard(title:String,rows:List<Pair<String,String>>,e:CopyExtractEngine){
 Card(Modifier.fillMaxWidth()){Column(Modifier.padding(10.dp),verticalArrangement=Arrangement.spacedBy(2.dp)){Text(title,style=MaterialTheme.typography.titleSmall);rows.forEach{r->Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Column(Modifier.weight(1f)){Text(r.first,style=MaterialTheme.typography.labelSmall);Text(r.second,maxLines=2,overflow=TextOverflow.Ellipsis)};TextButton(onClick={e.copy(r.first,r.second)},contentPadding=PaddingValues(horizontal=6.dp)){Text("COPY")}}}}}
}
@Composable private fun MoreSheet(e:CopyExtractEngine,onClose:()->Unit){
 Card(Modifier.fillMaxWidth()){Column(Modifier.padding(10.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(5.dp)){
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("TOOLS",style=MaterialTheme.typography.titleMedium);TextButton(onClick=onClose){Text("CLOSE")}}
  Text("FILES  filesdata • compare • offset • realname")
  Text("EX4/EX5  strings • mqlapi • dll • trading markers")
  Text("CODE  extract • read • view • method • style • function • stress")
  Text("ARCH  Code • Stack • Layer • Store • View • Method • Clone")
  Text("CRYPTO  Encrypt • Decrypt (workspace / supplied password)")
  Text("LIBRARY  Dart • Hermes • JS • MQ4/5 • M3U/M3U8 • HLS #Tags • Jiagu • ARM • CSS • C/C++")
  Text("DISCOVERY  Server • Link • Routes • Map • Viewer • Name")
  Text("WEB  Target • Method • Layer • Player • Stack • Clone")
  Text("WEB MAP  Routes • RealURLs • ServerName • Host • Dir • Index")
  Text("WEB LIVE  Security • Mirror • JSON • Callback • Release • Buffer")
  Text("CONNECTION  URL • Host • DNS/IP evidence • Ping • Config • INI • HTML")
  Text("STRUCTURE  VPN context • DNS • Mirror • Clone • LayerStack • FolderStack")
  Text("WEB INDEX  IndexOf • Chrome hints • EXT • Plugin refs • Ads evidence • Structure")
  Text("ANALYSIS  Semgrep • BIN • Grep • Strings")
  Text("TRACE IMPORT  Procmon logs • Wireshark captures")
  Text("DEFENSIVE  Phishing indicators • Malware triage")
  Text("SECURITY360  Evidence • Map • Network • Process • File • Findings")
  Text("WEB CRYPTO  Encrypt • Decrypt (owned workspace data)")
  Text("LAYERS  Hidden • HiddenLayer • HiddenStack • HiddenLog • HiddenCache")
  Text("Also  Kotlin • Java • Smali • DEX • ELF")
  Text("SAFE  legal • authorise • approve • readonly • redact • audit")
  Text("Workspace: "+e.workspacePath(),style=MaterialTheme.typography.labelSmall)
 }}
}
