/*
 * MIT License
 *
 * Copyright (c) 2019 squti
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.github.squti.androidwaverecorder

import android.media.AudioFormat
import android.media.MediaRecorder
import android.os.Build
import android.util.Log

/**
 * Configuration for recording file.
 * @property [sampleRate] the number of samples that audio carried per second.
 * @property [channels] number and position of sound source when the sound is recording.
 * @property [audioEncoding] size of data per sample.
 * @property [audioSource] the input source for the audio recorder
 */
data class WaveConfig(
    var sampleRate: Int = 16000,
    var channels: Int = AudioFormat.CHANNEL_IN_MONO,
    var audioEncoding: Int = AudioFormat.ENCODING_PCM_16BIT,
    var audioSource: Int = MediaRecorder.AudioSource.MIC
)

internal fun bitPerSample(audioEncoding: Int) = when (audioEncoding) {
    AudioFormat.ENCODING_PCM_8BIT -> 8
    AudioFormat.ENCODING_PCM_16BIT -> 16
    AudioFormat.ENCODING_PCM_32BIT -> 32
    AudioFormat.ENCODING_PCM_FLOAT -> 32
    else -> throw IllegalArgumentException("Unsupported audio format for encoding $audioEncoding")
}

internal fun channelCount(channels: Int) = when (channels) {
    AudioFormat.CHANNEL_IN_MONO -> 1
    AudioFormat.CHANNEL_IN_STEREO -> 2
    else -> throw IllegalArgumentException("Unsupported audio channel")
}

internal fun validateAudioSource(audioSource: Int) {
    when (audioSource) {
        MediaRecorder.AudioSource.DEFAULT,
        MediaRecorder.AudioSource.MIC,
        MediaRecorder.AudioSource.VOICE_UPLINK,
        MediaRecorder.AudioSource.VOICE_DOWNLINK,
        MediaRecorder.AudioSource.VOICE_CALL,
        MediaRecorder.AudioSource.CAMCORDER,
        MediaRecorder.AudioSource.VOICE_RECOGNITION,
        MediaRecorder.AudioSource.VOICE_COMMUNICATION,
        MediaRecorder.AudioSource.REMOTE_SUBMIX -> {
            Log.d("WaveConfig", "Using valid audio source: $audioSource")
        }

        MediaRecorder.AudioSource.UNPROCESSED,
        MediaRecorder.AudioSource.VOICE_PERFORMANCE -> {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                throw IllegalArgumentException("AudioSource $audioSource requires API level 19+")
            }
        }

        else -> throw IllegalArgumentException("Unsupported MediaRecorder.AudioSource: $audioSource")
    }
}