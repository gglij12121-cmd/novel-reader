package com.reader.novel.ui.screen.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.reader.novel.ui.components.BookCard
import com.reader.novel.ui.components.DebugLogDialog
import com.reader.novel.ui.components.ErrorView
import com.reader.novel.ui.components.LoadingIndicator

/**
 * 首页 (发现页)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToSearch: () -> Unit = {},
    onNavigateToDetail: (String, String) -> Unit = { _, _ -> },
    onNavigateToBookshelf: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var showDebugLog by remember { mutableStateOf(false) }

    if (showDebugLog) {
        DebugLogDialog(onDismiss = { showDebugLog = false })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("小说阅读器") },
                actions = {
                    IconButton(onClick = { showDebugLog = true }) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "调试日志"
                        )
                    }
                    IconButton(onClick = onNavigateToBookshelf) {
                        Icon(
                            imageVector = Icons.Default.Book,
                            contentDescription = "书架"
                        )
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "设置"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("输入书名或作者") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "搜索"
                    )
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { viewModel.search(searchQuery) })
            )

            when {
                uiState.isLoading -> {
                    LoadingIndicator(message = "正在搜索...")
                }
                uiState.error != null -> {
                    ErrorView(
                        errorMessage = uiState.error!!,
                        onRetry = { viewModel.search(searchQuery) }
                    )
                }
                uiState.books.isNotEmpty() -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.books) { book ->
                            BookCard(
                                book = book,
                                onClick = {
                                    onNavigateToDetail(book.sourceUrl, book.source)
                                }
                            )
                        }
                    }
                }
                else -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "搜索你喜欢的小说",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "支持笔趣阁、八一中文网、起点中文网等100+书源",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    }
}
