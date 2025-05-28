package com.example.emotionlink.Repository

interface MessageCallback {

    fun onTextMessageReceived(
        fromLang: String,
        toLang: String,
        text: String,
        wavUrl: String,
        duration: String
    )

    fun onError(e: Exception)
}