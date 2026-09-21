package com.msa.playback360

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.io.File

private enum class MqlDashboardTab(val label:String) {
    OVERVIEW("Overview"), BINARY("Binary"), STRINGS("Strings"), FUNCTIONS("Functions"),
    DLL("DLL / PE"), GRAPH("Graph"), RECONSTRUCT("Reconstruct"), COMPARE("Compare"), MODEL("AI Model")
}

@Composable
fun Mql360Dashboard(
    file: File,
    report: MqlBinaryReport360,
    onBack: () -> Unit
) {
    var tab by remember { mutableStateOf(MqlDashboardTab.OVERVIEW) }
    var output by remember { mutableStateOf("") }
    var referenceSource by remember { mutableStateOf<Pair<File,MqlBinaryReport360>?>(null) }
    var referenceCompiled by remember { mutableStateOf<Pair<File,MqlBinaryReport360>?>(if(report.kind==MqlBinaryKind.EX4 || report.kind==MqlBinaryKind.EX5) file to report else null) }
    var pairBusy by remember { mutableStateOf(false) }
    var pairError by remember { mutableStateOf<String?>(null) }
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    val scroll = rememberScrollState()

    fun importReference(uri:android.net.Uri, source:Boolean) {
        scope.launch {
            pairBusy=true
            pairError=null
            runCatching {
                withContext(Dispatchers.IO) {
                    val imported=TargetImporter360.import(context,uri)
                    imported to MqlBinaryScanner360.scan(imported)
                }
            }.onSuccess {
                if(source) referenceSource=it else referenceCompiled=it
            }.onFailure { pairError=it.message ?: "Reference import failed" }
            pairBusy=false
        }
    }
    val sourcePicker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){ uri -> uri?.let{importReference(it,true)} }
    val compiledPicker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){ uri -> uri?.let{importReference(it,false)} }

    Scaffold(
        topBar = {
            Surface(tonalElevation = 3.dp) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("MQL360", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "EX4 / EX5 Analyzer & Reconstructor • " + file.name,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    TextButton(onClick = onBack) { Text("BACK") }
                }
            }
        }
    ) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(scroll).padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StageCard("1", "Import", file.name, "Loaded")
                StageCard("2", "Scan", "Binary • Structure • Strings", "Ready")
                StageCard("3", "Analyze", "Functions • Evidence • DLL", "Ready")
                StageCard("4", "Reconstruct", "MQ4 / MQ5 evidence scaffold", "Ready")
                StageCard("5", "Compare", "Reference-pair verification", "Ready")
                StageCard("6", "AI Model", "Learn • Improve • Copy", "Ready")
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("FILE INFORMATION", style = MaterialTheme.typography.titleSmall)
                    Text(file.name, style = MaterialTheme.typography.titleMedium)
                    Text("Type: " + report.kind + "   •   Size: " + report.size + " bytes")
                    Text("SHA-256: " + report.sha256, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("Evidence: " + report.evidence.size + " findings")
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("QUICK ACTIONS", style = MaterialTheme.typography.titleSmall)
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(onClick = {
                            output = CodeDispatcher360.execute("/learnintelligentmodel", file, report).message
                            tab = MqlDashboardTab.MODEL
                        }) { Text("Learn Model") }
                        Button(onClick = {
                            output = CodeDispatcher360.execute("/improvemodel", file, report).message
                            tab = MqlDashboardTab.MODEL
                        }) { Text("Improve Model") }
                        OutlinedButton(onClick = {
                            output = CodeDispatcher360.execute("/modelcopy", file, report).message
                            tab = MqlDashboardTab.MODEL
                        }) { Text("Model Copy") }
                        OutlinedButton(onClick = {
                            val target = if (report.kind == MqlBinaryKind.EX4) "mq4" else "mq5"
                            output = CodeDispatcher360.execute("/codedeepreconstruct " + target, file, report).message
                            tab = MqlDashboardTab.RECONSTRUCT
                        }) { Text("Reconstruct") }
                    }
                }
            }

            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                MqlDashboardTab.entries.forEach { item ->
                    if (tab == item) Button(onClick = { tab = item }) { Text(item.label) }
                    else OutlinedButton(onClick = { tab = item }) { Text(item.label) }
                }
            }

            when (tab) {
                MqlDashboardTab.OVERVIEW -> OverviewPanel(file, report)
                MqlDashboardTab.BINARY -> CommandPanel("/codestructure", file, report)
                MqlDashboardTab.STRINGS -> EvidencePanel(report.evidence.filter { it.kind == MqlObjectKind.STRING })
                MqlDashboardTab.FUNCTIONS -> EvidencePanel(report.evidence.filter {
                    it.kind == MqlObjectKind.MQL_EVENT || it.kind == MqlObjectKind.TRADING_API ||
                        it.kind == MqlObjectKind.INDICATOR || it.kind == MqlObjectKind.INFERRED_FUNCTION
                })
                MqlDashboardTab.DLL -> EvidencePanel(report.evidence.filter { it.kind == MqlObjectKind.DLL || it.kind == MqlObjectKind.NATIVE_REFERENCE })
                MqlDashboardTab.GRAPH -> CommandPanel("/codemap", file, report)
                MqlDashboardTab.RECONSTRUCT -> {
                    val target = if (report.kind == MqlBinaryKind.EX4) "mq4" else "mq5"
                    CommandPanel("/codedeepreconstruct " + target, file, report)
                }
                MqlDashboardTab.COMPARE -> ReferencePairPanel(
                    source=referenceSource,
                    compiled=referenceCompiled,
                    busy=pairBusy,
                    error=pairError,
                    onPickSource={sourcePicker.launch(arrayOf("*/*"))},
                    onPickCompiled={compiledPicker.launch(arrayOf("*/*"))}
                )
                MqlDashboardTab.MODEL -> Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("INTELLIGENT EVIDENCE MODEL", style = MaterialTheme.typography.titleSmall)
                        Text(if (output.isBlank()) "Run Learn Model or Improve Model to build the session evidence model." else output)
                    }
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text("EVIDENCE POLICY", style = MaterialTheme.typography.titleSmall)
                    Text("OBSERVED • EXTRACTED • RECONSTRUCTED • INFERRED • USER_LABEL")
                    Text("Original files remain read-only. Reconstruction confidence is evidence-based.")
                }
            }
        }
    }
}

@Composable
private fun StageCard(number:String, title:String, subtitle:String, status:String) {
    Card(Modifier.width(230.dp)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(number + "  " + title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(status, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun OverviewPanel(file:File, report:MqlBinaryReport360) {
    val events = report.evidence.count { it.kind == MqlObjectKind.MQL_EVENT }
    val api = report.evidence.count { it.kind == MqlObjectKind.TRADING_API }
    val indicators = report.evidence.count { it.kind == MqlObjectKind.INDICATOR }
    val dll = report.evidence.count { it.kind == MqlObjectKind.DLL }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("ANALYSIS OVERVIEW", style = MaterialTheme.typography.titleSmall)
            Text("Target: " + file.name)
            Text("Events: $events   •   Trading APIs: $api   •   Indicators: $indicators   •   DLL: $dll")
            Text("ASCII: " + report.asciiStrings + "   •   UTF-16LE: " + report.utf16LeStrings + "   •   UTF-16BE: " + report.utf16BeStrings)
            Text("Entropy: " + "%.4f".format(report.entropy))
        }
    }
}

@Composable
private fun EvidencePanel(rows:List<MqlEvidence360>) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text("EVIDENCE (" + rows.size + ")", style = MaterialTheme.typography.titleSmall)
            if (rows.isEmpty()) Text("No direct evidence observed.")
            rows.take(100).forEach { e ->
                val offset = e.offset?.let { "0x" + it.toString(16).uppercase() } ?: "N/A"
                Text("[" + e.status + " " + e.confidence + "%] " + e.value + "  @" + offset)
            }
            if (rows.size > 100) Text("Showing first 100 of " + rows.size + " findings.")
        }
    }
}

@Composable
private fun CommandPanel(command:String, file:File, report:MqlBinaryReport360) {
    val result = remember(command, file, report) { CodeDispatcher360.execute(command, file, report) }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(result.title, style = MaterialTheme.typography.titleSmall)
            Text(result.message)
            result.warnings.forEach { Text("Warning: " + it, style = MaterialTheme.typography.labelSmall) }
        }
    }
}


@Composable
private fun ReferencePairPanel(
    source:Pair<File,MqlBinaryReport360>?,
    compiled:Pair<File,MqlBinaryReport360>?,
    busy:Boolean,
    error:String?,
    onPickSource:()->Unit,
    onPickCompiled:()->Unit
) {
    val verification=remember(source,compiled) {
        if(source!=null && compiled!=null) MqlReferencePair360.compare(source.second,compiled.second) else null
    }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Text("REFERENCE PAIR VERIFICATION",style=MaterialTheme.typography.titleMedium)
            Text("Use MQ4 ↔ EX4 or MQ5 ↔ EX5. Files are scanned read-only.")
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                Button(onClick=onPickSource,enabled=!busy){Text(if(source==null)"Select MQ4 / MQ5" else "Change Source")}
                OutlinedButton(onClick=onPickCompiled,enabled=!busy){Text(if(compiled==null)"Select EX4 / EX5" else "Change Compiled")}
            }
            if(busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            error?.let{Text(it,color=MaterialTheme.colorScheme.error)}
            PairFileCard("SOURCE",source)
            PairFileCard("COMPILED",compiled)
            verification?.let { v ->
                val label=when {
                    !v.validPair -> "PAIR MISMATCH"
                    v.overallConfidence>=80 -> "HIGH"
                    v.overallConfidence>=50 -> "MEDIUM"
                    else -> "LOW"
                }
                Text("Reconstruction confidence: "+v.overallConfidence+"% • "+label,style=MaterialTheme.typography.titleMedium)
                LinearProgressIndicator(progress={v.overallConfidence/100f},modifier=Modifier.fillMaxWidth())
                if(!v.validPair) Text("Choose MQ4 with EX4, or MQ5 with EX5.",color=MaterialTheme.colorScheme.error)
                v.metrics.forEach { m ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(9.dp)) {
                            Text(m.category,style=MaterialTheme.typography.titleSmall)
                            Text("Matched "+m.matched+" / "+m.sourceCount+" source findings • compiled "+m.compiledCount)
                            LinearProgressIndicator(progress={m.confidence/100f},modifier=Modifier.fillMaxWidth())
                            Text(m.confidence.toString()+"% evidence overlap",style=MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                v.notes.forEach{Text(it,style=MaterialTheme.typography.labelSmall)}
            }
        }
    }
}

@Composable
private fun PairFileCard(label:String,pair:Pair<File,MqlBinaryReport360>?) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(9.dp)) {
            Text(label,style=MaterialTheme.typography.labelSmall)
            if(pair==null) Text("Not selected")
            else {
                Text(pair.first.name,style=MaterialTheme.typography.titleSmall)
                Text(pair.second.kind.toString()+" • "+pair.second.size+" bytes • evidence "+pair.second.evidence.size)
                Text("SHA-256 "+pair.second.sha256,maxLines=1,overflow=TextOverflow.Ellipsis,style=MaterialTheme.typography.labelSmall)
            }
        }
    }
}
