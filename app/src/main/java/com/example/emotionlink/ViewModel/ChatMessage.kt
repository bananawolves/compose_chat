package com.example.emotionlink.ViewModel


sealed class ChatMessage {
    data class Voice(
        val duration: String,
        val textContent: String,
        val isMe: Boolean = true,
        val audioPath: String // 添加音频路径
    ) : ChatMessage()
}
