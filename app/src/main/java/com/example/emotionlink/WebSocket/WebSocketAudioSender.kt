package com.example.emotionlink.WebSocket

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.example.emotionlink.MyApplication
import com.example.emotionlink.Utils.AudioConvert
import com.example.emotionlink.Repository.WebSocketUploader
import com.example.emotionlink.Utils.LogUtils
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean

class WebSocketAudioSender(
    private var wsClient: WebSocketUploader?,
    private var language: String = "11"
) {
    private val context= MyApplication.context
    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private val sampleRate = 16000
    private val fileLock = Any()
    private var onRecordingFinished: (() -> Unit)? = null
    
    companion object {
        private var instanceCount = 0
    }

    var currentIndex = 0
    private lateinit var audioFile: File
    lateinit var audioWavFile: File
    private lateinit var outputStream: FileOutputStream
    private var recordingThread: Thread? = null
    val tag = "socketAudioSender"

    fun setLanguage(newLang: String) {
        this.language = newLang
//        LogUtils.d(tag, "语言已更新为 $language")
    }

    fun setOnRecordingFinishedListener(listener: () -> Unit) {
        onRecordingFinished = listener
    }

    fun clearListener() {
        onRecordingFinished = null
    }

    fun startStreaming() {
        currentIndex = instanceCount++
        audioFile = File(context.filesDir, "recorded_audio_$currentIndex.pcm")
        audioWavFile = File(context.filesDir, "recorded_audio_$currentIndex.wav")
        outputStream = FileOutputStream(audioFile)
        if (wsClient == null) {
            LogUtils.d(tag, "wsClient为空")
        }
        wsClient?.sendInit()
        startAudioRecordingLoop()
    }

    private fun startAudioRecordingLoop() {
        //获取录音权限
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(context, "未获得录音权限", Toast.LENGTH_SHORT).show()
            return
        }
        recordingThread = Thread {
            try {
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
                    if (read > 0) {
                        wsClient?.sendAudioChunk(buffer, read)
                        try {
                            synchronized(fileLock) {
                                if (isRecording) {
                                    outputStream.write(buffer, 0, read)
                                }
                            }
                        } catch (e: IOException) {
                            LogUtils.e(tag, "写入已关闭的流$e")
                            break // 退出循环
                        }
                    }
                }
                audioRecord?.stop()
                audioRecord?.release()
                audioRecord = null
                
                synchronized(fileLock) {
                    try {
                        outputStream.flush()
                        outputStream.close()
                        AudioConvert.convertPcmToWav(audioFile, audioWavFile)
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            } finally {
                onRecordingFinished?.invoke()
            }
        }.also { it.start() }
    }

    fun stopStreaming() {
        isRecording = false
    }

}

