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
 *
 * 页面结构：
 * - 搜索: /search.php?keyword=xxx
 * - 详情: /book/xxx/
 * - 章节: /book/xxx/xxx.html
 */
class BayiScraper : BaseScraper() {

    override val sourceName: String = Constants.BAYI_NAME
    override val sourceId: String = Constants.SOURCE_BAYI

    /**
     * 搜索小说
     *
     * 八一中文网搜索页面结构：
     * <div class="novelslist">
     *   <li>
     *     <span class="s2"><a href="详情URL">书名</a></span>
     *     <span class="s4">作者</span>
     *     <span class="s6">最新章节</span>
     *   </li>
     * </div>
     */
    override suspend fun search(keyword: String): SearchResult {
        return try {
            val url = "${Constants.BAYI_SEARCH_URL}?keyword=${java.net.URLEncoder.encode(keyword, "UTF-8")}"
            val doc = fetchDocument(url)

            val books = doc.select("div.novelslist li, table.tb li").mapNotNull { element ->
                try {
                    val titleElement = element.selectFirst("a") ?: return@mapNotNull null
                    val title = titleElement.text().trim()
                    val detailUrl = titleElement.attr("href")

                    // 跳过表头
                    if (title == "书名" || title.isEmpty()) return@mapNotNull null

                    // 补全URL
                    val fullUrl = if (detailUrl.startsWith("http")) {
                        detailUrl
                    } else if (detailUrl.startsWith("/")) {
                        "${Constants.BAYI_BASE_URL}$detailUrl"
                    } else {
                        "${Constants.BAYI_BASE_URL}/$detailUrl"
                    }

                    val author = element.select("span").getOrNull(1)?.text()?.trim() ?: ""
                    val latestChapter = element.select("span").getOrNull(3)?.text()?.trim() ?: ""

                    Book(
                        title = title,
                        author = author,
                        source = sourceId,
                        sourceUrl = fullUrl,
                        latestChapter = latestChapter
                    )
                } catch (e: Exception) {
                    null
                }
            }

            // 如果上面的选择器没有结果，尝试另一种结构
            if (books.isEmpty()) {
                val altBooks = doc.select("div.booklist li, .search-item").mapNotNull { element ->
                    try {
                        val titleElement = element.selectFirst("a") ?: return@mapNotNull null
                        val title = titleElement.text().trim()
                        val detailUrl = titleElement.attr("href")

                        if (title.isEmpty()) return@mapNotNull null

                        val fullUrl = if (detailUrl.startsWith("http")) {
                            detailUrl
                        } else {
                            "${Constants.BAYI_BASE_URL}$detailUrl"
                        }

                        val author = element.selectFirst(".author, .s4")?.text()?.trim() ?: ""

                        Book(
                            title = title,
                            author = author,
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
     *
     * 详情页结构：
     * <div id="info">
     *   <h1>书名</h1>
     *   <p>作者：xxx</p>
     * </div>
     * <div id="intro">简介</div>
     * <div id="list">
     *   <dl>
     *     <dd><a href="章节URL">章节标题</a></dd>
     *   </dl>
     * </div>
     */
    override suspend fun getBookDetail(bookUrl: String): Pair<Book, List<Chapter>> {
        val doc = fetchDocument(bookUrl)

        // 解析书籍信息
        val title = doc.selectFirst("#info h1, .booktitle h1")?.text()?.trim() ?: ""
        val author = doc.select("#info p, .booktitle p").firstOrNull()?.text()
            ?.replace("作者：", "")?.replace("作者:", "")?.trim() ?: ""
        val description = doc.selectFirst("#intro, .bookintro")?.text()?.trim() ?: ""
        val coverUrl = doc.selectFirst("#fmimg img, .bookimg img")?.attr("src") ?: ""
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
        val chapterElements = doc.select("#list dl dd a, .booklist a")

        chapterElements.forEachIndexed { index, element ->
            val chapterTitle = element.text().trim()
            val chapterUrl = element.attr("href")

            if (chapterTitle.isNotEmpty()) {
                val fullUrl = if (chapterUrl.startsWith("http")) {
                    chapterUrl
                } else {
                    "${Constants.BAYI_BASE_URL}$chapterUrl"
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
     *
     * 章节页结构：
     * <div id="content">
     *   正文内容...
     * </div>
     */
    override suspend fun getChapterContent(chapterUrl: String): String {
        val doc = fetchDocument(chapterUrl)

        // 获取正文内容
        val contentElement = doc.selectFirst("#content, .content, #booktext") ?: return ""

        // 获取HTML内容并清理
        val rawContent = contentElement.html()

        // 清理广告和无用内容
        return cleanContent(rawContent)
    }
}
