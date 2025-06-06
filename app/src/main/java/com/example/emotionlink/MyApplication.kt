package com.example.emotionlink

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context


class MyApplication : Application() {
    //全局可用的context
    companion object {
        //忽略内存泄露提醒，由于是Application中的Context，它全局只会存在一份实例，
        //并且在整个应用程序的生命周期内都不会回收
        @SuppressLint("StaticFieldLeak")
        lateinit var context: Context
    }
    override fun onCreate() {
        super.onCreate()
        context=applicationContext
    }
}