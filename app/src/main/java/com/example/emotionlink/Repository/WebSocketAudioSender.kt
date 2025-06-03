package com.example.emotionlink.Repository

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.example.emotionlink.data.AudioConvert
import kotlinx.coroutines.delay
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

class WebSocketAudioSender(
    private val context: Context,
    private var wsClient: WebSocketUploader?,
    private var inCancelZone: Boolean?,
    private var language: String = "zh"
) {

    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private val sampleRate = 16000
    private val fileLock = Any()

    companion object {
        private var instanceCount = 0
    }

    private var currentIndex = 0
    private lateinit var audioFile: File
    lateinit var audioWavFile: File
    private lateinit var outputStream: FileOutputStream

    val Tag = "socketAudioSender"

    fun setLanguage(newLang: String) {
        this.language = newLang
        Log.d(Tag, "语言已更新为 $language")
    }


    fun startStreaming() {
        Log.d(Tag, "进入 startStreaming，准备初始化 WebSocketUploader")

        currentIndex = instanceCount++
        audioFile = File(context.filesDir, "recorded_audio_$currentIndex.pcm")
        audioWavFile = File(context.filesDir, "recorded_audio_$currentIndex.wav")
        outputStream = FileOutputStream(audioFile)
        if (wsClient == null) {
            Log.d(Tag, "wsClient为空")
        }
        wsClient?.sendInit()
        startAudioRecordingLoop()
    }

    private fun startAudioRecordingLoop() {
        val startTime = System.currentTimeMillis()

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(context, "未获得录音权限", Toast.LENGTH_SHORT).show()
            return
        }
        Thread {
            val bufferSize = AudioRecord.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )*10

            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )

            val buffer = ByteArray(bufferSize)
            audioRecord?.startRecording()
            isRecording = true

            while (isRecording && wsClient != null) {
                val read= audioRecord!!.read(buffer, 0, buffer.size)
                Log.d(Tag,read.toString())
                if (read > 0) {
                    wsClient?.sendAudioChunk(buffer, read)
                    synchronized(fileLock) {
                        try {
                            outputStream.write(buffer, 0, read)
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                }
            }
            //测试用，无网络连接
//            while (isRecording) {
//                val read = audioRecord!!.read(buffer, 0, buffer.size)
//                if (read > 0) {
//                    wsClient?.sendAudioChunk(buffer, read)
//                    synchronized(fileLock) {
//                        try {
//                            outputStream.write(buffer, 0, read)
//                        } catch (e: IOException) {
//                            e.printStackTrace()
//                        }
//                    }
//                }
//            }
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
            val endTime = System.currentTimeMillis()
            val duration = "${(endTime - startTime) / 1000}\""

            wsClient?.sendEnd(duration, inCancelZone == true)
        }.start()
    }

    fun stopStreaming() {
        isRecording = false
        synchronized(fileLock) {
            outputStream.flush()
            outputStream.close()
            AudioConvert.convertPcmToWav(audioFile, audioWavFile)
        }
    }


}

