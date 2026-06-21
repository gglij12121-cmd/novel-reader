package com.reader.novel

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application类
 *
 * @HiltAndroidApp 注解：
 * - 触发Hilt的代码生成
 * - 创建应用级别的依赖容器
 * - 是使用Hilt的入口点
 */
@HiltAndroidApp
class NovelReaderApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // 可以在这里初始化一些全局配置
    }
}
