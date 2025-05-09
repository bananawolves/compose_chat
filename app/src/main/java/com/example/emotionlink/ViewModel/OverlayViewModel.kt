package com.example.emotionlink.ViewModel

import android.media.MediaRecorder
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import timber.log.Timber
import java.io.File
import kotlin.random.Random

class OverlayViewModel : ViewModel() {
    var released by mutableStateOf(false)
    private var mediaRecorder: MediaRecorder? = null
    private var outputFile: String? = null
    var inCancelZone by mutableStateOf(false)
    private var startTime: Long = 0
    private var endTime: Long = 0
    var onVoiceMessageSent: ((String, String) -> Unit)? = null

    fun triggerReleased() {
        Timber.d("OverlayViewModel", "triggerReleased 被调用，准备返回")
        stopRecording()
        released = true
    }

    fun resetReleased() {
        released = false
        startRecording()
    }

    fun cancelRecording() {
        stopRecording(cancel = true)
        released = true
    }

     fun startRecording() {
         println("录音开始")
        try {
            outputFile = File(
                android.os.Environment.getExternalStoragePublicDirectory(
                    android.os.Environment.DIRECTORY_DCIM
                ).resolve("Sound"),
                "recorded_audio_${System.currentTimeMillis()}.m4a"
            ).absolutePath

            // 确保目录存在
            File(outputFile!!).parentFile?.mkdirs()

            @Suppress("DEPRECATION")
            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(outputFile)
                prepare()
                start()
            }
            startTime = System.currentTimeMillis()
        } catch (e: Exception) {
            println("录音初始化失败: ${e.message}")
        }
    }

    private fun stopRecording(cancel: Boolean = false) {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            endTime = System.currentTimeMillis()
            mediaRecorder = null

            if (cancel) {
                File(outputFile!!).delete()
                Timber.d("OverlayViewModel", "录音已取消")
            } else {
                val duration = "${(endTime - startTime) / 1000}''"
                val recognizedText = "这是识别出的语音内容"
                onVoiceMessageSent?.invoke(duration, recognizedText)
            }
        } catch (e: Exception) {
            Timber.e("OverlayViewModel", "停止录音失败: ${e.message}")
        }
    }

    fun getRecordedInfo(): Pair<String, String>? {
        return if (outputFile != null && !released) {
            val duration = "${(endTime - startTime) / 1000}''"
            // 这里应该添加语音识别逻辑，返回识别后的文本
            // 暂时用固定文本代替
            val recognizedText = "这是识别出的语音内容"
            duration to recognizedText
        } else {
            null
        }
    }
}