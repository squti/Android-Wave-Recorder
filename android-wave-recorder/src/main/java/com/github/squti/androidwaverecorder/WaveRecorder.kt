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
import android.util.Log
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
     * Register a callback to get elapsed recording time in seconds
     */
    var onTimeElapsed: ((Long) -> Unit)? = null

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
    var skipSilence: Boolean = false

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
        var continuousSilence: Int = 0
        val bufferSize = AudioRecord.getMinBufferSize(
            waveConfig.sampleRate,
            waveConfig.channels,
            waveConfig.audioEncoding
        )
        val data = ShortArray(bufferSize)
        val file = File(filePath)
        val outputStream = file.outputStream()
        while (isRecording) {
            val operationStatus = audioRecorder.read(data, 0, bufferSize)

            if (AudioRecord.ERROR_INVALID_OPERATION != operationStatus) {
                if (!isPaused) {
                    if (skipSilence) {
                        val foundPeak = searchThreshold(data, waveConfig.threshold)
                        if (foundPeak == -1) {
                            if (continuousSilence <= waveConfig.max_continuous_silence_allowed) {
                                continuousSilence++
                            }

                        } else {
                            continuousSilence = 0
                        }
                        Log.d(
                            "Wacerecorder",
                            "writeAudioDataToStorage: continuousSilence $continuousSilence"
                        )
                        //stop to send, only when counter became equals SILENCE_DEGREE
                        if (continuousSilence < waveConfig.max_continuous_silence_allowed) {  //found signal
                            //SEND USEFUL DATA
                            outputStream.write(data.toByteArray())  //record signal
                        }
                    } else {
                        outputStream.write(data.toByteArray())
                    }

                }

                withContext(Dispatchers.Main) {
                    onAmplitudeListener?.let {
                        it(calculateAmplitudeMax(data))
                    }
                    onTimeElapsed?.let {
                        val audioLengthInSeconds: Long = file.length() / (2 * waveConfig.sampleRate)
                        it(audioLengthInSeconds)
                    }
                }


            }
        }

        outputStream.close()
        noiseSuppressor?.release()
    }

    fun ShortArray.toByteArray(): ByteArray {
        val buffer = ByteBuffer.allocate(size * 2)
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        buffer.asShortBuffer().put(this)
        return buffer.array()
    }

    private fun searchThreshold(arr: ShortArray, thr: Short): Int {
        val arrLen = arr.size
        var peakIndex = 0
        while (peakIndex < arrLen) {
            if (arr[peakIndex] >= thr || arr[peakIndex] <= -thr) {
                //if it exceeds the threshold, exit and return peakindex-half kernel.
                return peakIndex
            }
            peakIndex++
        }
        return -1 //not found
    }

    private fun calculateAmplitudeMax(data: ShortArray): Int {
        return data.max()?.toInt() ?: 0
    }

    /** Changes @property filePath to @param newFilePath
     * Calling this method while still recording throws an IllegalStateException
     */
    fun changeFilePath(newFilePath: String) {
        if (isRecording)
            throw IllegalStateException("Cannot change filePath when still recording.")
        else
            filePath = newFilePath
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
