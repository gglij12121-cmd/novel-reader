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
 */
class ScraperManager {

    /** 所有可用的爬虫 */
    private val scrapers: Map<String, BaseScraper> = mapOf(
        Constants.SOURCE_BIQUGE to BiqugeScraper(),
        Constants.SOURCE_BAYI to BayiScraper(),
        Constants.SOURCE_QIDIAN to QidianScraper()
    )

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
        return scrapers.map { (id, scraper) -> Pair(id, scraper.sourceName) }
    }

    /**
     * 多源并行搜索
     * 同时从所有网站搜索，汇总结果
     *
     * @param keyword 搜索关键词
     * @return 所有来源的搜索结果列表
     */
    suspend fun searchAll(keyword: String): List<SearchResult> = coroutineScope {
        scrapers.values.map { scraper ->
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
        }.awaitAll()
    }

    /**
     * 从指定来源搜索
     *
     * @param keyword 搜索关键词
     * @param sourceId 来源标识
     * @return 搜索结果
     */
    suspend fun searchFrom(keyword: String, sourceId: String): SearchResult {
        val scraper = scrapers[sourceId] ?: return SearchResult(
            source = sourceId,
            isSuccess = false,
            errorMessage = "未知的来源: $sourceId"
        )
        return scraper.search(keyword)
    }
}
