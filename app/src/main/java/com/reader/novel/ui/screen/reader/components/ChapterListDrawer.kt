package com.reader.novel.ui.screen.reader.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.reader.novel.data.model.Chapter

/**
 * 章节目录抽屉
 *
 * @param chapters 章节列表
 * @param currentIndex 当前章节索引
 * @param onChapterClick 章节点击回调
 * @param onDismiss 关闭抽屉回调
 */
@Composable
fun ChapterListDrawer(
    chapters: List<Chapter>,
    currentIndex: Int,
    onChapterClick: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    // 创建列表状态，用于自动滚动到当前章节
    val listState = rememberLazyListState()

    // 自动滚动到当前章节
    LaunchedEffect(currentIndex) {
        if (currentIndex > 0) {
            listState.animateScrollToItem(currentIndex)
        }
    }

    // 半透明背景遮罩
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(onClick = onDismiss)
    ) {
        // 章节列表面板
        Card(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.75f)
                .clickable { /* 阻止点击穿透 */ },
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // 标题
                Text(
                    text = "目录 (${chapters.size}章)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(16.dp)
                )

                HorizontalDivider()

                // 章节列表
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(chapters) { index, chapter ->
                        ChapterListItem(
                            title = chapter.title,
                            index = index,
                            isCurrent = index == currentIndex,
                            onClick = { onChapterClick(index) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * 章节列表项
 */
@Composable
private fun ChapterListItem(
    title: String,
    index: Int,
    isCurrent: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isCurrent) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
    } else {
        Color.Transparent
    }

    val textColor = if (isCurrent) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = backgroundColor
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = textColor,
            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )
    }
}
