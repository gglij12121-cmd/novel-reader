package com.reader.novel.data.scraper

import com.reader.novel.data.model.Book
import com.reader.novel.data.model.Chapter
import com.reader.novel.data.model.SearchResult
import com.reader.novel.util.Constants

/**
 * 八一中文网爬虫
 *
 * 网站特点：
 * - 结构规整，内容丰富
 * - 反爬措施较弱
 * - HTML结构清晰
 */
class BayiScraper : BaseScraper() {

    override val sourceName: String = Constants.BAYI_NAME
    override val sourceId: String = Constants.SOURCE_BAYI

    /**
     * 搜索小说
     */
    override suspend fun search(keyword: String): SearchResult {
        return try {
            val url = "${Constants.BAYI_SEARCH_URL}?keyword=${java.net.URLEncoder.encode(keyword, "UTF-8")}"
            val doc = fetchDocument(url)

            val books = doc.select("div.novelslist li, table.tb li, div.search-item, div.book-item, ul.novels-list li").mapNotNull { element ->
                try {
                    val titleElement = element.selectFirst("a[href*=book], .book-name a, h3 a, a[title]") ?: return@mapNotNull null
                    val title = titleElement.text().trim().ifEmpty { titleElement.attr("title") }
                    val detailUrl = titleElement.attr("href")

                    if (title.isEmpty() || title == "书名") return@mapNotNull null

                    val fullUrl = if (detailUrl.startsWith("http")) {
                        detailUrl
                    } else if (detailUrl.startsWith("/")) {
                        "${Constants.BAYI_BASE_URL}$detailUrl"
                    } else {
                        "${Constants.BAYI_BASE_URL}/$detailUrl"
                    }

                    val author = element.selectFirst(".author, .s4, .book-author")?.text()?.trim() ?: ""
                    val latestChapter = element.selectFirst(".new-chapter, .s6, .latest")?.text()?.trim() ?: ""
                    val description = element.selectFirst(".intro, .book-desc")?.text()?.trim() ?: ""

                    Book(
                        title = title,
                        author = author,
                        source = sourceId,
                        sourceUrl = fullUrl,
                        latestChapter = latestChapter,
                        description = description
                    )
                } catch (e: Exception) {
                    null
                }
            }

            if (books.isEmpty()) {
                val altBooks = doc.select("table tr, div.list-item, li").mapNotNull { element ->
                    try {
                        val link = element.selectFirst("a") ?: return@mapNotNull null
                        val title = link.text().trim()
                        val href = link.attr("href")

                        if (title.isEmpty() || title == "书名" || title.length < 2) return@mapNotNull null

                        val fullUrl = if (href.startsWith("http")) href else "${Constants.BAYI_BASE_URL}$href"

                        Book(
                            title = title,
                            author = element.select("td, span, .author").getOrNull(1)?.text()?.trim() ?: "",
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

        val title = doc.selectFirst("h1, #info h1, .book-title, .book-name")?.text()?.trim() ?: ""
        val author = doc.selectFirst("#info p, .book-author, .author, .writer")?.text()
            ?.replace("作者：", "")?.replace("作者:", "")?.trim() ?: ""
        val description = doc.selectFirst("#intro, .bookintro, .intro, .book-desc")?.text()?.trim() ?: ""
        val coverUrl = doc.selectFirst("#fmimg img, .bookimg img, .cover img")?.attr("src") ?: ""
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

        val chapters = mutableListOf<Chapter>()
        val chapterElements = doc.select("#list dl dd a, .booklist a, .chapter-list a, a[href*=chapter], a[href*=.html]")

        chapterElements.forEachIndexed { index, element ->
            val chapterTitle = element.text().trim()
            val chapterUrl = element.attr("href")

            if (chapterTitle.isNotEmpty()) {
                val fullUrl = if (chapterUrl.startsWith("http")) {
                    chapterUrl
                } else if (chapterUrl.startsWith("/")) {
                    "${Constants.BAYI_BASE_URL}$chapterUrl"
                } else {
                    "${Constants.BAYI_BASE_URL}/$chapterUrl"
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

        val contentElement = doc.selectFirst("#content, .content, #booktext, .chapter-content, #chaptercontent") ?: return ""

        val rawContent = contentElement.html()

        return cleanContent(rawContent)
    }
}
