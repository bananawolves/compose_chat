package com.example.emotionlink.ViewModel

import android.widget.Toast
import androidx.lifecycle.ViewModel
import com.example.emotionlink.Repository.WebsocketUploaderManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class LanguageViewModel :ViewModel() {
    private val _language = MutableStateFlow("zh") // 默认中文
    val language: StateFlow<String> = _language

    fun setLanguage(langCode: String) {
        _language.value = langCode
//        WebsocketUploaderManager.initUploader(langCode)
    }

}