package com.reader.novel.data.scraper

import com.reader.novel.data.model.Book
import com.reader.novel.data.model.Chapter
import com.reader.novel.data.model.SearchResult
import com.reader.novel.ui.components.LogManager
import com.reader.novel.util.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.util.concurrent.TimeUnit

/**
 * 八一中文网爬虫 - 使用OkHttp + IO线程
 */
class BayiScraper : BaseScraper() {

    override val sourceName: String = Constants.BAYI_NAME
    override val sourceId: String = Constants.SOURCE_BAYI

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    override suspend fun search(keyword: String): SearchResult {
        return try {
            LogManager.addLog("[$sourceName] 开始搜索: $keyword")
            val url = "${Constants.BAYI_SEARCH_URL}?q=${java.net.URLEncoder.encode(keyword, "UTF-8")}"
            LogManager.addLog("[$sourceName] URL: $url")

            val html = withContext(Dispatchers.IO) {
                val request = Request.Builder()
                    .url(url)
                    .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36")
                    .addHeader("Accept", "text/html,*/*")
                    .build()
                client.newCall(request).execute().body?.source()?.readUtf8() ?: ""
            }

            LogManager.addLog("[$sourceName] 响应长度: ${html.length}")

            if (html.isEmpty()) {
                return SearchResult(books = emptyList(), source = sourceId, isSuccess = false, errorMessage = "空响应")
            }

            val doc = Jsoup.parse(html)
            val books = mutableListOf<Book>()

            // 81zw.cc 使用 dt/dd 结构，和 biquge5.com 相同
            val dtElements = doc.select("dt")
            LogManager.addLog("[$sourceName] 找到dt元素: ${dtElements.size}")

            for (dt in dtElements) {
                try {
                    val container = dt.parent() ?: continue
                    val dds = container.select("dd")

                    val titleLink = dds.select("h3 a").first() ?: continue
                    val title = titleLink.text().trim()
                    val href = titleLink.attr("href")

                    if (title.isEmpty() || title.length < 2 || href.isEmpty() || href == "#") continue

                    val fullUrl = if (href.startsWith("http")) href
                        else if (href.startsWith("/")) "${Constants.BAYI_BASE_URL}$href"
                        else "${Constants.BAYI_BASE_URL}/$href"

                    val author = dds.select("dd.book_other span").first()?.text()?.trim() ?: ""
                    val latestChapter = dds.select("dd.book_other").last()?.selectFirst("a")?.text()?.trim() ?: ""

                    books.add(Book(
                        title = title, author = author, description = "",
                        source = sourceId, sourceUrl = fullUrl, latestChapter = latestChapter
                    ))
                } catch (e: Exception) {
                    continue
                }
            }

            // 兜底
            if (books.isEmpty()) {
                for (element in doc.select("li")) {
                    try {
                        val link = element.select("a[href]").first() ?: continue
                        val title = link.text().trim().ifEmpty { link.attr("title") }
                        val href = link.attr("href")
                        if (title.isEmpty() || title.length < 2 || href.isEmpty() || href == "#") continue

                        val fullUrl = if (href.startsWith("http")) href
                            else if (href.startsWith("/")) "${Constants.BAYI_BASE_URL}$href"
                            else "${Constants.BAYI_BASE_URL}/$href"

                        val author = element.select(".author, .s4, .book-author").first()?.text()?.trim() ?: ""
                        books.add(Book(
                            title = title, author = author, source = sourceId, sourceUrl = fullUrl
                        ))
                    } catch (e: Exception) { continue }
                }
            }

            LogManager.addLog("[$sourceName] 找到书籍: ${books.size}")

            if (books.isEmpty()) {
                SearchResult(books = emptyList(), source = sourceId, isSuccess = false, errorMessage = "未找到")
            } else {
                SearchResult(books = books, source = sourceId, isSuccess = true)
            }
        } catch (e: Exception) {
            LogManager.addLog("[$sourceName] 异常: ${e.javaClass.simpleName}: ${e.message}")
            SearchResult(books = emptyList(), source = sourceId, isSuccess = false, errorMessage = e.message)
        }
    }

    override suspend fun getBookDetail(bookUrl: String): Pair<Book, List<Chapter>> {
        LogManager.addLog("[$sourceName] 加载详情: $bookUrl")
        val html = withContext(Dispatchers.IO) {
            val request = Request.Builder().url(bookUrl)
                .addHeader("User-Agent", Constants.USER_AGENT)
                .addHeader("Accept", "text/html,*/*")
                .build()
            client.newCall(request).execute().body?.source()?.readUtf8() ?: ""
        }
        LogManager.addLog("[$sourceName] 详情页长度: ${html.length}")
        val doc = Jsoup.parse(html)

        val title = doc.select("h1").first()?.text()?.trim() ?: ""
        val author = doc.select(".info li").firstOrNull { it.text().contains("作者") }
            ?.select("a")?.first()?.text()?.trim() ?: ""
        val description = doc.select("#intro_pc").first()?.text()?.trim()
            ?.replace("简介：", "")?.trim() ?: ""
        val coverUrl = doc.select(".img-thumbnail, .book_info img").first()?.attr("src") ?: ""
        val fullCoverUrl = if (coverUrl.startsWith("http")) coverUrl
            else if (coverUrl.startsWith("//")) "https:$coverUrl"
            else "${Constants.BAYI_BASE_URL}$coverUrl"

        val book = Book(
            title = title, author = author, description = description,
            coverUrl = fullCoverUrl, source = sourceId, sourceUrl = bookUrl
        )

        val chapters = mutableListOf<Chapter>()
        val chapterElements = doc.select(".book_list ul li a")
        LogManager.addLog("[$sourceName] 章节元素数: ${chapterElements.size}")
        chapterElements.forEachIndexed { index, element ->
            val name = element.text().trim()
            val url = element.attr("href")
            if (name.isNotEmpty()) {
                val fullUrl = if (url.startsWith("http")) url
                    else if (url.startsWith("/")) "${Constants.BAYI_BASE_URL}$url"
                    else "${Constants.BAYI_BASE_URL}/$url"
                chapters.add(Chapter(title = name, url = fullUrl, chapterIndex = index))
            }
        }

        LogManager.addLog("[$sourceName] 详情加载成功: $title, 章节: ${chapters.size}")
        return Pair(book, chapters)
    }

    override suspend fun getChapterContent(chapterUrl: String): String {
        val html = withContext(Dispatchers.IO) {
            val request = Request.Builder().url(chapterUrl)
                .addHeader("User-Agent", Constants.USER_AGENT)
                .addHeader("Accept", "text/html,*/*")
                .build()
            client.newCall(request).execute().body?.source()?.readUtf8() ?: ""
        }
        val doc = Jsoup.parse(html)
        val contentEl = doc.select("#content, .content, #booktext, #chaptercontent").first()
        if (contentEl != null) {
            return contentEl.html()
                .replace("<br>", "\n").replace("<br/>", "\n").replace("<br />", "\n")
                .replace("<p>", "").replace("</p>", "\n")
                .replace(Regex("<[^>]+>"), "")
                .replace(Regex("天才一秒记住.*?m\\..*?\\.com"), "")
                .replace(Regex("手机用户请浏览.*?阅读"), "")
                .replace(Regex("请记住本书首发域名.*?。"), "")
                .replace(Regex("\n{3,}"), "\n\n")
                .trim()
        }
        return "章节内容加载失败"
    }
}
