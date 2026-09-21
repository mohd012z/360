package com.msa.playback360

import java.io.File

enum class CodeBrainView360 {
 SYMMETRY_SHOT, COLLECTOR_BOX, SUBWAY_AD, PACKSHOT, WIDE_ANGLE,
 TECHNICAL_EXPLODED_VIEW, DRONE_VIEW, EXPLODED_VIEW
}

object CodeBrainVisual360 {
 fun render(file:File, r:MqlBinaryReport360, view:CodeBrainView360):String {
  val structure=ExStructure360.inspect(file)
  val methods=MqlMethodCluster360.build(file,r)
  val relations=CodeRelationGraph360.build(file,r)
  return buildString {
   appendLine("CODE BRAIN 360 • "+view.name.replace('_',' '))
   appendLine(file.name+" • "+r.kind+" • "+file.length()+" bytes")
   appendLine()
   when(view){
    CodeBrainView360.SYMMETRY_SHOT -> {
     appendLine("SYMMETRY SHOT • compare the binary around its midpoint")
     val mid=file.length()/2
     structure.regions.sortedBy{ kotlin.math.abs((it.offset+it.size/2)-mid) }.take(24).forEach{
      appendLine(region(it)+" • mirror-distance="+kotlin.math.abs((it.offset+it.size/2)-mid))
     }
    }
    CodeBrainView360.COLLECTOR_BOX -> {
     appendLine("COLLECTOR BOX • evidence grouped into inspectable containers")
     r.evidence.groupBy{it.kind}.toList().sortedByDescending{it.second.size}.forEach{(k,v)->
      appendLine("["+k+"] "+v.size+" findings")
      v.take(8).forEach{appendLine("  "+at(it.offset)+" "+it.value+" ["+it.confidence+"%]")}
     }
    }
    CodeBrainView360.SUBWAY_AD -> {
     appendLine("SUBWAY MAP • relationships shown as stations/links")
     relations.take(120).forEach{appendLine(at(it.leftOffset)+" "+it.left+" -> "+at(it.rightOffset)+" "+it.right+" • score="+it.score)}
    }
    CodeBrainView360.PACKSHOT -> {
     appendLine("PACKSHOT • compact target identity")
     appendLine("SHA-256 "+r.sha256)
     appendLine("Entropy "+"%.5f".format(r.entropy))
     appendLine("Evidence "+r.evidence.size+" • Methods "+methods.size+" • Relations "+relations.size)
     structure.regions.groupingBy{it.label}.eachCount().forEach{(k,v)->appendLine(k+" "+v)}
    }
    CodeBrainView360.WIDE_ANGLE -> {
     appendLine("WIDE ANGLE • broad structural map")
     structure.regions.take(256).forEach{appendLine(region(it))}
    }
    CodeBrainView360.TECHNICAL_EXPLODED_VIEW, CodeBrainView360.EXPLODED_VIEW -> {
     appendLine("TECHNICAL EXPLODED VIEW • layers separated without changing the target")
     appendLine("L1 CONTAINER/HEADER")
     appendLine(structure.headerHex.chunked(2).take(64).joinToString(" "))
     appendLine("L2 STRUCTURAL WINDOWS • "+structure.regions.size)
     appendLine("L3 SEMANTIC EVIDENCE • "+r.evidence.size)
     appendLine("L4 METHOD CLUSTERS • "+methods.size)
     methods.take(32).forEach{appendLine("  "+it.name+" "+at(it.startOffset)+".."+at(it.endOffset)+" ["+it.confidence+"%]")}
     appendLine("L5 RELATION EDGES • "+relations.size)
     appendLine("L6 RECONSTRUCTION • evidence-backed scaffold only")
    }
    CodeBrainView360.DRONE_VIEW -> {
     appendLine("DRONE VIEW • top-down region overview")
     structure.regions.groupBy{it.label}.forEach{(label,rows)->
      appendLine(label+" • "+rows.size+" windows • "+rows.sumOf{it.size}+" sampled bytes")
     }
     appendLine("Method anchors: "+methods.joinToString(", "){it.name+"@"+at(it.anchorOffset)})
    }
   }
   appendLine()
   append("Visual modes reorganize observed evidence; they do not expose protected or unavailable source.")
  }
 }
 private fun region(x:ExRegion360)="0x"+x.offset.toString(16).uppercase()+" +"+x.size+" "+x.label+" H="+"%.3f".format(x.entropy)+" text="+"%.1f%%".format(x.printableRatio*100)
 private fun at(x:Long?)=x?.let{"0x"+it.toString(16).uppercase()}?:"N/A"
}
