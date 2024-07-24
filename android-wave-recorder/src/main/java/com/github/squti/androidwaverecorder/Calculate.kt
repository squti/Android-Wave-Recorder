/*
 * MIT License
 *
 * Copyright (c) 2024 squti
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
import android.media.AudioRecord
import java.io.File
import java.math.BigDecimal
import java.math.MathContext
import java.nio.ByteBuffer
import java.nio.ByteOrder


internal fun calculateMinBufferSize(waveConfig: WaveConfig): Int {
    return AudioRecord.getMinBufferSize(
        waveConfig.sampleRate,
        waveConfig.channels,
        waveConfig.audioEncoding
    )
}

internal fun calculateAmplitude(data: ByteArray, audioFormat: Int): Int {
    return when (audioFormat) {
        AudioFormat.ENCODING_PCM_8BIT -> {
            val scaleFactor = 32767.0 / 255.0
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

        else -> throw IllegalArgumentException("Unsupported audio format for encoding $audioFormat")
    }
}

internal fun calculateAmplitude(data: FloatArray): Int {
    val maxFloatAmplitude = data.maxOrNull() ?: 0f
    return (maxFloatAmplitude * 32768).toInt()
}

internal fun calculateDurationInMillis(data: ByteArray, waveConfig: WaveConfig): BigDecimal {
    return when (waveConfig.audioEncoding) {
        AudioFormat.ENCODING_PCM_8BIT -> {
            BigDecimal(data.size).divide(
                BigDecimal(1 * channelCount(waveConfig.channels) * waveConfig.sampleRate),
                MathContext.DECIMAL64
            ) * BigDecimal(1000)
        }

        AudioFormat.ENCODING_PCM_16BIT -> {
            BigDecimal(data.size).divide(
                BigDecimal(2 * channelCount(waveConfig.channels) * waveConfig.sampleRate),
                MathContext.DECIMAL64
            ) * BigDecimal(1000)
        }

        AudioFormat.ENCODING_PCM_32BIT -> {
            BigDecimal(data.size).divide(
                BigDecimal(4 * channelCount(waveConfig.channels) * waveConfig.sampleRate),
                MathContext.DECIMAL64
            ) * BigDecimal(1000)
        }

        else -> throw IllegalArgumentException("Unsupported audio format for encoding ${waveConfig.audioEncoding}")
    }
}

internal fun calculateDurationInMillis(data: FloatArray, waveConfig: WaveConfig): BigDecimal {
    return BigDecimal(data.size).divide(
        BigDecimal(channelCount(waveConfig.channels) * waveConfig.sampleRate),
        MathContext.DECIMAL64
    ) * BigDecimal(1000)
}

internal fun calculateDurationInMillis(audioFile: File, waveConfig: WaveConfig): Long {
    val bytesPerSample = bitPerSample(waveConfig.audioEncoding) / 8
    val totalSamplesRead =
        (audioFile.length() / bytesPerSample) / channelCount(waveConfig.channels)
    return (totalSamplesRead * 1000 / waveConfig.sampleRate)
}