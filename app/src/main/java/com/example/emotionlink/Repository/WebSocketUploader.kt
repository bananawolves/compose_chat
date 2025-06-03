package com.example.emotionlink.Repository

import android.util.Log
import com.example.emotionlink.AudioDemo.WebSocketStatusListener
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
import kotlin.concurrent.thread
import kotlin.io.encoding.ExperimentalEncodingApi

class WebSocketUploader(
    wsUrl: String,
    private val messageCallback: MessageCallback
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
                Log.d(TAG, "WebSocket connected")
                statusListener?.onConnected()
                startHeartbeat()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d(TAG, "Received: $text")
                handleWebSocketMessage(text, messageCallback)
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {

                Log.d(TAG, "Received binary message: ${bytes.hex()}")
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket error: ${t.message}")
                statusListener?.onError(Exception(t))
                reconnect()
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closing: $reason")
                webSocket.close(code, reason)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closed: $reason")
            }
        })
    }

    private fun startHeartbeat() {
        heartbeatJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                delay(10_000)
                Log.d(TAG, "发送ping")
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
        CoroutineScope(Dispatchers.IO).launch {
            delay(5_000)
            Log.d(TAG, "尝试重连")
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
                put("chunk_size", length/2)//1280
            }
            webSocket?.send(meta.toString())

            val chunk = audio.copyOfRange(0, length)
            webSocket?.send(ByteString.of(*chunk))
            Log.d(TAG, "正在发送")
            chunkId++
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun sendEnd(duration: String,inCancelZone: Boolean) {
        try {
            val status = if (inCancelZone) "discard" else "end"
            val end = JSONObject().apply {
                put("msg_type", "chunk_info")
                put("chunk_id", "-1")
                put("status", status)
                put("chunk_size", 0)
                put("duration", duration)
            }
            Log.d(TAG, "发送结束")
            webSocket?.send(end.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun close() {
        heartbeatJob?.cancel()
        webSocket?.close(1000, "Normal closure")
    }

    @OptIn(ExperimentalEncodingApi::class)
    fun handleWebSocketMessage(message: String, callback: MessageCallback) {
        try {
            val json = JSONObject(message)

            val data = json.optJSONObject("data")
            if (data != null) {
                val fromLang = data.optString("from_language")
                val toLang = data.optString("language")
                val text = data.optString("text")
                val wavFile = data.optString("wav_file")
                val duration = data.optString("duration")

                if (!text.isNullOrEmpty() || !wavFile.isNullOrEmpty()) {
                    callback.onTextMessageReceived(fromLang, toLang, text, wavFile, duration)
                }
            }
        } catch (e: Exception) {
            callback.onError(e)
        }
    }
}
