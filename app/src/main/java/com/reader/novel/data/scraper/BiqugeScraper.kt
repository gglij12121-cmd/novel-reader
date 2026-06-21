package com.reader.novel.data.scraper

import com.reader.novel.data.model.Book
import com.reader.novel.data.model.Chapter
import com.reader.novel.data.model.SearchResult
import com.reader.novel.util.Constants

/**
 * 笔趣阁爬虫
 *
 * 网站特点：
 * - HTML结构简单，易于解析
 * - 反爬措施较弱
 * - 搜索结果页和详情页结构清晰
 */
class BiqugeScraper : BaseScraper() {

    override val sourceName: String = Constants.BIQUGE_NAME
    override val sourceId: String = Constants.SOURCE_BIQUGE

    /**
     * 搜索小说
     */
    override suspend fun search(keyword: String): SearchResult {
        return try {
            val url = "${Constants.BIQUGE_SEARCH_URL}?keyword=${java.net.URLEncoder.encode(keyword, "UTF-8")}"
            val doc = fetchDocument(url)

            val books = doc.select("div.result-item, li, div.book-item").mapNotNull { element ->
                try {
                    val titleElement = element.selectFirst("a[href*=book], h3 a, .book-name a, a[title]") ?: return@mapNotNull null
                    val title = titleElement.text().trim().ifEmpty { titleElement.attr("title") }
                    val detailUrl = titleElement.attr("href")

                    if (title.isEmpty()) return@mapNotNull null

                    // 补全URL
                    val fullUrl = if (detailUrl.startsWith("http")) {
                        detailUrl
                    } else if (detailUrl.startsWith("/")) {
                        "${Constants.BIQUGE_BASE_URL}$detailUrl"
                    } else {
                        "${Constants.BIQUGE_BASE_URL}/$detailUrl"
                    }

                    val coverUrl = element.selectFirst("img")?.attr("src") ?: ""
                    val author = element.selectFirst(".author, .book-author, span.s4")?.text()?.trim() ?: ""
                    val description = element.selectFirst(".intro, .book-desc, .description")?.text()?.trim() ?: ""
                    val latestChapter = element.selectFirst(".latest, .new-chapter, span.s6")?.text()?.trim() ?: ""

                    Book(
                        title = title,
                        author = author,
                        coverUrl = coverUrl,
                        description = description,
                        source = sourceId,
                        sourceUrl = fullUrl,
                        latestChapter = latestChapter
                    )
                } catch (e: Exception) {
                    null
                }
            }

            if (books.isEmpty()) {
                // 尝试另一种选择器
                val altBooks = doc.select("table tr, div.list-item").mapNotNull { element ->
                    try {
                        val link = element.selectFirst("a") ?: return@mapNotNull null
                        val title = link.text().trim()
                        val href = link.attr("href")

                        if (title.isEmpty() || title == "书名") return@mapNotNull null

                        val fullUrl = if (href.startsWith("http")) href else "${Constants.BIQUGE_BASE_URL}$href"

                        Book(
                            title = title,
                            author = element.select("td, span").getOrNull(1)?.text()?.trim() ?: "",
                            source = sourceId,
                            sourceUrl = fullUrl
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                return SearchResult(books = altBooks, source = sourceId, isSuccess = true)
            }

            SearchResult(books = books, source = sourceId, isSuccess = true)
        } catch (e: Exception) {
            SearchResult(
                books = emptyList(),
                source = sourceId,
                isSuccess = false,
                errorMessage = "搜索失败: ${e.message}"
            )
        }
    }

    /**
     * 获取小说详情和章节列表
     */
    override suspend fun getBookDetail(bookUrl: String): Pair<Book, List<Chapter>> {
        val doc = fetchDocument(bookUrl)

        // 解析书籍信息
        val title = doc.selectFirst("h1, #info h1, .book-title")?.text()?.trim() ?: ""
        val author = doc.selectFirst("#info p, .book-author, .author")?.text()
            ?.replace("作者：", "")?.replace("作者:", "")?.trim() ?: ""
        val description = doc.selectFirst("#intro, .book-desc, .intro")?.text()?.trim() ?: ""
        val coverUrl = doc.selectFirst("#fmimg img, .book-img img, .cover img")?.attr("src") ?: ""
        val latestChapter = doc.select("#info p").lastOrNull()?.text()
            ?.replace("最新章节：", "")?.replace("最新章节:", "")?.trim() ?: ""

        val book = Book(
            title = title,
            author = author,
            coverUrl = coverUrl,
            description = description,
            source = sourceId,
            sourceUrl = bookUrl,
            latestChapter = latestChapter
        )

        // 解析章节列表
        val chapters = mutableListOf<Chapter>()
        val chapterElements = doc.select("#list dl dd a, .book-list a, .chapter-list a, a[href*=chapter]")

        chapterElements.forEachIndexed { index, element ->
            val chapterTitle = element.text().trim()
            val chapterUrl = element.attr("href")

            if (chapterTitle.isNotEmpty()) {
                val fullUrl = if (chapterUrl.startsWith("http")) {
                    chapterUrl
                } else if (chapterUrl.startsWith("/")) {
                    "${Constants.BIQUGE_BASE_URL}$chapterUrl"
                } else {
                    "${Constants.BIQUGE_BASE_URL}/$chapterUrl"
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

        // 获取正文内容
        val contentElement = doc.selectFirst("#content, .content, .chapter-content, #booktext, #chaptercontent") ?: return ""

        // 获取HTML内容并清理
        val rawContent = contentElement.html()

        // 清理广告和无用内容
        return cleanContent(rawContent)
    }
}
