package com.reader.novel.ui.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reader.novel.data.model.Book
import com.reader.novel.data.repository.SearchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 首页ViewModel
 * 管理首页的推荐数据和搜索功能
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val searchRepository: SearchRepository
) : ViewModel() {

    /** UI状态 */
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    /** 搜索关键词 */
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    /**
     * 更新搜索关键词
     */
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /**
     * 搜索小说
     *
     * @param keyword 搜索关键词
     */
    fun search(keyword: String) {
        if (keyword.isBlank()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                val results = searchRepository.searchAll(keyword)
                val allBooks = searchRepository.mergeResults(results)

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    books = allBooks,
                    searchResults = results
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "搜索失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 获取可用的搜索来源
     */
    fun getAvailableSources(): List<Pair<String, String>> {
        return searchRepository.getAvailableSources()
    }
}

/**
 * 首页UI状态
 *
 * @property isLoading 是否正在加载
 * @property books 搜索结果书籍列表
 * @property searchResults 各来源的搜索结果
 * @property error 错误信息
 */
data class HomeUiState(
    val isLoading: Boolean = false,
    val books: List<Book> = emptyList(),
    val searchResults: List<com.reader.novel.data.model.SearchResult> = emptyList(),
    val error: String? = null
)
