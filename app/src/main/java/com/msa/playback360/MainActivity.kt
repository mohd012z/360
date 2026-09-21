package com.msa.playback360
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.ui.PlayerView

class MainActivity:ComponentActivity(){
 override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);setContent{
  var inspector by remember{mutableStateOf(false)}
  if(inspector){MaterialTheme{InspectorScreen{inspector=false}}}
  else{
   val player=remember{val rf=DefaultRenderersFactory(this).setEnableDecoderFallback(true);ExoPlayer.Builder(this,rf).build()}
   val trace=remember{Trace360(player)};val snap by trace.snapshot.collectAsState();var url by remember{mutableStateOf("")}
   DisposableEffect(Unit){onDispose{trace.close();player.release()}}
   MaterialTheme{Column(Modifier.fillMaxSize().padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
     Text("360 Playback Inspector",style=MaterialTheme.typography.headlineSmall)
     Button(onClick={inspector=true}){Text("CODEBRAIN")}
    }
    OutlinedTextField(url,{url=it},Modifier.fillMaxWidth(),label={Text("Stream URL (HLS/DASH/MP4)")},singleLine=true)
    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
     Button(onClick={player.setMediaItem(MediaItem.fromUri(url.trim()));player.prepare();player.play()},enabled=url.isNotBlank()){Text("Play")}
     OutlinedButton(onClick={trace.capture()}){Text("Snapshot")}
     OutlinedButton(onClick={player.stop()}){Text("Stop")}
    }
    AndroidView({PlayerView(it).apply{this.player=player}},Modifier.fillMaxWidth().weight(1f))
    Card(Modifier.fillMaxWidth()){Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){
     Text("LIVE 360 TRACE",style=MaterialTheme.typography.titleMedium)
     Text("State: "+snap.state+"   Position: "+snap.positionMs+" ms");Text("Buffered ahead: "+snap.bufferedAheadMs+" ms")
     Text("Rebuffers: "+snap.rebuffers+"   Total: "+snap.totalRebufferMs+" ms");Text("Retries: "+snap.retries)
     snap.lastError?.let{e->Text("Error: "+e)}
    }}
   }}
  }
 }}
}