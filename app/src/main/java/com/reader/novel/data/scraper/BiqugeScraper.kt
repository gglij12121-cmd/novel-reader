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
 * 笔趣阁爬虫 - 使用OkHttp + IO线程
 */
class BiqugeScraper : BaseScraper() {

    override val sourceName: String = Constants.BIQUGE_NAME
    override val sourceId: String = Constants.SOURCE_BIQUGE

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    override suspend fun search(keyword: String): SearchResult {
        return try {
            LogManager.addLog("[$sourceName] 开始搜索: $keyword")
            val url = "${Constants.BIQUGE_SEARCH_URL}?q=${java.net.URLEncoder.encode(keyword, "UTF-8")}"
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

            // 解析搜索结果 - biquge5.com 使用 dt/dd 结构:
            // <dt><a href="/67_67129/">封面链接</a></dt>
            // <dd><h3><a href="/67_67129/">书名</a></h3></dd>
            // <dd class="book_other">作者：<span>作者名</span></dd>
            val dtElements = doc.select("dt")
            LogManager.addLog("[$sourceName] 找到dt元素: ${dtElements.size}")

            for (dt in dtElements) {
                try {
                    // 获取dt的父容器，里面包含所有相关的dd
                    val container = dt.parent() ?: continue
                    val dds = container.select("dd")

                    // 书名在 dd > h3 > a
                    val titleLink = dds.selectFirst("h3 a") ?: continue
                    val title = titleLink.text().trim()
                    val href = titleLink.attr("href")

                    if (title.isEmpty() || title.length < 2 || href.isEmpty() || href == "#") continue

                    val fullUrl = if (href.startsWith("http")) href
                        else if (href.startsWith("/")) "${Constants.BIQUGE_BASE_URL}$href"
                        else "${Constants.BIQUGE_BASE_URL}/$href"

                    // 作者在 dd.book_other span
                    val author = dds.selectFirst("dd.book_other span")?.text()?.trim() ?: ""
                    // 最新章节在最后一个 dd.book_other a
                    val latestChapter = dds.select("dd.book_other").lastOrNull()?.selectFirst("a")?.text()?.trim() ?: ""

                    books.add(Book(
                        title = title, author = author, description = "",
                        source = sourceId, sourceUrl = fullUrl, latestChapter = latestChapter
                    ))
                } catch (e: Exception) {
                    continue
                }
            }

            // 兜底：尝试通用 li 选择器
            if (books.isEmpty()) {
                for (element in doc.select("li")) {
                    try {
                        val link = element.select("a[href]").first() ?: continue
                        val title = link.text().trim().ifEmpty { link.attr("title") }
                        val href = link.attr("href")
                        if (title.isEmpty() || title.length < 2 || href.isEmpty() || href == "#") continue

                        val fullUrl = if (href.startsWith("http")) href
                            else if (href.startsWith("/")) "${Constants.BIQUGE_BASE_URL}$href"
                            else "${Constants.BIQUGE_BASE_URL}/$href"

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
        val html = withContext(Dispatchers.IO) {
            val request = Request.Builder().url(bookUrl)
                .addHeader("User-Agent", "Mozilla/5.0").build()
            client.newCall(request).execute().body?.source()?.readUtf8() ?: ""
        }
        val doc = Jsoup.parse(html)

        val title = doc.select("h1, #info h1").first()?.text()?.trim() ?: ""
        val author = doc.select("#info p").first()?.text()?.replace("作者：", "")?.trim() ?: ""
        val description = doc.select("#intro").first()?.text()?.trim() ?: ""

        val book = Book(title = title, author = author, description = description, source = sourceId, sourceUrl = bookUrl)

        val chapters = mutableListOf<Chapter>()
        doc.select("#list dl dd a").forEachIndexed { index, element ->
            val name = element.text().trim()
            val url = element.attr("href")
            if (name.isNotEmpty()) {
                val fullUrl = if (url.startsWith("http")) url else "${Constants.BIQUGE_BASE_URL}$url"
                chapters.add(Chapter(title = name, url = fullUrl, chapterIndex = index))
            }
        }

        return Pair(book, chapters)
    }

    override suspend fun getChapterContent(chapterUrl: String): String {
        val html = withContext(Dispatchers.IO) {
            val request = Request.Builder().url(chapterUrl)
                .addHeader("User-Agent", "Mozilla/5.0").build()
            client.newCall(request).execute().body?.source()?.readUtf8() ?: ""
        }
        val doc = Jsoup.parse(html)
        val content = doc.select("#content, .content").first()?.text() ?: ""
        return content.ifEmpty { "章节内容加载失败" }
    }
}
