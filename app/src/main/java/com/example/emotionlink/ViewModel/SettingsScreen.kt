package com.example.emotionlink.ViewModel

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.emotionlink.data.UserProfileManager
import coil.compose.AsyncImage
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onLanguageChosen: (String) -> Unit,
    onBack: () -> Unit // 添加返回事件回调
) {
    val context = LocalContext.current
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            selectedImageUri = it
            UserProfileManager.avatarUri = it
            Toast.makeText(context, "头像选择成功：$uri", Toast.LENGTH_SHORT).show()
        }
    }

    val displayUri = selectedImageUri ?: UserProfileManager.avatarUri

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("选择语音环境", fontSize = 20.sp)

            Button(onClick = { onLanguageChosen("cn") }) {
                Text("中文")
            }

            Button(onClick = { onLanguageChosen("en") }) {
                Text("英文")
            }

            Button(onClick = { onLanguageChosen("sh") }) {
                Text("方言")
            }

            Button(onClick = { onLanguageChosen("11") }) {
                Text("重启服务中转")
            }
            Spacer(modifier = Modifier.height(24.dp))

            Text("头像设置", fontSize = 20.sp)
            Button(onClick = {
                launcher.launch("image/*")
            }) {
                Text("从相册选择头像")
            }

            displayUri?.let { uri ->
                AsyncImage(
                    model = uri,
                    contentDescription = "头像",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(200.dp)
                        .clip(CircleShape)
                )
            }
        }
    }
}
