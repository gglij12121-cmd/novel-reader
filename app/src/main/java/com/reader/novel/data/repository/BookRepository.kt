package com.reader.novel.data.repository

import com.reader.novel.data.local.dao.BookDao
import com.reader.novel.data.local.dao.ChapterDao
import com.reader.novel.data.local.dao.ReadProgressDao
import com.reader.novel.data.local.entity.BookEntity
import com.reader.novel.data.local.entity.ChapterEntity
import com.reader.novel.data.local.entity.ReadProgress
import com.reader.novel.data.model.Book
import com.reader.novel.data.model.Chapter
import com.reader.novel.data.scraper.ScraperManager
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 书籍数据仓库
 * 整合本地数据库和网络爬虫数据
 *
 * 职责：
 * 1. 提供书籍数据的统一访问接口
 * 2. 协调本地缓存和网络请求
 * 3. 处理数据转换 (Entity <-> Model)
 */
@Singleton
class BookRepository @Inject constructor(
    private val bookDao: BookDao,
    private val chapterDao: ChapterDao,
    private val readProgressDao: ReadProgressDao,
    private val scraperManager: ScraperManager
) {
    // ==================== 书架相关 ====================

    /**
     * 获取书架上的书籍列表 (Flow)
     */
    fun getBookshelfBooks(): Flow<List<BookEntity>> {
        return bookDao.getBookshelfBooks()
    }

    /**
     * 将书籍添加到书架
     *
     * @param book 书籍模型
     * @return 书籍在数据库中的ID
     */
    suspend fun addToBookshelf(book: Book): Long {
        // 先检查是否已存在
        val existing = bookDao.getBookBySourceUrl(book.sourceUrl)
        if (existing != null) {
            // 已存在，更新书架状态
            bookDao.addToBookshelf(existing.id)
            return existing.id
        }

        // 不存在，插入新记录
        val entity = BookEntity(
            title = book.title,
            author = book.author,
            coverUrl = book.coverUrl,
            description = book.description,
            source = book.source,
            sourceUrl = book.sourceUrl,
            latestChapter = book.latestChapter,
            category = book.category,
            status = book.status
        )
        return bookDao.insertBook(entity)
    }

    /**
     * 将书籍从书架移除
     */
    suspend fun removeFromBookshelf(bookId: Long) {
        bookDao.removeFromBookshelf(bookId)
    }

    /**
     * 删除书籍及其所有数据
     */
    suspend fun deleteBook(bookId: Long) {
        readProgressDao.deleteProgress(bookId)
        chapterDao.deleteChaptersByBookId(bookId)
        bookDao.deleteBookById(bookId)
    }

    /**
     * 检查书籍是否在书架上
     */
    suspend fun isOnBookshelf(sourceUrl: String): Boolean {
        return bookDao.isOnBookshelf(sourceUrl)
    }

    /**
     * 获取书籍详情
     */
    suspend fun getBookById(bookId: Long): BookEntity? {
        return bookDao.getBookById(bookId)
    }

    // ==================== 章节相关 ====================

    /**
     * 获取章节列表
     * 优先从本地缓存获取，如果没有则从网络获取
     *
     * @param bookId 书籍ID
     * @param bookUrl 书籍详情页URL (用于网络请求)
     * @param source 来源标识
     * @return 章节列表
     */
    suspend fun getChapters(bookId: Long, bookUrl: String, source: String): List<Chapter> {
        // 先检查本地是否有缓存
        val cachedChapters = chapterDao.getChapterList(bookId)
        if (cachedChapters.isNotEmpty()) {
            return cachedChapters.map { entity ->
                Chapter(
                    title = entity.title,
                    url = entity.url,
                    chapterIndex = entity.chapterIndex,
                    content = entity.content,
                    isCached = entity.isCached
                )
            }
        }

        // 本地没有，从网络获取
        val scraper = scraperManager.getScraper(source) ?: return emptyList()
        val (_, chapters) = scraper.getBookDetail(bookUrl)

        // 保存章节列表到本地
        val entities = chapters.map { chapter ->
            ChapterEntity(
                bookId = bookId,
                chapterIndex = chapter.chapterIndex,
                title = chapter.title,
                url = chapter.url
            )
        }
        chapterDao.insertChapters(entities)

        return chapters
    }

    /**
     * 获取章节正文
     * 优先从本地缓存获取，如果没有则从网络获取并缓存
     *
     * @param bookId 书籍ID
     * @param chapterIndex 章节序号
     * @param chapterUrl 章节URL
     * @param source 来源标识
     * @return 章节正文
     */
    suspend fun getChapterContent(
        bookId: Long,
        chapterIndex: Int,
        chapterUrl: String,
        source: String
    ): String {
        // 先检查本地缓存
        val cachedChapter = chapterDao.getChapter(bookId, chapterIndex)
        if (cachedChapter != null && cachedChapter.isCached) {
            return cachedChapter.content
        }

        // 本地没有，从网络获取
        val scraper = scraperManager.getScraper(source) ?: return "无法获取章节内容"
        val content = scraper.getChapterContent(chapterUrl)

        // 保存到本地缓存
        if (cachedChapter != null) {
            chapterDao.updateChapterContent(cachedChapter.id, content)
        } else {
            chapterDao.insertChapter(
                ChapterEntity(
                    bookId = bookId,
                    chapterIndex = chapterIndex,
                    title = "",
                    url = chapterUrl,
                    content = content,
                    isCached = true
                )
            )
        }

        return content
    }

    /**
     * 缓存整本书
     *
     * @param bookId 书籍ID
     * @param bookUrl 书籍详情页URL
     * @param source 来源标识
     * @param onProgress 进度回调 (当前章节数, 总章节数)
     */
    suspend fun cacheBook(
        bookId: Long,
        bookUrl: String,
        source: String,
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ) {
        val scraper = scraperManager.getScraper(source) ?: return
        val (_, chapters) = scraper.getBookDetail(bookUrl)

        chapters.forEachIndexed { index, chapter ->
            try {
                val content = scraper.getChapterContent(chapter.url)
                chapterDao.insertChapter(
                    ChapterEntity(
                        bookId = bookId,
                        chapterIndex = chapter.chapterIndex,
                        title = chapter.title,
                        url = chapter.url,
                        content = content,
                        isCached = true
                    )
                )
                onProgress(index + 1, chapters.size)
            } catch (e: Exception) {
                // 单个章节失败不影响其他章节
                e.printStackTrace()
            }
        }
    }

    // ==================== 阅读进度相关 ====================

    /**
     * 获取阅读进度
     */
    suspend fun getReadProgress(bookId: Long): ReadProgress? {
        return readProgressDao.getProgress(bookId)
    }

    /**
     * 保存阅读进度
     */
    suspend fun saveReadProgress(
        bookId: Long,
        chapterIndex: Int,
        paragraphIndex: Int = 0,
        scrollPosition: Int = 0,
        progressPercent: Float = 0f
    ) {
        val progress = ReadProgress(
            bookId = bookId,
            chapterIndex = chapterIndex,
            paragraphIndex = paragraphIndex,
            scrollPosition = scrollPosition,
            progressPercent = progressPercent
        )
        readProgressDao.insertOrUpdateProgress(progress)

        // 同时更新最后阅读时间
        bookDao.updateLastReadTime(bookId)
    }

    // ==================== 阅读历史 ====================

    /**
     * 获取阅读历史
     */
    fun getReadingHistory(limit: Int = 50): Flow<List<BookEntity>> {
        return bookDao.getReadingHistory(limit)
    }
}
