package com.example.emotionlink.WebSocket

interface CallMessageCallback {
    fun onCallMessageReceived(
        fromLang: String,
        toLang: String,
        text: String,
        wavUrl: String,
        duration: String
    )
    fun onIncomingCall(fromUserId: String)
    fun onError(e: Exception)
}