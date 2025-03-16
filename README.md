# Android Wave Recorder
[![](https://jitpack.io/v/squti/Android-Wave-Recorder.svg)](https://jitpack.io/#squti/Android-Wave-Recorder)

A powerful and efficient library to record WAVE form audio files (WAV) in Android with Float and 32-bit encoding support.
<p align="center">
  <img width="300" height="300" src="https://raw.githubusercontent.com/squti/Android-Wave-Recorder/master/static/android-wave-recorder-logo.png">
</p>

Android Wave Recorder is a lightweight library written in Kotlin to record audio files in WAVE (WAV) format on Android. It’s memory efficient and easy to use, with customizable recording options like Silence Detection and high-quality audio encoding (Float and 32-bit)

### Download
Step 1. Add this to your root (Project) `build.gradle` at the end of repositories:
```gradle
allprojects {
        repositories {
            ...
            maven { url "https://jitpack.io" }
        }
    }
```
Step 2. Add the following dependency to your module `build.gradle`:
```gradle
dependencies{
    implementation 'com.github.squti:Android-Wave-Recorder:2.1.0'
}
```
### Permission
Add these permissions to your `AndroidManifest.xml` and [request them at runtime for Android 6.0+](https://developer.android.com/training/permissions/requesting.html)
```xml
<uses-permission android:name="android.permission.RECORD_AUDIO"/>
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
```
If you use [Scoped Storage](https://source.android.com/docs/core/storage/scoped) there is an example in the [Sample](https://github.com/squti/Android-Wave-Recorder/tree/master/sample) project.
### Usage
Pass the path of the output file to the `WaveRecorder` class and call `startRecording()`:
```kotlin
/**
 * This path points to the file directory in the application's internal storage.
 * you can change it based on your usage
 */
val filePath:String = filesDir.absolutePath + "/audioFile.wav"

val waveRecorder = WaveRecorder(filePath)
waveRecorder.startRecording()

```
You can also pass a `URI` for the file path if you are dealing with [Scoped Storage](https://source.android.com/docs/core/storage/scoped):
```kotlin
val audioUri = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
val contentValues = ContentValues().apply {
    put(MediaStore.Audio.Media.DISPLAY_NAME, "audioFile.wav")
    put(MediaStore.Audio.Media.MIME_TYPE, "audio/x-wav")
    put(MediaStore.Audio.Media.RELATIVE_PATH, "Music/Android-Wave-Recorder")
}
/**
 * This URI points to audioFile.wav in the Android-Wave-Recorder directory in Android's default Music folder.
 * you can change it based on your usage
 */
val uri = contentResolver.insert(audioUri, contentValues)

val waveRecorder = WaveRecorder(uri, context = this)
waveRecorder.startRecording()

```
_[Here](https://github.com/squti/Android-Wave-Recorder/blob/master/sample/src/main/java/com/github/squti/androidwaverecordersample/MainActivity.kt) is an example of how to get URI before and after Android 10._

To stop recording, call the `stopRecording()` function:
```kotlin
waveRecorder.stopRecording()

```

To pause and resume recording, use the `pauseRecording()` and `resumeRecording()` functions:
```kotlin
//Pause
waveRecorder.pauseRecording()

//Resume
waveRecorder.resumeRecording()

```

To listen to audio amplitude during recording, register a listener to `onAmplitudeListener`:
```kotlin
waveRecorder.onAmplitudeListener = {
    Log.i(TAG, "Amplitude : $it")
}
```

To capture audio data chunks during recording, register a listener to `onAudioChunkCaptured`:
```kotlin
waveRecorder.onAudioChunkCaptured = { dataChunk ->
    //doProcess(dataChunk)
}
```
### Silence Detection
To activate the *Silence Detection*, set `silenceDetection` to `true`:
```kotlin
waveRecorder.silenceDetection = true

```
You can adjust the silence amplitude level based on your needs. The default threshold is 1500, meaning amplitudes below 1500 are considered silence. The recorder will pause until the amplitude goes above 1500 again. By default, the recorder buffers the last 2 seconds of silence and adds it to the file when recording resumes. Silence detection waits for 2 seconds after detecting silence; if silence continues, recording pauses. The recorder will resume when it detects sound again. You can adjust the **buffer time, silence waiting time, and amplitude threshold** using `configureSilenceDetection`:
```kotlin
waveRecorder.configureSilenceDetection {
    minAmplitudeThreshold = 2000
    bufferDurationInMillis = 1500
    preSilenceDurationInMillis = 1500
}

```
_Note 1: Buffer and Silence Waiting Time may have slight inaccuracies due to the conversion from milliseconds to bytes in the background. Adjust these settings to achieve more accurate results._

_Note 2: Big buffer size can reduce the performance_

### Noise Suppression
To activate the `Noise Suppressor`, set `noiseSuppressorActive` to `true`:
```kotlin
waveRecorder.noiseSuppressorActive = true

```
_Note: If the device does not support Noise Suppressor it will not affect the output_

### Recording States
To listen to recording state changes **(RECORDING, STOP, PAUSE and SKIPPING SILENCE)**, register a listener to `onStateChangeListener`:
```kotlin
waveRecorder.onStateChangeListener = {
    when (it) {
        RecorderState.RECORDING -> TODO()
        RecorderState.STOP -> TODO()
        RecorderState.PAUSE -> TODO()
        RecorderState.SKIPPING_SILENCE -> TODO()
    }
}
```
### Configuration
Android Wave Recorder supports **Float, 32-bit, 16-bit and 8-bit** encoding.

The default configuration for recording audio is:

| Property | Value |
| :---: | :---: |
| sampleRate | 16000 |
| channels | AudioFormat.CHANNEL_IN_MONO |
| audioEncoding | AudioFormat.ENCODING_PCM_16BIT |

You can change the configuration using `configureWaveSettings`:
```kotlin
waveRecorder.configureWaveSettings {
    sampleRate = 44100
    channels = AudioFormat.CHANNEL_IN_STEREO
    audioEncoding = AudioFormat.ENCODING_PCM_FLOAT
}
```

### License
```
MIT License

Copyright (c) 2019 squti

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

