package com.example.emotionlink.ViewModel

import android.util.Log
import android.widget.Toast
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.emotionlink.ViewModel.ChatMessage
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.time.delay
import timber.log.Timber
import kotlin.concurrent.timer
import kotlin.math.log

@OptIn(ExperimentalMaterial3Api::class)
//@Preview
@Composable
fun ChatScreen(
    langue_viewModel: LanguageViewModel,
    onRecordButtonPressed: () -> Unit,
    onLanguageSelected: (String) -> Unit
) {
    val backgroundColor = Color(0xFFF5F5F5)
    val language by langue_viewModel.language.collectAsState()
    var showOverlay by remember { mutableStateOf(false) }
    val overlay_viewModel: OverlayViewModel = viewModel()
    val chatMessages = remember { mutableStateListOf<ChatMessage.Voice>() }


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
                        var expanded by remember { mutableStateOf(false) }

                        IconButton(onClick = { expanded = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "更多"
                            )
                        }

                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Chinese") },
                                onClick = {
                                    expanded = false
                                    onLanguageSelected("zh")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("English") },
                                onClick = {
                                    expanded = false
                                    onLanguageSelected("en")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Dialect") },
                                onClick = {
                                    expanded = false
                                    onLanguageSelected("dialect")
                                }
                            )
                        }
                    }
                )
            },
            bottomBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF5F5F5))
                            .pointerInput(Unit) {
                                while (true) {
                                    awaitPointerEventScope {
                                        val down = awaitFirstDown(requireUnconsumed = false) // 允许消费过的事件
                                        overlay_viewModel.resetReleased()
                                        showOverlay = true
                                        // 等待松手或取消
                                        do {
                                            val event = awaitPointerEvent()
                                        } while (event.changes.any { it.pressed })
                                        println("while后面")

//                                        overlay_viewModel.triggerReleased()
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text( text = when (language) {
                            "zh" -> "按住 说话"
                            "en" -> "Hold to Talk"
                            "dialect" -> "揿牢 讲闲话"
                            else -> "按住 说话"
                        },
                            color = Color.Black)
                    }
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
                    ChatVoiceItem(
                        duration = message.duration,
                        isMe = message.isMe,
                        textContent = message.textContent
                    )
                }
            }
        }
    }
    if (showOverlay) {
        OverlayDialog(
            language=language,
            viewModel = overlay_viewModel.apply {
                // 设置回调
                onVoiceMessageSent = { duration, content ->
                    chatMessages.add(
                        ChatMessage.Voice(
                            duration = duration,
                            textContent = content,
                            isMe = true//后续需要根据后端返回数据做修改
                        )
                    )
                }
            },
            onDismiss = { showOverlay = false },
        )
    }
}


@Composable
fun OverlayDialog(
    language:String,
    onDismiss: () -> Unit,
    viewModel: OverlayViewModel,
) {
    val context = LocalContext.current
    val released by viewModel::released
    var cancelZoneLoaction by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
    var isInCancelZone by remember { mutableStateOf(false) }
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

    // 初始化时执行
    LaunchedEffect(Unit) {
        viewModel.startRecording()
    }
    LaunchedEffect(viewModel.released) {
        println("released"+viewModel.released)
//        if (viewModel.released) {
//            if (!viewModel.inCancelZone) {
//                val recordedInfo = viewModel.getRecordedInfo()
//                recordedInfo?.let { (duration, content) ->
//                    onVoiceMessageSent(duration, content)
//                }
//            }
//            viewModel.onExitScreen()
//            onDismiss()
//        }
    }


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

                        isInCancelZone = cancelZoneLoaction?.contains(currentTouchPosition) == true
                        viewModel.inCancelZone = isInCancelZone
                    },
                    onDragEnd = {
                        if (isInCancelZone) {
                            println("取消了")
                            viewModel.cancelRecording()
                            onDismiss()
                        } else {
                            println("松手了")
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
                .onGloballyPositioned { coords -> cancelZoneLoaction = coords.boundsInRoot() }
                .background(if (isInCancelZone) Color(0xFFD32F2F) else Color.LightGray, shape = CircleShape)
                .clip(RoundedCornerShape(50))
                .padding(horizontal = 24.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = when (language) {
                    "zh" -> "取消"
                    "en" -> "Cancel"
                    "dialect" -> "取消"
                    else -> "取消"},
                color = if (isInCancelZone) Color.Black else Color.White, fontSize = 25.sp
            )
        }

        // 松开发送文字
        Text(
            text = when (language) {
                "zh" -> "松开发送文字"
                "en" -> "Release to Send"
                "dialect" -> "放手发送"
                else -> "松开发送文字"},
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


@Composable
fun ChatVoiceItem(duration: String, isMe: Boolean, textContent: String = "") {
    var showText by remember { mutableStateOf(false) }
    val durationSeconds = duration.filter { it.isDigit() }.toIntOrNull() ?: 1
    val minWidth = 80.dp
    val maxWidth = 220.dp
    val bubbleWidth = minWidth + (maxWidth - minWidth) * (durationSeconds.coerceAtMost(60) / 60f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
    ) {
        if (!isMe) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .align(Alignment.Top)
                    .clip(CircleShape)
                    .background(Color.Gray)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(horizontalAlignment = if (isMe) Alignment.End else Alignment.Start) {
            Surface(
                color = if (isMe) Color(0xFF9EEA6A) else Color.White,
                shape = MaterialTheme.shapes.medium,
                tonalElevation = 2.dp,
                modifier = Modifier.width(bubbleWidth)
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isMe) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "语音播放",
                                tint = Color.Black
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(duration, fontSize = 14.sp, color = Color.Black)

                        } else {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "语音播放",
                                tint = Color.Black
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(duration, fontSize = 14.sp, color = Color.Black)
                        }
                    }
                    if (showText) {
                        Spacer(modifier = Modifier.height(6.dp))
                        var visibleText by remember { mutableStateOf("") }
                        LaunchedEffect(textContent) {
                            visibleText = ""
                            for (i in textContent.indices) {
                                visibleText += textContent[i]
                                kotlinx.coroutines.delay(30)
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

        if (isMe) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .align(Alignment.Top)
                    .clip(CircleShape)
                    .background(Color.Gray)
            )
        }
    }
}

@Composable
fun ChatMessageItem(text: String, isMe: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
    ) {
        if (!isMe) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .align(Alignment.Top)
                    .clip(CircleShape)
                    .background(Color.Gray)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        Surface(
            color = if (isMe) Color(0xFF9EEA6A) else Color.White,
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 2.dp,//阴影纹理
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                text = text,
                modifier = Modifier.padding(12.dp),
                fontSize = 14.sp,
                color = Color.Black
            )
        }

        if (isMe) {
            Spacer(modifier = Modifier.width(4.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .align(Alignment.Top)
                    .clip(CircleShape)
                    .background(Color.Gray)
            )
        }
    }
}
