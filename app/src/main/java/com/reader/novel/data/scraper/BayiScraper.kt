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
     * 搜索小说 - 支持模糊搜索
     */
    override suspend fun search(keyword: String): SearchResult {
        return try {
            val results = mutableListOf<Book>()

            // 方式1：直接搜索 (使用 ?q= 参数)
            val url1 = "${Constants.BAYI_SEARCH_URL}?q=${java.net.URLEncoder.encode(keyword, "UTF-8")}"
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
                val url2 = "${Constants.BAYI_SEARCH_URL}?q=${java.net.URLEncoder.encode(shortKeyword, "UTF-8")}"
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
     * 81zw.cc 使用 dt/dd 结构:
     * <dt><a href="/book/34889/">链接</a></dt>
     * <dd><h3><a href="/book/34889/">书名</a></h3></dd>
     * <dd class="book_other">作者：<span>作者名</span></dd>
     * <dd class="book_other">最新章节：<a href="...">章节名</a></dd>
     */
    private fun parseSearchResults(doc: org.jsoup.nodes.Document): List<Book> {
        val books = mutableListOf<Book>()

        // 方式1: dt/dd 结构 (81zw.cc 等新站点)
        val dtElements = doc.select("dt")
        for (dt in dtElements) {
            try {
                val link = dt.selectFirst("a[href]") ?: continue
                val href = link.attr("href")
                if (href.isEmpty() || href == "#" || href == "/") continue

                // 获取相邻的 dd 元素
                val ddContainer = dt.parent() ?: dt
                val allDds = ddContainer.select("dd")

                // 书名: dd > h3 > a
                val titleLink = allDds.first()?.select("h3 a")?.first()
                val title = titleLink?.text()?.trim() ?: link.text().trim()
                if (title.isEmpty() || title.length < 2) continue

                // 作者: dd.book_other span
                val author = allDds.select("dd.book_other span").first()?.text()?.trim() ?: ""

                // 最新章节
                val latestChapter = allDds.select("dd.book_other").lastOrNull()
                    ?.select("a")?.first()?.text()?.trim() ?: ""

                val fullUrl = when {
                    href.startsWith("http") -> href
                    href.startsWith("//") -> "https:$href"
                    href.startsWith("/") -> "${Constants.BAYI_BASE_URL}$href"
                    else -> "${Constants.BAYI_BASE_URL}/$href"
                }

                books.add(
                    Book(
                        title = title,
                        author = author,
                        coverUrl = "",
                        description = "",
                        source = sourceId,
                        sourceUrl = fullUrl,
                        latestChapter = latestChapter
                    )
                )
            } catch (e: Exception) {
                continue
            }
        }

        // 方式2: 通用 li 选择器 (兜底)
        if (books.isEmpty()) {
            val selectors = listOf("li", "div.result-item", "div.book-item", "div.search-item")
            for (selector in selectors) {
                val elements = doc.select(selector)
                for (element in elements) {
                    try {
                        val link = element.selectFirst("a[href]") ?: continue
                        val title = link.text().trim().ifEmpty { link.attr("title") }
                        val href = link.attr("href")

                        if (title.isEmpty() || title.length < 2 || title == "书名" || title == "首页") continue
                        if (href.isEmpty() || href == "#" || href == "/") continue

                        val fullUrl = when {
                            href.startsWith("http") -> href
                            href.startsWith("//") -> "https:$href"
                            href.startsWith("/") -> "${Constants.BAYI_BASE_URL}$href"
                            else -> "${Constants.BAYI_BASE_URL}/$href"
                        }

                        val author = element.selectFirst(".author, .s4, .book-author, .writer")?.text()?.trim() ?: ""
                        val latestChapter = element.selectFirst(".new-chapter, .s6, .latest, .update")?.text()?.trim() ?: ""

                        books.add(
                            Book(
                                title = title,
                                author = author,
                                coverUrl = "",
                                description = "",
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
        }

        return books
    }

    /**
     * 获取小说详情和章节列表
     */
    override suspend fun getBookDetail(bookUrl: String): Pair<Book, List<Chapter>> {
        val doc = fetchDocument(bookUrl)

        val title = doc.selectFirst("h1, #info h1, .book-title, .book-name, .title")?.text()?.trim() ?: ""
        val author = doc.selectFirst("#info p, .book-author, .author, .writer")?.text()
            ?.replace("作者：", "")?.replace("作者:", "")?.trim() ?: ""
        val description = doc.selectFirst("#intro, .bookintro, .intro, .book-desc, .description")?.text()?.trim() ?: ""
        val coverUrl = doc.selectFirst("#fmimg img, .bookimg img, .cover img, .bookcover img")?.attr("src") ?: ""
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

        val chapters = mutableListOf<Chapter>()
        val chapterSelectors = listOf(
            "#list dl dd a",
            ".booklist a",
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
                            chapterUrl.startsWith("/") -> "${Constants.BAYI_BASE_URL}$chapterUrl"
                            else -> "${Constants.BAYI_BASE_URL}/$chapterUrl"
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
            "#content",
            ".content",
            "#booktext",
            ".chapter-content",
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
