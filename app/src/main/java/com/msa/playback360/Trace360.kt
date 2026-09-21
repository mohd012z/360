package com.msa.playback360
import android.os.SystemClock
import androidx.media3.common.Player
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class TraceSnapshot(val timeMs:Long=System.currentTimeMillis(),val state:String="IDLE",val positionMs:Long=0,val bufferedAheadMs:Long=0,val rebuffers:Int=0,val totalRebufferMs:Long=0,val retries:Int=0,val lastError:String?=null)

class Trace360(private val player:Player):Player.Listener {
 private val _snapshot=MutableStateFlow(TraceSnapshot()); val snapshot=_snapshot.asStateFlow()
 private var rebufferStart:Long?=null; private var rebufferCount=0; private var totalRebuffer=0L
 init { player.addListener(this) }
 override fun onPlaybackStateChanged(state:Int){
  if(state==Player.STATE_BUFFERING && player.playWhenReady && rebufferStart==null){rebufferStart=SystemClock.elapsedRealtime();rebufferCount++}
  else if(state==Player.STATE_READY && rebufferStart!=null){totalRebuffer+=SystemClock.elapsedRealtime()-rebufferStart!!;rebufferStart=null}
  capture()
 }
 override fun onPlayerError(error:androidx.media3.common.PlaybackException){capture(error.errorCodeName+": "+(error.message?:""))}
 fun capture(error:String?=_snapshot.value.lastError){
  val pos=player.currentPosition.coerceAtLeast(0)
  _snapshot.value=TraceSnapshot(state=when(player.playbackState){Player.STATE_BUFFERING->"BUFFERING";Player.STATE_READY->"READY";Player.STATE_ENDED->"ENDED";else->"IDLE"},positionMs=pos,bufferedAheadMs=(player.bufferedPosition-pos).coerceAtLeast(0),rebuffers=rebufferCount,totalRebufferMs=totalRebuffer,retries=_snapshot.value.retries,lastError=error)
 }
 fun markRetry(){_snapshot.value=_snapshot.value.copy(retries=_snapshot.value.retries+1)}
 fun close()=player.removeListener(this)
}