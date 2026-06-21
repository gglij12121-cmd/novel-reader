package com.reader.novel.ui.screen.bookshelf

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reader.novel.data.local.entity.BookEntity
import com.reader.novel.data.repository.BookRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 书架ViewModel
 */
@HiltViewModel
class BookshelfViewModel @Inject constructor(
    private val bookRepository: BookRepository
) : ViewModel() {

    /** 书架书籍列表 */
    val bookshelfBooks: StateFlow<List<BookEntity>> = bookRepository.getBookshelfBooks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /** 阅读历史 */
    val readingHistory: StateFlow<List<BookEntity>> = bookRepository.getReadingHistory()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /** UI状态 */
    private val _uiState = MutableStateFlow(BookshelfUiState())
    val uiState: StateFlow<BookshelfUiState> = _uiState.asStateFlow()

    /**
     * 从书架移除书籍
     */
    fun removeFromBookshelf(bookId: Long) {
        viewModelScope.launch {
            try {
                bookRepository.removeFromBookshelf(bookId)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "操作失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 删除书籍及其所有数据
     */
    fun deleteBook(bookId: Long) {
        viewModelScope.launch {
            try {
                bookRepository.deleteBook(bookId)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "删除失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 清除错误信息
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

/**
 * 书架UI状态
 */
data class BookshelfUiState(
    val error: String? = null
)
