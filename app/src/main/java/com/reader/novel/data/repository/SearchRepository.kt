package com.reader.novel.data.repository

import com.reader.novel.data.model.Book
import com.reader.novel.data.model.SearchResult
import com.reader.novel.data.scraper.ScraperManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 搜索数据仓库
 * 处理搜索相关的业务逻辑
 *
 * 职责：
 * 1. 多源搜索聚合
 * 2. 搜索结果去重
 * 3. 错误处理
 */
@Singleton
class SearchRepository @Inject constructor(
    private val scraperManager: ScraperManager
) {
    /**
     * 多源搜索
     * 从所有可用来源并行搜索，汇总结果
     *
     * @param keyword 搜索关键词
     * @return 搜索结果列表，按来源分组
     */
    suspend fun searchAll(keyword: String): List<SearchResult> {
        if (keyword.isBlank()) return emptyList()
        return scraperManager.searchAll(keyword)
    }

    /**
     * 单源搜索
     *
     * @param keyword 搜索关键词
     * @param sourceId 来源标识
     * @return 搜索结果
     */
    suspend fun searchFrom(keyword: String, sourceId: String): SearchResult {
        if (keyword.isBlank()) return SearchResult(
            source = sourceId,
            isSuccess = false,
            errorMessage = "搜索关键词不能为空"
        )
        return scraperManager.searchFrom(keyword, sourceId)
    }

    /**
     * 合并所有来源的搜索结果
     * 去除重复书籍
     *
     * @param results 各来源的搜索结果
     * @return 合并后的书籍列表
     */
    fun mergeResults(results: List<SearchResult>, keyword: String = ""): List<Book> {
        val allBooks = results
            .filter { it.isSuccess }
            .flatMap { it.books }

        // 去重：根据书名+作者
        val uniqueBooks = allBooks.distinctBy { "${it.title}_${it.author}" }

        if (keyword.isBlank()) return uniqueBooks

        // 按匹配度排序
        val lowerKeyword = keyword.lowercase()
        return uniqueBooks.sortedWith(compareByDescending<Book> { book ->
            val title = book.title.lowercase()
            when {
                // 完全匹配最优先
                title == lowerKeyword -> 100
                // 书名以关键词开头
                title.startsWith(lowerKeyword) -> 90
                // 书名包含关键词（去掉方括号标签后匹配）
                title.replace(Regex("\\[.*?\\]"), "").trim().startsWith(lowerKeyword) -> 85
                // 书名包含关键词
                title.contains(lowerKeyword) -> 80
                // 作者包含关键词
                book.author.lowercase().contains(lowerKeyword) -> 50
                // 其他
                else -> 0
            }
        }.thenByDescending { book ->
            // 内置源优先
            when (book.source) {
                "biquge", "bayi", "qidian" -> 1
                else -> 0
            }
        })
    }

    /**
     * 获取可用的搜索来源
     */
    fun getAvailableSources(): List<Pair<String, String>> {
        return scraperManager.getAvailableSources()
    }
}
