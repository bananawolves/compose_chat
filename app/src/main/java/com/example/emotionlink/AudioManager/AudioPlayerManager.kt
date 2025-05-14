import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer

object AudioPlayerManager {
    private var mediaPlayer: MediaPlayer? = null
     var currentPath: String? = null
    private var onCompletion: (() -> Unit)? = null

    fun play(
        context: Context,
        path: String,
        onStarted: () -> Unit,
        onStopped: () -> Unit
    ) {
        // 如果当前就是这个音频，什么都不做
        if (currentPath == path && mediaPlayer?.isPlaying == true) return

        // 播放前先停止上一个
        stop()

        try {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(path)
                prepare()
                start()
                currentPath = path
                onStarted()
                setOnCompletionListener {
                    stop()
                    onStopped()
                }
                setOnErrorListener { mp, _, _ ->
                    stop()
                    onStopped()
                    true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            stop()
            onStopped()
        }
    }

    fun stop() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        currentPath = null
        onCompletion = null
    }

    fun isPlaying(path: String): Boolean {
        return currentPath == path && mediaPlayer?.isPlaying == true
    }
}
