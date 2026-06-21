package com.reader.novel.ui.screen.reader

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.reader.novel.ui.components.ErrorView
import com.reader.novel.ui.components.LoadingIndicator
import com.reader.novel.ui.theme.ReaderColors
import com.reader.novel.ui.screen.reader.components.ChapterListDrawer
import com.reader.novel.ui.screen.reader.components.ReaderSettingsPanel
import com.reader.novel.ui.screen.reader.components.presetFonts
import kotlin.math.abs

/**
 * 阅读器页面
 *
 * 支持三种翻页模式：
 * - 上下滚动 (SCROLL)
 * - 左右滑动 (SLIDE)
 * - 仿真翻页 (SIMULATION)
 *
 * 支持音量键翻页
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

    var showMenu by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showChapterList by remember { mutableStateOf(false) }

    LaunchedEffect(bookId) {
        viewModel.initialize(bookId, initialChapterIndex)
    }

    val bgColor = ReaderColors.allColors.getOrElse(readerSettings.bgColorIndex) { ReaderColors.White }
    val textColor = if (readerSettings.bgColorIndex >= 5) Color.White else Color.Black
    val fontFamily = presetFonts.getOrElse(readerSettings.fontIndex) { presetFonts[0] }.fontFamily

    // 音量键翻页
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when (event.key) {
                        Key.VolumeUp -> {
                            viewModel.loadPreviousChapter()
                            true
                        }
                        Key.VolumeDown -> {
                            viewModel.loadNextChapter()
                            true
                        }
                        else -> false
                    }
                } else false
            }
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
                when (readerSettings.pageFlipMode) {
                    PageFlipMode.SCROLL -> ScrollModeContent(
                        uiState = uiState,
                        viewModel = viewModel,
                        textColor = textColor,
                        fontFamily = fontFamily,
                        fontSize = readerSettings.fontSize,
                        lineHeight = readerSettings.lineHeight,
                        onToggleMenu = { showMenu = !showMenu }
                    )
                    PageFlipMode.SLIDE -> SlideModeContent(
                        uiState = uiState,
                        viewModel = viewModel,
                        textColor = textColor,
                        fontFamily = fontFamily,
                        fontSize = readerSettings.fontSize,
                        lineHeight = readerSettings.lineHeight,
                        onToggleMenu = { showMenu = !showMenu }
                    )
                    PageFlipMode.SIMULATION -> SimulationModeContent(
                        uiState = uiState,
                        viewModel = viewModel,
                        textColor = textColor,
                        fontFamily = fontFamily,
                        fontSize = readerSettings.fontSize,
                        lineHeight = readerSettings.lineHeight,
                        onToggleMenu = { showMenu = !showMenu }
                    )
                }
            }
        }

        // 顶部菜单
        if (showMenu) {
            TopAppBar(
                title = { Text(uiState.chapterTitle, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        showChapterList = true
                        showMenu = false
                    }) {
                        Icon(imageVector = Icons.Default.List, contentDescription = "目录")
                    }
                    IconButton(onClick = {
                        showSettings = true
                        showMenu = false
                    }) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = "设置")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = bgColor.copy(alpha = 0.9f)
                )
            )
        }

        // 底部菜单
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
                    Row {
                        IconButton(
                            onClick = { viewModel.loadPreviousChapter() },
                            enabled = viewModel.hasPreviousChapter()
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipPrevious,
                                contentDescription = "上一章",
                                tint = if (viewModel.hasPreviousChapter()) textColor else textColor.copy(alpha = 0.3f)
                            )
                        }
                        IconButton(
                            onClick = { viewModel.loadNextChapter() },
                            enabled = viewModel.hasNextChapter()
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "下一章",
                                tint = if (viewModel.hasNextChapter()) textColor else textColor.copy(alpha = 0.3f)
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
                onFontChange = { viewModel.updateFontIndex(it) },
                onPageFlipModeChange = { viewModel.updatePageFlipMode(it) },
                onDismiss = { showSettings = false },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        // 章节目录
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

// ==================== 上下滚动模式 ====================

@Composable
private fun ScrollModeContent(
    uiState: ReaderUiState,
    viewModel: ReaderViewModel,
    textColor: Color,
    fontFamily: androidx.compose.ui.text.font.FontFamily,
    fontSize: Float,
    lineHeight: Float,
    onToggleMenu: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .clickable(onClick = onToggleMenu)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = uiState.chapterTitle,
            style = TextStyle(
                fontFamily = fontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = textColor
            ),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            text = uiState.content,
            style = TextStyle(
                fontFamily = fontFamily,
                fontSize = fontSize.sp,
                lineHeight = (fontSize * lineHeight).sp,
                color = textColor
            )
        )

        Spacer(modifier = Modifier.height(32.dp))

        // 上下章按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (viewModel.hasPreviousChapter()) {
                OutlinedButton(onClick = { viewModel.loadPreviousChapter() }) {
                    Icon(Icons.Default.ArrowBack, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("上一章")
                }
            } else {
                Spacer(Modifier.width(1.dp))
            }
            if (viewModel.hasNextChapter()) {
                Button(onClick = { viewModel.loadNextChapter() }) {
                    Text("下一章")
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Default.ArrowForward, null, Modifier.size(18.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(64.dp))
    }
}

// ==================== 左右滑动模式 ====================

@Composable
private fun SlideModeContent(
    uiState: ReaderUiState,
    viewModel: ReaderViewModel,
    textColor: Color,
    fontFamily: androidx.compose.ui.text.font.FontFamily,
    fontSize: Float,
    lineHeight: Float,
    onToggleMenu: () -> Unit
) {
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var isAnimating by remember { mutableStateOf(false) }
    val animatedOffset by animateFloatAsState(
        targetValue = if (isAnimating) 0f else dragOffset,
        animationSpec = tween(200),
        finishedListener = { isAnimating = false },
        label = "slide"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { dragOffset = 0f },
                    onDragEnd = {
                        if (abs(dragOffset) > 100) {
                            if (dragOffset > 0 && viewModel.hasPreviousChapter()) {
                                viewModel.loadPreviousChapter()
                            } else if (dragOffset < 0 && viewModel.hasNextChapter()) {
                                viewModel.loadNextChapter()
                            }
                        }
                        isAnimating = true
                        dragOffset = 0f
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        dragOffset += dragAmount
                    }
                )
            }
            .clickable(onClick = onToggleMenu)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { translationX = animatedOffset }
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = uiState.chapterTitle,
                style = TextStyle(
                    fontFamily = fontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = textColor
                ),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Text(
                text = uiState.content,
                style = TextStyle(
                    fontFamily = fontFamily,
                    fontSize = fontSize.sp,
                    lineHeight = (fontSize * lineHeight).sp,
                    color = textColor
                )
            )

            Spacer(modifier = Modifier.height(64.dp))
        }

        // 左右滑动提示
        if (abs(animatedOffset) > 50) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp),
                contentAlignment = if (animatedOffset > 0) Alignment.CenterStart else Alignment.CenterEnd
            ) {
                Text(
                    text = if (animatedOffset > 0) "上一章" else "下一章",
                    color = textColor.copy(alpha = 0.3f),
                    fontSize = 16.sp
                )
            }
        }
    }
}

// ==================== 仿真翻页模式 ====================

@Composable
private fun SimulationModeContent(
    uiState: ReaderUiState,
    viewModel: ReaderViewModel,
    textColor: Color,
    fontFamily: androidx.compose.ui.text.font.FontFamily,
    fontSize: Float,
    lineHeight: Float,
    onToggleMenu: () -> Unit
) {
    // 仿真翻页使用左右滑动 + 翻转动画效果
    var offsetX by remember { mutableFloatStateOf(0f) }
    var pageFlipped by remember { mutableStateOf(false) }

    val rotation by animateFloatAsState(
        targetValue = if (pageFlipped) 0f else (offsetX / 5f).coerceIn(-30f, 30f),
        animationSpec = tween(300),
        label = "rotation"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (abs(offsetX) > 100) {
                            if (offsetX > 0 && viewModel.hasPreviousChapter()) {
                                viewModel.loadPreviousChapter()
                            } else if (offsetX < 0 && viewModel.hasNextChapter()) {
                                viewModel.loadNextChapter()
                            }
                        }
                        pageFlipped = true
                        offsetX = 0f
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        offsetX += dragAmount
                        pageFlipped = false
                    }
                )
            }
            .clickable(onClick = onToggleMenu)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    // 仿真翻页：沿Y轴旋转
                    rotationY = rotation
                    cameraDistance = 12f * density
                }
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = uiState.chapterTitle,
                style = TextStyle(
                    fontFamily = fontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = textColor
                ),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Text(
                text = uiState.content,
                style = TextStyle(
                    fontFamily = fontFamily,
                    fontSize = fontSize.sp,
                    lineHeight = (fontSize * lineHeight).sp,
                    color = textColor
                )
            )

            Spacer(modifier = Modifier.height(64.dp))
        }
    }
}
