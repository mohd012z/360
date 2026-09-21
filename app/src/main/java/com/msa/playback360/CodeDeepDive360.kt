package com.msa.playback360

import java.io.File

data class CodeModel360(
    val identity: String,
    val regions: Int,
    val highEntropy: Int,
    val textEvidence: Int,
    val semanticEvidence: Int,
    val numericCandidates: Int,
    val verified: Int,
    val uncertain: Int
)

object CodeDeepDive360 {
    fun model(file: File, report: MqlBinaryReport360): CodeModel360 {
        val structure = ExStructure360.inspect(file)
        val numeric = NumericEvidenceScanner360.scan(file)
        val semanticKinds = setOf(
            MqlObjectKind.MQL_EVENT,
            MqlObjectKind.TRADING_API,
            MqlObjectKind.INDICATOR,
            MqlObjectKind.DLL,
            MqlObjectKind.URL
        )
        val semantic = report.evidence.count { it.kind in semanticKinds }
        val verified = report.evidence.count { it.confidence >= 90 }
        return CodeModel360(
            identity = report.kind.toString() + ":" + report.fileName,
            regions = structure.regions.size,
            highEntropy = structure.regions.count { it.label == "HIGH_ENTROPY" },
            textEvidence = report.strings,
            semanticEvidence = semantic,
            numericCandidates = numeric.size,
            verified = verified,
            uncertain = report.evidence.size - verified
        )
    }

    fun renderModel(model: CodeModel360): String = buildString {
        appendLine("CODE MODEL")
        appendLine(model.identity)
        appendLine("Regions: ${model.regions}")
        appendLine("High entropy: ${model.highEntropy}")
        appendLine("Text evidence: ${model.textEvidence}")
        appendLine("Semantic evidence: ${model.semanticEvidence}")
        appendLine("Numeric candidates: ${model.numericCandidates}")
        appendLine("High-confidence evidence: ${model.verified}")
        append("Uncertain evidence: ${model.uncertain}")
    }

    fun evidence(report: MqlBinaryReport360): List<MqlEvidence360> =
        report.evidence.sortedWith(
            compareByDescending<MqlEvidence360> { it.confidence }
                .thenBy { it.offset ?: Long.MAX_VALUE }
        )

    fun verify(file: File, report: MqlBinaryReport360): String {
        val structure = ExStructure360.inspect(file)
        val problems = mutableListOf<String>()
        if (report.size != file.length()) problems += "Reported size differs from file size."
        if (report.sha256.length != 64) problems += "SHA-256 identity is invalid."

        val badOffsets = report.evidence.count { evidence ->
            val offset = evidence.offset
            offset != null && (offset < 0L || offset >= report.size)
        }
        if (badOffsets > 0) problems += "$badOffsets evidence offsets are outside file bounds."

        return if (problems.isEmpty()) {
            buildString {
                appendLine("VERIFY")
                appendLine("Identity: PASS")
                appendLine("SHA-256: ${report.sha256}")
                appendLine("Evidence offsets checked: ${report.evidence.size}")
                appendLine("High-entropy regions: ${structure.regions.count { it.label == "HIGH_ENTROPY" }}/${structure.regions.size}")
                appendLine("No structural consistency errors detected.")
                append("\nVerification confirms evidence consistency only; it does not prove recovery of original source.")
            }
        } else {
            "VERIFY\nIdentity: CHECK\n" + problems.joinToString("\n")
        }
    }

    fun extract(file: File, report: MqlBinaryReport360): String =
        renderModel(model(file, report)) +
            "\n\nEXTRACTOR LAYERS\n" +
            "1 identity/hash\n2 region statistics\n3 ASCII/Unicode evidence\n" +
            "4 numeric candidates\n5 MQL semantic classification\n6 DLL/URL references\n" +
            "7 offset/proximity relationships\n8 reconstructed source model"

    fun cli(): String =
        "MQL360 COMMAND PIPELINE\n/codebase\n/codeextractor\n/codemodel\n/codeevidence\n" +
            "/codeverify\n/codenumeric\n/codeunicode\n/structure\n/codereconstruct mq4|mq5\n" +
            "/codegrep <text>\n/codeoffset <0xOFFSET>\n/coderelated <text>\n/code*\n\n" +
            "All operations are read-only analysis; reconstruction is evidence-backed."

    fun catalog(): String =
        (MqlCodeLibrary360.commands.map { it.command + "  [" + it.domain + "] " + it.action } +
            listOf(
                "/codebase", "/codeextractor", "/codemodel", "/codeevidence", "/codeverify",
                "/codecli", "/codeconstants", "/codenumeric", "/codestructure", "/coderegions",
                "/codereconstruct", "/codecompare", "/codediff", "/codemap", "/coderelationships",
                "/codedeepreconstruct"
            )).distinct().sorted().joinToString("\n")
}
