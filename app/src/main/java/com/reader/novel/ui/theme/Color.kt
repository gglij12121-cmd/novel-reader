package com.reader.novel.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * 颜色定义
 * 包含App中使用的所有颜色
 */

// ==================== 主题色 ====================
val Primary = Color(0xFF1976D2)          // 主色调 - 蓝色
val PrimaryVariant = Color(0xFF1565C0)   // 主色调变体 - 深蓝
val Secondary = Color(0xFF4CAF50)        // 辅助色 - 绿色
val SecondaryVariant = Color(0xFF388E3C) // 辅助色变体 - 深绿

// ==================== 背景色 ====================
val Background = Color(0xFFF5F5F5)       // 页面背景 - 浅灰
val Surface = Color(0xFFFFFFFF)          // 卡片/表面背景 - 白色
val Error = Color(0xFFD32F2F)            // 错误色 - 红色

// ==================== 文字色 ====================
val OnPrimary = Color(0xFFFFFFFF)        // 主色上的文字 - 白色
val OnSecondary = Color(0xFFFFFFFF)      // 辅助色上的文字 - 白色
val OnBackground = Color(0xFF212121)     // 背景上的文字 - 深灰
val OnSurface = Color(0xFF212121)        // 表面上的文字 - 深灰
val OnError = Color(0xFFFFFFFF)          // 错误色上的文字 - 白色

// ==================== 暗黑主题色 ====================
val DarkPrimary = Color(0xFF90CAF9)          // 暗黑主题主色 - 浅蓝
val DarkPrimaryVariant = Color(0xFF64B5F6)   // 暗黑主题主色变体
val DarkSecondary = Color(0xFF81C784)        // 暗黑主题辅助色 - 浅绿
val DarkBackground = Color(0xFF121212)       // 暗黑主题背景
val DarkSurface = Color(0xFF1E1E1E)          // 暗黑主题表面
val DarkOnBackground = Color(0xFFE0E0E0)     // 暗黑主题背景文字
val DarkOnSurface = Color(0xFFE0E0E0)        // 暗黑主题表面文字

// ==================== 阅读器背景色 ====================
object ReaderColors {
    /** 白色背景 */
    val White = Color(0xFFFFFFFF)
    /** 护眼绿 */
    val Green = Color(0xFFCCE8CF)
    /** 羊皮纸 */
    val Parchment = Color(0xFFF5E6C8)
    /** 浅黄 */
    val LightYellow = Color(0xFFFDF8E1)
    /** 浅灰 */
    val LightGray = Color(0xFFEEEEEE)
    /** 深色模式 */
    val Dark = Color(0xFF1E1E1E)
    /** 暗黑模式 (纯黑) */
    val Black = Color(0xFF000000)

    /** 所有阅读器背景色列表 */
    val allColors = listOf(White, Green, Parchment, LightYellow, LightGray, Dark, Black)
}

// ==================== 来源标签色 ====================
object SourceColors {
    val Biquge = Color(0xFF4CAF50)   // 笔趣阁 - 绿色
    val Bayi = Color(0xFF2196F3)     // 八一中文网 - 蓝色
    val Qidian = Color(0xFFFF9800)   // 起点 - 橙色
}
