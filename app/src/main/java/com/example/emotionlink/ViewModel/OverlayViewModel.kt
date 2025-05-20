package com.example.emotionlink.ViewModel

import android.R
import android.content.Context
import android.media.AudioRecord
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.nfc.Tag
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.emotionlink.AudioDemo.AudioUrlCallback
import com.example.emotionlink.AudioDemo.Client.WebSocketUploader
import com.example.emotionlink.Repository.WebSocketAudioSender
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File

class OverlayViewModel (private val context: Context,
                        private val languageViewModel: LanguageViewModel
): ViewModel() {

    private var audioSender: WebSocketAudioSender? = null
    var released by mutableStateOf(false)
    private var outputFile: String? = null
    var inCancelZone by mutableStateOf(false)
    private var startTime: Long = 0
    private var endTime: Long = 0
    var onVoiceMessageSent: ((String, String, String) -> Unit)? = null
    val Tag="OverlayViewModel"
    private var latestLanguage: String = "zh" // 设置默认语言

    init {
        viewModelScope.launch {
            languageViewModel.language.collectLatest { lang ->
                Log.d(Tag, "语言发生变化: $lang")
                latestLanguage = lang // 缓存语言
                audioSender?.setLanguage(lang) // 若已有 sender，则更新语言
            }
        }
    }
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
         Log.d(Tag,"录音开始")
         if (audioSender == null) {
             audioSender = WebSocketAudioSender(context, object : AudioUrlCallback {
                 override fun onAudioUrlReceived(audioUrl: String?) {
//                     val duration = "5''" // 假设值
//                     val recognizedText = "这是识别出的语音内容"
//                     onVoiceMessageSent?.invoke(duration, recognizedText)
                 }
             }).apply { setLanguage(latestLanguage) }
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
            Log.d(Tag,"startTime:"+startTime+"endTime:"+endTime+"duration:"+duration)
            val recognizedText = "这是识别出的语音内容"
//            val wavFile = audioSender!!.audioWavFile
//            Log.d(Tag,"录音文件大小: ${wavFile.length()} 字节")
//            onVoiceMessageSent?.invoke(duration, recognizedText,wavFile.absolutePath)
            audioSender?.let { sender ->
                val wavFile = sender.audioWavFile
                Log.d(Tag, "录音文件大小: ${wavFile.length()} 字节")
                onVoiceMessageSent?.invoke(duration, recognizedText, wavFile.absolutePath)
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
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