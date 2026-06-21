package com.reader.novel.ui.screen.settings

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * 设置ViewModel
 * 使用AndroidViewModel以访问Context
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val application: Application
) : AndroidViewModel(application) {

    /** SharedPreferences key */
    companion object {
        const val PREF_NAME = "novel_reader_prefs"
        const val KEY_DARK_MODE = "dark_mode"
        const val KEY_FONT_SIZE = "font_size"
        const val KEY_LINE_HEIGHT = "line_height"
        const val KEY_BG_COLOR_INDEX = "bg_color_index"
    }

    private val prefs = application.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    /** 设置状态 */
    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    /**
     * 从SharedPreferences加载设置
     */
    private fun loadSettings(): AppSettings {
        return AppSettings(
            isDarkMode = prefs.getBoolean(KEY_DARK_MODE, false),
            fontSize = prefs.getFloat(KEY_FONT_SIZE, 18f),
            lineHeight = prefs.getFloat(KEY_LINE_HEIGHT, 1.5f),
            bgColorIndex = prefs.getInt(KEY_BG_COLOR_INDEX, 0)
        )
    }

    /**
     * 保存设置到SharedPreferences
     */
    private fun saveSettings(settings: AppSettings) {
        prefs.edit().apply {
            putBoolean(KEY_DARK_MODE, settings.isDarkMode)
            putFloat(KEY_FONT_SIZE, settings.fontSize)
            putFloat(KEY_LINE_HEIGHT, settings.lineHeight)
            putInt(KEY_BG_COLOR_INDEX, settings.bgColorIndex)
            apply()
        }
    }

    /**
     * 切换深色模式
     */
    fun toggleDarkMode() {
        val newSettings = _settings.value.copy(isDarkMode = !_settings.value.isDarkMode)
        _settings.value = newSettings
        saveSettings(newSettings)
    }

    /**
     * 更新字体大小
     */
    fun updateFontSize(size: Float) {
        val newSettings = _settings.value.copy(fontSize = size)
        _settings.value = newSettings
        saveSettings(newSettings)
    }

    /**
     * 更新行间距
     */
    fun updateLineHeight(height: Float) {
        val newSettings = _settings.value.copy(lineHeight = height)
        _settings.value = newSettings
        saveSettings(newSettings)
    }

    /**
     * 更新背景颜色索引
     */
    fun updateBgColorIndex(index: Int) {
        val newSettings = _settings.value.copy(bgColorIndex = index)
        _settings.value = newSettings
        saveSettings(newSettings)
    }

    /**
     * 清除缓存
     */
    fun clearCache() {
        // TODO: 实现清除缓存功能
    }
}

/**
 * App设置数据类
 */
data class AppSettings(
    val isDarkMode: Boolean = false,
    val fontSize: Float = 18f,
    val lineHeight: Float = 1.5f,
    val bgColorIndex: Int = 0
)
