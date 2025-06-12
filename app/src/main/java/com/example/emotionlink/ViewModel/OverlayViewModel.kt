package com.example.emotionlink.ViewModel

import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.emotionlink.MyApplication.Companion.context
import com.example.emotionlink.Repository.WebSocketUploader
import com.example.emotionlink.Utils.LogUtils
import com.example.emotionlink.WebSocket.MessageCallback
import com.example.emotionlink.WebSocket.WebSocketAudioSender
import com.example.emotionlink.WebSocket.WebSocketStatusListener
import com.example.emotionlink.WebSocket.WebsocketUploaderManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File

class OverlayViewModel(
    private val languageViewModel: LanguageViewModel
) : ViewModel() {
    private val Tag = "OverlayViewModel"
    private val mainHandler = Handler(Looper.getMainLooper())
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

    private var currentLanguage: String = "11" // 设置默认语言

    private lateinit var localWavFile: File
    private var wsClient: WebSocketUploader? = null

    init {
        viewModelScope.launch {
            languageViewModel.language.collectLatest { lang ->
                LogUtils.d(Tag, "语言发生变化: $lang")
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
                            LogUtils.d(
                                Tag,
                                "来自$fromLang,目标$toLang,文字$text,是否相等${text == wavUrl},"
                                        + "音频地址: $wavUrl,持续时间$duration\""
                            )
                            if (!isMe) {
                                if (text != wavUrl) {
                                    onVoiceMessageSent?.invoke(
                                        "$duration\"",
                                        text, isMe, fromLang, toLang, wavUrl
                                    )
                                } else {
                                    showToast("检测到网络波动,请重试")
                                }
                            }
                            setReceiveState(true)
                        }

                        override fun onError(e: Exception) {
                            LogUtils.e(Tag, "WebSocket 错误: ${e.message}")
                        }
                    })

                    WebsocketUploaderManager.initUploader(
                        lang,
                        object : WebSocketStatusListener {
                            override fun onConnected() {
                                LogUtils.d(Tag, "WebSocket 已连接（语言：$lang）")
                                wsClient = WebsocketUploaderManager.getUploader()
                                wsClient?.sendInit()
                            }

                            override fun onError(e: Exception) {
                                showToast("WebSocket 断开连接")
                                LogUtils.e(Tag, "WebSocket 初始化失败: $e")
                            }
                        })

                    audioSender?.setLanguage(lang) // 若已有 sender，则更新语言
                }
            }
        }
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
        startTime = System.currentTimeMillis()
        audioSender = WebSocketAudioSender(wsClient)
        audioSender?.setLanguage(currentLanguage)
        audioSender?.startStreaming()
    }

    override fun onCleared() {
        super.onCleared()
        // 在 ViewModel 销毁时只清理 WebSocket 连接
        wsClient = null
        mainHandler.removeCallbacksAndMessages(null)
    }

    private fun stopRecording() {
        try {
            // 停止 WebSocket 实时录音
            audioSender?.stopStreaming()
            endTime = System.currentTimeMillis()
            firstDuration = "${(endTime - startTime) / 1000}\"".toString()

            audioSender?.let { sender ->
                localWavFile = sender.audioWavFile
            }
            //本地录言优先显示
            if (!inCancelZone) {
                onVoiceMessageSent?.invoke(
                    firstDuration, "",
                    true, currentLanguage, currentLanguage, localWavFile.absolutePath
                )
            }

            // 设置录音完成回调，不在audiosender发送的原因是未来获取inCancelZone状态，不让后端处理本该取消的音频
            audioSender?.setOnRecordingFinishedListener {
                LogUtils.d(Tag, "!!!!!!!是否取消区域$inCancelZone")
                if (!inCancelZone) {
                    LogUtils.d(Tag, "!!!!!!!发送结束消息")
                    wsClient?.sendEnd(firstDuration)
                }
                setReceiveState(true)
                // 只清理录音相关资源，保持 WebSocket 连接
                audioSender?.clearListener()
                audioSender = null
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showToast(message: String) {
        mainHandler.post {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
}