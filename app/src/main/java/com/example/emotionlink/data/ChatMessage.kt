package com.example.emotionlink.data

import java.util.UUID

sealed class ChatMessage {
    data class Voice(
        val duration: String,
        val textContent: String,
        val isMe: Boolean,
        val fromLanguage: String,
        val toLanguage: String,
        val audioUrl: String, // 单个音频路径或 URL
        val id: String = UUID.randomUUID().toString() // 增加唯一ID
        ) : ChatMessage() {
        fun getAudioPath(): String {
            return audioUrl
        }
    }
}