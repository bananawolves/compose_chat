package com.example.emotionlink

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModelProvider
//import com.example.emotionlink.ViewModel.ChatScreen
import com.example.emotionlink.ViewModel.LanguageViewModel
import com.example.emotionlink.ViewModel.NewChatScreen
import com.example.emotionlink.ViewModel.SettingsScreen


class MainActivity : ComponentActivity() {
    private lateinit var languageViewModel: LanguageViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        languageViewModel = ViewModelProvider(this)[LanguageViewModel::class.java]

        enableEdgeToEdge()

        setContent {
            var currentScreen by remember { mutableStateOf("chat") }

            when (currentScreen) {
                "chat" -> NewChatScreen(
                    langue_viewModel = languageViewModel,
                    onLanguageSelected = { lang -> languageViewModel.setLanguage(lang) },
                    onNavigateToSettings = {
                        currentScreen = "settings"
                    }
                )

                "settings" -> SettingsScreen(
                    onLanguageChosen = { lang ->
                        languageViewModel.setLanguage(lang)
                        currentScreen = "chat"
                    },
                    onBack = {
                        currentScreen = "chat"
                    }
                )
            }
        }
    }
}
