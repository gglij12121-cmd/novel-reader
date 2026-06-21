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
 *
 * 页面结构：
 * - 搜索: /search.php?keyword=xxx
 * - 详情: /book/xxx/
 * - 章节: /book/xxx/xxx.html
 */
class BiqugeScraper : BaseScraper() {

    override val sourceName: String = Constants.BIQUGE_NAME
    override val sourceId: String = Constants.SOURCE_BIQUGE

    /**
     * 搜索小说
     *
     * 笔趣阁搜索页面结构：
     * <div class="result-item">
     *   <div class="result-game-item-pic">
     *     <img src="封面URL" />
     *   </div>
     *   <div class="result-game-item-detail">
     *     <h3><a href="详情URL">书名</a></h3>
     *     <p class="result-game-item-info-tag"><span>作者</span></p>
     *     <p class="result-game-item-desc">简介</p>
     *   </div>
     * </div>
     */
    override suspend fun search(keyword: String): SearchResult {
        return try {
            val url = "${Constants.BIQUGE_SEARCH_URL}?keyword=${java.net.URLEncoder.encode(keyword, "UTF-8")}"
            val doc = fetchDocument(url)

            val books = doc.select("div.result-item").mapNotNull { element ->
                try {
                    val titleElement = element.selectFirst("h3 a") ?: return@mapNotNull null
                    val title = titleElement.text().trim()
                    val detailUrl = titleElement.attr("href")

                    // 补全URL
                    val fullUrl = if (detailUrl.startsWith("http")) {
                        detailUrl
                    } else {
                        "${Constants.BIQUGE_BASE_URL}$detailUrl"
                    }

                    val coverUrl = element.selectFirst("img")?.attr("src") ?: ""
                    val author = element.selectFirst(".result-game-item-info-tag span")?.text()?.trim() ?: ""
                    val description = element.selectFirst(".result-game-item-desc")?.text()?.trim() ?: ""

                    Book(
                        title = title,
                        author = author,
                        coverUrl = coverUrl,
                        description = description,
                        source = sourceId,
                        sourceUrl = fullUrl
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
     *   <p>最新章节：xxx</p>
     * </div>
     * <div id="intro">简介</div>
     * <div id="list">
     *   <dl>
     *     <dt>最新章节</dt>
     *     <dd><a href="章节URL">章节标题</a></dd>
     *     ...
     *   </dl>
     * </div>
     */
    override suspend fun getBookDetail(bookUrl: String): Pair<Book, List<Chapter>> {
        val doc = fetchDocument(bookUrl)

        // 解析书籍信息
        val title = doc.selectFirst("#info h1")?.text()?.trim() ?: ""
        val author = doc.select("#info p").firstOrNull()?.text()
            ?.replace("作者：", "")?.replace("作者:", "")?.trim() ?: ""
        val latestChapter = doc.select("#info p").lastOrNull()?.text()
            ?.replace("最新章节：", "")?.replace("最新章节:", "")?.trim() ?: ""
        val description = doc.selectFirst("#intro")?.text()?.trim() ?: ""
        val coverUrl = doc.selectFirst("#fmimg img")?.attr("src") ?: ""

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
        val chapterElements = doc.select("#list dl dd a")

        chapterElements.forEachIndexed { index, element ->
            val chapterTitle = element.text().trim()
            val chapterUrl = element.attr("href")

            // 补全URL
            val fullUrl = if (chapterUrl.startsWith("http")) {
                chapterUrl
            } else {
                "${Constants.BIQUGE_BASE_URL}$chapterUrl"
            }

            chapters.add(
                Chapter(
                    title = chapterTitle,
                    url = fullUrl,
                    chapterIndex = index
                )
            )
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
        val contentElement = doc.selectFirst("#content") ?: return ""

        // 获取HTML内容并清理
        val rawContent = contentElement.html()

        // 清理广告和无用内容
        return cleanContent(rawContent)
    }
}
