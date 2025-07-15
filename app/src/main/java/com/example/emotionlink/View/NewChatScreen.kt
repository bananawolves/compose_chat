package com.example.emotionlink.View

import AudioPlayerManager
import android.annotation.SuppressLint
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.emotionlink.R
import com.example.emotionlink.ViewModel.ChatViewModel
import com.example.emotionlink.ViewModel.LanguageViewModel
import com.example.emotionlink.ViewModel.OverlayViewModel
import com.example.emotionlink.ViewModel.OverlayViewModelFactory
import com.example.emotionlink.ViewModel.VoiceCallViewModel
import com.example.emotionlink.data.ChatMessage
import kotlinx.coroutines.delay
@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewChatScreen(
    langue_viewModel: LanguageViewModel,
    voiceCallViewModel: VoiceCallViewModel,
    onLanguageSelected: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToCall: () -> Unit,
    chatViewModel: ChatViewModel = viewModel(),
) {
    val backgroundColor = Color(0xFFF5F5F5)
    val language by langue_viewModel.language.collectAsState()
    var showOverlay by remember { mutableStateOf(false) }
    val overlay_viewModel: OverlayViewModel =
        viewModel(factory = OverlayViewModelFactory(langue_viewModel))
    val isReceive by overlay_viewModel.isReceive.collectAsState()
    val chatMessages by chatViewModel.chatVoiceItems.collectAsState()
    val currentPlayingId by chatViewModel.currentPlayingId.collectAsState()
    val listState = rememberLazyListState()
    var showDialog by remember { mutableStateOf(false) }
    voiceCallViewModel.bindOverlayViewModel(overlay_viewModel)
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
                                    "cn" -> "情译同传"
                                    "en" -> "EmotionLink"
                                    "sh" -> "情译（方言）"
                                    else -> "情译同传(请选择语言环境)"
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(WindowInsets.navigationBars.asPaddingValues())
                        .height(80.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 录音按钮占据剩余空间
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        AndroidView(
                            modifier = Modifier.fillMaxSize(),
                            factory = { ctx ->
                                RecordButton(ctx).apply {
                                    text = when (language) {
                                        "cn" -> "按住 说话"
                                        "en" -> "Hold to Talk"
                                        "sh" -> "揿牢 讲闲话"
                                        else -> "按住 说话"
                                    }
                                    setLanguage(language)
                                    setOverlayDialogCallback { visible ->
                                        showOverlay = visible
                                    }
                                    bindOverlayViewModel(overlay_viewModel)
                                }
                            }
                        )
                    }

                    // + 按钮固定宽度靠右
                    Box(
                        modifier = Modifier
                            .width(80.dp)
                            .fillMaxHeight()
                            .background(Color(0xFF448AFF))
                            .clickable { showDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        ) { paddingValues ->
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(backgroundColor),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(chatMessages.size) { index ->
                    val message = chatMessages[index]
                    NewChatVoiceItem(
                        message = message,
                        currentPlayingId = currentPlayingId,
                        onPlay = { id ->
                            chatViewModel.setPlayingId(id)
                        },
                        onStop = {
                            chatViewModel.setPlayingId(null)
                        }
                    )
                }
            }
        }
    }

    //显示语音通话选择弹窗
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("选择用户通话") },
            text = {
                val allUsers = listOf("en", "cn", "sh")
                val otherUsers = allUsers.filter { it != language }

                Column {
                    otherUsers.forEach { userId ->
                        Text(
                            text = "用户：$userId",
                            modifier = Modifier
                                .clickable {
                                    voiceCallViewModel.setTargetUser(userId) // 设置目标用户
                                    voiceCallViewModel.setCurrentLanguage(language) // 设置目标用户
                                    voiceCallViewModel.startCall()
                                    showDialog = false
                                    onNavigateToCall()
                                }
                                .padding(8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    //聚焦最新的消息，避免手动下滑去找新消息
    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.size - 1)
        }
    }
    //解决首次登录时需要发送音频才能接收消息的问题（初始化就可以接收回调）
    LaunchedEffect(isReceive) {
        //语音url追加
        overlay_viewModel.onVoiceMessageSent =
            { duration, content, isMe, fromLanguage, toLanguage, audioUrl ->
                val isEnd = audioUrl.endsWith("end")
                val cleanPath = if (isEnd) audioUrl.removeSuffix(",end") else audioUrl

                chatViewModel.appendOrUpdateVoiceUrl(
                    duration = duration,
                    content = content,
                    isMe = isMe,
                    fromLanguage = fromLanguage,
                    toLanguage = toLanguage,
                    audioUrl = cleanPath,
                    isEnd = isEnd
                )
            }
//        语音列表
//                chatViewModel.addVoiceMessage( //用chatViewModel管理
//                    ChatMessage.Voice(
//                        duration = duration,
//                        textContent = content,
//                        isMe = isMe,
//                        fromLanguage = fromLanguage,
//                        toLanguage = toLanguage,
//                        audioUrl = audioUrl
//                    )
//                )
//            }
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NewChatVoiceItem(
    message: ChatMessage.Voice,
    currentPlayingId: String?,
    onPlay: (String) -> Unit,
    onStop: () -> Unit
) {
    var showText by remember { mutableStateOf(false) }
    val durationSeconds =
        message.duration.filter { it.isDigit() }.toIntOrNull()?.coerceIn(1, 60) ?: 1
    val minWidth = 80.dp
    val maxWidth = 240.dp
    val bubbleWidth = minWidth + (maxWidth - minWidth) * (durationSeconds / 60f)
    val isPlaying = message.id == currentPlayingId
    // 添加旋转动画效果
    val rotation by animateFloatAsState(
        targetValue = if (isPlaying) 360f else 0f,
        animationSpec = if (isPlaying) {
            infiniteRepeatable(
                animation = tween(1000),
                repeatMode = RepeatMode.Restart
            )
        } else {
            tween(0)
        }
    )

    val audioPath = message.getAudioPath()
    val audioPaths = audioPath.split(",")
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        alpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 300)
        )
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha.value)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = if (message.isMe) Arrangement.End else Arrangement.Start
    ) {
        //其他人的头像
        if (!message.isMe) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    painter = painterResource(id = R.drawable.avatar), // 本地头像资源
                    contentDescription = "User Avatar",
                    contentScale = ContentScale.Crop, // 图片裁剪填满容器
                    modifier = Modifier.size(36.dp) // 方形大小
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(horizontalAlignment = if (message.isMe) Alignment.End else Alignment.Start) {
            if (message.isMe) {
                // 自己的名字显示在右侧气泡上方
                Text(
                    text = when (message.fromLanguage) {
                        "cn" -> "中文"
                        "en" -> "英文"
                        "sh" -> "方言"
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
                        "cn" -> "中文"
                        "en" -> "英文"
                        "sh" -> "方言"
                        else -> "张三"
                    },
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier
                        .padding(end = 4.dp, bottom = 2.dp)
                        .align(Alignment.Start)
                )
            }
            //语言消息区
            Surface(
                color = if (message.isMe) Color(0xFF9EEA6A) else Color.White,
                shape = MaterialTheme.shapes.medium,
                tonalElevation = 2.dp,
                modifier = Modifier.wrapContentWidth()
            ) {
                Column(
                    modifier = Modifier
                        .width(bubbleWidth)
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = {
                                    if (!AudioPlayerManager.isPlaying(audioPaths)) {
                                        onPlay(message.id)
                                        AudioPlayerManager.play(
                                            paths = audioPaths,
                                            onStarted = { },
                                            onStopped = { onStop() }
                                        )
                                    } else {
                                        onStop()
                                        AudioPlayerManager.stop()
                                    }
                                },
                                onLongPress = {
                                    if (!message.isMe) {
                                        showText = !showText
                                    }
                                }
                            )
                        }
                ) {
                    //语言消息图标
                    Row(verticalAlignment = Alignment.CenterVertically)
                    {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "语音播放",
                            tint = Color.Black,
                            modifier = Modifier
                                .size(24.dp)
                                .graphicsLayer {
                                    rotationZ = rotation
                                }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(message.duration, fontSize = 14.sp, color = Color.Black)
                    }
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
                Surface(
                    color = Color.White,
                    shape = MaterialTheme.shapes.medium,
                    tonalElevation = 2.dp,
                    modifier = Modifier.width(250.dp)
                ) {
                    Text(
                        text = visibleText,
                        fontSize = 13.sp,
                        color = Color.DarkGray,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
            //语言转文字显示区
            if (showText) {
                TextButton(
                    onClick = { showText = false },
                    modifier = Modifier.defaultMinSize(minHeight = 16.dp)
                ) {
                    Text(
                        "收起",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
        }

        //右侧头像
        if (message.isMe) {
            Spacer(modifier = Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    painter = painterResource(id = R.drawable.avatar), // 本地头像资源
                    contentDescription = "User Avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(36.dp)
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
    var cancelZoneLoaction by remember { mutableStateOf<Rect?>(null) }
    val isInCancelZone by viewModel::inCancelZone

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
            .background(Color.Black.copy(alpha = 0.3f)),
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
                    "cn" -> "取消"
                    "en" -> "Cancel"
                    "sh" -> "取消"
                    else -> "取消"
                },
                color = if (isInCancelZone) Color.Black else Color.White, fontSize = 23.sp
            )
        }

        // 录音提示区
        Text(
            text = when (language) {
                "cn" -> "松开发送文字"
                "en" -> "Release to Send"
                "sh" -> "放手发送"
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
