package com.example.emotionlink.ViewModel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class LanguageViewModel :ViewModel() {
    private val _language = MutableStateFlow("11") // 默认中文
    val language: StateFlow<String> = _language

    fun setLanguage(langCode: String) {
        _language.value = langCode
    }

}