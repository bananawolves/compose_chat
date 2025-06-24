package com.example.emotionlink.WebSocket

import com.example.emotionlink.Repository.WebSocketUploader
import com.example.emotionlink.Utils.LogUtils

object WebsocketUploaderManager {
    private var uploader: WebSocketUploader? = null
    private var messageCallback: MessageCallback? = null
    private var callMessageCallback: CallMessageCallback? = null

    fun seMessageCallback(callback: MessageCallback) {
        messageCallback = callback
    }
    fun setCallMessageCallback(callback: CallMessageCallback) {
        callMessageCallback = callback
    }

    fun initUploader(language: String,statusListener: WebSocketStatusListener) {
        // 先关闭之前的网络连接
        uploader?.close()

        val wsUrl = buildUrlFor(language)
        uploader = WebSocketUploader(wsUrl, messageCallback!!,callMessageCallback!!)
        uploader?.setStatusListener(statusListener)
        uploader?.connect()
    }

    fun getUploader(): WebSocketUploader? = uploader

    private fun buildUrlFor(language: String): String {
        val groupId = "JQBHLtzmz8uxUpV9sexxbJ"
        val userId = when (language) {
            "cn" -> "h7tX4B5KHETk2dkjVjVnwx"
            "en" -> "VT6iqWi98w9cXPfuxkHqzM"
            "sh" -> "Djehfea5ErSfC6ZrbHSfxH"
            else -> ""
        }
        LogUtils.d("WebsocketUploaderManager",userId)
        return "http://117.160.123.34:48483/waic/apitest/start_chat/?group_id=$groupId&user_id=$userId"
    }
}
