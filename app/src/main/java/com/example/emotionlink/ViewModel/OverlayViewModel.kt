package com.example.emotionlink.ViewModel

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.emotionlink.AudioDemo.WebSocketStatusListener
import com.example.emotionlink.AudioManager.AudioConvert
import com.example.emotionlink.Repository.WebSocketUploader
import com.example.emotionlink.WebSocket.MessageCallback
import com.example.emotionlink.WebSocket.WebSocketAudioSender
//import com.example.emotionlink.WebSocket.WebSocketStatusListener
import com.example.emotionlink.WebSocket.WebsocketUploaderManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File

class OverlayViewModel(
    private val context: Context,
    private val languageViewModel: LanguageViewModel
) : ViewModel() {
    private var _isReceive = MutableStateFlow(false) // 默认中文
    var isReceive: StateFlow<Boolean> = _isReceive
    fun setReceiveState(whether: Boolean) {
        _isReceive.value = whether
    }

    private var audioSender: WebSocketAudioSender? = null
    var released by mutableStateOf(false)
    var inCancelZone by mutableStateOf(false)
    var cancelZoneLocation: Rect? = null  //用于存放取消按钮位置
    private var startTime: Long = 0
    private var endTime: Long = 0
    private var firstDuration: String = ""
    var onVoiceMessageSent: ((String, String, Boolean, String, String, String) -> Unit)? = null
    val Tag = "OverlayViewModel"

    private var currentLanguage: String = "11" // 设置默认语言

    private lateinit var localWavFile: File
    private var transformPcm: ByteArray? = null
    private var wsClient: WebSocketUploader? = null
    var cancel: Boolean=false
    init {
        viewModelScope.launch {
            languageViewModel.language.collectLatest { lang ->
                Log.d(Tag, "语言发生变化: $lang")
                currentLanguage = lang // 缓存语言
                if (currentLanguage in listOf("cn", "sh", "en")) {
                    WebsocketUploaderManager.seMessageCallback(object : MessageCallback {
                        override fun onTextMessageReceived(
                            fromLang: String,
                            toLang: String,
                            text: String,
                            wavUrl: String,
                            duration: String
                        ) {
                            val isMe = fromLang == toLang
//                        val finalDuration = if (isMe) firstDuration else "$duration\""
                            val audioPathToUse = if (isMe) localWavFile.absolutePath else wavUrl
                            Log.d(Tag, "来自$fromLang,目标$toLang,音频地址: $audioPathToUse,持续时间$duration\"")

                            if (!isMe) {
                                onVoiceMessageSent?.invoke(
                                    "$duration\"", text, isMe, fromLang, toLang, audioPathToUse
                                )
                            }
                            setReceiveState(true)
                        }

                        override fun onError(e: Exception) {
                            Log.e(Tag, "WebSocket 错误: ${e.message}")
                        }
                    })

                    WebsocketUploaderManager.initUploader(
                        lang,
                        object : WebSocketStatusListener {
                            override fun onConnected() {
                                Log.d(Tag, "WebSocket 已连接（语言：$lang）")
                                wsClient = WebsocketUploaderManager.getUploader()
                                wsClient?.sendInit()
                            }

                            override fun onError(e: Exception) {
                                Handler(Looper.getMainLooper()).post {
                                    Toast.makeText(context, "WebSocket 断开连接", Toast.LENGTH_SHORT).show()
                                }
                                Log.e(Tag, "WebSocket 初始化失败: $e")
                            }
                        })

                    audioSender?.setLanguage(lang) // 若已有 sender，则更新语言
                }
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
        stopRecording()
        released = true
    }

    fun startRecording() {
        Log.d(Tag, "录音开始")
        startTime = System.currentTimeMillis()
        audioSender = WebSocketAudioSender(context, wsClient, inCancelZone,cancel)
        audioSender?.setLanguage(currentLanguage)

        audioSender?.startStreaming()
    }


    private fun stopRecording() {
        Log.d(Tag, "取消录音")
        try {
            // 停止 WebSocket 实时录音
            audioSender?.stopStreaming()
            endTime = System.currentTimeMillis()
            firstDuration = "${(endTime - startTime) / 1000}\"".toString()

            audioSender?.let { sender ->
                localWavFile = sender.audioWavFile
                val baseName = localWavFile.nameWithoutExtension
                val pcmFileName = "${baseName}_transform.pcm"
                val walFileName = "${baseName}_transform.wav"
                val transformPcmFile = File(localWavFile.parentFile, pcmFileName)
                val transformWavFile = File(localWavFile.parentFile, walFileName)
                if (transformPcm != null) {
                    transformPcmFile.outputStream().use {
                        it.write(transformPcm)
                    }
                    AudioConvert.convertPcmToWav(transformPcmFile, transformWavFile)
                }
            }
            //本地录言优先显示
            if (!inCancelZone) {
                onVoiceMessageSent?.invoke(
                    firstDuration, "",
                    true, currentLanguage, currentLanguage, localWavFile.absolutePath
                )
            }
            Log.d(Tag,"!!!!!!!是否取消区域$inCancelZone.toString()")

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}