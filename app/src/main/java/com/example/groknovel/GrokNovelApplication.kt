package com.example.groknovel

import android.app.Application
import com.example.groknovel.data.StoryDatabase

/**
 * Application 类，用于初始化全局资源
 */
class GrokNovelApplication : Application() {

    // 延迟初始化数据库实例
    val database: StoryDatabase by lazy {
        StoryDatabase.getInstance(this)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: GrokNovelApplication
            private set
    }
}
