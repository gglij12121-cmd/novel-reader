package com.reader.novel.data.scraper

import com.reader.novel.data.model.Book
import com.reader.novel.data.model.Chapter
import com.reader.novel.data.model.SearchResult
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.net.Proxy
import java.net.ProxySelector
import java.net.URI
import java.util.concurrent.TimeUnit

/**
 * 书源管理器 - 使用OkHttp替代Jsoup直接请求
 */
class BookSourceManager {

    private val bookSources = mutableListOf<BookSource>()

    /** OkHttp客户端 - 使用系统代理设置 */
    private val client = run {
        // 获取系统代理
        val proxyList = ProxySelector.getDefault().select(URI.create("http://www.example.com"))
        val systemProxy = if (proxyList.isNotEmpty()) proxyList.first() else Proxy.NO_PROXY

        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .proxy(systemProxy)
            .build()
    }

    fun loadFromJson(json: String) {
        try {
            val sources = parseBookSourceJson(json)
            bookSources.clear()
            bookSources.addAll(sources.filter { it.enable })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

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

    fun getAvailableSources(): List<BookSource> = bookSources.filter { it.enable }

    suspend fun searchAll(keyword: String, maxSources: Int = 10): List<SearchResult> = coroutineScope {
        val sourcesToSearch = bookSources.filter { it.enable }.take(maxSources)

        sourcesToSearch.map { source ->
            async {
                try {
                    searchFromSource(source, keyword)
                } catch (e: Exception) {
                    SearchResult(
                        source = source.name,
                        isSuccess = false,
                        errorMessage = "${source.name}: ${e.message}"
                    )
                }
            }
        }.awaitAll()
    }

    private suspend fun searchFromSource(source: BookSource, keyword: String): SearchResult {
        val searchUrl = buildSearchUrl(source, keyword)
        com.reader.novel.ui.components.LogManager.addLog("书源: ${source.name}")
        com.reader.novel.ui.components.LogManager.addLog("URL: $searchUrl")

        return try {
            // 使用OkHttp请求，模拟真实浏览器
            val request = Request.Builder()
                .url(searchUrl)
                .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                .addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                .addHeader("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                .addHeader("Accept-Encoding", "gzip, deflate")
                .addHeader("Connection", "keep-alive")
                .addHeader("Referer", source.url)
                .build()

            val response = client.newCall(request).execute()
            val html = response.body?.string() ?: ""
            val statusCode = response.code

            com.reader.novel.ui.components.LogManager.addLog("响应状态: $statusCode, 长度: ${html.length}")

            if (html.isEmpty() || statusCode != 200) {
                return SearchResult(books = emptyList(), source = source.name, isSuccess = false, errorMessage = "HTTP $statusCode")
            }

            val doc = Jsoup.parse(html)
            val books = mutableListOf<Book>()

            // 解析搜索结果
            val listElements = parseRule(doc, source.searchList)
            com.reader.novel.ui.components.LogManager.addLog("找到元素: ${listElements.size}")

            for (element in listElements) {
                try {
                    val name = parseRuleForText(element, source.searchName)
                    val author = parseRuleForText(element, source.searchAuthor)
                    val noteUrl = parseRuleForAttr(element, source.searchNoteUrl, "href")
                    val coverUrl = parseRuleForAttr(element, source.searchCoverUrl, "src")

                    if (name.isNotEmpty() && noteUrl.isNotEmpty()) {
                        val fullUrl = resolveUrl(source.url, noteUrl)
                        books.add(Book(
                            title = name, author = author, coverUrl = coverUrl,
                            source = source.name, sourceUrl = fullUrl
                        ))
                    }
                } catch (e: Exception) {
                    continue
                }
            }

            com.reader.novel.ui.components.LogManager.addLog("找到书籍: ${books.size}")
            SearchResult(books = books, source = source.name, isSuccess = true)
        } catch (e: Exception) {
            com.reader.novel.ui.components.LogManager.addLog("异常: ${e.javaClass.simpleName}: ${e.message}")
            SearchResult(books = emptyList(), source = source.name, isSuccess = false, errorMessage = "${e.message}")
        }
    }

    private fun buildSearchUrl(source: BookSource, keyword: String): String {
        var url = source.searchUrl
        val hasGbk = url.contains("|char=gbk")
        url = url.replace("|char=gbk", "")

        val encodedKeyword = if (hasGbk) {
            java.net.URLEncoder.encode(keyword, "GBK")
        } else {
            java.net.URLEncoder.encode(keyword, "UTF-8")
        }

        url = url.replace("searchKey", encodedKeyword)
        url = url.replace("searchPage", "1")

        if (url.contains("@")) {
            val parts = url.split("@")
            url = parts[0] + "?" + parts.drop(1).joinToString("&")
        }

        if (url.contains("|")) {
            url = url.split("|")[0]
        }

        return url
    }

    private fun parseRule(element: org.jsoup.nodes.Element, rule: String): List<org.jsoup.nodes.Element> {
        if (rule.isEmpty()) return emptyList()
        try {
            if (rule.contains("|")) {
                for (r in rule.split("|")) {
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

    private fun parseSingleRule(element: org.jsoup.nodes.Element, rule: String): List<org.jsoup.nodes.Element> {
        if (rule.isEmpty()) return emptyList()
        try {
            val parts = rule.split("@")
            var currentElements = listOf(element)
            for (part in parts) {
                if (part.isEmpty()) continue
                val nextElements = mutableListOf<org.jsoup.nodes.Element>()
                for (elem in currentElements) {
                    nextElements.addAll(parsePart(elem, part))
                }
                currentElements = nextElements
                if (currentElements.isEmpty()) break
            }
            return currentElements
        } catch (e: Exception) {
            return emptyList()
        }
    }

    private fun parsePart(element: org.jsoup.nodes.Element, part: String): List<org.jsoup.nodes.Element> {
        if (part.isEmpty()) return listOf(element)
        try {
            if (part.startsWith("!")) {
                val indices = part.removePrefix("!").split(":").mapNotNull { it.toIntOrNull() }
                return indices.mapNotNull { element.children().getOrNull(it) }
            }
            if (part.startsWith("id.")) {
                val id = part.removePrefix("id.")
                val found = element.getElementById(id)
                return if (found != null) listOf(found) else emptyList()
            }
            if (part.startsWith("class.")) {
                val className = part.removePrefix("class.")
                val subParts = className.split(".")
                val cls = subParts[0]
                val index = if (subParts.size > 1) subParts[1].toIntOrNull() else null
                val elements = element.getElementsByClass(cls)
                return if (index != null) listOfNotNull(elements.getOrNull(index)) else elements.toList()
            }
            if (part.startsWith("tag.")) {
                val tag = part.removePrefix("tag.")
                if (tag.contains("!")) {
                    val tagParts = tag.split("!")
                    val tagName = tagParts[0]
                    val indices = tagParts[1].split(":").mapNotNull { it.toIntOrNull() }
                    val elements = element.getElementsByTag(tagName)
                    return indices.mapNotNull { elements.getOrNull(it) }
                }
                val tagParts = tag.split(".")
                val tagName = tagParts[0]
                val index = if (tagParts.size > 1) tagParts[1].toIntOrNull() else null
                val elements = element.getElementsByTag(tagName)
                return if (index != null) listOfNotNull(elements.getOrNull(index)) else elements.toList()
            }
            if (part.startsWith("$")) {
                return parsePart(element, part.removePrefix("$"))
            }
            val found = element.select(part)
            return if (found.isNotEmpty()) found.toList() else emptyList()
        } catch (e: Exception) {
            return emptyList()
        }
    }

    private fun parseRuleForText(element: org.jsoup.nodes.Element, rule: String): String {
        if (rule.isEmpty()) return ""
        try {
            val parts = rule.split("#")
            val selector = parts[0]
            val regex = if (parts.size > 1) parts[1] else null
            val elements = parseRule(element, selector)
            if (elements.isEmpty()) return ""
            val text = elements.first().text().trim()
            return if (regex != null) text.replace(Regex(regex), "").trim() else text
        } catch (e: Exception) {
            return ""
        }
    }

    private fun parseRuleForAttr(element: org.jsoup.nodes.Element, rule: String, defaultAttr: String): String {
        if (rule.isEmpty()) return ""
        try {
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

    private fun resolveUrl(baseUrl: String, url: String): String {
        return when {
            url.startsWith("http") -> url
            url.startsWith("//") -> "https:$url"
            url.startsWith("/") -> "$baseUrl$url"
            else -> "$baseUrl/$url"
        }
    }
}

data class BookSource(
    val name: String, val url: String, val enable: Boolean = true,
    val searchUrl: String = "", val searchList: String = "", val searchName: String = "",
    val searchAuthor: String = "", val searchNoteUrl: String = "", val searchCoverUrl: String = "",
    val chapterList: String = "", val chapterName: String = "", val contentUrl: String = "",
    val bookContent: String = "", val coverUrl: String = "", val bookIntro: String = "",
    val userAgent: String = ""
)
