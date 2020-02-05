# Android Wave Recorder
[![](https://jitpack.io/v/squti/Android-Wave-Recorder.svg)](https://jitpack.io/#squti/Android-Wave-Recorder)
[![Android Arsenal](https://img.shields.io/badge/Android%20Arsenal-Android%20Wave%20Recorder-brightgreen.svg?style=flat)](https://android-arsenal.com/details/1/7939)

A powerful and efficient library to record WAVE form audio files (WAV) in Android
<p align="center">
  <img width="300" height="300" src="https://raw.githubusercontent.com/squti/Android-Wave-Recorder/master/static/android-wave-recorder-logo.png">
</p>

Android Wave Recorder is a lightweight library written in Kotlin to record audio files with WAVE (WAV due to its filename extension) format in Android. It's very memory efficient and easy to use library with recording customizations.

### Download
Step 1. Add this in your root (Project) build.gradle at the end of repositories:
```gradle
allprojects {
        repositories {
            ...
            maven { url "https://jitpack.io" }
        }
    }
```
Step 2. Add the dependency
```gradle
dependencies{
    implementation 'com.github.squti:Android-Wave-Recorder:1.4.0'
}
```
### Permission
Add these permissions into your `AndroidManifest.xml` and [request for them in Android 6.0+](https://developer.android.com/training/permissions/requesting.html)
```xml
<uses-permission android:name="android.permission.RECORD_AUDIO"/>
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
```

### Usage
Pass in the path of the output file to `WaveRecorder` class and call `startRecording()` like this:
```kotlin
/**
 * This path points to application cache directory.
 * you could change it based on your usage
 */
val filePath:String = externalCacheDir?.absolutePath + "/audioFile.wav"

val waveRecorder = WaveRecorder(filePath)
waveRecorder.startRecording()

```
To stop recording call `stopRecording()` function:
```kotlin
waveRecorder.stopRecording()

```

To pause and resume recording you could use `pauseRecording()` and `resumeRecording()` functions:
```kotlin
//Pause
waveRecorder.pauseRecording()

//Resume
waveRecorder.resumeRecording()

```

To activate `Noise Suppressor` you could set `noiseSuppressorActive` to true:
```kotlin
waveRecorder.noiseSuppressorActive = true

```

To listen to audio amplitude during recording you need to register a listener to `onAmplitudeListener`:
```kotlin
waveRecorder.onAmplitudeListener = {
    Log.i(TAG, "Amplitude : $it")
}
```
### Configuration
The default configuration for recording audio is like so: 

| Property | Value |
| :---: | :---: |
| sampleRate | 16000 |
| channels | AudioFormat.CHANNEL_IN_MONO |
| audioEncoding | AudioFormat.ENCODING_PCM_16BIT |

But you could change it using `waveConfig` property in `WaveRecorder` class based on your usage. This is an example:
```kotlin
val waveRecorder = WaveRecorder(filePath)
waveRecorder.waveConfig.sampleRate = 44100
waveRecorder.waveConfig.channels = AudioFormat.CHANNEL_IN_STEREO
waveRecorder.waveConfig.audioEncoding = AudioFormat.ENCODING_PCM_8BIT
waveRecorder.startRecording()
```
_Note: Wrong configuration may impacts output quality_


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

