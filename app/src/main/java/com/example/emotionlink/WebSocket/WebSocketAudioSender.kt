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
import com.example.emotionlink.AudioManager.AudioConvert
import com.example.emotionlink.Repository.WebSocketUploader
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class WebSocketAudioSender(
    private val context: Context,
    private var wsClient: WebSocketUploader?,
    private var inCancelZone: Boolean?,
    private var cancel: Boolean,
    private var language: String = "11"
) {
    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private val sampleRate = 16000
    private val fileLock = Any()
    var duration: String=""
    companion object {
        private var instanceCount = 0
    }

    private var currentIndex = 0
    private lateinit var audioFile: File
    lateinit var audioWavFile: File
    private lateinit var outputStream: FileOutputStream
    private var recordingThread: Thread? = null
    val tag = "socketAudioSender"

    fun setLanguage(newLang: String) {
        this.language = newLang
        Log.d(tag, "语言已更新为 $language")
    }


    fun startStreaming() {
        Log.d(tag, "进入 startStreaming，准备初始化 WebSocketUploader")

        currentIndex = instanceCount++
        audioFile = File(context.filesDir, "recorded_audio_$currentIndex.pcm")
        audioWavFile = File(context.filesDir, "recorded_audio_$currentIndex.wav")
        outputStream = FileOutputStream(audioFile)
        if (wsClient == null) {
            Log.d(tag, "wsClient为空")
        }
        wsClient?.sendInit()
        startAudioRecordingLoop()
    }

    private fun startAudioRecordingLoop() {
        Log.d("audioSender","是否取消$cancel.toString()")
        val startTime = System.currentTimeMillis()

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(context, "未获得录音权限", Toast.LENGTH_SHORT).show()
            return
        }
        recordingThread = Thread {
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
                Log.d(tag,read.toString())
                if (read > 0) {
                        wsClient?.sendAudioChunk(buffer, read)
                    try {
                        //在录音还在进行时，防止录音线程正在写入文件的过程中，音频流被关闭或操作状态发生冲突。
                        synchronized(fileLock) {
                            if (isRecording) {
                                outputStream.write(buffer, 0, read)
                            }
                        }
                    } catch (e: IOException) {
                        Log.e(tag, "写入已关闭的流", e)
                        break // 退出循环
                    }
                }
            }
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
            val endTime = System.currentTimeMillis()
            duration = "${(endTime - startTime) / 1000}\""
            //利用锁，确保“写入音频数据”和“关闭音频流”的行为不会同时进行，从而防止程序崩溃或数据丢失
            synchronized(fileLock) {
                try {
                    outputStream.flush()
                    outputStream.close()
                    AudioConvert.convertPcmToWav(audioFile, audioWavFile)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
//            inCancelZone?.let {
//                if (!it){
//                    Log.d("AudioSender","运行了")
//                }
//            }
//            Log.d("AudioSender","!!!!!!!是否取消区域$inCancelZone.toString()")
            wsClient?.sendEnd(duration, inCancelZone == true)

        }.also { it.start() }
    }

    fun stopStreaming() {
        isRecording = false
//        recordingThread?.join()  // 等待录音线程安全退出
//
//        synchronized(fileLock) {
//            try {
//                outputStream.flush()
//                outputStream.close()
//                AudioConvert.convertPcmToWav(audioFile, audioWavFile)
//            } catch (e: IOException) {
//                e.printStackTrace()
//            }
//        }
    }



}

