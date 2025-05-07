package com.example.emotionlink.ViewModel

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun ChatScreen() {
    val backgroundColor = Color(0xFFF5F5F5)

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
                            text = "情译同传",
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO: 设置操作 */ }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "更多"
                        )
                    }
                }
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick = { /* TODO: 底部按钮操作 */ },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = backgroundColor) // 骨白色
                ) {
                    Text("按住  说话", color = Color.Black)
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
            item {
                ChatMessageItem("你好，这是第一条消息。", isMe = false)
            }
            item {
                ChatMessageItem("这是一条比较长的消息内容，用于测试自动换行和适配效果。", isMe = true)
            }
            item {
                ChatMessageItem("你好！很高兴见到你。", isMe = false)
            }
            item {
                ChatVoiceItem(duration = "8''", isMe = true, textContent = "你好，欢迎来到上海TeleAI")
            }
            item {
                ChatVoiceItem(duration = "5''", isMe = false, textContent = "很高兴与你相遇")
            }
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
            tonalElevation = 2.dp,
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
