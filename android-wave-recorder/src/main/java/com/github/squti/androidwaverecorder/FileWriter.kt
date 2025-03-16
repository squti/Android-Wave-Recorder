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

import java.io.DataOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.LinkedList

internal class FileWriter(
    private val outputStream: DataOutputStream,
    private val onAudioChunkCaptured: ((ByteArray) -> Unit)?
) {
    fun writeDataToStream(
        lastSkippedData: LinkedList<ByteArray>, data: ByteArray
    ) {
        val totalSize = lastSkippedData.sumOf { it.size } + data.size
        val byteBuffer = ByteBuffer.allocate(totalSize).order(ByteOrder.LITTLE_ENDIAN)

        lastSkippedData.forEach { byteArray ->
            byteBuffer.put(byteArray)
        }
        byteBuffer.put(data)
        lastSkippedData.clear()

        outputStream.write(byteBuffer.array())
        onAudioChunkCaptured?.let {
            it(byteBuffer.array())
        }
    }

    fun writeDataToStream(
        lastSkippedData: LinkedList<FloatArray>, data: FloatArray
    ) {
        val totalFloats = lastSkippedData.sumOf { it.size } + data.size
        val totalSize = totalFloats * 4
        val byteBuffer = ByteBuffer.allocate(totalSize).order(ByteOrder.LITTLE_ENDIAN)

        lastSkippedData.forEach { floatArray ->
            floatArray.forEach { floatValue ->
                byteBuffer.putFloat(floatValue)
            }
        }
        data.forEach { floatValue ->
            byteBuffer.putFloat(floatValue)
        }
        lastSkippedData.clear()

        outputStream.write(byteBuffer.array())

        onAudioChunkCaptured?.let {
            it(byteBuffer.array())
        }
    }

}