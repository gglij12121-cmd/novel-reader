package com.reader.novel.ui.screen.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.reader.novel.ui.components.ErrorView
import com.reader.novel.ui.components.LoadingIndicator
import com.reader.novel.ui.theme.ReaderColors

/**
 * 阅读器页面
 *
 * 功能：
 * 1. 显示章节正文
 * 2. 上下章切换
 * 3. 阅读设置面板
 * 4. 目录侧边栏
 *
 * @param bookId 书籍ID
 * @param initialChapterIndex 初始章节索引
 * @param onNavigateBack 返回
 * @param viewModel 阅读器ViewModel
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    bookId: Long,
    initialChapterIndex: Int = 0,
    onNavigateBack: () -> Unit = {},
    viewModel: ReaderViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val readerSettings by viewModel.readerSettings.collectAsState()

    // 控制菜单显示
    var showMenu by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showChapterList by remember { mutableStateOf(false) }

    // 初始化
    LaunchedEffect(bookId) {
        viewModel.initialize(bookId, initialChapterIndex)
    }

    // 背景颜色
    val bgColor = ReaderColors.allColors.getOrElse(readerSettings.bgColorIndex) {
        ReaderColors.White
    }

    // 文字颜色 (深色背景用白字)
    val textColor = if (readerSettings.bgColorIndex >= 5) {
        Color.White
    } else {
        Color.Black
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        when {
            uiState.isLoading -> {
                LoadingIndicator(message = "正在加载章节...")
            }
            uiState.error != null -> {
                ErrorView(
                    errorMessage = uiState.error!!,
                    onRetry = { viewModel.loadChapter(uiState.currentChapterIndex) }
                )
            }
            else -> {
                // 正文内容
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .clickable { showMenu = !showMenu }
                        .padding(horizontal = 16.dp)
                ) {
                    Spacer(modifier = Modifier.height(48.dp))

                    // 章节标题
                    Text(
                        text = uiState.chapterTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // 正文内容
                    Text(
                        text = uiState.content,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = readerSettings.fontSize.sp,
                            lineHeight = (readerSettings.fontSize * readerSettings.lineHeight).sp
                        ),
                        color = textColor
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // 上下章按钮
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (viewModel.hasPreviousChapter()) {
                            OutlinedButton(
                                onClick = { viewModel.loadPreviousChapter() }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("上一章")
                            }
                        } else {
                            Spacer(modifier = Modifier.width(1.dp))
                        }

                        if (viewModel.hasNextChapter()) {
                            Button(
                                onClick = { viewModel.loadNextChapter() }
                            ) {
                                Text("下一章")
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(64.dp))
                }
            }
        }

        // 顶部菜单栏
        if (showMenu) {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.chapterTitle,
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
                    // 目录按钮
                    IconButton(onClick = {
                        showChapterList = true
                        showMenu = false
                    }) {
                        Icon(
                            imageVector = Icons.Default.List,
                            contentDescription = "目录"
                        )
                    }
                    // 设置按钮
                    IconButton(onClick = {
                        showSettings = true
                        showMenu = false
                    }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "设置"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = bgColor.copy(alpha = 0.9f)
                )
            )
        }

        // 底部信息栏
        if (showMenu) {
            BottomAppBar(
                containerColor = bgColor.copy(alpha = 0.9f),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${uiState.currentChapterIndex + 1}/${uiState.totalChapters}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor
                    )

                    // 上下章按钮
                    Row {
                        IconButton(
                            onClick = { viewModel.loadPreviousChapter() },
                            enabled = viewModel.hasPreviousChapter()
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipPrevious,
                                contentDescription = "上一章",
                                tint = if (viewModel.hasPreviousChapter()) {
                                    textColor
                                } else {
                                    textColor.copy(alpha = 0.3f)
                                }
                            )
                        }

                        IconButton(
                            onClick = { viewModel.loadNextChapter() },
                            enabled = viewModel.hasNextChapter()
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "下一章",
                                tint = if (viewModel.hasNextChapter()) {
                                    textColor
                                } else {
                                    textColor.copy(alpha = 0.3f)
                                }
                            )
                        }
                    }
                }
            }
        }

        // 设置面板
        if (showSettings) {
            ReaderSettingsPanel(
                settings = readerSettings,
                onFontSizeChange = { viewModel.updateFontSize(it) },
                onLineHeightChange = { viewModel.updateLineHeight(it) },
                onBgColorChange = { viewModel.updateBgColorIndex(it) },
                onDismiss = { showSettings = false },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        // 章节列表
        if (showChapterList) {
            ChapterListDrawer(
                chapters = viewModel.getChapterList(),
                currentIndex = uiState.currentChapterIndex,
                onChapterClick = { index ->
                    viewModel.loadChapter(index)
                    showChapterList = false
                },
                onDismiss = { showChapterList = false }
            )
        }
    }
}
