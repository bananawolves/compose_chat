package com.example.emotionlink.Repository

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.*
import android.nfc.Tag
import android.os.SystemClock
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.app.ActivityCompat
import com.example.emotionlink.AudioDemo.AudioUrlCallback
import com.example.emotionlink.AudioDemo.Client.WebSocketAuthGenerator
import com.example.emotionlink.AudioDemo.Client.WebSocketUploader
import com.example.emotionlink.AudioDemo.WebSocketStatusListener
import com.example.emotionlink.ViewModel.LanguageViewModel
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

import java.net.URI

class WebSocketAudioSender(
    private val context: Context,
    private val callback: AudioUrlCallback,
    private var language: String = "zh"
) {
    private var wsClient: WebSocketUploader? = null
    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private val sampleRate = 16000
    private val fileLock=Any()
    companion object {
        private var instanceCount = 0
    }
    private var currentIndex=0
    private lateinit var audioFile: File
    lateinit var audioWavFile: File
    private lateinit var outputStream: FileOutputStream
    val Tag="WebSocketAudioSender"
    fun setLanguage(newLang: String) {
        this.language = newLang
        Log.d(Tag, "语言已更新为 $language")
    }
    fun startStreaming() {

        Log.d(Tag,"进入java录音")
        try {
            val groupId = "JQBHLtzmz8uxUpV9sexxbJ"
            val userId = when (language) {
                "ch" -> "h7tX4B5KHETk2dkjVjVnwx"
                "en" -> "VT6iqWi98w9cXPfuxkHqzM"
                "sh" -> "Djehfea5ErSfC6ZrbHSfxH"
                else -> ""
            }


            val url = "ws://158.178.230.179:60688/waic/apitest/start_chat/?group_id=$groupId&user_id=$userId"
            val uri = URI(url)

            currentIndex = instanceCount++
            audioFile = File(context.filesDir, "recorded_audio_$currentIndex.pcm")
            audioWavFile = File(context.filesDir, "recorded_audio_$currentIndex.wav")
            Log.d(Tag,"当前录音文件名: ${audioFile.name}")

            outputStream = FileOutputStream(audioFile)
            wsClient = WebSocketUploader(uri, callback).apply {
                statusListener = object : WebSocketStatusListener {
                    override fun onConnected() {
                        Log.d(Tag,"WebSocket 准备就绪，开始录音")
                        startAudioRecordingLoop()
                    }
                    override fun onError(e: Exception) {
                        Log.d(Tag,"WebSocket 连接失败：$e")
                    }
                }
                connect()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "录音失败", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }
    private fun startAudioRecordingLoop() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(context, "未获得录音权限", Toast.LENGTH_SHORT).show()
            return
        }
        Thread {
            wsClient?.sendInit()

            val bufferSize = AudioRecord.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )

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

            while (isRecording && wsClient?.isOpen == true) {
                val read = audioRecord!!.read(buffer, 0, buffer.size)
                if (read > 0) {
                    wsClient?.sendAudioChunk(buffer, read)
                    synchronized(fileLock) {//可以同步锁，也可以将outputStream放在一个线程中
                        try {
                            outputStream.write(buffer, 0, read)
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                }
            }
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
            wsClient?.sendEnd()
        }.start()
    }

    fun stopStreaming() {
        isRecording = false
        synchronized(fileLock) {
            outputStream.flush()
            outputStream.close()
            convertPcmToWav(audioFile, audioWavFile)
        }
    }

    fun convertPcmToWav(pcmFile: File, wavFile: File) {
        val sampleRate = 16000
        val channels = 1
        val byteRate = sampleRate * channels * 16 / 8

        val pcmSize = pcmFile.length().toInt()
        val wavOut = FileOutputStream(wavFile)
        val header = ByteArray(44)

        // RIFF/WAVE header
        header[0] = 'R'.code.toByte(); header[1] = 'I'.code.toByte(); header[2] = 'F'.code.toByte(); header[3] = 'F'.code.toByte()
        writeInt(header, 4, pcmSize + 36)
        header[8] = 'W'.code.toByte(); header[9] = 'A'.code.toByte(); header[10] = 'V'.code.toByte(); header[11] = 'E'.code.toByte()

        // fmt subchunk
        header[12] = 'f'.code.toByte(); header[13] = 'm'.code.toByte(); header[14] = 't'.code.toByte(); header[15] = ' '.code.toByte()
        writeInt(header, 16, 16) // Subchunk1Size for PCM
        writeShort(header, 20, 1.toShort()) // AudioFormat = 1
        writeShort(header, 22, channels.toShort())
        writeInt(header, 24, sampleRate)
        writeInt(header, 28, byteRate)
        writeShort(header, 32, (channels * 16 / 8).toShort()) // BlockAlign
        writeShort(header, 34, 16.toShort()) // BitsPerSample

        // data subchunk
        header[36] = 'd'.code.toByte(); header[37] = 'a'.code.toByte(); header[38] = 't'.code.toByte(); header[39] = 'a'.code.toByte()
        writeInt(header, 40, pcmSize)

        wavOut.write(header)

        val pcmIn = pcmFile.inputStream()
        val buffer = ByteArray(1024)
        var bytesRead: Int
        while (pcmIn.read(buffer).also { bytesRead = it } != -1) {
            wavOut.write(buffer, 0, bytesRead)
        }
        pcmIn.close()
        wavOut.close()
    }

    // 工具函数
    fun writeInt(b: ByteArray, offset: Int, value: Int) {
        b[offset] = (value and 0xff).toByte()
        b[offset + 1] = ((value shr 8) and 0xff).toByte()
        b[offset + 2] = ((value shr 16) and 0xff).toByte()
        b[offset + 3] = ((value shr 24) and 0xff).toByte()
    }

    fun writeShort(b: ByteArray, offset: Int, value: Short) {
        b[offset] = (value.toInt() and 0xff).toByte()
        b[offset + 1] = ((value.toInt() shr 8) and 0xff).toByte()
    }
}

