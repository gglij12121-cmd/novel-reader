package com.reader.novel.ui.screen.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reader.novel.data.model.Book
import com.reader.novel.data.model.Chapter
import com.reader.novel.data.repository.BookRepository
import com.reader.novel.data.scraper.ScraperManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 详情页ViewModel
 */
@HiltViewModel
class DetailViewModel @Inject constructor(
    private val bookRepository: BookRepository,
    private val scraperManager: ScraperManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    private var currentBook: Book? = null
    private var currentBookId: Long = 0L

    /**
     * 加载书籍详情
     */
    fun loadBookDetail(bookUrl: String, source: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                val scraper = scraperManager.getScraper(source)
                if (scraper == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "不支持的来源: $source"
                    )
                    return@launch
                }

                val (book, chapters) = scraper.getBookDetail(bookUrl)
                currentBook = book

                // 检查是否在书架上
                val isOnBookshelf = bookRepository.isOnBookshelf(bookUrl)

                // 如果在书架上，获取本地书籍ID
                if (isOnBookshelf) {
                    val localBook = bookRepository.getBookById(
                        bookRepository.addToBookshelf(book)
                    )
                    currentBookId = localBook?.id ?: 0L
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    book = book,
                    chapters = chapters,
                    isOnBookshelf = isOnBookshelf,
                    isChapterListExpanded = true
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "加载失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 加入/移出书架
     */
    fun toggleBookshelf() {
        val book = currentBook ?: return

        viewModelScope.launch {
            try {
                if (_uiState.value.isOnBookshelf) {
                    bookRepository.removeFromBookshelf(currentBookId)
                    _uiState.value = _uiState.value.copy(isOnBookshelf = false)
                } else {
                    currentBookId = bookRepository.addToBookshelf(book)
                    _uiState.value = _uiState.value.copy(isOnBookshelf = true)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "操作失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 切换章节列表展开/收起
     */
    fun toggleChapterList() {
        _uiState.value = _uiState.value.copy(
            isChapterListExpanded = !_uiState.value.isChapterListExpanded
        )
    }

    /**
     * 缓存整本书
     */
    fun cacheBook() {
        val book = currentBook ?: return
        val bookUrl = book.sourceUrl
        val source = book.source

        if (currentBookId == 0L) {
            _uiState.value = _uiState.value.copy(
                error = "请先加入书架"
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isCaching = true,
                cacheProgress = 0,
                cacheTotal = _uiState.value.chapters.size
            )

            try {
                bookRepository.cacheBook(
                    bookId = currentBookId,
                    bookUrl = bookUrl,
                    source = source
                ) { current, total ->
                    _uiState.value = _uiState.value.copy(
                        cacheProgress = current,
                        cacheTotal = total
                    )
                }

                _uiState.value = _uiState.value.copy(
                    isCaching = false,
                    cacheProgress = _uiState.value.cacheTotal
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isCaching = false,
                    error = "缓存失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 获取当前书籍ID
     */
    fun getBookId(): Long = currentBookId

    /**
     * 获取书籍来源
     */
    fun getBookSource(): String = currentBook?.source ?: ""
}

/**
 * 详情页UI状态
 */
data class DetailUiState(
    val isLoading: Boolean = false,
    val book: Book? = null,
    val chapters: List<Chapter> = emptyList(),
    val isOnBookshelf: Boolean = false,
    val isChapterListExpanded: Boolean = false,
    val isCaching: Boolean = false,
    val cacheProgress: Int = 0,
    val cacheTotal: Int = 0,
    val error: String? = null
)
