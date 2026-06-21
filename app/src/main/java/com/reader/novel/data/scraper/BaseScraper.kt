package com.reader.novel.data.scraper

import com.reader.novel.data.model.Book
import com.reader.novel.data.model.Chapter
import com.reader.novel.data.model.SearchResult
import com.reader.novel.util.Constants
import kotlinx.coroutines.delay
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

/**
 * 爬虫基类
 * 所有网站爬虫都继承此类
 *
 * 提供通用的：
 * - 网页请求方法
 * - HTML解析工具
 * - 请求限流 (避免被封)
 *
 * 子类需要实现：
 * - search(): 搜索小说
 * - getBookDetail(): 获取小说详情和目录
 * - getChapterContent(): 获取章节正文
 */
abstract class BaseScraper {

    /** 来源网站名称 */
    abstract val sourceName: String

    /** 来源网站标识 */
    abstract val sourceId: String

    /** 上次请求时间 - 用于限流 */
    private var lastRequestTime = 0L

    /**
     * 搜索小说
     *
     * @param keyword 搜索关键词
     * @return 搜索结果
     */
    abstract suspend fun search(keyword: String): SearchResult

    /**
     * 获取小说详情和章节列表
     *
     * @param bookUrl 小说详情页URL
     * @return Pair<书籍信息, 章节列表>
     */
    abstract suspend fun getBookDetail(bookUrl: String): Pair<Book, List<Chapter>>

    /**
     * 获取章节正文内容
     *
     * @param chapterUrl 章节页面URL
     * @return 章节正文文本
     */
    abstract suspend fun getChapterContent(chapterUrl: String): String

    /**
     * 请求网页并解析为Document
     * 包含限流逻辑，避免请求过快被封
     *
     * @param url 网页URL
     * @return Jsoup Document对象
     * @throws Exception 网络请求失败时抛出异常
     */
    protected suspend fun fetchDocument(url: String): Document {
        // 限流：确保两次请求之间有足够间隔
        val currentTime = System.currentTimeMillis()
        val timeSinceLastRequest = currentTime - lastRequestTime
        if (timeSinceLastRequest < Constants.REQUEST_DELAY) {
            delay(Constants.REQUEST_DELAY - timeSinceLastRequest)
        }

        // 发起请求
        lastRequestTime = System.currentTimeMillis()

        return Jsoup.connect(url)
            .userAgent(Constants.USER_AGENT)
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
            .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
            .timeout((Constants.CONNECT_TIMEOUT * 1000).toInt())
            .get()
    }

    /**
     * 安全地获取网页文本
     * 处理空值和空白字符
     *
     * @param element Jsoup Element
     * @param selector CSS选择器
     * @return 清理后的文本
     */
    protected fun safeText(element: org.jsoup.nodes.Element?, selector: String): String {
        return try {
            element?.selectFirst(selector)?.text()?.trim() ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * 安全地获取网页属性值
     *
     * @param element Jsoup Element
     * @param selector CSS选择器
     * @param attr 属性名
     * @return 属性值
     */
    protected fun safeAttr(element: org.jsoup.nodes.Element?, selector: String, attr: String): String {
        return try {
            element?.selectFirst(selector)?.attr(attr)?.trim() ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * 清理章节正文
     * 去除广告、多余空白等
     *
     * @param rawContent 原始正文
     * @return 清理后的正文
     */
    protected fun cleanContent(rawContent: String): String {
        return rawContent
            // 替换HTML标签
            .replace("<br>", "\n")
            .replace("<br/>", "\n")
            .replace("<br />", "\n")
            .replace("<p>", "")
            .replace("</p>", "\n")
            // 去除常见广告文本
            .replace(Regex("天才一秒记住.*?m\\..*?\\.com"), "")
            .replace(Regex("手机用户请浏览.*?阅读"), "")
            .replace(Regex("请记住本书首发域名.*?。"), "")
            .replace(Regex("本章未完.*?点击下一页继续阅读"), "")
            // 清理多余空白
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()
    }
}
