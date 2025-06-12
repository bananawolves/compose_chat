package com.example.emotionlink.WebSocket

interface WebSocketStatusListener {
    fun onConnected()
    fun onError(e: Exception)
}