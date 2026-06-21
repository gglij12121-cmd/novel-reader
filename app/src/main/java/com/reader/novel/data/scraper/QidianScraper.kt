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
     * 搜索小说 - 支持模糊搜索
     */
    override suspend fun search(keyword: String): SearchResult {
        return try {
            val results = mutableListOf<Book>()

            // 方式1：直接搜索
            val url1 = "${Constants.QIDIAN_SEARCH_URL}?kw=${java.net.URLEncoder.encode(keyword, "UTF-8")}"
            try {
                val doc1 = fetchDocument(url1)
                val books1 = parseSearchResults(doc1)
                results.addAll(books1)
            } catch (e: Exception) {
                // 忽略错误
            }

            // 方式2：如果结果太少，尝试搜索部分关键词
            if (results.size < 3 && keyword.length > 2) {
                val shortKeyword = keyword.substring(0, keyword.length / 2)
                val url2 = "${Constants.QIDIAN_SEARCH_URL}?kw=${java.net.URLEncoder.encode(shortKeyword, "UTF-8")}"
                try {
                    val doc2 = fetchDocument(url2)
                    val books2 = parseSearchResults(doc2)
                        .filter { book -> book.title.contains(keyword) || book.author.contains(keyword) }
                    results.addAll(books2)
                } catch (e: Exception) {
                    // 忽略错误
                }
            }

            val uniqueBooks = results.distinctBy { it.title + it.author }

            if (uniqueBooks.isEmpty()) {
                SearchResult(
                    books = emptyList(),
                    source = sourceId,
                    isSuccess = false,
                    errorMessage = "起点搜索失败 (可能需要翻墙或登录)"
                )
            } else {
                SearchResult(books = uniqueBooks, source = sourceId, isSuccess = true)
            }
        } catch (e: Exception) {
            SearchResult(
                books = emptyList(),
                source = sourceId,
                isSuccess = false,
                errorMessage = "起点搜索失败: ${e.message}"
            )
        }
    }

    /**
     * 解析搜索结果
     */
    private fun parseSearchResults(doc: org.jsoup.nodes.Document): List<Book> {
        val books = mutableListOf<Book>()

        val selectors = listOf(
            "li.book-mid-info",
            ".res-book-item",
            ".book-img-text li",
            "div.book-mid-info",
            ".book-mid-info",
            "li",
            "div.search-item"
        )

        for (selector in selectors) {
            val elements = doc.select(selector)
            for (element in elements) {
                try {
                    val link = element.selectFirst("h4 a, h2 a, a.name, a[href]") ?: continue
                    val title = link.text().trim()
                    val href = link.attr("href")

                    if (title.isEmpty() || title.length < 2) continue
                    if (href.isEmpty() || href == "#") continue

                    val fullUrl = when {
                        href.startsWith("http") -> href
                        href.startsWith("//") -> "https:$href"
                        href.startsWith("/") -> "https://www.qidian.com$href"
                        else -> "https://www.qidian.com/$href"
                    }

                    val author = element.selectFirst("p.author a, .author a.name, a.writer, .author")?.text()?.trim() ?: ""
                    val description = element.selectFirst("p.intro, .book-desc, .intro, .description")?.text()?.trim() ?: ""
                    val latestChapter = element.selectFirst("p.update a, .latest-chapter a, .update")?.text()?.trim() ?: ""

                    val parentElement = element.parent()
                    val coverUrl = parentElement?.selectFirst("img")?.attr("src") ?: ""
                    val fullCoverUrl = if (coverUrl.startsWith("//")) "https:$coverUrl" else coverUrl

                    books.add(
                        Book(
                            title = title,
                            author = author,
                            coverUrl = fullCoverUrl,
                            description = description,
                            source = sourceId,
                            sourceUrl = fullUrl,
                            latestChapter = latestChapter
                        )
                    )
                } catch (e: Exception) {
                    continue
                }
            }

            if (books.isNotEmpty()) break
        }

        return books
    }

    /**
     * 获取小说详情和章节列表
     */
    override suspend fun getBookDetail(bookUrl: String): Pair<Book, List<Chapter>> {
        val doc = fetchDocument(bookUrl)

        val title = doc.selectFirst("h1, .book-info h1, .book-name, .title")?.text()?.trim() ?: ""
        val author = doc.selectFirst("p.writer a, .book-info a.writer, a.name, .author")?.text()?.trim() ?: ""
        val description = doc.selectFirst("p.intro, .book-desc, .book-intro, .description")?.text()?.trim() ?: ""
        val coverUrl = doc.selectFirst(".book-img img, .book-information img, .cover img")?.attr("src") ?: ""
        val latestChapter = doc.selectFirst(".latest-chapter a, p.update a, .update")?.text()?.trim() ?: ""

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
        val chapterSelectors = listOf(
            "ul.volume li a",
            ".chapter-list li a",
            ".catalog-content a",
            "a[href*=chapter]",
            ".list-chapter a"
        )

        for (selector in chapterSelectors) {
            val chapterElements = doc.select(selector)
            if (chapterElements.isNotEmpty()) {
                chapterElements.forEachIndexed { index, element ->
                    val chapterTitle = element.text().trim()
                    val chapterUrl = element.attr("href")

                    if (chapterTitle.isNotEmpty() && !chapterTitle.contains("vip")) {
                        val fullUrl = when {
                            chapterUrl.startsWith("http") -> chapterUrl
                            chapterUrl.startsWith("//") -> "https:$chapterUrl"
                            chapterUrl.startsWith("/") -> "https://www.qidian.com$chapterUrl"
                            else -> "https://www.qidian.com/$chapterUrl"
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
                break
            }
        }

        return Pair(book, chapters)
    }

    /**
     * 获取章节正文内容
     */
    override suspend fun getChapterContent(chapterUrl: String): String {
        val doc = fetchDocument(chapterUrl)

        val contentSelectors = listOf(
            ".read-content",
            ".chapter-content",
            "#chapterContent",
            "#BookText",
            ".text-content",
            "#content",
            ".content",
            "article"
        )

        for (selector in contentSelectors) {
            val contentElement = doc.selectFirst(selector)
            if (contentElement != null) {
                val paragraphs = contentElement.select("p")
                if (paragraphs.isNotEmpty()) {
                    val text = paragraphs.map { it.text().trim() }
                        .filter { it.isNotEmpty() }
                        .joinToString("\n\n")
                    if (text.isNotEmpty()) {
                        return text
                    }
                }

                val rawContent = contentElement.html()
                val cleaned = cleanContent(rawContent)
                if (cleaned.isNotEmpty()) {
                    return cleaned
                }
            }
        }

        return "章节内容加载失败"
    }
}
