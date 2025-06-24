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
import com.example.emotionlink.View.CallScreen
//import com.example.emotionlink.ViewModel.ChatScreen
import com.example.emotionlink.ViewModel.LanguageViewModel
import com.example.emotionlink.View.NewChatScreen
import com.example.emotionlink.View.SettingsScreen
import com.example.emotionlink.ViewModel.VoiceCallViewModel


class MainActivity : ComponentActivity() {
    private lateinit var languageViewModel: LanguageViewModel
    private lateinit var voiceCallViewModel: VoiceCallViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        languageViewModel = ViewModelProvider(this,)[LanguageViewModel::class.java]
        voiceCallViewModel = ViewModelProvider(this)[VoiceCallViewModel::class.java]
        enableEdgeToEdge()

        setContent {
            var currentScreen by remember { mutableStateOf("chat") }

            when (currentScreen) {
                "chat" -> NewChatScreen(
                    langue_viewModel = languageViewModel,
                    voiceCallViewModel = voiceCallViewModel,
                    onLanguageSelected = { lang -> languageViewModel.setLanguage(lang) },
                    onNavigateToSettings = {
                        currentScreen = "settings"
                    },
                    onNavigateToCall = {
                        currentScreen = "CallScreen"
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

                "CallScreen" -> CallScreen(
                    viewModel = voiceCallViewModel,
                    onBack = {
                        currentScreen = "chat"
                    }
                )
            }
        }
    }
}
