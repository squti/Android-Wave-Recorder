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

import android.content.Context
import android.media.AudioFormat
import android.net.Uri
import java.io.File
import java.io.FileInputStream
import java.io.RandomAccessFile

class WaveHeaderWriter {
    private var fileUri: Uri? = null
    private var filePath: String? = null
    private lateinit var context: Context
    private var waveConfig: WaveConfig

    constructor(fileUri: Uri, context: Context, waveConfig: WaveConfig) {
        this.fileUri = fileUri
        this.context = context
        this.waveConfig = waveConfig
    }

    constructor(filePath: String, waveConfig: WaveConfig) {
        this.filePath = filePath
        this.waveConfig = waveConfig
    }

    fun writeHeader() {
        val inputStream: FileInputStream = if (fileUri != null) {
            val fileDescriptor =
                context.contentResolver.openFileDescriptor(fileUri!!, "rw")?.fileDescriptor
            FileInputStream(fileDescriptor)
        } else {
            File(filePath!!).inputStream()
        }
        val totalAudioLen = inputStream.channel.size() - 44
        val totalDataLen = totalAudioLen + 36
        val channels = if (waveConfig.channels == AudioFormat.CHANNEL_IN_MONO)
            1
        else
            2

        val sampleRate = waveConfig.sampleRate.toLong()
        val byteRate =
            (bitPerSample(waveConfig.audioEncoding) * waveConfig.sampleRate * channels / 8).toLong()
        val header = getWavFileHeaderByteArray(
            totalAudioLen,
            totalDataLen,
            sampleRate,
            channels,
            byteRate,
            bitPerSample(waveConfig.audioEncoding),
            waveConfig.audioEncoding == AudioFormat.ENCODING_PCM_FLOAT
        )

        if (fileUri != null) {
            val outputStream = context.contentResolver.openOutputStream(fileUri!!, "rw")
            outputStream?.write(header)
            outputStream?.close()
        } else {
            val randomAccessFile = RandomAccessFile(File(filePath!!), "rw")
            randomAccessFile.seek(0)
            randomAccessFile.write(header)
            randomAccessFile.close()
        }
    }

    private fun getWavFileHeaderByteArray(
        totalAudioLen: Long, totalDataLen: Long, longSampleRate: Long,
        channels: Int, byteRate: Long, bitsPerSample: Int, isFloat: Boolean
    ): ByteArray {
        val header = ByteArray(44)
        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        header[4] = (totalDataLen and 0xff).toByte()
        header[5] = (totalDataLen shr 8 and 0xff).toByte()
        header[6] = (totalDataLen shr 16 and 0xff).toByte()
        header[7] = (totalDataLen shr 24 and 0xff).toByte()
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        header[16] = 16
        header[17] = 0
        header[18] = 0
        header[19] = 0
        header[20] = if (isFloat) 3 else 1
        header[21] = 0
        header[22] = channels.toByte()
        header[23] = 0
        header[24] = (longSampleRate and 0xff).toByte()
        header[25] = (longSampleRate shr 8 and 0xff).toByte()
        header[26] = (longSampleRate shr 16 and 0xff).toByte()
        header[27] = (longSampleRate shr 24 and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte()
        header[29] = (byteRate shr 8 and 0xff).toByte()
        header[30] = (byteRate shr 16 and 0xff).toByte()
        header[31] = (byteRate shr 24 and 0xff).toByte()
        header[32] = (channels * bitsPerSample / 8).toByte()
        header[33] = 0
        header[34] = bitsPerSample.toByte()
        header[35] = 0
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        header[40] = (totalAudioLen and 0xff).toByte()
        header[41] = (totalAudioLen shr 8 and 0xff).toByte()
        header[42] = (totalAudioLen shr 16 and 0xff).toByte()
        header[43] = (totalAudioLen shr 24 and 0xff).toByte()
        return header
    }
}