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
    }

    private val gson = Gson()

    private val _chatVoiceItems = MutableStateFlow<List<ChatMessage.Voice>>(
        savedStateHandle.get<String>(CHAT_ITEMS_KEY)?.let { json ->
            val type = object : TypeToken<List<ChatMessage.Voice>>() {}.type
            gson.fromJson(json, type)
        } ?: emptyList()
    )
    val chatVoiceItems: StateFlow<List<ChatMessage.Voice>> = _chatVoiceItems.asStateFlow()

    // 新增：记录当前播放的语音消息 ID
    private val _currentPlayingId = MutableStateFlow<String?>(null)
    val currentPlayingId: StateFlow<String?> = _currentPlayingId.asStateFlow()

    fun setPlayingId(id: String?) {
        _currentPlayingId.value = id
    }
    private val stateHandle = savedStateHandle

    fun addVoiceMessage(message: ChatMessage.Voice) {
        val updated = _chatVoiceItems.value + message
        _chatVoiceItems.value = updated
        saveToStateHandle(updated)
    }


    private fun saveToStateHandle(list: List<ChatMessage.Voice>) {
        val json = gson.toJson(list)
        stateHandle[CHAT_ITEMS_KEY] = json
    }
}

