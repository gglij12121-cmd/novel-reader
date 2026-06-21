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
     * 搜索小说 - 支持模糊搜索
     */
    override suspend fun search(keyword: String): SearchResult {
        return try {
            // 尝试多种搜索方式
            val results = mutableListOf<Book>()

            // 方式1：直接搜索
            val url1 = "${Constants.BIQUGE_SEARCH_URL}?keyword=${java.net.URLEncoder.encode(keyword, "UTF-8")}"
            try {
                val doc1 = fetchDocument(url1)
                val books1 = parseSearchResults(doc1)
                results.addAll(books1)
            } catch (e: Exception) {
                // 忽略错误，尝试下一种方式
            }

            // 方式2：如果结果太少，尝试搜索部分关键词
            if (results.size < 3 && keyword.length > 2) {
                val shortKeyword = keyword.substring(0, keyword.length / 2)
                val url2 = "${Constants.BIQUGE_SEARCH_URL}?keyword=${java.net.URLEncoder.encode(shortKeyword, "UTF-8")}"
                try {
                    val doc2 = fetchDocument(url2)
                    val books2 = parseSearchResults(doc2)
                        .filter { book -> book.title.contains(keyword) || book.author.contains(keyword) }
                    results.addAll(books2)
                } catch (e: Exception) {
                    // 忽略错误
                }
            }

            // 去重
            val uniqueBooks = results.distinctBy { it.title + it.author }

            if (uniqueBooks.isEmpty()) {
                SearchResult(
                    books = emptyList(),
                    source = sourceId,
                    isSuccess = false,
                    errorMessage = "未找到相关小说，请尝试其他关键词"
                )
            } else {
                SearchResult(books = uniqueBooks, source = sourceId, isSuccess = true)
            }
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
     * 解析搜索结果
     */
    private fun parseSearchResults(doc: org.jsoup.nodes.Document): List<Book> {
        val books = mutableListOf<Book>()

        // 尝试多种选择器
        val selectors = listOf(
            "div.result-item",
            "li",
            "div.book-item",
            "table tr",
            "div.search-item",
            "ul li",
            "div.list-item"
        )

        for (selector in selectors) {
            val elements = doc.select(selector)
            for (element in elements) {
                try {
                    val link = element.selectFirst("a[href]") ?: continue
                    val title = link.text().trim().ifEmpty { link.attr("title") }
                    val href = link.attr("href")

                    // 过滤无效结果
                    if (title.isEmpty() || title.length < 2 || title == "书名" || title == "首页") continue
                    if (href.isEmpty() || href == "#" || href == "/") continue

                    val fullUrl = when {
                        href.startsWith("http") -> href
                        href.startsWith("//") -> "https:$href"
                        href.startsWith("/") -> "${Constants.BIQUGE_BASE_URL}$href"
                        else -> "${Constants.BIQUGE_BASE_URL}/$href"
                    }

                    val author = element.selectFirst(".author, .book-author, span.s4, .writer")?.text()?.trim() ?: ""
                    val description = element.selectFirst(".intro, .book-desc, .description, .summary")?.text()?.trim() ?: ""
                    val latestChapter = element.selectFirst(".latest, .new-chapter, span.s6, .update")?.text()?.trim() ?: ""
                    val coverUrl = element.selectFirst("img")?.attr("src") ?: ""

                    books.add(
                        Book(
                            title = title,
                            author = author,
                            coverUrl = coverUrl,
                            description = description,
                            source = sourceId,
                            sourceUrl = fullUrl,
                            latestChapter = latestChapter
                        )
                    )
                } catch (e: Exception) {
                    // 跳过解析失败的元素
                    continue
                }
            }

            // 如果找到了结果，就不再尝试其他选择器
            if (books.isNotEmpty()) break
        }

        return books
    }

    /**
     * 获取小说详情和章节列表
     */
    override suspend fun getBookDetail(bookUrl: String): Pair<Book, List<Chapter>> {
        val doc = fetchDocument(bookUrl)

        // 解析书籍信息
        val title = doc.selectFirst("h1, #info h1, .book-title, .book-name, .title")?.text()?.trim() ?: ""
        val author = doc.selectFirst("#info p, .book-author, .author, .writer")?.text()
            ?.replace("作者：", "")?.replace("作者:", "")?.trim() ?: ""
        val description = doc.selectFirst("#intro, .book-desc, .intro, .description, .summary")?.text()?.trim() ?: ""
        val coverUrl = doc.selectFirst("#fmimg img, .book-img img, .cover img, .bookcover img")?.attr("src") ?: ""
        val latestChapter = doc.select("#info p, .info p").lastOrNull()?.text()
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
        val chapterSelectors = listOf(
            "#list dl dd a",
            ".book-list a",
            ".chapter-list a",
            "a[href*=chapter]",
            "a[href*=.html]",
            ".list-chapter a"
        )

        for (selector in chapterSelectors) {
            val chapterElements = doc.select(selector)
            if (chapterElements.isNotEmpty()) {
                chapterElements.forEachIndexed { index, element ->
                    val chapterTitle = element.text().trim()
                    val chapterUrl = element.attr("href")

                    if (chapterTitle.isNotEmpty()) {
                        val fullUrl = when {
                            chapterUrl.startsWith("http") -> chapterUrl
                            chapterUrl.startsWith("//") -> "https:$chapterUrl"
                            chapterUrl.startsWith("/") -> "${Constants.BIQUGE_BASE_URL}$chapterUrl"
                            else -> "${Constants.BIQUGE_BASE_URL}/$chapterUrl"
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

        // 尝试多种选择器
        val contentSelectors = listOf(
            "#content",
            ".content",
            ".chapter-content",
            "#booktext",
            "#chaptercontent",
            ".text-content",
            ".read-content",
            "article"
        )

        for (selector in contentSelectors) {
            val contentElement = doc.selectFirst(selector)
            if (contentElement != null) {
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
