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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.reader.novel.ui.screen.reader.PageFlipMode
import com.reader.novel.ui.screen.reader.ReaderSettings
import com.reader.novel.ui.theme.ReaderColors

/**
 * 阅读器设置面板
 *
 * @param settings 当前阅读设置
 * @param onFontSizeChange 字体大小变化回调
 * @param onLineHeightChange 行间距变化回调
 * @param onBgColorChange 背景颜色变化回调
 * @param onDismiss 关闭面板回调
 * @param modifier Modifier
 */
@Composable
fun ReaderSettingsPanel(
    settings: ReaderSettings,
    onFontSizeChange: (Float) -> Unit,
    onLineHeightChange: (Float) -> Unit,
    onBgColorChange: (Int) -> Unit,
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
            modifier = Modifier
                .padding(16.dp)
        ) {
            // 标题栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "阅读设置",
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "关闭"
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 字体大小
            Text(
                text = "字体大小: ${settings.fontSize.toInt()}",
                style = MaterialTheme.typography.bodyMedium
            )
            Slider(
                value = settings.fontSize,
                onValueChange = onFontSizeChange,
                valueRange = 12f..30f,
                steps = 17,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 行间距
            Text(
                text = "行间距: ${"%.1f".format(settings.lineHeight)}",
                style = MaterialTheme.typography.bodyMedium
            )
            Slider(
                value = settings.lineHeight,
                onValueChange = onLineHeightChange,
                valueRange = 1.0f..2.5f,
                steps = 14,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 背景颜色选择
            Text(
                text = "背景颜色",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(ReaderColors.allColors) { index, color ->
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = if (settings.bgColorIndex == index) 3.dp else 1.dp,
                                color = if (settings.bgColorIndex == index) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    Color.Gray
                                },
                                shape = CircleShape
                            )
                            .clickable { onBgColorChange(index) }
                    )
                }
            }
        }
    }
}
