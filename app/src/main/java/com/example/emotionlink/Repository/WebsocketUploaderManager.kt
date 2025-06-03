package com.example.emotionlink.Repository

import android.util.Log
import com.example.emotionlink.AudioDemo.WebSocketStatusListener

object WebsocketUploaderManager {
    private var uploader: WebSocketUploader? = null
    private var messageCallback: MessageCallback? = null

    fun seMessageCallback(callback: MessageCallback) {
        messageCallback = callback
    }

    fun initUploader(language: String,statusListener: WebSocketStatusListener) {
        // 先关闭之前的
        uploader?.close()

        val wsUrl = buildUrlFor(language)
        uploader = WebSocketUploader(wsUrl, messageCallback!!)
        uploader?.setStatusListener(statusListener)
        uploader?.connect()
    }

    fun getUploader(): WebSocketUploader? = uploader

    private fun buildUrlFor(language: String): String {
        val groupId = "JQBHLtzmz8uxUpV9sexxbJ"
        val userId = when (language) {
            "zh" -> "h7tX4B5KHETk2dkjVjVnwx"
            "en" -> "VT6iqWi98w9cXPfuxkHqzM"
            "dialect" -> "Djehfea5ErSfC6ZrbHSfxH"
            else -> ""
        }
        Log.d("WebsocketUploaderManager",userId)
//        return "http://10.18.63.60:8000/waic/apitest/start_chat/?group_id=$groupId&user_id=$userId"
        return "ws://117.160.123.34:43073/waic/apitest/start_chat/?group_id=$groupId&user_id=$userId"
    }
}
