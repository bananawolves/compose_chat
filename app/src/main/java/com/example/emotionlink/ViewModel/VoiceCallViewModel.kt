package com.example.emotionlink.ViewModel

import android.Manifest
import android.app.Application
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresPermission
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.emotionlink.Utils.LogUtils
import com.example.emotionlink.WebSocket.CallMessageCallback
import com.example.emotionlink.WebSocket.MessageCallback
import com.example.emotionlink.WebSocket.WebSocketStatusListener
import com.example.emotionlink.WebSocket.WebsocketUploaderManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs

class VoiceCallViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "VoiceCallViewModel"
    private val audioManager = application.getSystemService(Application.AUDIO_SERVICE) as AudioManager
    private val mainHandler = Handler(Looper.getMainLooper())

    private var audioRecord: AudioRecord? = null
    private var mediaPlayer: MediaPlayer? = null
    private var isRecording = AtomicBoolean(false)
    private var isPlaying = AtomicBoolean(false)
    private var recordingJob: Job? = null
    
    // VAD相关参数
    private val SILENCE_THRESHOLD = 400 // 静音阈值，可以根据实际情况调整
    private val SILENCE_DURATION = 1000L // 静音持续时间（毫秒）
    private var lastVoiceTime = 0L // 上次检测到声音的时间
    private var isVoiceActive = false // 是否正在说话
    
    private var remoteUserId: String? = null
    private var callId: String? = null
    private var _language: String=""
    private val _currentTargetUser = MutableStateFlow<String?>(null)
    val currentTargetUser: StateFlow<String?> = _currentTargetUser
    //直接从overlayViewModel里获得wsClient实例
    var overlayViewModel: OverlayViewModel? = null
    fun bindOverlayViewModel(vm: OverlayViewModel) {
        overlayViewModel = vm
    }
    private var wsClient = overlayViewModel?.wsClient

    fun setTargetUser(userId: String) {
        _currentTargetUser.value = userId
    }

    fun setCurrentLanguage(language: String) {
        _language = language
    }
    // 状态流
    private val _callState = MutableStateFlow(CallState.IDLE)
    val callState: StateFlow<CallState> = _callState.asStateFlow()
    
    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()
    
    private val _isSpeakerOn = MutableStateFlow(true)
    val isSpeakerOn: StateFlow<Boolean> = _isSpeakerOn.asStateFlow()

    private val _incomingCall = MutableStateFlow<String?>(null) // userId 或 null
    val incomingCall: StateFlow<String?> = _incomingCall.asStateFlow()

    fun notifyIncomingCall(fromUserId: String) {
        _incomingCall.value = fromUserId
    }

    enum class CallState {
        IDLE,
        CALLING,
        CONNECTED,
        ENDED
    }

    init {
        LogUtils.d(TAG,"$TAG init")
        WebsocketUploaderManager.setCallMessageCallback(object : CallMessageCallback {
            override fun onCallMessageReceived(
                fromLang: String,
                toLang: String,
                text: String,
                wavUrl: String,
                duration: String
            ) {
//                LogUtils.d(TAG,  "来自$fromLang,目标$toLang,文字$text,是否相等${text == wavUrl},"
//                        + "音频地址: $wavUrl,持续时间$duration\"")
                startPlaying()
//                onAudioDataReceived(wavUrl)
                wsClient?.sendInit()
            }

            @RequiresPermission(Manifest.permission.RECORD_AUDIO)
            override fun onIncomingCall(fromUserId: String) {
                LogUtils.d(TAG, "📞 来电：$fromUserId")
                notifyIncomingCall(fromUserId)
                acceptCall()
            }

            override fun onError(e: Exception) {
            }
        })
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun startCall() {
        if (isRecording.get() || isPlaying.get()) return
        LogUtils.d(TAG,_language)
//        wsClient?.sendInit()
//        wsClient?.sendCallRequest(_language,remoteUserId)
        if (wsClient == null) {
            LogUtils.d(TAG, "WebSocket未初始化，正在初始化...")
            // 初始化WebSocket
            WebsocketUploaderManager.initUploader(_language, object : WebSocketStatusListener {
                @RequiresPermission(Manifest.permission.RECORD_AUDIO)
                override fun onConnected() {
                    LogUtils.d(TAG, "WebSocket连接成功，开始通话")
                    wsClient = WebsocketUploaderManager.getUploader()
                    wsClient?.sendCallRequest(_language)
                    wsClient?.sendInit()
                }

                override fun onError(e: Exception) {
                    LogUtils.e(TAG, "WebSocket连接失败: ${e.message}")
                    updateCallState(CallState.ENDED)
                }
            })
            return
        }
        initializeAudio()
        startRecording()
        updateCallState(CallState.CALLING)
    }
    
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun acceptCall() {
        if (isRecording.get() || isPlaying.get()) return

        wsClient?.sendAcceptRequest(_language)//代改，应该用回调来显示谁拨打的电话

        initializeAudio()
        startRecording()
        updateCallState(CallState.CONNECTED)
    }
    
    fun rejectCall() {
        if (_currentTargetUser.value == null||_language.isEmpty()) return
        

        wsClient?.sendReject(_language)//代改
        updateCallState(CallState.ENDED)
    }
    
    fun endCall() {
        if (_currentTargetUser.value != null && _language.isNotEmpty()) {
            wsClient?.sendCallEnd(_language)//代改
        }
        stopRecording()
        stopPlaying()
        releaseResources()
        updateCallState(CallState.ENDED)
    }
    
    fun toggleMute() {
        _isMuted.value = !_isMuted.value
        audioRecord?.let { record ->
            if (_isMuted.value) {
                record.stop()
            } else {
                record.startRecording()
            }
        }
    }
    
    fun toggleSpeaker() {
        _isSpeakerOn.value = !_isSpeakerOn.value
        audioManager.isSpeakerphoneOn = _isSpeakerOn.value
    }

    //设置音频播放模式。双向传输
    private fun initializeAudio() {
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        audioManager.isSpeakerphoneOn = _isSpeakerOn.value
    }

    //开始录音
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun startRecording() {
        if (isRecording.get()) return
        
        val minBufferSize = AudioRecord.getMinBufferSize(
            16000,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )*10
        
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.VOICE_COMMUNICATION,
            16000,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            minBufferSize
        )
        
        isRecording.set(true)
        LogUtils.d(TAG,"开始录音")
        recordingJob = CoroutineScope(Dispatchers.IO).launch {
            val buffer = ByteArray(minBufferSize)
            audioRecord?.startRecording()
            
            while (isRecording.get()) {
                val readSize = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                if (readSize > 0) {
                    // 检测是否有声音
                    val hasVoice = detectVoice(buffer, readSize)
                    val currentTime = System.currentTimeMillis()
                    
                    if (hasVoice) {
                        lastVoiceTime = currentTime
                        if (!isVoiceActive) {
                            // 开始新的语音段
                            isVoiceActive = true
                            LogUtils.d(TAG, "检测到声音，开始发送音频")
                            wsClient?.sendInit()
                        }
                        // 发送音频数据
                        wsClient?.sendAudioChunk(buffer, readSize)
                    } else if (isVoiceActive && (currentTime - lastVoiceTime > SILENCE_DURATION)) {
                        // 静音超过阈值，结束当前语音段
                        isVoiceActive = false
                        LogUtils.d(TAG, "检测到静音，停止发送音频")
                        wsClient?.sendEnd("1")
                    }
                }
            }
        }
    }
    
    private fun startPlaying() {
        if (isPlaying.get()) return
        isPlaying.set(true)
    }
    
    private fun stopRecording() {
        isRecording.set(false)
        recordingJob?.cancel()
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
    }
    
    private fun stopPlaying() {
        isPlaying.set(false)
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }
    
    private fun releaseResources() {
        audioManager.mode = AudioManager.MODE_NORMAL
        audioManager.isSpeakerphoneOn = false
        callId = null
        remoteUserId = null
    }
    
    private fun updateCallState(state: CallState) {
        mainHandler.post {
            _callState.value = state
        }
    }
    
    private fun onAudioDataReceived(wavUrl: String) {
        try {
            LogUtils.d(TAG, "播放音频")
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                setDataSource(wavUrl)
                setOnPreparedListener {
                    LogUtils.d(TAG, "MediaPlayer 准备完毕，开始播放")
                    it.start()
                }

                setOnErrorListener { mp, what, extra ->
                    LogUtils.e(TAG, "MediaPlayer 播放出错 what=$what extra=$extra")
                    true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            LogUtils.e(TAG, "播放音频失败: ${e.message}")
        }
    }
    
    // 检测是否有声音
    private fun detectVoice(buffer: ByteArray, size: Int): Boolean {
        var sum = 0.0
        // 将字节数组转换为短整型数组（16位音频）
        val shorts = ShortArray(size / 2)
        for (i in 0 until size / 2) {
            shorts[i] = (buffer[i * 2].toInt() and 0xFF or (buffer[i * 2 + 1].toInt() shl 8)).toShort()
        }
        
        // 计算音频数据的能量
        for (i in shorts.indices) {
            sum += abs(shorts[i].toDouble())
        }
        val average = sum / shorts.size
        
        return average > SILENCE_THRESHOLD
    }
    
    override fun onCleared() {
        super.onCleared()
        endCall()
    }
} 