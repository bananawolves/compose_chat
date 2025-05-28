package com.example.emotionlink.data

sealed class ChatMessage {
    data class Voice(
        val duration: String,
        val textContent: String,
        val isMe: Boolean,
        val audioUrl: String // 单个音频路径或 URL
    ) : ChatMessage() {
        fun getAudioPath(): String {
            return audioUrl
        }
    }
}