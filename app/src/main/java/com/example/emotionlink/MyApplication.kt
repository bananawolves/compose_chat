package com.example.emotionlink

import android.app.Application
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import com.example.emotionlink.Repository.WebSocketManager



class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        //Toast.makeText(this, "App 正在启动", Toast.LENGTH_SHORT).show()
        WebSocketManager.connect()
    }
}
