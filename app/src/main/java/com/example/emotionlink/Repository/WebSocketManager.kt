package com.example.emotionlink.Repository

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.util.Log
import java.util.concurrent.TimeUnit


object WebSocketManager {
    private var webSocket: WebSocket? = null
    private var client: OkHttpClient = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    private val request = Request.Builder()
        .url("wss://openapi.teleagi.cn:443/aipaas/voice/v1/asr/fy")
        .addHeader("Authorization", "")
        .addHeader("X-APP-ID", "d0a488df365749648010ec85133e6273")
        .build()
    private var isReconnecting = false

    fun connect() {
        if (webSocket != null) return
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                startHeartbeat()
            }

            override fun onMessage(ws: WebSocket, text: String) {
                handleMessage(text)
            }

            override fun onMessage(ws: WebSocket, bytes: ByteString) {
                handleMessage(bytes.utf8())
            }

            override fun onClosing(ws: WebSocket, code: Int, reason: String) {
                ws.close(1000, null)
                Log.d("WebSocket", "连接关闭: $reason")
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {

                reconnect()
            }
        })
    }

    private fun startHeartbeat() {
        CoroutineScope(Dispatchers.IO).launch {
            while (true) {
                delay(10_000)
                send("ping")
            }
        }
    }

    private fun reconnect() {
        if (isReconnecting) return
        isReconnecting = true
        CoroutineScope(Dispatchers.IO).launch {
            delay(5_000)
            webSocket = null
            connect()
            isReconnecting = false
        }
    }

    fun send(message: String) {
        webSocket?.send(message)
    }

    fun close() {
        webSocket?.close(1000, null)
        webSocket = null
    }

    private fun handleMessage(message: String) {
        Log.d("WebSocket", "收到消息: $message")
        // TODO: 处理服务端发来的消息
    }
}