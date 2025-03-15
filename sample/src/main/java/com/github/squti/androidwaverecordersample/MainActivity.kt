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
import android.R
import android.content.ContentUris
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.github.squti.androidwaverecorder.RecorderState
import com.github.squti.androidwaverecorder.WaveRecorder
import com.github.squti.androidwaverecordersample.databinding.ActivityMainBinding
import java.io.File
import java.util.Locale
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private val PERMISSIONS_REQUEST_RECORD_AUDIO = 77
    private val PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 88

    private lateinit var waveRecorder: WaveRecorder
    private lateinit var filePath: String
    private var isRecording = false
    private var isPaused = false
    private lateinit var binding: ActivityMainBinding
    private var selectedEncoding = AudioFormat.ENCODING_PCM_FLOAT
    private val encodingOptions = listOf(
        "8-bit" to AudioFormat.ENCODING_PCM_8BIT,
        "16-bit" to AudioFormat.ENCODING_PCM_16BIT,
        "32-bit" to AudioFormat.ENCODING_PCM_32BIT,
        "Float 32-bit" to AudioFormat.ENCODING_PCM_FLOAT
    )
    private var isSaveToExternalStorageFlag = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val encodingSpinner = binding.encodingSpinner
        val adapter = ArrayAdapter(
            this,
            R.layout.simple_spinner_item,
            encodingOptions.map { it.first } // Display names only
        )
        adapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item)

        encodingSpinner.adapter = adapter
        encodingSpinner.setSelection(encodingOptions.indexOfFirst { it.second == AudioFormat.ENCODING_PCM_FLOAT })
        encodingSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedEncoding = encodingOptions[position].second
                initRecorder(isSaveToExternalStorageFlag)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        initRecorder(isSaveToExternalStorage = false)

        binding.saveToExternalStorageSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (!isChecked) {
                resetSwitches()
                initRecorder(isSaveToExternalStorage = false)
                return@setOnCheckedChangeListener
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                resetSwitches()
                initRecorder(isSaveToExternalStorage = true)
                return@setOnCheckedChangeListener
            }

            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                resetSwitches()
                initRecorder(isSaveToExternalStorage = true)
                return@setOnCheckedChangeListener
            }

            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            ) {
                showPermissionSettingsDialog()
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE
                )
            }
        }

        binding.startStopRecordingButton.setOnClickListener {
            if (isRecording) {
                waveRecorder.stopRecording()
                return@setOnClickListener
            }

            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                waveRecorder.startRecording()
                return@setOnClickListener
            }

            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.RECORD_AUDIO
                )
            ) {
                showPermissionSettingsDialog()
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.RECORD_AUDIO),
                    PERMISSIONS_REQUEST_RECORD_AUDIO
                )
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
            if (this::waveRecorder.isInitialized) {
                if (isChecked) {
                    binding.amplitudeTextView.text = "Amplitude : 0"
                    binding.amplitudeTextView.visibility = View.VISIBLE
                    waveRecorder.onAmplitudeListener = {
                        binding.amplitudeTextView.text = "Amplitude : $it"
                    }

                } else {
                    waveRecorder.onAmplitudeListener = null
                    binding.amplitudeTextView.visibility = View.GONE
                }
            }
        }

        binding.silenceDetectionSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (this::waveRecorder.isInitialized) {
                waveRecorder.silenceDetection = isChecked
                if (isChecked)
                    Toast.makeText(this, "Silence Detection Activated", Toast.LENGTH_SHORT).show()
            }
        }

        binding.noiseSuppressorSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (this::waveRecorder.isInitialized) {
                waveRecorder.noiseSuppressorActive = isChecked

                if (isChecked) {
                    Toast.makeText(this, "Noise Suppressor Activated", Toast.LENGTH_SHORT).show()
                }
            }

        }
    }

    private fun resetSwitches() {
        binding.showAmplitudeSwitch.isChecked = false
        binding.silenceDetectionSwitch.isChecked = false
        binding.noiseSuppressorSwitch.isChecked = false
    }

    private fun initRecorder(isSaveToExternalStorage: Boolean) {
        isSaveToExternalStorageFlag = isSaveToExternalStorage
        if (isSaveToExternalStorage)
            initWithExternalStorage("audioFile")
        else
            initWithInternalStorage("audioFile")

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
        binding.encodingSpinner.isEnabled = false
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
        binding.encodingSpinner.isEnabled = false
    }

    private fun stopRecording() {
        Log.d(TAG, "Recording Stopped")
        isRecording = false
        isPaused = false
        binding.recordingTextView.visibility = View.GONE
        binding.messageTextView.visibility = View.VISIBLE
        binding.pauseResumeRecordingButton.visibility = View.GONE
        binding.startStopRecordingButton.text = "START"
        binding.noiseSuppressorSwitch.isEnabled = true
        binding.encodingSpinner.isEnabled = true
        resetSwitches()
        Toast.makeText(this, "File saved at : $filePath", Toast.LENGTH_LONG).show()
    }

    private fun pauseRecording() {
        Log.d(TAG, "Recording Paused")
        binding.recordingTextView.text = "PAUSE"
        binding.pauseResumeRecordingButton.text = "RESUME"
        isPaused = true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_RECORD_AUDIO && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            waveRecorder.startRecording()
        }
        if (requestCode == PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            resetSwitches()
            initRecorder(isSaveToExternalStorage = true)
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

    private fun initWithExternalStorage(fileName: String) {
        val folderName = "Android-Wave-Recorder"
        val audioUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val path =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).absolutePath + "/$folderName/$fileName.wav"
        val contentValues = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, "$fileName.wav")
            put(MediaStore.Audio.Media.MIME_TYPE, "audio/x-wav")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Audio.Media.RELATIVE_PATH, "Music/$folderName")
            } else {
                val file = File(path)
                val parentFile = file.parentFile
                if (parentFile != null && !parentFile.exists()) {
                    parentFile.mkdirs()
                }
                put(MediaStore.Audio.AudioColumns.DATA, path)
            }
        }

        try {
            val existingUri = contentResolver.query(
                audioUri,
                arrayOf(MediaStore.Audio.Media._ID),
                "${MediaStore.Audio.AudioColumns.DATA}=?",
                arrayOf(path),
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    ContentUris.withAppendedId(
                        audioUri,
                        cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID))
                    )
                } else {
                    null
                }
            }

            val uri = existingUri ?: contentResolver.insert(audioUri, contentValues)
            uri?.let {
                waveRecorder = WaveRecorder(it, context = this)
                    .configureWaveSettings {
                        sampleRate = 44100
                        channels = AudioFormat.CHANNEL_IN_MONO
                        audioEncoding = selectedEncoding
                    }.configureSilenceDetection {
                        minAmplitudeThreshold = 2000
                        bufferDurationInMillis = 1500
                        preSilenceDurationInMillis = 1500
                    }
                filePath = "/Music/$folderName/$fileName.wav"
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun initWithInternalStorage(fileName: String) {

        filePath = filesDir.absolutePath + "/$fileName.wav"

        waveRecorder = WaveRecorder(filePath)
            .configureWaveSettings {
                sampleRate = 44100
                channels = AudioFormat.CHANNEL_IN_MONO
                audioEncoding = selectedEncoding
            }.configureSilenceDetection {
                minAmplitudeThreshold = 2000
                bufferDurationInMillis = 1500
                preSilenceDurationInMillis = 1500
            }
    }

    private fun showPermissionSettingsDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permission Required")
            .setMessage("This app needs audio recording permission to function. Please grant the permission in the app settings.")
            .setPositiveButton("Go to Settings") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                }
                startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

}