package com.reader.novel.ui.screen.bookshelf

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.reader.novel.data.local.entity.BookEntity

/**
 * 书架页面
 *
 * 功能：
 * 1. 显示书架上的书籍
 * 2. 继续阅读
 * 3. 移出书架
 * 4. 删除书籍
 *
 * @param onNavigateBack 返回
 * @param onNavigateToDetail 导航到详情页
 * @param viewModel 书架ViewModel
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookshelfScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToDetail: (String, String) -> Unit = { _, _ -> },
    viewModel: BookshelfViewModel = hiltViewModel()
) {
    val bookshelfBooks by viewModel.bookshelfBooks.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    // 删除确认对话框
    var showDeleteDialog by remember { mutableStateOf(false) }
    var bookToDelete by remember { mutableStateOf<BookEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("我的书架") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        if (bookshelfBooks.isEmpty()) {
            // 空书架提示
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Book,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "书架空空如也",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "去搜索添加你喜欢的小说吧",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            // 书籍列表
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(bookshelfBooks) { book ->
                    BookshelfItem(
                        book = book,
                        onClick = {
                            onNavigateToDetail(book.sourceUrl, book.source)
                        },
                        onRemoveFromBookshelf = {
                            viewModel.removeFromBookshelf(book.id)
                        },
                        onDelete = {
                            bookToDelete = book
                            showDeleteDialog = true
                        }
                    )
                }
            }
        }

        // 错误提示
        uiState.error?.let { error ->
            LaunchedEffect(error) {
                // 自动清除错误
                viewModel.clearError()
            }
        }

        // 删除确认对话框
        if (showDeleteDialog && bookToDelete != null) {
            AlertDialog(
                onDismissRequest = {
                    showDeleteDialog = false
                    bookToDelete = null
                },
                title = { Text("确认删除") },
                text = { Text("确定要删除《${bookToDelete!!.title}》吗？删除后将清除所有缓存数据。") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteBook(bookToDelete!!.id)
                            showDeleteDialog = false
                            bookToDelete = null
                        }
                    ) {
                        Text("删除", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showDeleteDialog = false
                            bookToDelete = null
                        }
                    ) {
                        Text("取消")
                    }
                }
            )
        }
    }
}

/**
 * 书架列表项
 */
@Composable
private fun BookshelfItem(
    book: BookEntity,
    onClick: () -> Unit,
    onRemoveFromBookshelf: () -> Unit,
    onDelete: () -> Unit
) {
    // 长按菜单
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth()
        ) {
            // 封面
            AsyncImage(
                model = book.coverUrl,
                contentDescription = book.title,
                modifier = Modifier
                    .width(70.dp)
                    .height(95.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.LightGray),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            // 书籍信息
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                // 书名
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // 作者
                Text(
                    text = book.author,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.weight(1f))

                // 最新章节
                if (book.latestChapter.isNotEmpty()) {
                    Text(
                        text = "最新: ${book.latestChapter}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // 更多操作按钮
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "更多"
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("移出书架") },
                        onClick = {
                            onRemoveFromBookshelf()
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.BookmarkRemove,
                                contentDescription = null
                            )
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("删除", color = MaterialTheme.colorScheme.error) },
                        onClick = {
                            onDelete()
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    )
                }
            }
        }
    }
}
