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

package com.github.squti.androidwaverecordersample

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.github.squti.androidwaverecorder.WaveRecorder
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val PERMISSIONS_REQUEST_RECORD_AUDIO = 77

    private lateinit var waveRecorder: WaveRecorder
    private lateinit var filePath: String
    private var isRecording = false
    private var isPaused = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        filePath = externalCacheDir?.absolutePath + "/audioFile.wav"

        waveRecorder = WaveRecorder(filePath)

        startStopRecordingButton.setOnClickListener {

            if (!isRecording) {
                if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.RECORD_AUDIO
                    )
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.RECORD_AUDIO),
                        PERMISSIONS_REQUEST_RECORD_AUDIO
                    )
                } else {
                    startRecording()
                }
            } else {
                stopRecording()
            }
        }

        pauseResumeRecordingButton.setOnClickListener {
            if (!isPaused) {
                pauseRecording()
            } else {
                resumeRecording()
            }
        }
        showAmplitudeSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                amplitudeTextView.text = "Amplitude : 0"
                amplitudeTextView.visibility = View.VISIBLE
                waveRecorder.onAmplitudeListener = {
                    GlobalScope.launch(Dispatchers.Main) {
                        amplitudeTextView.text = "Amplitude : $it"
                    }
                }

            } else {
                waveRecorder.onAmplitudeListener = null
                amplitudeTextView.visibility = View.GONE
            }
        }

        noiseSuppressorSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            waveRecorder.noiseSuppressorActive = isChecked
            if (isChecked)
                Toast.makeText(this, "Noise Suppressor Activated", Toast.LENGTH_SHORT).show()

        }
    }

    private fun startRecording() {
        isRecording = true
        waveRecorder.startRecording()
        messageTextView.visibility = View.GONE
        recordingTextView.text = "Recording..."
        recordingTextView.visibility = View.VISIBLE
        startStopRecordingButton.text = "STOP"
        pauseResumeRecordingButton.text = "PAUSE"
        pauseResumeRecordingButton.visibility = View.VISIBLE
        noiseSuppressorSwitch.isEnabled = false
    }

    private fun stopRecording() {
        isRecording = false
        waveRecorder.stopRecording()
        recordingTextView.visibility = View.GONE
        messageTextView.visibility = View.VISIBLE
        pauseResumeRecordingButton.visibility = View.GONE
        showAmplitudeSwitch.isChecked = false
        Toast.makeText(this, "File saved at : $filePath", Toast.LENGTH_LONG).show()
        startStopRecordingButton.text = "START"
        noiseSuppressorSwitch.isEnabled = true
    }

    private fun pauseRecording() {
        recordingTextView.text = "PAUSE"
        pauseResumeRecordingButton.text = "RESUME"
        isPaused = true
        waveRecorder.pauseRecording()
    }

    private fun resumeRecording() {
        recordingTextView.text = "Recording..."
        pauseResumeRecordingButton.text = "PAUSE"
        isPaused = false
        waveRecorder.resumeRecording()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        when (requestCode) {
            PERMISSIONS_REQUEST_RECORD_AUDIO -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    startRecording()
                }
                return
            }

            else -> {
            }
        }
    }
}
