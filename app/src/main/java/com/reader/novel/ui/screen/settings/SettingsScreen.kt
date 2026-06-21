package com.reader.novel.ui.screen.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * 设置页面
 *
 * 功能：
 * 1. 深色模式切换
 * 2. 默认阅读设置
 * 3. 缓存管理
 * 4. 关于信息
 *
 * @param onNavigateBack 返回
 * @param viewModel 设置ViewModel
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // 外观设置
            SettingsSection(title = "外观") {
                // 深色模式
                SettingsSwitch(
                    title = "深色模式",
                    description = "切换深色/浅色主题",
                    icon = Icons.Default.DarkMode,
                    checked = settings.isDarkMode,
                    onCheckedChange = { viewModel.toggleDarkMode() }
                )
            }

            // 阅读设置
            SettingsSection(title = "阅读设置") {
                // 字体大小
                SettingsSlider(
                    title = "默认字体大小",
                    icon = Icons.Default.FormatSize,
                    value = settings.fontSize,
                    onValueChange = { viewModel.updateFontSize(it) },
                    valueRange = 12f..30f,
                    steps = 17,
                    valueText = "${settings.fontSize.toInt()}sp"
                )

                // 行间距
                SettingsSlider(
                    title = "默认行间距",
                    icon = Icons.Default.FormatLineSpacing,
                    value = settings.lineHeight,
                    onValueChange = { viewModel.updateLineHeight(it) },
                    valueRange = 1.0f..2.5f,
                    steps = 14,
                    valueText = "${"%.1f".format(settings.lineHeight)}倍"
                )
            }

            // 数据管理
            SettingsSection(title = "数据管理") {
                SettingsClickable(
                    title = "清除缓存",
                    description = "清除已下载的章节内容",
                    icon = Icons.Default.DeleteSweep,
                    onClick = { viewModel.clearCache() }
                )
            }

            // 关于
            SettingsSection(title = "关于") {
                SettingsClickable(
                    title = "关于应用",
                    description = "小说阅读器 v1.0.0",
                    icon = Icons.Default.Info,
                    onClick = { /* TODO: 显示关于对话框 */ }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

/**
 * 设置分组
 */
@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            Column(content = content)
        }
    }
}

/**
 * 开关设置项
 */
@Composable
private fun SettingsSwitch(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}

/**
 * 滑块设置项
 */
@Composable
private fun SettingsSlider(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    valueText: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = valueText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                steps = steps,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 40.dp)
            )
        }
    }
}

/**
 * 可点击设置项
 */
@Composable
private fun SettingsClickable(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 0.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
