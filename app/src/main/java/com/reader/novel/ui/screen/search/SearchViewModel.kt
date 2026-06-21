package com.reader.novel.ui.screen.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reader.novel.data.model.Book
import com.reader.novel.data.model.SearchResult
import com.reader.novel.data.repository.SearchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 搜索页ViewModel
 */
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchRepository: SearchRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

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
}

data class SearchUiState(
    val isLoading: Boolean = false,
    val books: List<Book> = emptyList(),
    val searchResults: List<SearchResult> = emptyList(),
    val error: String? = null
)
