package com.msa.playback360

import java.net.InetAddress

data class DnsEvidence360(
 val host:String,
 val addresses:List<String>,
 val canonicalNames:List<String>,
 val source:String="SYSTEM_DNS"
)

data class ConnectionContext360(
 val target:String,
 val dns:DnsEvidence360?,
 val vpnContext:String,
 val notes:List<String>
)

object ConnectionEvidence360 {
 fun resolve(host:String):DnsEvidence360?=runCatching{
  val all=InetAddress.getAllByName(host).toList()
  DnsEvidence360(
   host=host,
   addresses=all.mapNotNull{it.hostAddress}.distinct(),
   canonicalNames=all.map{it.canonicalHostName}.filter{it.isNotBlank()}.distinct()
  )
 }.getOrNull()

 fun describe(target:String,host:String,vpnActive:Boolean?):ConnectionContext360{
  val vpn=when(vpnActive){true->"VPN transport reported by Android";false->"No VPN transport reported";null->"VPN state not observed"}
  return ConnectionContext360(target,resolve(host),vpn,listOf(
   "DNS is ordinary resolver evidence only.",
   "IP addresses are not treated as proof of an origin server when CDN/proxying may exist."
  ))
 }
}
