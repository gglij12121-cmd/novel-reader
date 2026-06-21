package com.reader.novel.ui.screen.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.reader.novel.ui.components.ErrorView
import com.reader.novel.ui.components.LoadingIndicator
import com.reader.novel.ui.components.SourceTag

/**
 * 小说详情页
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
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
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
                            },
                            onCache = { viewModel.cacheBook() }
                        )
                    }

                    // 缓存进度
                    if (uiState.isCaching) {
                        item {
                            CachingProgress(
                                progress = uiState.cacheProgress,
                                total = uiState.cacheTotal
                            )
                        }
                    }

                    // 章节目录标题
                    item {
                        ChapterListHeader(
                            chapterCount = uiState.chapters.size,
                            isExpanded = uiState.isChapterListExpanded,
                            onToggle = { viewModel.toggleChapterList() }
                        )
                    }

                    // 章节列表
                    if (uiState.isChapterListExpanded) {
                        itemsIndexed(uiState.chapters) { index, chapter ->
                            ChapterItem(
                                chapterTitle = chapter.title,
                                chapterIndex = index,
                                isCached = chapter.isCached,
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
}

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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier
                    .width(120.dp)
                    .height(170.dp),
                shape = RoundedCornerShape(8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                AsyncImage(
                    model = coverUrl,
                    contentDescription = title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "作者: $author",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                SourceTag(source = source)

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
    }

    if (description.isNotEmpty()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "简介",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 24.sp
                )
            }
        }
    }
}

@Composable
private fun ActionButtons(
    isOnBookshelf: Boolean,
    onToggleBookshelf: () -> Unit,
    onStartReading: () -> Unit,
    onCache: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
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
            Text(if (isOnBookshelf) "移出" else "书架")
        }

        OutlinedButton(
            onClick = onCache,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("缓存")
        }

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
            Text("阅读")
        }
    }
}

@Composable
private fun CachingProgress(progress: Int, total: Int) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "正在缓存...", style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = "$progress/$total",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { if (total > 0) progress.toFloat() / total else 0f },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ChapterListHeader(
    chapterCount: Int,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "目录",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${chapterCount}章",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null
            )
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
}

@Composable
private fun ChapterItem(
    chapterTitle: String,
    chapterIndex: Int,
    isCached: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
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
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (isCached) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "已缓存",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
}
