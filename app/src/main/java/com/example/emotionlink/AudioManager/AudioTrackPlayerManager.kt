package com.example.emotionlink.AudioManager

import android.media.*
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.util.concurrent.LinkedBlockingQueue
import kotlin.concurrent.thread
//备选方案，流式返回时启用
object AudioTrackPlayerManager {
    private var audioTrack: AudioTrack? = null
    private var playThread: Thread? = null
    private var isPlaying = false
    private val audioQueue = LinkedBlockingQueue<ByteArray>()
    private const val TAG = "AudioTrackPlayerManager"

    // 音频参数（必须与服务端一致）
    private const val SAMPLE_RATE = 16000
    private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_OUT_MONO
    private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT

    fun start(onStarted: () -> Unit = {}) {
        if (isPlaying) return

        val minBufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        audioTrack = AudioTrack(
            AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .build(),
            AudioFormat.Builder()
                .setEncoding(AUDIO_FORMAT)
                .setSampleRate(SAMPLE_RATE)
                .setChannelMask(CHANNEL_CONFIG)
                .build(),
            minBufferSize,
            AudioTrack.MODE_STREAM,
            AudioManager.AUDIO_SESSION_ID_GENERATE
        )

        audioTrack?.play()
        isPlaying = true
        onStarted()

        playThread = thread(start = true) {
            try {
                while (isPlaying) {
                    val chunk = audioQueue.take() // 阻塞直到拿到数据
                    audioTrack?.write(chunk, 0, chunk.size)
                }
            } catch (e: Exception) {
                Log.e(TAG, "播放线程异常: ${e.message}")
            }
        }
    }

    fun stop(onStopped: () -> Unit = {}) {
        if (!isPlaying) return

        isPlaying = false
        playThread?.interrupt()
        playThread = null

        audioTrack?.stop()
        audioTrack?.release()
        audioTrack = null

        audioQueue.clear()
        onStopped()
    }

    fun writeChunk(pcmData: ByteArray) {
        if (isPlaying) {
            audioQueue.offer(pcmData) // 放入播放队列
        }
    }

    fun isCurrentlyPlaying(): Boolean = isPlaying
    /**
     * 直接播放完整 PCM 文件（阻塞线程，非 chunk 模式）
     */
    fun playFile(path: String, onStarted: () -> Unit = {}, onCompleted: () -> Unit = {}) {
        stop() // 防止多次播放重叠

        thread {
            try {
                val file = File(path)

                val bufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
                val track = AudioTrack(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build(),
                    AudioFormat.Builder()
                        .setSampleRate(SAMPLE_RATE)
                        .setEncoding(AUDIO_FORMAT)
                        .setChannelMask(CHANNEL_CONFIG)
                        .build(),
                    bufferSize,
                    AudioTrack.MODE_STREAM,
                    AudioManager.AUDIO_SESSION_ID_GENERATE
                )

                val buffer = ByteArray(bufferSize)
                val inputStream = FileInputStream(file)

                track.play()
                onStarted()

                var read: Int
                while (inputStream.read(buffer).also { read = it } > 0) {
                    track.write(buffer, 0, read)
                }

                inputStream.close()
                track.stop()
                track.release()
                onCompleted()

            } catch (e: Exception) {
                Log.e(TAG, "播放PCM文件异常: ${e.message}")
            }
        }
    }

}
