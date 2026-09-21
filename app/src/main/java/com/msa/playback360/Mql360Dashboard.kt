package com.msa.playback360

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
    val scroll = rememberScrollState()

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
                MqlDashboardTab.COMPARE -> Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text("REFERENCE PAIR", style = MaterialTheme.typography.titleSmall)
                        Text("Select a known MQ4/MQ5 + EX4/EX5 pair to measure recovered evidence against ground truth.")
                        Text("Comparison does not claim unavailable original source.")
                    }
                }
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
