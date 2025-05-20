package com.example.emotionlink.ViewModel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class OverlayViewModelFactory(private val context: Context,
                              private val languageViewModel: LanguageViewModel
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(OverlayViewModel::class.java)) {
            return OverlayViewModel(context,languageViewModel) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
