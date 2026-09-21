package com.msa.playback360

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun Security360Screen(report:Security360Report,onBack:()->Unit){
 var selectedKind by remember{mutableStateOf<SecurityEvidenceKind360?>(null)}
 var selected by remember{mutableStateOf<SecurityEvidenceNode360?>(null)}
 val shown=if(selectedKind==null)report.nodes else report.byKind(selectedKind!!)

 Column(Modifier.fillMaxSize().padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
   Text("Security360",style=MaterialTheme.typography.headlineSmall)
   TextButton(onClick=onBack){Text("BACK")}
  }
  Text("Evidence "+report.nodes.size+" • Links "+report.edges.size+" • Findings "+report.findings().size,
   style=MaterialTheme.typography.labelMedium)

  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
   FilterChip(selected=selectedKind==null,onClick={selectedKind=null},label={Text("ALL")})
   listOf(SecurityEvidenceKind360.URL,SecurityEvidenceKind360.DNS,SecurityEvidenceKind360.NETWORK,
    SecurityEvidenceKind360.PROCESS,SecurityEvidenceKind360.FILE,SecurityEvidenceKind360.PHISHING,
    SecurityEvidenceKind360.MALWARE).take(3).forEach{k->
    FilterChip(selected=selectedKind==k,onClick={selectedKind=k},label={Text(k.name)})
   }
  }
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
   listOf(SecurityEvidenceKind360.PROCESS,SecurityEvidenceKind360.FILE,SecurityEvidenceKind360.PHISHING,
    SecurityEvidenceKind360.MALWARE).forEach{k->
    FilterChip(selected=selectedKind==k,onClick={selectedKind=k},label={Text(k.name)})
   }
  }

  LazyColumn(Modifier.weight(1f).fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(6.dp)){
   items(shown,key={it.id}){n->
    Card(Modifier.fillMaxWidth().clickable{selected=n}){
     Column(Modifier.padding(10.dp)){
      Text(n.kind.name+" • "+n.title,style=MaterialTheme.typography.titleSmall)
      Text(n.value,maxLines=2,style=MaterialTheme.typography.bodySmall)
      Text(n.source+" • "+n.severity,style=MaterialTheme.typography.labelSmall)
     }
    }
   }
  }

  selected?.let{n->
   AlertDialog(onDismissRequest={selected=null},confirmButton={TextButton(onClick={selected=null}){Text("CLOSE")}},
    title={Text(n.title)},text={Text(SecurityEvidenceView360.detail(report,n.id))})
  }
 }
}
