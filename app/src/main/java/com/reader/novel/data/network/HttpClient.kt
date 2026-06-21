package com.reader.novel.data.network

import com.reader.novel.util.Constants
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

/**
 * HTTP客户端配置
 * 使用OkHttp作为网络请求库
 *
 * 配置内容：
 * 1. 请求超时时间
 * 2. User-Agent伪装
 * 3. 日志拦截器 (仅Debug模式)
 */
object HttpClient {

    /**
     * 创建OkHttpClient实例
     *
     * @param enableLog 是否启用日志 (Debug模式开启)
     * @return 配置好的OkHttpClient
     */
    fun create(enableLog: Boolean = true): OkHttpClient {
        val builder = OkHttpClient.Builder()
            // 设置连接超时
            .connectTimeout(Constants.CONNECT_TIMEOUT, TimeUnit.SECONDS)
            // 设置读取超时
            .readTimeout(Constants.READ_TIMEOUT, TimeUnit.SECONDS)
            // 设置写入超时
            .writeTimeout(Constants.WRITE_TIMEOUT, TimeUnit.SECONDS)
            // 添加请求头拦截器 - 伪装成浏览器
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("User-Agent", Constants.USER_AGENT)
                    .addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                    .addHeader("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                    .addHeader("Connection", "keep-alive")
                    .build()
                chain.proceed(request)
            }

        // Debug模式下添加日志拦截器
        if (enableLog) {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            builder.addInterceptor(loggingInterceptor)
        }

        return builder.build()
    }
}
