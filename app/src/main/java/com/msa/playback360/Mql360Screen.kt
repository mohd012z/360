package com.msa.playback360

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.io.File

@Composable
fun Mql360Screen(file:File, onBack:()->Unit) {
 var report by remember(file){ mutableStateOf<MqlBinaryReport360?>(null) }
 var error by remember(file){ mutableStateOf<String?>(null) }
 var query by remember{ mutableStateOf("") }
 var selected by remember{ mutableStateOf<MqlEvidence360?>(null) }
 var filter by remember{ mutableStateOf<MqlObjectKind?>(null) }

 LaunchedEffect(file){
  runCatching { MqlBinaryScanner360.scan(file) }
   .onSuccess { report=it }
   .onFailure { error=it.message ?: "MQL360 scan failed" }
 }

 Column(Modifier.fillMaxSize().padding(10.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
   Column(Modifier.weight(1f)){
    Text("MQL360 CODE IDE",style=MaterialTheme.typography.titleLarge)
    Text(file.name,maxLines=1,overflow=TextOverflow.Ellipsis,style=MaterialTheme.typography.labelSmall)
   }
   TextButton(onClick=onBack){Text("BACK")}
  }
  error?.let{Text(it,color=MaterialTheme.colorScheme.error)}
  val r=report
  if(r==null && error==null){ LinearProgressIndicator(Modifier.fillMaxWidth()); Text("Scanning read-only evidence…") }
  if(r!=null){
   Card(Modifier.fillMaxWidth()){Column(Modifier.padding(10.dp)){
    Text(r.kind.name+" • "+r.size+" bytes")
    Text("SHA-256 "+r.sha256,maxLines=1,overflow=TextOverflow.Ellipsis,style=MaterialTheme.typography.labelSmall)
    Text("Entropy %.3f • Evidence %d • API %d • DLL %d • URL %d".format(r.entropy,r.evidence.size,r.apis,r.dlls,r.urls),style=MaterialTheme.typography.labelSmall)
   }}
   OutlinedTextField(query,{query=it},Modifier.fillMaxWidth(),singleLine=true,placeholder={Text("/codegrep or search evidence")})
   Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(5.dp)){
    FilterChip(filter==null,{filter=null},{Text("ALL")})
    listOf(MqlObjectKind.MQL_EVENT,MqlObjectKind.TRADING_API,MqlObjectKind.INDICATOR,MqlObjectKind.DLL,MqlObjectKind.URL,MqlObjectKind.STRING).forEach{k->
     FilterChip(filter==k,{filter=k},{Text(k.name)})
    }
   }
   val rows=r.evidence.asSequence()
    .filter{filter==null || it.kind==filter}
    .filter{query.isBlank() || it.value.contains(query,true) || it.details.values.any{v->v.contains(query,true)}}
    .take(2000).toList()
   Text("Showing "+rows.size+" evidence objects",style=MaterialTheme.typography.labelSmall)
   LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)){
    items(rows){e->
     Card(onClick={selected=e},modifier=Modifier.fillMaxWidth()){
      Column(Modifier.padding(8.dp)){
       Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
        Text(e.kind.name,style=MaterialTheme.typography.labelMedium)
        Text(e.offset?.let{"0x"+it.toString(16).uppercase()}?:"—",style=MaterialTheme.typography.labelSmall)
       }
       Text(e.value,maxLines=2,overflow=TextOverflow.Ellipsis)
       if(e.details.isNotEmpty()) Text(e.details.entries.joinToString(" • "){it.key+"="+it.value},maxLines=1,overflow=TextOverflow.Ellipsis,style=MaterialTheme.typography.labelSmall)
      }
     }
    }
   }
  }
 }
 selected?.let{e->
  AlertDialog(onDismissRequest={selected=null},confirmButton={TextButton(onClick={selected=null}){Text("CLOSE")}},
   title={Text(e.kind.name)},text={Text(Mql360.transparent(e)+"\n\n"+e.details.entries.joinToString("\n"){it.key+": "+it.value})})
 }
}
