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

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.NoiseSuppressor
import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.DataOutputStream
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

    /**
     * Starts audio recording asynchronously and writes recorded data chunks on storage.
     */
    fun startRecording() {
        if (!isAudioRecorderInitialized()) {
            initializeAudioRecorder()
            GlobalScope.launch(Dispatchers.IO) {
                if (waveConfig.audioEncoding == AudioFormat.ENCODING_PCM_FLOAT) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        writeFloatAudioDataToStorage()
                    } else {
                        throw UnsupportedOperationException("Float audio is not supported on this version of Android. You need Android Android 6.0 or above")
                    }
                } else
                    writeAudioDataToStorage()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun initializeAudioRecorder() {
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
    }

    private suspend fun writeAudioDataToStorage() {
        val bufferSize = AudioRecord.getMinBufferSize(
            waveConfig.sampleRate,
            waveConfig.channels,
            waveConfig.audioEncoding
        )
        val data = ByteArray(bufferSize)
        val file = File(filePath)
        val outputStream = file.outputStream()
        while (isRecording) {
            val operationStatus = audioRecorder.read(data, 0, bufferSize)

            if (AudioRecord.ERROR_INVALID_OPERATION != operationStatus) {
                if (!isPaused) outputStream.write(data)

                withContext(Dispatchers.Main) {
                    onAmplitudeListener?.let {
                        it(
                            calculateAmplitude(
                                data = data,
                                audioFormat = waveConfig.audioEncoding
                            )
                        )
                    }
                    onTimeElapsed?.let {
                        val audioLengthInSeconds = calculateElapsedTime(
                            file,
                            waveConfig
                        )
                        it(audioLengthInSeconds)
                    }
                }


            }
        }

        outputStream.close()
        noiseSuppressor?.release()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private suspend fun writeFloatAudioDataToStorage() {
        val bufferSize = AudioRecord.getMinBufferSize(
            waveConfig.sampleRate,
            waveConfig.channels,
            waveConfig.audioEncoding
        )
        val data = FloatArray(bufferSize)
        val file = File(filePath)
        val outputStream = DataOutputStream(file.outputStream())
        while (isRecording) {
            val operationStatus = audioRecorder.read(data, 0, bufferSize, AudioRecord.READ_BLOCKING)

            if (AudioRecord.ERROR_INVALID_OPERATION != operationStatus) {
                if (!isPaused) {
                    data.forEach {
                        val bytes =
                            ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putFloat(it)
                                .array()
                        outputStream.write(bytes)
                    }
                }

                withContext(Dispatchers.Main) {
                    onAmplitudeListener?.let {
                        it(
                            calculateAmplitude(data)
                        )
                    }
                    onTimeElapsed?.let {
                        val audioLengthInSeconds = calculateElapsedTime(
                            file,
                            waveConfig
                        )
                        it(audioLengthInSeconds)
                    }
                }


            }
        }

        outputStream.close()
        noiseSuppressor?.release()
    }

    private fun calculateAmplitude(data: ByteArray, audioFormat: Int): Int {
        return when (audioFormat) {
            AudioFormat.ENCODING_PCM_8BIT -> {
                val scaleFactor = 32767.0 / 255.0
                println(data.average().plus(128) * scaleFactor)
                (data.average().plus(128) * scaleFactor).toInt()
            }

            AudioFormat.ENCODING_PCM_16BIT -> {
                val shortData = ShortArray(data.size / 2)
                ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shortData)
                shortData.maxOrNull()?.toInt() ?: 0
            }

            AudioFormat.ENCODING_PCM_32BIT -> {
                val intData = IntArray(data.size / 4)
                ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).asIntBuffer().get(intData)
                val maxAmplitude = intData.maxOrNull() ?: 0
                val scaledAmplitude = ((maxAmplitude / Int.MAX_VALUE.toFloat()) * 32768).toInt()
                scaledAmplitude
            }

            else -> throw IllegalArgumentException("Unsupported audio format for encoding $audioFormat bit")
        }
    }

    private fun calculateAmplitude(data: FloatArray): Int {
        val maxFloatAmplitude = data.maxOrNull() ?: 0f
        return (maxFloatAmplitude * 32768).toInt()
    }

    private fun calculateElapsedTime(audioFile: File, waveConfig: WaveConfig): Long {
        val bytesPerSample = bitPerSample(waveConfig.audioEncoding) / 8
        val channelNumbers = when (waveConfig.channels) {
            AudioFormat.CHANNEL_IN_MONO -> 1
            AudioFormat.CHANNEL_IN_STEREO -> 2
            else -> throw IllegalArgumentException("Unsupported audio channel")
        }
        val totalSamplesRead = (audioFile.length() / bytesPerSample) / channelNumbers
        return (totalSamplesRead / waveConfig.sampleRate)
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
            isPaused = false
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
