package com.msa.playback360

import java.io.File

/**
 * APK-side bridge for a Python-compatible extraction pipeline.
 * The Android build stays dependency-light: it produces a deterministic Python
 * extraction script and Evidence360/MQ4-text plan without executing the target.
 * A future embedded Python runtime can execute this script in the app sandbox.
 */
object PythonExtractionBridge360 {
 fun render(file:File,r:MqlBinaryReport360):String=buildString{
  appendLine("APK • PYTHON • EX4/EX5 EXTRACTION")
  appendLine("Target: "+file.name+" • "+r.kind+" • "+file.length()+" bytes")
  appendLine("Mode: static/read-only")
  appendLine("Pipeline:")
  appendLine("  1 raw bytes + SHA-256")
  appendLine("  2 multi-window entropy: 64/256/1024/4096/16384")
  appendLine("  3 ASCII + UTF-8 + UTF-16LE/BE candidates")
  appendLine("  4 numeric float/double candidates")
  appendLine("  5 embedded signatures/compression validation")
  appendLine("  6 built-in MQL code-library classification")
  appendLine("  7 adaptive regions + relationships")
  appendLine("  8 Evidence360 JSON")
  appendLine("  9 evidence-backed MQ4/MQ5 text scaffold")
  appendLine()
  appendLine("Built-in symbols: "+MqlCodeLibrary360.symbols.size)
  appendLine("Observed evidence: "+r.evidence.size)
  appendLine("Output: evidence360.json + reconstructed."+(if(r.kind==MqlBinaryKind.EX4)"mq4" else "mq5")+".txt")
  append("No protection bypass or claim of exact original source.")
 }

 fun pythonScript():String = """
import hashlib, json, math, re, struct, sys
from pathlib import Path
WINDOWS=(64,256,1024,4096,16384)

def entropy(b):
    if not b: return 0.0
    counts=[0]*256
    for x in b: counts[x]+=1
    n=len(b)
    return -sum((c/n)*math.log2(c/n) for c in counts if c)

def strings(data, minimum=5):
    out=[]
    for m in re.finditer(rb'[ -~]{%d,}'%minimum,data):
        out.append({"offset":m.start(),"encoding":"ASCII","value":m.group().decode("ascii","replace")})
    return out

def regions(data):
    result=[]
    for w in WINDOWS:
        for off in range(0,len(data),w):
            b=data[off:off+w]
            if not b: break
            result.append({"offset":off,"size":len(b),"window":w,"entropy":round(entropy(b),5),
                           "zero_ratio":round(b.count(0)/len(b),5)})
    return result

def main(path):
    p=Path(path); data=p.read_bytes()
    report={"schema":"Evidence360-Python-1","file":p.name,"size":len(data),
            "sha256":hashlib.sha256(data).hexdigest(),
            "strings":strings(data),"regions":regions(data)}
    print(json.dumps(report,ensure_ascii=False,separators=(',',':')))

if __name__=="__main__":
    main(sys.argv[1])
""".trimIndent()
}
