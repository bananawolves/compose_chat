package com.example.emotionlink.ViewModel

import AudioPlayerManager
import android.util.Log
import android.view.Gravity
import android.view.View.TEXT_ALIGNMENT_CENTER
import android.widget.Toast
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.emotionlink.RecordButton
import com.example.emotionlink.RecordButton.OnFinishedRecordListener
import com.example.emotionlink.data.ChatMessage
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewChatScreen(
    langue_viewModel: LanguageViewModel,
    onLanguageSelected: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    chatViewModel: ChatViewModel = viewModel()
) {
    val backgroundColor = Color(0xFFF5F5F5)
    val language by langue_viewModel.language.collectAsState()
    var showOverlay by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val overlay_viewModel: OverlayViewModel =
        viewModel(factory = OverlayViewModelFactory(context, langue_viewModel))
    val isReceive by overlay_viewModel.isReceive.collectAsState()
    val chatMessages by chatViewModel.chatVoiceItems.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    modifier = Modifier.height(68.dp),
                    navigationIcon = {
                        IconButton(onClick = { }) {
                            Spacer(modifier = Modifier.size(24.dp))
                        }
                    },
                    title = {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = when (language) {
                                    "zh" -> "情译同传"
                                    "en" -> "EmotionLink"
                                    "dialect" -> "情译（方言）"
                                    else -> "情译同传"
                                },
                                fontSize = 18.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { onNavigateToSettings() }) {
                            Icon(Icons.Default.Settings, contentDescription = "设置")
                        }
                    }
                )
            },
            bottomBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(WindowInsets.navigationBars.asPaddingValues()) //避开手机自带的底部导航栏遮挡

                        .height(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AndroidView(
                        modifier = Modifier
                            .fillMaxSize(),
                        factory = { ctx ->
                            RecordButton(ctx).apply {
                                text = when (language) {
                                    "zh" -> "按住 说话"
                                    "en" -> "Hold to Talk"
                                    "dialect" -> "揿牢 讲闲话"
                                    else -> "按住 说话"
                                }
                                setLanguage(language)
                                setOverlayDialogCallback { visible ->
                                    showOverlay = visible
                                }
                                bindOverlayViewModel(overlay_viewModel)
                                setOnFinishedRecordListener(object : OnFinishedRecordListener {
                                    override fun onFinishedRecord(audioPath: String, time: Int) {
                                        Toast.makeText(
                                            ctx,
                                            "录音完成: $audioPath ($time 秒)",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                })
                            }
                        }
                    )
                }
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(backgroundColor),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(chatMessages.size) { index ->
                    val message = chatMessages[index]
                    NewChatVoiceItem(message = message)
                }
            }
        }
    }

    //解决首次登录时需要发送音频才能接收消息的问题（初始化就可以接收回调）
    LaunchedEffect(isReceive) {
        overlay_viewModel.onVoiceMessageSent =
            { duration, content, isMe, fromLanguage, toLanguage, audioUrl ->
                chatViewModel.addVoiceMessage( //用chatViewModel管理
                    ChatMessage.Voice(
                        duration = duration,
                        textContent = content,
                        isMe = isMe,//后续需要根据后端返回数据做修改
                        fromLanguage=fromLanguage,
                        toLanguage=toLanguage,
                        audioUrl = audioUrl
                    )
                )
            }
        overlay_viewModel.setReceiveState(false)
    }

    if (showOverlay) {
        NewOverlayDialog(
            language = language,
            viewModel = overlay_viewModel,
            onDismiss = { showOverlay = false },
        )
    }
}

@Composable
fun NewChatVoiceItem(message: ChatMessage.Voice) {
    var showText by remember { mutableStateOf(false) }
    val durationSeconds = message.duration.filter { it.isDigit() }.toIntOrNull() ?: 1
    val minWidth = 80.dp
    val maxWidth = 220.dp
    val bubbleWidth = minWidth + (maxWidth - minWidth) * (durationSeconds.coerceAtMost(60) / 60f)
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    val audioPath = message.getAudioPath()
//    val avatarUri = UserProfileManager.avatarUri
    LaunchedEffect(audioPath) {
        isPlaying = AudioPlayerManager.isPlaying(audioPath)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = if (message.isMe) Arrangement.End else Arrangement.Start
    ) {
        if (!message.isMe) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.Gray)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(horizontalAlignment = if (message.isMe) Alignment.End else Alignment.Start) {
            if (message.isMe) {
                // 自己的名字显示在右侧气泡上方
                Text(
                    text = when (message.fromLanguage) {
                        "zh" -> "中文"
                        "en" -> "英文"
                        "dialect" -> "方言"
                        else -> "张三"
                    },
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier
                        .padding(end = 4.dp, bottom = 2.dp)
                        .align(Alignment.End)
                )
            } else {
                Text(
                    text = when (message.fromLanguage) {
                        "zh" -> "中文"
                        "en" -> "英文"
                        "dialect" -> "方言"
                        else -> "张三"
                    },
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier
                        .padding(end = 4.dp, bottom = 2.dp)
                        .align(Alignment.Start)
                )
            }
            Surface(
                color = if (message.isMe) Color(0xFF9EEA6A) else Color.White,
                shape = MaterialTheme.shapes.medium,
                tonalElevation = 2.dp,
                modifier = Modifier.width(bubbleWidth)
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable {
                            if (!AudioPlayerManager.isPlaying(audioPath)) {
                                AudioPlayerManager.play(
                                    context = context,
                                    path = audioPath,
                                    onStarted = { isPlaying = true },
                                    onStopped = { isPlaying = false }
                                )
                            } else {
                                AudioPlayerManager.stop()
                                isPlaying = false
                            }

                        }) {
                        if (message.isMe) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "语音播放",
                                tint = Color.Black
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(message.duration, fontSize = 14.sp, color = Color.Black)

                        } else {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "语音播放",
                                tint = Color.Black
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(message.duration, fontSize = 14.sp, color = Color.Black)
                        }
                    }
                    if (showText) {
                        Spacer(modifier = Modifier.height(6.dp))
                        var visibleText by remember { mutableStateOf("") }
                        LaunchedEffect(message.textContent) {
                            visibleText = ""
                            for (i in message.textContent.indices) {
                                visibleText += message.textContent[i]
                                delay(30)
                            }
                        }
                        Text(
                            text = visibleText,
                            fontSize = 13.sp,
                            color = Color.DarkGray
                        )
                    }
                }
            }
            TextButton(
                onClick = { showText = !showText },
                modifier = Modifier.defaultMinSize(minHeight = 16.dp)
            ) {
                Text(
                    if (showText) "收起" else "转文字",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
        if (message.isMe) {
            Spacer(modifier = Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.Gray)
                )
            }
        }

    }
}

@Composable
fun NewOverlayDialog(
    language: String,
    onDismiss: () -> Unit,
    viewModel: OverlayViewModel,
) {
    val context = LocalContext.current
    val released by viewModel::released
    var cancelZoneLoaction by remember { mutableStateOf<Rect?>(null) }
    val isInCancelZone by viewModel::inCancelZone
    var currentTouchPosition by remember { mutableStateOf(Offset.Zero) }

    // 跳动动画
    val infiniteTransition = rememberInfiniteTransition(label = "mic_jump")
    val micOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -16f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mic_bounce"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.3f))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        currentTouchPosition = offset
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        currentTouchPosition = change.position

//                        isInCancelZone = cancelZoneLoaction?.contains(currentTouchPosition) == true
//                        viewModel.inCancelZone = isInCancelZone
                    },
                    onDragEnd = {
                        if (isInCancelZone) {
                            viewModel.cancelRecording()
                            onDismiss()
                        } else {
                            viewModel.triggerReleased()
                            onDismiss()
                        }
                    },
                    onDragCancel = {
                        viewModel.triggerReleased() // 或者 cancelRecording，看你希望默认行为是什么
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // 取消按钮
        Box(
            modifier = Modifier
                .size(125.dp)
                .fillMaxWidth(0.1f)
                .fillMaxHeight(0.1f)
                .align(Alignment.Center)
                .offset(y = (20).dp)
                .onGloballyPositioned { coords ->
                    cancelZoneLoaction = coords.boundsInRoot()
                    viewModel.cancelZoneLocation = cancelZoneLoaction
                }
                .background(
                    if (isInCancelZone) Color(0xFFD32F2F) else Color.LightGray,
                    shape = CircleShape
                )
                .clip(RoundedCornerShape(50))
                .padding(horizontal = 24.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = when (language) {
                    "zh" -> "取消"
                    "en" -> "Cancel"
                    "dialect" -> "取消"
                    else -> "取消"
                },
                color = if (isInCancelZone) Color.Black else Color.White, fontSize = 23.sp
            )
        }

        // 松开发送文字
        Text(
            text = when (language) {
                "zh" -> "松开发送文字"
                "en" -> "Release to Send"
                "dialect" -> "放手发送"
                else -> "松开发送文字"
            },
            color = Color.White,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (100).dp),
            fontSize = 25.sp
        )

        // 扇形 + 图标区域（底部 1/5）
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.3f)
                .align(Alignment.BottomCenter)
                .background(
                    color = if (isInCancelZone) Color.LightGray else Color(0xFFF5F5F5),
                    shape = RoundedCornerShape(topStart = 100.dp, topEnd = 100.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = "录音",
                tint = Color.Black,
                modifier = Modifier
                    .size(48.dp)
                    .offset(y = micOffset.dp)
            )
        }
    }
}
