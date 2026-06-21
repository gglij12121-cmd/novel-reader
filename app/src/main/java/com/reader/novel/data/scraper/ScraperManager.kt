package com.reader.novel.data.scraper

import com.reader.novel.data.model.SearchResult
import com.reader.novel.util.Constants
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * 爬虫管理器
 * 统一管理所有网站的爬虫，提供多源搜索功能
 *
 * 主要功能：
 * 1. 管理所有爬虫实例
 * 2. 提供多源并行搜索
 * 3. 根据来源标识获取对应爬虫
 * 4. 支持从"阅读"APP导入书源
 */
class ScraperManager {

    /** 内置爬虫 */
    private val scrapers: Map<String, BaseScraper> = mapOf(
        Constants.SOURCE_BIQUGE to BiqugeScraper(),
        Constants.SOURCE_BAYI to BayiScraper(),
        Constants.SOURCE_QIDIAN to QidianScraper()
    )

    /** 书源管理器 */
    private val bookSourceManager = BookSourceManager()

    /**
     * 从Assets加载书源配置
     *
     * @param json 书源JSON字符串
     */
    fun loadBookSources(json: String) {
        bookSourceManager.loadFromJson(json)
    }

    /**
     * 获取指定来源的爬虫
     *
     * @param sourceId 来源标识 (biquge/bayi/qidian)
     * @return 对应的爬虫实例，如果不存在返回null
     */
    fun getScraper(sourceId: String): BaseScraper? {
        return scrapers[sourceId]
    }

    /**
     * 获取所有可用的来源名称
     */
    fun getAvailableSources(): List<Pair<String, String>> {
        val sources = scrapers.map { (id, scraper) -> Pair(id, scraper.sourceName) }
        val bookSources = bookSourceManager.getAvailableSources().map { source ->
            Pair("booksource_${source.name}", source.name)
        }
        return sources + bookSources
    }

    /**
     * 多源并行搜索
     * 同时从所有网站搜索，汇总结果
     *
     * @param keyword 搜索关键词
     * @return 所有来源的搜索结果列表
     */
    suspend fun searchAll(keyword: String): List<SearchResult> = coroutineScope {
        // 内置爬虫搜索
        val builtinResults = scrapers.values.map { scraper ->
            async {
                try {
                    scraper.search(keyword)
                } catch (e: Exception) {
                    SearchResult(
                        source = scraper.sourceId,
                        isSuccess = false,
                        errorMessage = "${scraper.sourceName}搜索失败: ${e.message}"
                    )
                }
            }
        }

        // 书源搜索（最多并行5个）
        val bookSourceResults = bookSourceManager.searchAll(keyword, maxSources = 5)

        // 合并结果
        (builtinResults.awaitAll() + bookSourceResults)
    }

    /**
     * 从指定来源搜索
     *
     * @param keyword 搜索关键词
     * @param sourceId 来源标识
     * @return 搜索结果
     */
    suspend fun searchFrom(keyword: String, sourceId: String): SearchResult {
        // 检查是否是内置爬虫
        val scraper = scrapers[sourceId]
        if (scraper != null) {
            return scraper.search(keyword)
        }

        // 检查是否是书源
        if (sourceId.startsWith("booksource_")) {
            val sourceName = sourceId.removePrefix("booksource_")
            val source = bookSourceManager.getAvailableSources().find { it.name == sourceName }
            if (source != null) {
                return bookSourceManager.searchAll(keyword, maxSources = 1).firstOrNull()
                    ?: SearchResult(source = sourceId, isSuccess = false, errorMessage = "搜索失败")
            }
        }

        return SearchResult(
            source = sourceId,
            isSuccess = false,
            errorMessage = "未知的来源: $sourceId"
        )
    }

    /**
     * 获取书源管理器
     */
    fun getBookSourceManager(): BookSourceManager {
        return bookSourceManager
    }
}
