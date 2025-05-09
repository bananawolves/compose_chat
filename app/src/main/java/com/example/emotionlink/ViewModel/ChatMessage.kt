package com.example.emotionlink.ViewModel


sealed class ChatMessage {
    data class Voice(
        val duration: String,
        val textContent: String,
        val isMe: Boolean = true
    ) : ChatMessage()
}
