package com.reader.novel.data.model

/**
 * 搜索结果数据模型
 * 包含来自不同来源的搜索结果
 *
 * @property books 搜索到的书籍列表
 * @property source 来源网站名称
 * @property errorMessage 错误信息 (如果搜索失败)
 * @property isSuccess 搜索是否成功
 */
data class SearchResult(
    val books: List<Book> = emptyList(),
    val source: String = "",
    val errorMessage: String? = null,
    val isSuccess: Boolean = true
)
