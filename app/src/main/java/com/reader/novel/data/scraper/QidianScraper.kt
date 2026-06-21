package com.reader.novel.data.scraper

import com.reader.novel.data.model.Book
import com.reader.novel.data.model.Chapter
import com.reader.novel.data.model.SearchResult
import com.reader.novel.util.Constants

/**
 * 起点中文网爬虫
 *
 * 网站特点：
 * - 反爬措施较强
 * - 部分内容需要登录
 * - 章节内容可能动态加载
 * - 使用HTTPS
 *
 * 注意：
 * - 起点有较强的反爬机制，可能需要更复杂的处理
 * - 仅爬取免费章节
 * - 建议配合WebView使用
 */
class QidianScraper : BaseScraper() {

    override val sourceName: String = Constants.QIDIAN_NAME
    override val sourceId: String = Constants.SOURCE_QIDIAN

    /**
     * 搜索小说
     */
    override suspend fun search(keyword: String): SearchResult {
        return try {
            val url = "${Constants.QIDIAN_SEARCH_URL}?kw=${java.net.URLEncoder.encode(keyword, "UTF-8")}"
            val doc = fetchDocument(url)

            val books = doc.select("li.book-mid-info, .res-book-item, .book-img-text li, div.book-mid-info").mapNotNull { element ->
                try {
                    val titleElement = element.selectFirst("h4 a, .book-mid-info h2 a, a.name, h2 a") ?: return@mapNotNull null
                    val title = titleElement.text().trim()
                    val detailUrl = titleElement.attr("href")

                    if (title.isEmpty()) return@mapNotNull null

                    val fullUrl = if (detailUrl.startsWith("http")) {
                        detailUrl
                    } else if (detailUrl.startsWith("//")) {
                        "https:$detailUrl"
                    } else {
                        "https://www.qidian.com$detailUrl"
                    }

                    val author = element.selectFirst("p.author a, .author a.name, a.writer, .author")?.text()?.trim() ?: ""
                    val description = element.selectFirst("p.intro, .book-desc, .intro")?.text()?.trim() ?: ""
                    val latestChapter = element.selectFirst("p.update a, .latest-chapter a, .update")?.text()?.trim() ?: ""

                    val parentElement = element.parent()
                    val coverUrl = parentElement?.selectFirst("img")?.attr("src") ?: ""

                    Book(
                        title = title,
                        author = author,
                        coverUrl = if (coverUrl.startsWith("//")) "https:$coverUrl" else coverUrl,
                        description = description,
                        source = sourceId,
                        sourceUrl = fullUrl,
                        latestChapter = latestChapter
                    )
                } catch (e: Exception) {
                    null
                }
            }

            SearchResult(books = books, source = sourceId, isSuccess = true)
        } catch (e: Exception) {
            SearchResult(
                books = emptyList(),
                source = sourceId,
                isSuccess = false,
                errorMessage = "起点搜索失败 (可能需要翻墙或登录): ${e.message}"
            )
        }
    }

    /**
     * 获取小说详情和章节列表
     */
    override suspend fun getBookDetail(bookUrl: String): Pair<Book, List<Chapter>> {
        val doc = fetchDocument(bookUrl)

        val title = doc.selectFirst("h1, .book-info h1, .book-name")?.text()?.trim() ?: ""
        val author = doc.selectFirst("p.writer a, .book-info a.writer, a.name")?.text()?.trim() ?: ""
        val description = doc.selectFirst("p.intro, .book-desc, .book-intro")?.text()?.trim() ?: ""
        val coverUrl = doc.selectFirst(".book-img img, .book-information img")?.attr("src") ?: ""
        val latestChapter = doc.selectFirst(".latest-chapter a, p.update a")?.text()?.trim() ?: ""

        val fullCoverUrl = if (coverUrl.startsWith("//")) "https:$coverUrl" else coverUrl

        val book = Book(
            title = title,
            author = author,
            coverUrl = fullCoverUrl,
            description = description,
            source = sourceId,
            sourceUrl = bookUrl,
            latestChapter = latestChapter
        )

        val chapters = mutableListOf<Chapter>()
        val chapterElements = doc.select("ul.volume li a, .chapter-list li a, .catalog-content a, a[href*=chapter]")

        chapterElements.forEachIndexed { index, element ->
            val chapterTitle = element.text().trim()
            val chapterUrl = element.attr("href")

            if (chapterTitle.isNotEmpty() && !chapterTitle.contains("vip")) {
                val fullUrl = if (chapterUrl.startsWith("http")) {
                    chapterUrl
                } else if (chapterUrl.startsWith("//")) {
                    "https:$chapterUrl"
                } else {
                    "https://www.qidian.com$chapterUrl"
                }

                chapters.add(
                    Chapter(
                        title = chapterTitle,
                        url = fullUrl,
                        chapterIndex = index
                    )
                )
            }
        }

        return Pair(book, chapters)
    }

    /**
     * 获取章节正文内容
     */
    override suspend fun getChapterContent(chapterUrl: String): String {
        val doc = fetchDocument(chapterUrl)

        val contentElement = doc.selectFirst(
            ".read-content, .chapter-content, #chapterContent, #BookText, .text-content"
        ) ?: return ""

        val paragraphs = contentElement.select("p")
        if (paragraphs.isNotEmpty()) {
            return paragraphs.map { it.text().trim() }
                .filter { it.isNotEmpty() }
                .joinToString("\n\n")
        }

        val rawContent = contentElement.html()
        return cleanContent(rawContent)
    }
}
