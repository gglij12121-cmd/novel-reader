package com.reader.novel.data.scraper

import com.reader.novel.data.model.Book
import com.reader.novel.data.model.Chapter
import com.reader.novel.data.model.SearchResult
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

/**
 * 书源管理器
 * 支持从"阅读"APP的书源配置文件导入书源
 *
 * 规则格式说明（阅读APP格式）：
 * - 使用 @ 分隔层级选择器
 * - 使用 . 指定class或tag
 * - 使用 # 指定id
 * - 使用 ! 过滤索引
 * - 使用 | 分隔多个选择器
 * - 使用 : 指定属性
 */
class BookSourceManager {

    /** 书源列表 */
    private val bookSources = mutableListOf<BookSource>()

    /**
     * 从JSON字符串加载书源
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
        // 构建搜索URL
        val searchUrl = buildSearchUrl(source, keyword)
        com.reader.novel.ui.components.LogManager.addLog("书源: ${source.name}")
        com.reader.novel.ui.components.LogManager.addLog("搜索URL: $searchUrl")

        return try {
            val userAgent = source.userAgent.ifEmpty {
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36"
            }

            com.reader.novel.ui.components.LogManager.addLog("正在连接...")
            val doc = Jsoup.connect(searchUrl)
                .userAgent(userAgent)
                .timeout(15000)
                .ignoreContentType(true)
                .followRedirects(true)
                .get()

            com.reader.novel.ui.components.LogManager.addLog("连接成功! 标题: ${doc.title()}")

            com.reader.novel.ui.components.LogManager.addLog("页面标题: ${doc.title()}")
            com.reader.novel.ui.components.LogManager.addLog("搜索列表规则: ${source.searchList}")
            com.reader.novel.ui.components.LogManager.addLog("页面HTML长度: ${doc.html().length}")

            val books = mutableListOf<Book>()

            // 解析搜索结果列表
            val listElements = parseRule(doc, source.searchList)
            com.reader.novel.ui.components.LogManager.addLog("找到元素数量: ${listElements.size}")

            for (element in listElements) {
                try {
                    val name = parseRuleForText(element, source.searchName)
                    val author = parseRuleForText(element, source.searchAuthor)
                    val noteUrl = parseRuleForAttr(element, source.searchNoteUrl, "href")
                    val coverUrl = parseRuleForAttr(element, source.searchCoverUrl, "src")

                    com.reader.novel.ui.components.LogManager.addLog("书名: $name, 作者: $author, 链接: $noteUrl")

                    if (name.isNotEmpty() && noteUrl.isNotEmpty()) {
                        val fullUrl = resolveUrl(source.url, noteUrl)

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

            com.reader.novel.ui.components.LogManager.addLog("搜索完成，找到 ${books.size} 本书")
            SearchResult(books = books, source = source.name, isSuccess = true)
        } catch (e: Exception) {
            com.reader.novel.ui.components.LogManager.addLog("${source.name}异常: ${e.message}")
            SearchResult(
                source = source.name,
                isSuccess = false,
                errorMessage = "${source.name}搜索失败: ${e.message}"
            )
        }
    }

    /**
     * 构建搜索URL
     */
    private fun buildSearchUrl(source: BookSource, keyword: String): String {
        var url = source.searchUrl

        // 处理编码
        val hasGbk = url.contains("|char=gbk")
        url = url.replace("|char=gbk", "")

        val encodedKeyword = if (hasGbk) {
            java.net.URLEncoder.encode(keyword, "GBK")
        } else {
            java.net.URLEncoder.encode(keyword, "UTF-8")
        }

        url = url.replace("searchKey", encodedKeyword)
        url = url.replace("searchPage", "1")

        // 处理 @ 分隔符（第一个替换为 ?，后面的替换为 &）
        if (url.contains("@")) {
            val parts = url.split("@")
            url = parts[0] + "?" + parts.drop(1).joinToString("&")
        }

        // 处理多个参数用 | 分隔的情况
        if (url.contains("|")) {
            url = url.split("|")[0]
        }

        return url
    }

    /**
     * 解析规则获取元素列表
     * 支持阅读APP的规则格式：
     * - id.xxx - 通过id获取
     * - class.xxx - 通过class获取
     * - tag.xxx - 通过tag获取
     * - @ - 分隔层级
     * - ! - 过滤索引
     */
    private fun parseRule(element: Element, rule: String): List<Element> {
        if (rule.isEmpty()) return emptyList()

        try {
            // 处理 | 分隔的多个选择器
            if (rule.contains("|")) {
                val rules = rule.split("|")
                for (r in rules) {
                    val result = parseSingleRule(element, r.trim())
                    if (result.isNotEmpty()) return result
                }
                return emptyList()
            }

            return parseSingleRule(element, rule)
        } catch (e: Exception) {
            return emptyList()
        }
    }

    /**
     * 解析单个规则
     */
    private fun parseSingleRule(element: Element, rule: String): List<Element> {
        if (rule.isEmpty()) return emptyList()

        try {
            // 分割层级
            val parts = rule.split("@")
            var currentElements = listOf(element)

            for (part in parts) {
                if (part.isEmpty()) continue

                val nextElements = mutableListOf<Element>()

                for (elem in currentElements) {
                    val found = parsePart(elem, part)
                    nextElements.addAll(found)
                }

                currentElements = nextElements

                if (currentElements.isEmpty()) break
            }

            return currentElements
        } catch (e: Exception) {
            return emptyList()
        }
    }

    /**
     * 解析规则的每个部分
     * 支持格式：
     * - id.xxx
     * - class.xxx
     * - tag.xxx
     * - tag.class
     * - !0:1:2 (过滤索引)
     */
    private fun parsePart(element: Element, part: String): List<Element> {
        if (part.isEmpty()) return listOf(element)

        try {
            // 处理索引过滤 !0:1:2
            if (part.startsWith("!")) {
                val indices = part.removePrefix("!")
                    .split(":")
                    .mapNotNull { it.toIntOrNull() }
                return indices.mapNotNull { index ->
                    element.children().getOrNull(index)
                }
            }

            // 处理 id.xxx
            if (part.startsWith("id.")) {
                val id = part.removePrefix("id.")
                val found = element.getElementById(id)
                return if (found != null) listOf(found) else emptyList()
            }

            // 处理 class.xxx
            if (part.startsWith("class.")) {
                val className = part.removePrefix("class.")
                // 处理索引 class.xxx.0
                val parts = className.split(".")
                val cls = parts[0]
                val index = if (parts.size > 1) parts[1].toIntOrNull() else null

                val elements = element.getElementsByClass(cls)
                return if (index != null) {
                    listOfNotNull(elements.getOrNull(index))
                } else {
                    elements.toList()
                }
            }

            // 处理 tag.xxx
            if (part.startsWith("tag.")) {
                val tag = part.removePrefix("tag.")
                // 处理 tag.a, tag.dd!0:1:2 等
                if (tag.contains("!")) {
                    val tagParts = tag.split("!")
                    val tagName = tagParts[0]
                    val indices = tagParts[1].split(":").mapNotNull { it.toIntOrNull() }
                    val elements = element.getElementsByTag(tagName)
                    return indices.mapNotNull { elements.getOrNull(it) }
                }
                // 处理 tag.span.3
                val tagParts = tag.split(".")
                val tagName = tagParts[0]
                val index = if (tagParts.size > 1) tagParts[1].toIntOrNull() else null
                val elements = element.getElementsByTag(tagName)
                return if (index != null) {
                    listOfNotNull(elements.getOrNull(index))
                } else {
                    elements.toList()
                }
            }

            // 处理 $id.xxx@class.xxx 等复合选择器
            if (part.startsWith("$")) {
                val subPart = part.removePrefix("$")
                return parsePart(element, subPart)
            }

            // 默认当作CSS选择器
            val found = element.select(part)
            return if (found.isNotEmpty()) found.toList() else emptyList()
        } catch (e: Exception) {
            return emptyList()
        }
    }

    /**
     * 解析规则获取文本
     */
    private fun parseRuleForText(element: Element, rule: String): String {
        if (rule.isEmpty()) return ""

        try {
            // 处理 # 分隔的内容替换
            val parts = rule.split("#")
            val selector = parts[0]
            val regex = if (parts.size > 1) parts[1] else null

            val elements = parseRule(element, selector)
            if (elements.isEmpty()) return ""

            val text = elements.first().text().trim()

            // 应用正则替换
            if (regex != null) {
                return text.replace(Regex(regex), "").trim()
            }

            return text
        } catch (e: Exception) {
            return ""
        }
    }

    /**
     * 解析规则获取属性
     */
    private fun parseRuleForAttr(element: Element, rule: String, defaultAttr: String): String {
        if (rule.isEmpty()) return ""

        try {
            // 处理属性指定 :attr
            val parts = rule.split(":")
            val selector = parts[0]
            val attr = if (parts.size > 1) parts[1] else defaultAttr

            val elements = parseRule(element, selector)
            if (elements.isEmpty()) return ""

            return when (attr) {
                "text" -> elements.first().text().trim()
                "html" -> elements.first().html().trim()
                else -> elements.first().attr(attr).trim()
            }
        } catch (e: Exception) {
            return ""
        }
    }

    /**
     * 解析URL
     */
    private fun resolveUrl(baseUrl: String, url: String): String {
        return when {
            url.startsWith("http") -> url
            url.startsWith("//") -> "https:$url"
            url.startsWith("/") -> "$baseUrl$url"
            else -> "$baseUrl/$url"
        }
    }

    /**
     * 获取章节列表
     */
    suspend fun getChapterList(source: BookSource, bookUrl: String): List<Chapter> {
        return try {
            val userAgent = source.userAgent.ifEmpty {
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
            }

            val doc = Jsoup.connect(bookUrl)
                .userAgent(userAgent)
                .timeout(15000)
                .get()

            val chapters = mutableListOf<Chapter>()
            val elements = parseRule(doc, source.chapterList)

            elements.forEachIndexed { index, element ->
                val name = parseRuleForText(element, source.chapterName)
                val url = parseRuleForAttr(element, source.contentUrl, "href")

                if (name.isNotEmpty() && url.isNotEmpty()) {
                    val fullUrl = resolveUrl(source.url, url)

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
            val userAgent = source.userAgent.ifEmpty {
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
            }

            val doc = Jsoup.connect(chapterUrl)
                .userAgent(userAgent)
                .timeout(15000)
                .get()

            // 解析内容规则
            val contentRule = source.bookContent
            if (contentRule.isEmpty()) return "内容加载失败"

            // 处理 $id.content@html 格式
            val elements = parseRule(doc, contentRule)
            if (elements.isEmpty()) return "内容加载失败"

            val content = elements.first()

            // 获取文本内容
            val text = content.html()
                .replace("<br>", "\n")
                .replace("<br/>", "\n")
                .replace("<p>", "\n")
                .replace("</p>", "")
                .replace(Regex("<[^>]+>"), "")
                .replace(Regex("\n{3,}"), "\n\n")
                .trim()

            // 清理广告
            text.replace(Regex("天才一秒记住.*?m\\..*?\\.com"), "")
                .replace(Regex("手机用户请浏览.*?阅读"), "")
                .replace(Regex("请记住本书首发域名.*?。"), "")
                .trim()
        } catch (e: Exception) {
            "内容加载失败: ${e.message}"
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
