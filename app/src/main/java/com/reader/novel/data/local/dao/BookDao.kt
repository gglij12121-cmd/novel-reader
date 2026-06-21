package com.reader.novel.data.local.dao

import androidx.room.*
import com.reader.novel.data.local.entity.BookEntity
import kotlinx.coroutines.flow.Flow

/**
 * 书籍数据访问对象 (DAO)
 * 提供对books表的增删改查操作
 *
 * 使用Flow实现数据变化的自动通知
 */
@Dao
interface BookDao {

    /**
     * 插入一本书
     * 如果已存在相同sourceUrl的书，则替换
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: BookEntity): Long

    /**
     * 更新书籍信息
     */
    @Update
    suspend fun updateBook(book: BookEntity)

    /**
     * 删除一本书
     */
    @Delete
    suspend fun deleteBook(book: BookEntity)

    /**
     * 根据ID删除书籍
     */
    @Query("DELETE FROM books WHERE id = :bookId")
    suspend fun deleteBookById(bookId: Long)

    /**
     * 获取书架上的所有书籍
     * 按最后阅读时间降序排列
     * 返回Flow，数据变化时自动通知
     */
    @Query("SELECT * FROM books WHERE isOnBookshelf = 1 ORDER BY lastReadTime DESC")
    fun getBookshelfBooks(): Flow<List<BookEntity>>

    /**
     * 获取阅读历史
     * 按最后阅读时间降序排列
     */
    @Query("SELECT * FROM books ORDER BY lastReadTime DESC LIMIT :limit")
    fun getReadingHistory(limit: Int = 50): Flow<List<BookEntity>>

    /**
     * 根据ID获取书籍
     */
    @Query("SELECT * FROM books WHERE id = :bookId")
    suspend fun getBookById(bookId: Long): BookEntity?

    /**
     * 根据来源URL获取书籍
     * 用于判断是否已存在
     */
    @Query("SELECT * FROM books WHERE sourceUrl = :sourceUrl LIMIT 1")
    suspend fun getBookBySourceUrl(sourceUrl: String): BookEntity?

    /**
     * 检查书籍是否在书架上
     */
    @Query("SELECT EXISTS(SELECT 1 FROM books WHERE sourceUrl = :sourceUrl AND isOnBookshelf = 1)")
    suspend fun isOnBookshelf(sourceUrl: String): Boolean

    /**
     * 更新最后阅读时间
     */
    @Query("UPDATE books SET lastReadTime = :timestamp WHERE id = :bookId")
    suspend fun updateLastReadTime(bookId: Long, timestamp: Long = System.currentTimeMillis())

    /**
     * 将书籍添加到书架
     */
    @Query("UPDATE books SET isOnBookshelf = 1, addedToShelf = :timestamp WHERE id = :bookId")
    suspend fun addToBookshelf(bookId: Long, timestamp: Long = System.currentTimeMillis())

    /**
     * 将书籍从书架移除
     */
    @Query("UPDATE books SET isOnBookshelf = 0 WHERE id = :bookId")
    suspend fun removeFromBookshelf(bookId: Long)

    /**
     * 搜索书籍 (本地搜索)
     */
    @Query("SELECT * FROM books WHERE title LIKE '%' || :keyword || '%' OR author LIKE '%' || :keyword || '%'")
    fun searchBooks(keyword: String): Flow<List<BookEntity>>
}
