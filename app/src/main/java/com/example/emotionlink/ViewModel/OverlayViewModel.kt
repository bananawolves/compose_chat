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
import java.io.FileOutputStream
import java.io.IOException

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

    private var currentIndex: Int = 0
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
     var wsClient: WebSocketUploader? = null
    private val downloadedFiles = mutableListOf<File>()
    var LocalText=""

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
                            duration: String,
                            sentence: String
                        ) {
                            if(LocalText==text)
                            {
                                LogUtils.d(Tag,"!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
                                return
                            }
                            LocalText=text
                            val fistEndTime= System.currentTimeMillis()
                            LogUtils.d(Tag,"${startTime-fistEndTime}")
                            val isMe = fromLang == toLang
                            LogUtils.d(
                                Tag,
                                "来自$fromLang,目标$toLang,文字$text,是否相等${text == wavUrl},"
                                        + "音频地址: $wavUrl,持续时间$duration\""
                            )

                            if (!isMe) {
                                if (text != wavUrl) {
                                    val fileName = "audio_${System.currentTimeMillis()}.wav"
                                    downloadAudioToFile(
                                        wavUrl, fileName,
                                        onSuccess = { localFile ->
                                            val path = localFile.absolutePath
                                            val isEnd = sentence == "end"

                                            // 填充式url载入
                                            onVoiceMessageSent?.invoke(
                                                duration,
                                                text,
                                                false,
                                                fromLang,
                                                toLang,
                                                if (isEnd) "$path,end" else path
                                            )
//                                            downloadedFiles.add(localFile)
//                                            if (sentence == "end") {
//                                                // 构建路径字符串列表
//                                                val filePathList = downloadedFiles.map { it.absolutePath }
//                                                val joinedPaths = filePathList.joinToString(",")
//                                                val totalDuration = downloadedFiles.size.toString() + "段"
//                                                onVoiceMessageSent?.invoke(
//                                                    totalDuration, "", false, fromLang, toLang, joinedPaths
//                                                )
//                                                downloadedFiles.clear()
//                                            }
                                        },
                                        onFailure = {
                                            showToast("下载音频失败: ${it.message}")
                                        }
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
                currentIndex = sender.currentIndex
            }
            //本地录言优先显示
            if (!inCancelZone) {
                onVoiceMessageSent?.invoke(
                    firstDuration, "",
                    true, currentLanguage, currentLanguage,  "${localWavFile.absolutePath},end"
                )
            }

            // 设置录音完成回调，不在audiosender发送的原因是未来获取inCancelZone状态，不让后端处理本该取消的音频
            audioSender?.setOnRecordingFinishedListener {
                LogUtils.d(Tag, "!!!!!!!是否取消区域$inCancelZone")
                if (!inCancelZone) {
                    LogUtils.d(Tag, "!!!!!!!发送结束消息")
                    wsClient?.sendEnd(firstDuration)
                }
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

    private fun downloadAudioToFile(
        url: String,
        fileName: String,
        onSuccess: (File) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val client = okhttp3.OkHttpClient()
        val request = okhttp3.Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                onFailure(e)
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                if (!response.isSuccessful) {
                    onFailure(IOException("Unexpected code $response"))
                    return
                }

                val audioDir = File(context.cacheDir, "recorded_audio")
                if (!audioDir.exists()) audioDir.mkdirs()
                val outputFile = File(audioDir, fileName)

                response.body?.byteStream().use { input ->
                    FileOutputStream(outputFile).use { output ->
                        input?.copyTo(output)
                    }
                }

                onSuccess(outputFile)
            }
        })
    }

}
