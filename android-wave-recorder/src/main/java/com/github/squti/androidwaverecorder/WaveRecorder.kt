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

import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.NoiseSuppressor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * The WaveRecorder class used to record Waveform audio file using AudioRecord class to get the audio stream in PCM encoding
 * and then convert it to WAVE file (WAV due to its filename extension) by adding appropriate headers. This class uses
 * Kotlin Coroutine with IO dispatcher to writing input data on storage asynchronously.
 * @property filePath the path of the file to be saved.
 */
class WaveRecorder(private var filePath: String) {
    /**
     * Configuration for recording audio file.
     */
    var waveConfig: WaveConfig = WaveConfig()

    /**
     * Register a callback to be invoked in every recorded chunk of audio data
     * to get max amplitude of that chunk.
     */
    var onAmplitudeListener: ((Int) -> Unit)? = null

    /**
     * Register a callback to be invoked in recording state changes
     */
    var onStateChangeListener: ((RecorderState) -> Unit)? = null

    /**
     * Activates Noise Suppressor during recording if the device implements noise
     * suppression.
     */
    var noiseSuppressorActive: Boolean = false

    /**
     * The ID of the audio session this WaveRecorder belongs to.
     * The default value is -1 which means no audio session exist.
     */
    var audioSessionId: Int = -1
        private set

    private var isRecording = false
    private var isPaused = false
    private lateinit var audioRecorder: AudioRecord
    private var noiseSuppressor: NoiseSuppressor? = null

    /**
     * Starts audio recording asynchronously and writes recorded data chunks on storage.
     */
    fun startRecording() {

        if (!isAudioRecorderInitialized()) {
            audioRecorder = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                waveConfig.sampleRate,
                waveConfig.channels,
                waveConfig.audioEncoding,
                AudioRecord.getMinBufferSize(
                    waveConfig.sampleRate,
                    waveConfig.channels,
                    waveConfig.audioEncoding
                )
            )

            audioSessionId = audioRecorder.audioSessionId

            isRecording = true

            audioRecorder.startRecording()

            if (noiseSuppressorActive) {
                noiseSuppressor = NoiseSuppressor.create(audioRecorder.audioSessionId)
            }

            onStateChangeListener?.let {
                it(RecorderState.RECORDING)
            }

            GlobalScope.launch(Dispatchers.IO) {
                writeAudioDataToStorage()
            }
        }
    }

    private suspend fun writeAudioDataToStorage() {
        val bufferSize = AudioRecord.getMinBufferSize(
            waveConfig.sampleRate,
            waveConfig.channels,
            waveConfig.audioEncoding
        )
        val data = ByteArray(bufferSize)
        val outputStream = File(filePath).outputStream()
        while (isRecording) {
            val operationStatus = audioRecorder.read(data, 0, bufferSize)

            if (AudioRecord.ERROR_INVALID_OPERATION != operationStatus) {
                if (!isPaused) outputStream.write(data)

                onAmplitudeListener?.let {

                    withContext(Dispatchers.Default) {
                        it(calculateAmplitudeMax(data))
                    }
                }


            }
        }

        outputStream.close()
        noiseSuppressor?.release()
    }

    private fun calculateAmplitudeMax(data: ByteArray): Int {
        val shortData = ShortArray(data.size / 2)
        ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
            .get(shortData)

        return shortData.max()?.toInt() ?: 0
    }

    /**
     * Stops audio recorder and release resources then writes recorded file headers.
     */
    fun stopRecording() {

        if (isAudioRecorderInitialized()) {
            isRecording = false
            audioRecorder.stop()
            audioRecorder.release()
            audioSessionId = -1
            WaveHeaderWriter(filePath, waveConfig).writeHeader()
            onStateChangeListener?.let {
                it(RecorderState.STOP)
            }
        }

    }

    private fun isAudioRecorderInitialized(): Boolean =
        this::audioRecorder.isInitialized && audioRecorder.state == AudioRecord.STATE_INITIALIZED

    fun pauseRecording() {
        isPaused = true
        onStateChangeListener?.let {
            it(RecorderState.PAUSE)
        }
    }

    fun resumeRecording() {
        isPaused = false
        onStateChangeListener?.let {
            it(RecorderState.RECORDING)
        }
    }

}