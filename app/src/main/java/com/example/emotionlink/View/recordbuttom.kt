package com.example.emotionlink.View

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import androidx.compose.ui.geometry.Offset
import com.example.emotionlink.ViewModel.OverlayViewModel

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

    private var language: String? = null
    var overlayViewModel: OverlayViewModel? = null
    private var startTime: Long = 0
    private var y: Float = 0f

    companion object {
        private const val MIN_INTERVAL_TIME = 500
    }


    override fun onTouchEvent(event: MotionEvent): Boolean {
        y = event.y
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                text = "松开发送"
                overlayViewModel?.resetReleased()
                overlayCallback?.invoke(true) // 👈 显示 OverlayDialog
                startTime = System.currentTimeMillis()
                overlayViewModel?.inCancelZone = false // ✅ 这里重置状态
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
                    "cn" -> "按住 说话"
                    "en" -> "Hold to Talk"
                    "sh" -> "揿牢 讲闲话"
                    else -> "按住 说话"
                }
                overlayCallback?.invoke(false)
                val duration = System.currentTimeMillis() - startTime
                if (duration < MIN_INTERVAL_TIME) {
                    overlayViewModel?.inCancelZone = true
                    overlayViewModel?.cancelRecording()
                    Toast.makeText(context, "录音时间太短", Toast.LENGTH_SHORT).show()
                } else {
                    overlayViewModel?.cancelRecording()
                }
            }

            MotionEvent.ACTION_CANCEL -> {
                overlayCallback?.invoke(false)
                overlayViewModel?.cancelRecording()
            }
        }
        return true
    }
}
