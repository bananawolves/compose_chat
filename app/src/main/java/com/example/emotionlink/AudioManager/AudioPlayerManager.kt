import android.media.AudioAttributes
import android.media.MediaPlayer

/*
放回url时的播放方式
 */
object AudioPlayerManager {
    private var mediaPlayer: MediaPlayer? = null
    private var currentPaths: List<String>? = null
    private var currentIndex = 0
    private var onAllComplete: (() -> Unit)? = null

    fun play(
        paths: List<String>,
        onStarted: () -> Unit,
        onStopped: () -> Unit
    ) {
        if (paths.isEmpty()) return

        stop() // 播放前先清理
        currentPaths = paths
        currentIndex = 0
        onAllComplete = onStopped
        playNext(paths, onStarted,onStopped)
    }

    private fun playNext(paths: List<String>, onStarted: () -> Unit,onStopped: () -> Unit) {
        if (currentIndex >= paths.size) {
            stop()
            onStopped()
            onAllComplete?.invoke()
            return
        }

        val path = paths[currentIndex]
        try {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(path)
                setOnPreparedListener {
                    it.start()
                    onStarted()
                }
                setOnCompletionListener {
                    currentIndex++
                    playNext(paths, onStarted,onStopped)
                }
                setOnErrorListener { _, _, _ ->
                    currentIndex++
                    playNext(paths, onStarted,onStopped)
                    true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            currentIndex++
            playNext(paths, onStarted,onStopped)
        }
    }

    fun stop() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        currentPaths = null
        currentIndex = 0
        onAllComplete = null
    }

    fun isPlaying(paths: List<String>): Boolean {
        return currentPaths == paths && mediaPlayer?.isPlaying == true
    }
}

