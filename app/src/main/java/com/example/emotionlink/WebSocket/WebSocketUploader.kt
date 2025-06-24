package com.example.emotionlink.Repository

import com.example.emotionlink.Utils.LogUtils
import com.example.emotionlink.WebSocket.CallMessageCallback
import com.example.emotionlink.WebSocket.MessageCallback
import com.example.emotionlink.WebSocket.WebSocketStatusListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.io.encoding.ExperimentalEncodingApi

class WebSocketUploader(
    wsUrl: String,
    private val messageCallback: MessageCallback,
    private val callMessageCallback: CallMessageCallback,

    ) {
    companion object {
        private const val TAG = "WebSocketUploader"
    }

    private var isReconnecting = false
    private var webSocket: WebSocket? = null
    private var statusListener: WebSocketStatusListener? = null
    private var chunkId: Int = 0
    private var heartbeatJob: Job? = null
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS) // 保持长连接
        .build()
    val request = Request.Builder()
        .url(wsUrl)
        .build()

    fun connect() {

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                LogUtils.d(TAG, "WebSocket connected")
                statusListener?.onConnected()
//                startHeartbeat()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                LogUtils.d(TAG, "Received: $text")
                handleWebSocketMessage(text, messageCallback, callMessageCallback)
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {

                LogUtils.d(TAG, "Received binary message: ${bytes.hex()}")
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                LogUtils.e(TAG, "WebSocket error: ${t.message}")
                statusListener?.onError(Exception(t))
                reconnect()
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                LogUtils.d(TAG, "WebSocket closing: $code")

//                webSocket.close(code, reason)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                LogUtils.d(TAG, "WebSocket closed: $reason")
            }
        })
    }

    private fun startHeartbeat() {
        heartbeatJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                delay(10_000)
                LogUtils.d(TAG, "发送ping")
                send("ping")
            }
        }
    }

    fun send(message: String) {
        webSocket?.send(message)
    }

    private fun reconnect() {
        if (isReconnecting) return
        isReconnecting = true
        CoroutineScope(Dispatchers.IO).launch {//有内存泄漏风险
            delay(5_000)
            LogUtils.d(TAG, "尝试重连")
            webSocket = null
            connect()
            isReconnecting = false
        }
    }

    fun setStatusListener(listener: WebSocketStatusListener) {
        statusListener = listener
    }

    fun sendInit() {
        try {
            val init = JSONObject().apply {
                put("msg_type", "init")
                put("dtype", "int16")
                put("sample_rate", 16000)
                put("channels", 1)
            }
            LogUtils.d(TAG, "发送初始化")
            webSocket?.send(init.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun sendAudioChunk(audio: ByteArray, length: Int) {
        try {
            val meta = JSONObject().apply {
                put("msg_type", "chunk_info")
                put("chunk_id", chunkId)
                put("status", "start")
                put("chunk_size", length / 2)//1280
            }
            webSocket?.send(meta.toString())

            val chunk = audio.copyOfRange(0, length)
            webSocket?.send(ByteString.of(*chunk))
            LogUtils.d(TAG, "正在发送")
            chunkId++
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun sendEnd(duration: String) {
        try {
            val end = JSONObject().apply {
                put("msg_type", "chunk_info")
                put("chunk_id", "-1")
                put("status", "end")
                put("chunk_size", 0)
                put("duration", duration)
            }
            LogUtils.d(TAG, "发送结束")
            chunkId = 0
            webSocket?.send(end.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun close() {
        heartbeatJob?.cancel()
        webSocket?.close(1000, "Normal closure")
    }

    fun sendCallRequest(fromUserId: String) {
        val callRequest = JSONObject().apply {
            put("type", "Call_request")
            put("fromUserId", fromUserId)
        }
        webSocket?.send(callRequest.toString())
    }

    fun sendAcceptRequest(toUserId: String) {
        val acceptRequest = JSONObject().apply {
            put("type", "Accept_request")
            put("toUserId", toUserId)
        }
        webSocket?.send(acceptRequest.toString())
    }

    fun sendReject(toUserId: String) {
        val rejectRequest = JSONObject().apply {
            put("type", "reject_request")
            put("toUserId", toUserId)
        }
        webSocket?.send(rejectRequest.toString())
    }

    fun sendCallEnd(toUserId: String) {
        val acceptRequest = JSONObject().apply {
            put("type", "end_request")
            put("toUserId", toUserId)
        }
        webSocket?.send(acceptRequest.toString())
    }

    @OptIn(ExperimentalEncodingApi::class)
    fun handleWebSocketMessage(
        message: String,
        callback: MessageCallback,
        callback2: CallMessageCallback
    ) {
        if (!isJsonObject(message)) {
            LogUtils.w(TAG, "非结构化消息，跳过: $message")
            return
        }
        try {
            val json = JSONObject(message)
            val data = json.optJSONObject("data") ?: return
            val fromLang = data.optString("from_language")
            val toLang = data.optString("language")
            val text = data.optString("text")
            val wavFile = data.optString("wav_file")
            val duration = data.optString("duration")
            val sentence = data.optString("sentence")
            if (wavFile != null && wavFile.isNotEmpty()) {
                callback.onTextMessageReceived(fromLang, toLang, text, wavFile, duration, sentence)
                callback2.onCallMessageReceived(fromLang, toLang, text, wavFile, duration)
            }

            if (json.optBoolean("callComing", false)) {
                val fromUserId = json.optString("fromUserId")
                callback2.onIncomingCall(fromUserId)  // 触发回调
            }
        } catch (e: Exception) {
            callback.onError(e)
        }
    }

    fun isJsonObject(str: String): Boolean {
        return str.trim().startsWith("{") && str.trim().endsWith("}")
    }
}
