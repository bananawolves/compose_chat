package com.example.emotionlink

import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import com.example.emotionlink.AudioDemo.WebSocketStatusListener
import com.example.emotionlink.Repository.MessageCallback
import com.example.emotionlink.Repository.WebSocketUploader
import com.example.emotionlink.Repository.WebsocketUploaderManager
import java.time.Duration
import kotlin.math.max


class MyApplication : Application() {
//    private var wsClient: WebSocketUploader? = null
//    var onVoiceMessageSent: ((String, String, Boolean, String) -> Unit)? = null
//
    override fun onCreate() {
        super.onCreate()
//
//        // 设置 WebSocket 回调（必须）
//        WebsocketUploaderManager.seMessageCallback(object : MessageCallback {
//            override fun onTextMessageReceived(
//                fromLang: String,
//                toLang: String,
//                text: String,
//                wavUrl: String,
//                duration: String
//
//            ) {
//                onVoiceMessageSent?.invoke(duration, text, false, wavUrl)
//
//                Log.d("AppInit", "初始化 WebSocket 成功，后端准备就绪")
//            }
//
//            override fun onError(e: Exception) {
//                Log.e("AppInit", "WebSocket 初始化失败", e)
//            }
//        })
//
//        // 初始化 WebSocket（可以用默认语言，也可以从持久化存储中获取）
//        WebsocketUploaderManager.initUploader("zh", object : WebSocketStatusListener {
//            override fun onConnected() {
//                wsClient = WebsocketUploaderManager.getUploader()
//                wsClient?.sendInit()
////                val fakeBuffer = ByteArray(320) { (0..255).random().toByte() }
////                wsClient?.sendAudioChunk(fakeBuffer, fakeBuffer.size)
////                wsClient?.sendEnd("1")
//
//                Log.d("AppInit", "WebSocket 已连接")
//            }
//
//
//            override fun onError(error: Exception) {
//                Log.e("AppInit", "WebSocket 连接异常", error)
//            }
//        })
    }
}