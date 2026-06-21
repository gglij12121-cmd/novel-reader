package com.reader.novel.data.scraper

import com.reader.novel.data.model.Book
import com.reader.novel.data.model.Chapter
import com.reader.novel.data.model.SearchResult
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.jsoup.Jsoup

/**
 * 书源管理器
 * 支持从"阅读"APP的书源配置文件导入书源
 *
 * 书源格式说明：
 * - bookSourceUrl: 网站地址
 * - ruleSearchUrl: 搜索URL，searchKey会被替换为搜索关键词
 * - ruleSearchList: 搜索结果列表选择器
 * - ruleSearchName: 书名选择器
 * - ruleSearchAuthor: 作者选择器
 * - ruleChapterList: 章节列表选择器
 * - ruleBookContent: 正文内容选择器
 */
class BookSourceManager {

    /** 书源列表 */
    private val bookSources = mutableListOf<BookSource>()

    /**
     * 从JSON字符串加载书源
     *
     * @param json 书源JSON字符串
     */
    fun loadFromJson(json: String) {
        try {
            val sources = parseBookSourceJson(json)
            bookSources.clear()
            bookSources.addAll(sources.filter { it.enable })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 解析书源JSON
     */
    private fun parseBookSourceJson(json: String): List<BookSource> {
        val sources = mutableListOf<BookSource>()

        try {
            val jsonArray = org.json.JSONArray(json)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                sources.add(
                    BookSource(
                        name = obj.optString("bookSourceName", ""),
                        url = obj.optString("bookSourceUrl", ""),
                        enable = obj.optBoolean("enable", true),
                        searchUrl = obj.optString("ruleSearchUrl", ""),
                        searchList = obj.optString("ruleSearchList", ""),
                        searchName = obj.optString("ruleSearchName", ""),
                        searchAuthor = obj.optString("ruleSearchAuthor", ""),
                        searchNoteUrl = obj.optString("ruleSearchNoteUrl", ""),
                        searchCoverUrl = obj.optString("ruleSearchCoverUrl", ""),
                        chapterList = obj.optString("ruleChapterList", ""),
                        chapterName = obj.optString("ruleChapterName", ""),
                        contentUrl = obj.optString("ruleContentUrl", ""),
                        bookContent = obj.optString("ruleBookContent", ""),
                        coverUrl = obj.optString("ruleCoverUrl", ""),
                        bookIntro = obj.optString("ruleIntroduce", ""),
                        userAgent = obj.optString("httpUserAgent", "")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return sources
    }

    /**
     * 获取所有可用书源
     */
    fun getAvailableSources(): List<BookSource> {
        return bookSources.filter { it.enable }
    }

    /**
     * 多源搜索
     *
     * @param keyword 搜索关键词
     * @param maxSources 最大并行搜索源数量
     * @return 搜索结果列表
     */
    suspend fun searchAll(keyword: String, maxSources: Int = 5): List<SearchResult> = coroutineScope {
        val sourcesToSearch = bookSources.filter { it.enable }.take(maxSources)

        sourcesToSearch.map { source ->
            async {
                try {
                    searchFromSource(source, keyword)
                } catch (e: Exception) {
                    SearchResult(
                        source = source.name,
                        isSuccess = false,
                        errorMessage = "${source.name}搜索失败: ${e.message}"
                    )
                }
            }
        }.awaitAll()
    }

    /**
     * 从单个书源搜索
     */
    private suspend fun searchFromSource(source: BookSource, keyword: String): SearchResult {
        return try {
            val searchUrl = source.searchUrl
                .replace("searchKey", java.net.URLEncoder.encode(keyword, "UTF-8"))
                .replace("searchPage", "1")
                .replace("@", "&")
                .replace("|char=gbk", "")

            val doc = Jsoup.connect(searchUrl)
                .userAgent(source.userAgent.ifEmpty { "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36" })
                .timeout(10000)
                .get()

            val books = mutableListOf<Book>()

            // 解析搜索结果
            val elements = doc.select(source.searchList)
            for (element in elements) {
                try {
                    val name = parseSelector(element, source.searchName)
                    val author = parseSelector(element, source.searchAuthor)
                    val noteUrl = parseSelector(element, source.searchNoteUrl)
                    val coverUrl = parseSelector(element, source.searchCoverUrl)

                    if (name.isNotEmpty() && noteUrl.isNotEmpty()) {
                        val fullUrl = when {
                            noteUrl.startsWith("http") -> noteUrl
                            noteUrl.startsWith("//") -> "https:$noteUrl"
                            noteUrl.startsWith("/") -> "${source.url}$noteUrl"
                            else -> "${source.url}/$noteUrl"
                        }

                        books.add(
                            Book(
                                title = name,
                                author = author,
                                coverUrl = coverUrl,
                                source = source.name,
                                sourceUrl = fullUrl
                            )
                        )
                    }
                } catch (e: Exception) {
                    continue
                }
            }

            SearchResult(books = books, source = source.name, isSuccess = true)
        } catch (e: Exception) {
            SearchResult(
                source = source.name,
                isSuccess = false,
                errorMessage = "${source.name}搜索失败: ${e.message}"
            )
        }
    }

    /**
     * 获取章节列表
     */
    suspend fun getChapterList(source: BookSource, bookUrl: String): List<Chapter> {
        return try {
            val doc = Jsoup.connect(bookUrl)
                .userAgent(source.userAgent.ifEmpty { "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36" })
                .timeout(10000)
                .get()

            val chapters = mutableListOf<Chapter>()
            val elements = doc.select(source.chapterList)

            elements.forEachIndexed { index, element ->
                val name = parseSelector(element, source.chapterName)
                val url = parseSelector(element, source.contentUrl)

                if (name.isNotEmpty() && url.isNotEmpty()) {
                    val fullUrl = when {
                        url.startsWith("http") -> url
                        url.startsWith("//") -> "https:$url"
                        url.startsWith("/") -> "${source.url}$url"
                        else -> "${source.url}/$url"
                    }

                    chapters.add(
                        Chapter(
                            title = name,
                            url = fullUrl,
                            chapterIndex = index
                        )
                    )
                }
            }

            chapters
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 获取章节内容
     */
    suspend fun getChapterContent(source: BookSource, chapterUrl: String): String {
        return try {
            val doc = Jsoup.connect(chapterUrl)
                .userAgent(source.userAgent.ifEmpty { "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36" })
                .timeout(10000)
                .get()

            val contentElement = doc.selectFirst(source.bookContent)
            contentElement?.text() ?: "内容加载失败"
        } catch (e: Exception) {
            "内容加载失败: ${e.message}"
        }
    }

    /**
     * 解析选择器
     * 支持简单的选择器语法：tag.class@attr
     */
    private fun parseSelector(element: org.jsoup.nodes.Element, selector: String): String {
        if (selector.isEmpty()) return ""

        return try {
            // 简单选择器实现
            val parts = selector.split("@")
            val cssSelector = parts[0]
            val attr = if (parts.size > 1) parts[1] else "text"

            val targetElement = if (cssSelector.contains(".")) {
                element.selectFirst(cssSelector)
            } else if (cssSelector.startsWith("id.")) {
                element.getElementById(cssSelector.removePrefix("id."))
            } else if (cssSelector.startsWith("class.")) {
                element.getElementsByClass(cssSelector.removePrefix("class.")).firstOrNull()
            } else {
                element.selectFirst(cssSelector)
            }

            when (attr) {
                "text" -> targetElement?.text()?.trim() ?: ""
                "html" -> targetElement?.html()?.trim() ?: ""
                "href" -> targetElement?.attr("href")?.trim() ?: ""
                "src" -> targetElement?.attr("src")?.trim() ?: ""
                else -> targetElement?.attr(attr)?.trim() ?: ""
            }
        } catch (e: Exception) {
            ""
        }
    }
}

/**
 * 书源数据类
 */
data class BookSource(
    val name: String,
    val url: String,
    val enable: Boolean = true,
    val searchUrl: String = "",
    val searchList: String = "",
    val searchName: String = "",
    val searchAuthor: String = "",
    val searchNoteUrl: String = "",
    val searchCoverUrl: String = "",
    val chapterList: String = "",
    val chapterName: String = "",
    val contentUrl: String = "",
    val bookContent: String = "",
    val coverUrl: String = "",
    val bookIntro: String = "",
    val userAgent: String = ""
)
