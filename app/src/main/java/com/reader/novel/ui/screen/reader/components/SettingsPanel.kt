package com.reader.novel.ui.screen.reader.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reader.novel.ui.screen.reader.PageFlipMode
import com.reader.novel.ui.screen.reader.ReaderSettings
import com.reader.novel.ui.theme.ReaderColors

/**
 * 字体配置
 */
data class FontConfig(
    val name: String,
    val fontFamily: FontFamily
)

/**
 * 预设字体列表
 */
val presetFonts = listOf(
    FontConfig("默认", FontFamily.Default),
    FontConfig("宋体", FontFamily.Serif),
    FontConfig("黑体", FontFamily.SansSerif),
    FontConfig("等宽", FontFamily.Monospace),
    FontConfig("楷体", FontFamily.Cursive)
)

/**
 * 阅读器设置面板 - 全新设计
 *
 * 功能：
 * 1. 字体选择
 * 2. 字体大小
 * 3. 行间距
 * 4. 背景颜色（护眼模式）
 * 5. 翻页模式
 */
@Composable
fun ReaderSettingsPanel(
    settings: ReaderSettings,
    onFontSizeChange: (Float) -> Unit,
    onLineHeightChange: (Float) -> Unit,
    onBgColorChange: (Int) -> Unit,
    onFontChange: (Int) -> Unit,
    onPageFlipModeChange: (PageFlipMode) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 标题栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "阅读设置",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "关闭"
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 字体选择
            Text(
                text = "字体",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(presetFonts) { index, font ->
                    FontChip(
                        name = font.name,
                        isSelected = settings.fontIndex == index,
                        onClick = { onFontChange(index) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 字体大小
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "字体大小",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "${settings.fontSize.toInt()}sp",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Slider(
                value = settings.fontSize,
                onValueChange = onFontSizeChange,
                valueRange = 12f..30f,
                steps = 17,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 行间距
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "行间距",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "${"%.1f".format(settings.lineHeight)}倍",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Slider(
                value = settings.lineHeight,
                onValueChange = onLineHeightChange,
                valueRange = 1.0f..2.5f,
                steps = 14,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 背景颜色 - 护眼模式
            Text(
                text = "背景颜色（护眼模式）",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(ReaderColors.allColors) { index, color ->
                    val name = when (index) {
                        0 -> "白色"
                        1 -> "护眼绿"
                        2 -> "羊皮纸"
                        3 -> "浅黄"
                        4 -> "浅灰"
                        5 -> "深色"
                        6 -> "纯黑"
                        else -> ""
                    }
                    ColorChip(
                        color = color,
                        name = name,
                        isSelected = settings.bgColorIndex == index,
                        onClick = { onBgColorChange(index) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 翻页模式
            Text(
                text = "翻页模式",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PageFlipMode.values().forEach { mode ->
                    val name = when (mode) {
                        PageFlipMode.SLIDE -> "左右滑动"
                        PageFlipMode.SCROLL -> "上下滚动"
                        PageFlipMode.SIMULATION -> "仿真翻页"
                    }
                    FilterChip(
                        selected = settings.pageFlipMode == mode,
                        onClick = { onPageFlipModeChange(mode) },
                        label = { Text(name) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * 字体选择芯片
 */
@Composable
private fun FontChip(
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        modifier = Modifier
            .clickable(onClick = onClick)
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodySmall,
            color = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

/**
 * 颜色选择芯片
 */
@Composable
private fun ColorChip(
    color: Color,
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(color)
                .border(
                    width = if (isSelected) 3.dp else 1.dp,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        Color.Gray.copy(alpha = 0.3f)
                    },
                    shape = CircleShape
                )
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}
