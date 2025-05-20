package com.example.emotionlink

import android.app.Application
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import com.example.emotionlink.Repository.WebSocketManager
import kotlin.math.max


class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        WebSocketManager.connect()

    }
}