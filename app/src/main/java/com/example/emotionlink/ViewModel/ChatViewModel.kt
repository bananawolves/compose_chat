package com.example.emotionlink.ViewModel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.example.emotionlink.data.ChatMessage
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ChatViewModel(
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    companion object {
        private const val CHAT_ITEMS_KEY = "chat_voice_items"
        private const val LANGUAGE_KEY = "chat_language"
    }

    private val gson = Gson()

    private val _chatVoiceItems = MutableStateFlow<List<ChatMessage.Voice>>(
        savedStateHandle.get<String>(CHAT_ITEMS_KEY)?.let { json ->
            val type = object : TypeToken<List<ChatMessage.Voice>>() {}.type
            gson.fromJson(json, type)
        } ?: emptyList()
    )
    val chatVoiceItems: StateFlow<List<ChatMessage.Voice>> = _chatVoiceItems.asStateFlow()

    // 当前语言（持久化）
    private val _currentLanguage = MutableStateFlow(
        savedStateHandle[LANGUAGE_KEY] ?: "zh"
    )
    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()

    private val stateHandle = savedStateHandle

    fun addVoiceMessage(message: ChatMessage.Voice) {
        val updated = _chatVoiceItems.value + message
        _chatVoiceItems.value = updated
        saveToStateHandle(updated)
    }

    // 设置语言
    fun updateLanguage(lang: String) {
        _currentLanguage.value = lang
        stateHandle[LANGUAGE_KEY] = lang
    }

    private fun saveToStateHandle(list: List<ChatMessage.Voice>) {
        val json = gson.toJson(list)
        stateHandle[CHAT_ITEMS_KEY] = json
    }
}

