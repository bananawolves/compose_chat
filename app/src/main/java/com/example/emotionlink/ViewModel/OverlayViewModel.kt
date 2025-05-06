package com.example.emotionlink.ViewModel

import android.media.MediaRecorder
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import timber.log.Timber
import java.io.File

class OverlayViewModel : ViewModel() {
    var released by mutableStateOf(false)
    private var mediaRecorder: MediaRecorder? = null
    private var outputFile: String? = null

    fun triggerReleased() {
        Timber.d("OverlayViewModel", "triggerReleased 被调用，准备返回")
        released = true
    }

    fun onEnterScreen(context: android.content.Context) {

        Timber.d("OverlayViewModel", "OverlayScreen 初始化命令执行，开始录音")
        try {
            outputFile = File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DCIM).resolve("Sound"), "recorded_audio.3gp").absolutePath
            Toast.makeText(context, "录音开始", Toast.LENGTH_SHORT).show()
            @Suppress("DEPRECATION")
            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(outputFile)
                prepare()
                start()
            }
        } catch (e: Exception) {
            Timber.e("OverlayViewModel", "录音初始化失败: ${e.message}")
        }
    }
    fun onExitScreen() {
        Timber.d("OverlayViewModel", "OverlayScreen 页面退出，停止录音")
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
        } catch (e: Exception) {
            Timber.e("OverlayViewModel", "停止录音失败: ${e.message}")
        }
    }

}