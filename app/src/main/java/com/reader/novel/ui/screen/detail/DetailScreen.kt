package com.reader.novel.ui.screen.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.reader.novel.ui.components.ErrorView
import com.reader.novel.ui.components.LoadingIndicator
import com.reader.novel.ui.components.SourceTag

/**
 * 小说详情页
 *
 * 功能：
 * 1. 显示书籍封面、简介、作者等信息
 * 2. 章节目录列表
 * 3. 加入/移出书架
 * 4. 开始阅读
 *
 * @param bookUrl 书籍详情页URL
 * @param source 来源标识
 * @param onNavigateBack 返回
 * @param onNavigateToReader 导航到阅读器
 * @param viewModel 详情页ViewModel
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    bookUrl: String,
    source: String,
    onNavigateBack: () -> Unit = {},
    onNavigateToReader: (Long, Int) -> Unit = { _, _ -> },
    viewModel: DetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // 加载数据
    LaunchedEffect(bookUrl, source) {
        viewModel.loadBookDetail(bookUrl, source)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("书籍详情") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
                    // 书架按钮
                    IconButton(onClick = { viewModel.toggleBookshelf() }) {
                        Icon(
                            imageVector = if (uiState.isOnBookshelf) {
                                Icons.Default.Bookmark
                            } else {
                                Icons.Default.BookmarkBorder
                            },
                            contentDescription = if (uiState.isOnBookshelf) "移出书架" else "加入书架",
                            tint = if (uiState.isOnBookshelf) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                LoadingIndicator(
                    message = "加载中...",
                    modifier = Modifier.padding(paddingValues)
                )
            }
            uiState.error != null -> {
                ErrorView(
                    errorMessage = uiState.error!!,
                    onRetry = { viewModel.loadBookDetail(bookUrl, source) },
                    modifier = Modifier.padding(paddingValues)
                )
            }
            uiState.book != null -> {
                val book = uiState.book!!

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    // 书籍信息区域
                    item {
                        BookInfoSection(
                            coverUrl = book.coverUrl,
                            title = book.title,
                            author = book.author,
                            source = book.source,
                            status = book.status,
                            latestChapter = book.latestChapter,
                            description = book.description
                        )
                    }

                    // 操作按钮
                    item {
                        ActionButtons(
                            isOnBookshelf = uiState.isOnBookshelf,
                            onToggleBookshelf = { viewModel.toggleBookshelf() },
                            onStartReading = {
                                val bookId = viewModel.getBookId()
                                if (bookId > 0) {
                                    onNavigateToReader(bookId, 0)
                                }
                            }
                        )
                    }

                    // 章节目录标题
                    item {
                        Text(
                            text = "目录 (${uiState.chapters.size}章)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }

                    // 章节列表
                    itemsIndexed(uiState.chapters) { index, chapter ->
                        ChapterItem(
                            chapterTitle = chapter.title,
                            chapterIndex = index,
                            onClick = {
                                val bookId = viewModel.getBookId()
                                if (bookId > 0) {
                                    onNavigateToReader(bookId, index)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * 书籍信息区域
 */
@Composable
private fun BookInfoSection(
    coverUrl: String,
    title: String,
    author: String,
    source: String,
    status: String,
    latestChapter: String,
    description: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            // 封面
            AsyncImage(
                model = coverUrl,
                contentDescription = title,
                modifier = Modifier
                    .width(120.dp)
                    .height(160.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.LightGray),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(16.dp))

            // 书籍信息
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // 书名
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 作者
                Text(
                    text = "作者: $author",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                // 来源
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "来源: ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    SourceTag(source = source)
                }

                if (status.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "状态: $status",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (latestChapter.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "最新: $latestChapter",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // 简介
        if (description.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "简介",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 操作按钮区域
 */
@Composable
private fun ActionButtons(
    isOnBookshelf: Boolean,
    onToggleBookshelf: () -> Unit,
    onStartReading: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 加入/移出书架按钮
        OutlinedButton(
            onClick = onToggleBookshelf,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = if (isOnBookshelf) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(if (isOnBookshelf) "移出书架" else "加入书架")
        }

        // 开始阅读按钮
        Button(
            onClick = onStartReading,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("开始阅读")
        }
    }
}

/**
 * 章节列表项
 */
@Composable
private fun ChapterItem(
    chapterTitle: String,
    chapterIndex: Int,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${chapterIndex + 1}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(40.dp)
            )
            Text(
                text = chapterTitle,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
}
