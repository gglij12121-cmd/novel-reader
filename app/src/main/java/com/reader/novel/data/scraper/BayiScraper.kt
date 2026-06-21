package com.reader.novel.data.scraper

import com.reader.novel.data.model.Book
import com.reader.novel.data.model.Chapter
import com.reader.novel.data.model.SearchResult
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
            val url = "${Constants.BAYI_SEARCH_URL}?q=${java.net.URLEncoder.encode(keyword, "UTF-8")}"

            val html = withContext(Dispatchers.IO) {
                val request = Request.Builder()
                    .url(url)
                    .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36")
                    .addHeader("Accept", "text/html,*/*")
                    .build()
                client.newCall(request).execute().body?.source()?.readUtf8() ?: ""
            }

            if (html.isEmpty()) {
                return SearchResult(books = emptyList(), source = sourceId, isSuccess = false, errorMessage = "空响应")
            }

            val doc = Jsoup.parse(html)
            val books = mutableListOf<Book>()

            val elements = doc.select("li, div.result-item, div.book-item, dt")
            for (element in elements) {
                try {
                    val link = element.select("a[href]").first() ?: continue
                    val title = link.text().trim().ifEmpty { link.attr("title") }
                    val href = link.attr("href")

                    if (title.isEmpty() || title.length < 2 || href.isEmpty() || href == "#") continue

                    val fullUrl = if (href.startsWith("http")) href
                        else if (href.startsWith("/")) "${Constants.BAYI_BASE_URL}$href"
                        else "${Constants.BAYI_BASE_URL}/$href"

                    val author = element.select(".author, .s4, .book-author").first()?.text()?.trim() ?: ""
                    val description = element.select(".intro, .s6, .book-desc").first()?.text()?.trim() ?: ""

                    books.add(Book(
                        title = title, author = author, description = description,
                        source = sourceId, sourceUrl = fullUrl
                    ))
                } catch (e: Exception) {
                    continue
                }
            }

            if (books.isEmpty()) {
                SearchResult(books = emptyList(), source = sourceId, isSuccess = false, errorMessage = "未找到")
            } else {
                SearchResult(books = books, source = sourceId, isSuccess = true)
            }
        } catch (e: Exception) {
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
                val fullUrl = if (url.startsWith("http")) url else "${Constants.BAYI_BASE_URL}$url"
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
