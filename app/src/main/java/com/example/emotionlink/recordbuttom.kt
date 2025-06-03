package com.example.emotionlink

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.graphics.drawable.AnimationDrawable
import android.media.MediaRecorder
import android.nfc.Tag
import android.os.CountDownTimer
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.Vibrator
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import com.example.emotionlink.R
import com.example.emotionlink.ViewModel.OverlayViewModel
import java.io.File

@SuppressLint("ViewConstructor")
class RecordButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatButton(context, attrs, defStyleAttr) {
    init {
        gravity = Gravity.CENTER // 让自定义控件文字居中
    }

    var overlayCallback: ((Boolean) -> Unit)? = null

    fun setOverlayDialogCallback(callback: (Boolean) -> Unit) {
        overlayCallback = callback
    }
    fun bindOverlayViewModel(vm: OverlayViewModel) {
        overlayViewModel = vm
    }
    fun setLanguage(lang: String) {
        this.language = lang
    }
    private var language: String?=null
    var overlayViewModel: OverlayViewModel? = null
    private var mFileName: String? = null
    private var finishedListener: OnFinishedRecordListener? = null
    private var startTime: Long = 0
    private var recordDialog: Dialog? = null
    private var recorder: MediaRecorder? = null
    private var view: ImageView? = null
    private var y: Float = 0f
    private val Tag="RecordButton"
    companion object {
        private const val MIN_INTERVAL_TIME = 1000
        private const val MAX_TIME = 60500L
        private val SAVE_PATH = Environment.getExternalStorageDirectory().path + "/acoe/demo/voice"
    }

    private val volumeHandler = Handler(Looper.getMainLooper()) { msg ->
        when (msg.what) {
            -100 -> {
                stopRecording()
                recordDialog?.dismiss()
            }
        }
        true
    }

    fun setOnFinishedRecordListener(listener: OnFinishedRecordListener) {
        finishedListener = listener
    }

    private val startTimer = object : CountDownTimer(MAX_TIME - 10500, 1000) {
        override fun onFinish() {
            recordTimer.start()
        }

        override fun onTick(millisUntilFinished: Long) {}
    }

    private val recordTimer = object : CountDownTimer(10000, 1000) {
        override fun onFinish() {
            finishRecord()
        }

        override fun onTick(millisUntilFinished: Long) {
            // 可根据需要添加UI更新代码
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        y = event.y
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                text = "松开发送"
                overlayViewModel?.resetReleased()
                overlayCallback?.invoke(true) // 👈 显示 OverlayDialog
//                initDialogAndStartRecord()
            }
            MotionEvent.ACTION_MOVE -> {
                val screenPos = IntArray(2)
                getLocationOnScreen(screenPos)
                val absoluteTouchY = screenPos[1].toFloat() + event.y
                val absoluteTouchX = screenPos[0].toFloat() + event.x

                val rect = overlayViewModel?.cancelZoneLocation
                val offset = Offset(absoluteTouchX, absoluteTouchY)
                val isInCancelZone = rect?.contains(offset) == true
                overlayViewModel?.inCancelZone = isInCancelZone

            }
            MotionEvent.ACTION_UP -> {
                text = when (language) {
                    "zh" -> "按住 说话"
                    "en" -> "Hold to Talk"
                    "dialect" -> "揿牢 讲闲话"
                    else -> "按住 说话"
                }
                overlayCallback?.invoke(false)
                startTimer.cancel()
                recordTimer.cancel()
                if (overlayViewModel?.inCancelZone == true) {
                    overlayViewModel?.cancelRecording()
                } else {
                    overlayViewModel?.triggerReleased()
                }
            }

            MotionEvent.ACTION_CANCEL -> {
                overlayCallback?.invoke(false)
                overlayViewModel?.triggerReleased()
            }
        }
        return true
    }

    private fun initDialogAndStartRecord() {
        startTime = System.currentTimeMillis()
        recordDialog = Dialog(context, R.style.like_toast_dialog_style).apply {
            view = ImageView(context)
            view?.setImageResource(android.R.drawable.ic_btn_speak_now)
            setContentView(
                view!!,
                WindowManager.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
            window?.attributes?.gravity = Gravity.CENTER
            setOnDismissListener { stopRecording() }
//            show()
        }
        startRecording()
    }

    private fun startRecording() {
        val path = "$SAVE_PATH/tmp_sound_${System.currentTimeMillis()}.3gp"
        val file = File(path)
        file.parentFile?.mkdirs()
        mFileName = path

        recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            setOutputFile(path)
            try {
                prepare()
                start()
                startTimer.start()
                (context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)?.vibrate(100)
            } catch (e: Exception) {
                Toast.makeText(context, "录音失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun stopRecording() {
        try {
            recorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            // 忽略 stop 时抛出的异常
        }
        recorder = null
    }

    private fun cancelRecord() {
        stopRecording()
        recordDialog?.dismiss()
        mFileName?.let { File(it).delete() }
    }

    private fun finishRecord() {
        val interval = System.currentTimeMillis() - startTime
        if (interval < MIN_INTERVAL_TIME) {
            view?.setImageResource(android.R.drawable.stat_notify_error)
            volumeHandler.sendEmptyMessageDelayed(-100, 1000)
            mFileName?.let { File(it).delete() }
        } else {
            stopRecording()
            recordDialog?.dismiss()
            finishedListener?.onFinishedRecord(mFileName ?: "", (interval / 1000).toInt())
        }
    }

    interface OnFinishedRecordListener {
        fun onFinishedRecord(audioPath: String, time: Int)
    }
}
