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
        //你的url
        return "你的url"
    }
}
