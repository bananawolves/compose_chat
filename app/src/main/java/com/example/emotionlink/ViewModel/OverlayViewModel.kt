package com.example.emotionlink.ViewModel

import android.content.Context
import android.media.AudioRecord
import android.media.MediaPlayer
import android.media.MediaRecorder
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.emotionlink.AudioDemo.AudioUrlCallback
import com.example.emotionlink.AudioDemo.Client.WebSocketUploader
import com.example.emotionlink.Repository.WebSocketAudioSender
import java.io.File

class OverlayViewModel (private val context: Context): ViewModel() {
    private var audioSender: WebSocketAudioSender? = null
    var released by mutableStateOf(false)
    private var mediaRecorder: MediaRecorder? = null
    private var outputFile: String? = null
    var inCancelZone by mutableStateOf(false)
    private var startTime: Long = 0
    private var endTime: Long = 0
    var onVoiceMessageSent: ((String, String, String) -> Unit)? = null

    fun triggerReleased() {
        stopRecording()
        released = true
    }

    fun resetReleased() {
        released = false
        startRecording()
    }

    fun cancelRecording() {
        stopRecording(cancel = true)
        released = true
    }

     fun startRecording() {
         println("录音开始")
         if (audioSender == null) {
             audioSender = WebSocketAudioSender(context, object : AudioUrlCallback {
                 override fun onAudioUrlReceived(audioUrl: String?) {
//                     val duration = "5''" // 假设值
//                     val recognizedText = "这是识别出的语音内容"
//                     onVoiceMessageSent?.invoke(duration, recognizedText)
                 }
             })
         }
         startTime= System.currentTimeMillis()
         audioSender?.startStreaming()
    }

    private fun stopRecording(cancel: Boolean = false) {
        try {
            // 停止 WebSocket 实时录音
            audioSender?.stopStreaming()
            endTime = System.currentTimeMillis()

            val duration = "${(endTime - startTime) / 1000}\""
            println("startTime:"+startTime+"endTime:"+endTime+"duration:"+duration)
            val recognizedText = "这是识别出的语音内容"

                // 播放 WAV 音频
//            val audioFile = File(context.filesDir, "recorded_audio.pcm")

//            var wavFile = File(context.filesDir, "recorded_audio.wav")
            val wavFile = audioSender!!.audioWavFile

            println("录音文件大小: ${wavFile.length()} 字节")
//            if (wavFile.exists()) {
//                val mediaPlayer = MediaPlayer()
//                mediaPlayer.setDataSource(wavFile.absolutePath)
//                mediaPlayer.prepare()
//                mediaPlayer.start()
//            }
            onVoiceMessageSent?.invoke(duration, recognizedText,wavFile.absolutePath)

        } catch (e: Exception) {
            e.printStackTrace()
        }
//        try {
//            mediaRecorder?.apply {
//                stop()
//                release()
//            }
//            endTime = System.currentTimeMillis()
//            mediaRecorder = null
//
//            if (cancel) {
//                File(outputFile!!).delete()
//            } else {
//                val duration = "${(endTime - startTime) / 1000}''"
//                val recognizedText = "这是识别出的语音内容"
//                onVoiceMessageSent?.invoke(duration, recognizedText)
//            }
//        } catch (e: Exception) {
//        }
    }

    fun getRecordedInfo(): Pair<String, String>? {
        return if (outputFile != null && !released) {
            val duration = "${(endTime - startTime) / 1000}''"
            // 这里应该添加语音识别逻辑，返回识别后的文本
            // 暂时用固定文本代替
            val recognizedText = "这是识别出的语音内容"
            duration to recognizedText
        } else {
            null
        }
    }
}