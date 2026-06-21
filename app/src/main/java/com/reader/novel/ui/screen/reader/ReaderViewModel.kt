package com.reader.novel.ui.screen.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reader.novel.data.model.Chapter
import com.reader.novel.data.repository.BookRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 阅读器ViewModel
 * 管理阅读器的状态和业务逻辑
 */
@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val bookRepository: BookRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    /** 当前书籍ID */
    private var bookId: Long = 0L

    /** 书籍来源 */
    private var bookSource: String = ""

    /** 章节列表 */
    private var chapters: List<Chapter> = emptyList()

    /** 阅读设置 */
    private val _readerSettings = MutableStateFlow(ReaderSettings())
    val readerSettings: StateFlow<ReaderSettings> = _readerSettings.asStateFlow()

    /**
     * 初始化阅读器
     */
    fun initialize(bookId: Long, initialChapterIndex: Int) {
        this.bookId = bookId

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                // 获取书籍信息
                val book = bookRepository.getBookById(bookId)
                bookSource = book?.source ?: ""

                // 获取章节列表
                val bookUrl = book?.sourceUrl ?: ""
                chapters = bookRepository.getChapters(bookId, bookUrl, bookSource)

                if (chapters.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "没有找到章节"
                    )
                    return@launch
                }

                // 获取阅读进度
                val progress = bookRepository.getReadProgress(bookId)
                val startIndex = progress?.chapterIndex ?: initialChapterIndex

                // 加载章节内容
                loadChapter(startIndex.coerceIn(0, chapters.size - 1))
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "初始化失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 加载指定章节
     */
    fun loadChapter(chapterIndex: Int) {
        if (chapterIndex < 0 || chapterIndex >= chapters.size) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                val chapter = chapters[chapterIndex]
                val content = bookRepository.getChapterContent(
                    bookId = bookId,
                    chapterIndex = chapterIndex,
                    chapterUrl = chapter.url,
                    source = bookSource
                )

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    currentChapterIndex = chapterIndex,
                    chapterTitle = chapter.title,
                    content = content,
                    totalChapters = chapters.size
                )

                // 保存阅读进度
                bookRepository.saveReadProgress(
                    bookId = bookId,
                    chapterIndex = chapterIndex
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "加载章节失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 加载下一章
     */
    fun loadNextChapter() {
        val currentIndex = _uiState.value.currentChapterIndex
        if (currentIndex < chapters.size - 1) {
            loadChapter(currentIndex + 1)
        }
    }

    /**
     * 加载上一章
     */
    fun loadPreviousChapter() {
        val currentIndex = _uiState.value.currentChapterIndex
        if (currentIndex > 0) {
            loadChapter(currentIndex - 1)
        }
    }

    /**
     * 是否有下一章
     */
    fun hasNextChapter(): Boolean {
        return _uiState.value.currentChapterIndex < chapters.size - 1
    }

    /**
     * 是否有上一章
     */
    fun hasPreviousChapter(): Boolean {
        return _uiState.value.currentChapterIndex > 0
    }

    /**
     * 更新阅读设置
     */
    fun updateSettings(settings: ReaderSettings) {
        _readerSettings.value = settings
    }

    /**
     * 更新字体大小
     */
    fun updateFontSize(size: Float) {
        _readerSettings.value = _readerSettings.value.copy(fontSize = size)
    }

    /**
     * 更新行间距
     */
    fun updateLineHeight(height: Float) {
        _readerSettings.value = _readerSettings.value.copy(lineHeight = height)
    }

    /**
     * 更新背景颜色索引
     */
    fun updateBgColorIndex(index: Int) {
        _readerSettings.value = _readerSettings.value.copy(bgColorIndex = index)
    }

    /**
     * 更新字体索引
     */
    fun updateFontIndex(index: Int) {
        _readerSettings.value = _readerSettings.value.copy(fontIndex = index)
    }

    /**
     * 更新翻页模式
     */
    fun updatePageFlipMode(mode: PageFlipMode) {
        _readerSettings.value = _readerSettings.value.copy(pageFlipMode = mode)
    }

    /**
     * 获取章节列表
     */
    fun getChapterList(): List<Chapter> = chapters
}

/**
 * 阅读器UI状态
 */
data class ReaderUiState(
    val isLoading: Boolean = false,
    val currentChapterIndex: Int = 0,
    val chapterTitle: String = "",
    val content: String = "",
    val totalChapters: Int = 0,
    val error: String? = null
)

/**
 * 阅读设置
 *
 * @property fontSize 字体大小
 * @property lineHeight 行间距倍数
 * @property bgColorIndex 背景颜色索引
 * @property fontIndex 字体索引
 * @property pageFlipMode 翻页模式
 */
data class ReaderSettings(
    val fontSize: Float = 18f,
    val lineHeight: Float = 1.5f,
    val bgColorIndex: Int = 0,
    val fontIndex: Int = 0,
    val pageFlipMode: PageFlipMode = PageFlipMode.SLIDE
)

/**
 * 翻页模式
 */
enum class PageFlipMode {
    SLIDE,      // 左右滑动
    SCROLL,     // 上下滚动
    SIMULATION  // 仿真翻页
}
