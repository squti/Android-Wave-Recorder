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
import android.media.AudioFormat
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.github.squti.androidwaverecorder.RecorderState
import com.github.squti.androidwaverecorder.WaveRecorder
import com.github.squti.androidwaverecordersample.databinding.ActivityMainBinding
import java.util.Locale
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private val PERMISSIONS_REQUEST_RECORD_AUDIO = 77

    private lateinit var waveRecorder: WaveRecorder
    private lateinit var filePath: String
    private var isRecording = false
    private var isPaused = false
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        filePath = filesDir.absolutePath + "/audioFile.wav"

        waveRecorder = WaveRecorder(filePath)
            .configureWaveSettings {
                sampleRate = 44100
                channels = AudioFormat.CHANNEL_IN_STEREO
                audioEncoding = AudioFormat.ENCODING_PCM_32BIT
            }.configureSilenceDetection {
                minAmplitudeThreshold = 80
                bufferDurationInMillis = 1500
                preSilenceDurationInMillis = 1500
            }



        waveRecorder.onStateChangeListener = {
            Log.d("RecorderState : ", it.name)

            when (it) {
                RecorderState.RECORDING -> startRecording()
                RecorderState.STOP -> stopRecording()
                RecorderState.PAUSE -> pauseRecording()
                RecorderState.SKIPPING_SILENCE -> skipRecording()
            }
        }
        waveRecorder.onTimeElapsedInMillis = {
            binding.timeTextView.text = formatTimeUnit(it)
        }

        binding.startStopRecordingButton.setOnClickListener {

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
                    waveRecorder.startRecording()
                }
            } else {
                waveRecorder.stopRecording()
            }
        }

        binding.pauseResumeRecordingButton.setOnClickListener {
            if (!isPaused) {
                waveRecorder.pauseRecording()
            } else {
                waveRecorder.resumeRecording()
            }
        }
        binding.showAmplitudeSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.amplitudeTextView.text = "Amplitude : 0"
                binding.amplitudeTextView.visibility = View.VISIBLE
                waveRecorder.onAmplitudeListener = {
                    Log.d("Amplitude", "Amplitude : $it")
                    binding.amplitudeTextView.text = "Amplitude : $it"
                }

            } else {
                waveRecorder.onAmplitudeListener = null
                binding.amplitudeTextView.visibility = View.GONE
            }
        }

        binding.silenceDetectionSwitch.setOnCheckedChangeListener { _, isChecked ->
            waveRecorder.silenceDetection = isChecked
            if (isChecked)
                Toast.makeText(this, "Noise Suppressor Activated", Toast.LENGTH_SHORT).show()

        }

        binding.noiseSuppressorSwitch.setOnCheckedChangeListener { _, isChecked ->
            waveRecorder.noiseSuppressorActive = isChecked
            if (isChecked)
                Toast.makeText(this, "Noise Suppressor Activated", Toast.LENGTH_SHORT).show()

        }
    }

    private fun startRecording() {
        Log.d(TAG, "Recording Started")
        isRecording = true
        isPaused = false
        binding.messageTextView.visibility = View.GONE
        binding.recordingTextView.text = "Recording..."
        binding.recordingTextView.visibility = View.VISIBLE
        binding.startStopRecordingButton.text = "STOP"
        binding.pauseResumeRecordingButton.text = "PAUSE"
        binding.pauseResumeRecordingButton.visibility = View.VISIBLE
        binding.noiseSuppressorSwitch.isEnabled = false
    }

    private fun skipRecording() {
        Log.d(TAG, "Recording Skipped")
        isRecording = true
        isPaused = false
        binding.messageTextView.visibility = View.GONE
        binding.recordingTextView.text = "Skipping..."
        binding.recordingTextView.visibility = View.VISIBLE
        binding.startStopRecordingButton.text = "STOP"
        binding.pauseResumeRecordingButton.visibility = View.INVISIBLE
        binding.noiseSuppressorSwitch.isEnabled = false
    }

    private fun stopRecording() {
        Log.d(TAG, "Recording Stopped")
        isRecording = false
        isPaused = false
        binding.recordingTextView.visibility = View.GONE
        binding.messageTextView.visibility = View.VISIBLE
        binding.pauseResumeRecordingButton.visibility = View.GONE
        binding.showAmplitudeSwitch.isChecked = false
        Toast.makeText(this, "File saved at : $filePath", Toast.LENGTH_LONG).show()
        binding.startStopRecordingButton.text = "START"
        binding.noiseSuppressorSwitch.isEnabled = true
    }

    private fun pauseRecording() {
        Log.d(TAG, "Recording Paused")
        binding.recordingTextView.text = "PAUSE"
        binding.pauseResumeRecordingButton.text = "RESUME"
        isPaused = true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSIONS_REQUEST_RECORD_AUDIO -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    waveRecorder.startRecording()
                }
                return
            }

            else -> {
            }
        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }

    private fun formatTimeUnit(timeInMilliseconds: Long): String {
        return try {
            String.format(
                Locale.getDefault(),
                "%02d:%02d:%03d",
                TimeUnit.MILLISECONDS.toMinutes(timeInMilliseconds),
                TimeUnit.MILLISECONDS.toSeconds(timeInMilliseconds) - TimeUnit.MINUTES.toSeconds(
                    TimeUnit.MILLISECONDS.toMinutes(timeInMilliseconds)
                ),
                timeInMilliseconds % 1000
            )
        } catch (e: Exception) {
            "00:00:000"
        }
    }
}