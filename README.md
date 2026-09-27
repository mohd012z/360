# Playback 360

Android media playback studio with live 360° trace diagnostics.

- Plays HLS / DASH / MP4 streams via Media3 (ExoPlayer)
- Live trace: buffer state, position, rebuffers, retry/error events
- CodeBrain inspector panels: architecture, crypto, encoding, deep-dive, binary inspection
- Kotlin + Jetpack Compose (Material 3)

## Build

```bash
./gradlew :app:assembleDebug
```

Requires the Android SDK (minSdk 24, targetSdk 35).
