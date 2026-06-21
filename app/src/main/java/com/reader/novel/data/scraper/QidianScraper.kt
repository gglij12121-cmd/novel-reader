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
 *
 * 页面结构：
 * - 搜索: /search?kw=xxx
 * - 详情: /book/xxx/
 * - 章节: /book/xxx/xxx.html
 */
class QidianScraper : BaseScraper() {

    override val sourceName: String = Constants.QIDIAN_NAME
    override val sourceId: String = Constants.SOURCE_QIDIAN

    /**
     * 搜索小说
     *
     * 起点搜索页面结构 (可能需要JS渲染)：
     * <div class="book-img-text">
     *   <ul>
     *     <li>
     *       <div class="book-mid-info">
     *         <h4><a href="详情URL">书名</a></h4>
     *         <p class="author">作者</p>
     *         <p class="intro">简介</p>
     *       </div>
     *     </li>
     *   </ul>
     * </div>
     */
    override suspend fun search(keyword: String): SearchResult {
        return try {
            val url = "${Constants.QIDIAN_SEARCH_URL}?kw=${java.net.URLEncoder.encode(keyword, "UTF-8")}"
            val doc = fetchDocument(url)

            val books = doc.select("li.book-mid-info, .res-book-item, .book-img-text li").mapNotNull { element ->
                try {
                    val titleElement = element.selectFirst("h4 a, .book-mid-info h2 a, a.name") ?: return@mapNotNull null
                    val title = titleElement.text().trim()
                    val detailUrl = titleElement.attr("href")

                    if (title.isEmpty()) return@mapNotNull null

                    // 补全URL
                    val fullUrl = if (detailUrl.startsWith("http")) {
                        detailUrl
                    } else {
                        "https:$detailUrl"
                    }

                    val author = element.selectFirst("p.author a, .author a.name, a.writer")?.text()?.trim() ?: ""
                    val description = element.selectFirst("p.intro, .book-desc, .intro")?.text()?.trim() ?: ""
                    val latestChapter = element.selectFirst("p.update a, .latest-chapter a")?.text()?.trim() ?: ""

                    // 封面图可能在父元素中
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
     *
     * 起点详情页结构：
     * <div class="book-information">
     *   <h1>书名</h1>
     *   <p class="writer">作者</p>
     *   <p class="intro">简介</p>
     * </div>
     * <div class="volume-wrap">
     *   <div class="volume">
     *     <h3>卷名</h3>
     *     <ul>
     *       <li><a href="章节URL">章节标题</a></li>
     *     </ul>
     *   </div>
     * </div>
     */
    override suspend fun getBookDetail(bookUrl: String): Pair<Book, List<Chapter>> {
        val doc = fetchDocument(bookUrl)

        // 解析书籍信息
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

        // 解析章节列表
        val chapters = mutableListOf<Chapter>()
        val chapterElements = doc.select("ul.volume li a, .chapter-list li a, .catalog-content a")

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
     *
     * 起点章节页结构：
     * <div class="read-content">
     *   <p>段落1</p>
     *   <p>段落2</p>
     * </div>
     *
     * 注意：
     * - 起点的章节内容可能需要登录才能查看
     * - 免费章节可以直接查看
     * - VIP章节需要登录并订阅
     */
    override suspend fun getChapterContent(chapterUrl: String): String {
        val doc = fetchDocument(chapterUrl)

        // 获取正文内容
        val contentElement = doc.selectFirst(
            ".read-content, .chapter-content, #chapterContent, #BookText"
        ) ?: return ""

        // 起点的段落是分开的<p>标签
        val paragraphs = contentElement.select("p")
        if (paragraphs.isNotEmpty()) {
            return paragraphs.map { it.text().trim() }
                .filter { it.isNotEmpty() }
                .joinToString("\n\n")
        }

        // 如果没有<p>标签，直接获取文本
        val rawContent = contentElement.html()
        return cleanContent(rawContent)
    }
}
