package com.example.emotionlink

import android.os.Bundle
import android.os.VibrationEffect
import android.util.Log
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import java.io.File
import kotlin.math.log10
import android.media.MediaRecorder
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.emotionlink.ViewModel.ChatScreen
import com.example.emotionlink.ViewModel.HomeViewModel
import com.example.emotionlink.ViewModel.LanguageViewModel
import com.example.emotionlink.ViewModel.OverlayViewModel
import timber.log.Timber


class MainActivity : ComponentActivity() {
    private lateinit var viewModel: LanguageViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = androidx.lifecycle.ViewModelProvider(this)[LanguageViewModel::class.java]
        enableEdgeToEdge()
        setContent {
            ChatScreen(
                langue_viewModel = viewModel,
                onRecordButtonPressed = { /* 触发显示 Overlay 的状态，而不是 navigate */ },
                onLanguageSelected = { lang -> viewModel.setLanguage(lang) }
            )
//            val navController = rememberNavController()
//            NavHost(navController = navController, startDestination = "chat") {
//                composable("chat") {
//                    ChatScreen(
//                        viewModel=viewModel,
//                        onRecordButtonPressed = {
//                            navController.navigate("overlay") // 这里跳转
//                        },
//                        onLanguageSelected = { lang -> viewModel.setLanguage(lang) }
//                    )
//                }
//                composable("overlay") {
//                    OverlayScreen(navController)
//                }
//            }
        }
    }
}
@Composable
fun HomeScreen(
    navController: NavHostController,
    viewModel: HomeViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val view = LocalView.current
    Column (
        modifier = Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center, // 垂直居中
        horizontalAlignment = Alignment.CenterHorizontally // 水平居中
    ){
        Box(
            modifier = Modifier
                .size(width = 200.dp, height = 50.dp)
                .background(Color.Blue, shape = RoundedCornerShape(12.dp))
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = {
                            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                            navController.navigate("overlay")
                        }
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Text("")
        }
    }
}



//
//@Composable
//fun OverlayScreen(
//    navController: NavHostController,
//    viewModel: OverlayViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
//) {
//    val context = LocalContext.current
//    val released by viewModel::released
//
//    // 初始化阶段：导航进入页面时触发
//    LaunchedEffect(Unit) {
//        Timber.d("OverlayScreen", "页面初始化命令执行")
//        viewModel.onEnterScreen(context)
//    }
//
//    // 离开页面前逻辑处理（触发功能）
//    LaunchedEffect(released) {
//        if (released) {
//            viewModel.onExitScreen()
//            Timber.d("OverlayScreen", "页面即将关闭，执行清理/启动命令")
//            Toast.makeText(context, "启动功能", Toast.LENGTH_SHORT).show()
//            navController.popBackStack()
//        }
//    }
//
//    Box(
//        modifier = Modifier.fillMaxSize(),
//        contentAlignment = Alignment.Center
//    ) {
//        // 透明的背景覆盖
//        Box(
//            modifier = Modifier
//                .fillMaxSize()
//                .background(Color.Black.copy(alpha = 0.3f)) // 使背景透明
//        )
//
//        // 录音提示界面
//        Column(horizontalAlignment = Alignment.CenterHorizontally) {
//            Text("松开启动功能", color = Color.White)
//            Spacer(modifier = Modifier.height(16.dp))
//            Button(onClick = {
//                Timber.d("OverlayScreen", "点击取消按钮，执行取消前命令")
//                navController.popBackStack()
//            }) {
//                Text("取消")
//            }
//        }
//
//        // 捕捉按住操作
//        Box(
//            modifier = Modifier
//                .fillMaxSize()
//                .background(Color.Transparent)
//                .pointerInput(Unit) {
//                    detectTapGestures(
//                        onPress = {
//                            val success = tryAwaitRelease()
//                            if (success) viewModel.triggerReleased()
//                        }
//                    )
//                }
//        )
//    }
//}