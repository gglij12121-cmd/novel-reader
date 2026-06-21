package com.reader.novel.data.local.dao

import androidx.room.*
import com.reader.novel.data.local.entity.ChapterEntity
import kotlinx.coroutines.flow.Flow

/**
 * 章节数据访问对象 (DAO)
 * 提供对chapters表的增删改查操作
 */
@Dao
interface ChapterDao {

    /**
     * 插入单个章节
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapter(chapter: ChapterEntity): Long

    /**
     * 批量插入章节
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapters(chapters: List<ChapterEntity>)

    /**
     * 更新章节内容
     */
    @Update
    suspend fun updateChapter(chapter: ChapterEntity)

    /**
     * 删除某本书的所有章节
     */
    @Query("DELETE FROM chapters WHERE bookId = :bookId")
    suspend fun deleteChaptersByBookId(bookId: Long)

    /**
     * 获取某本书的所有章节列表
     * 按章节序号升序排列
     */
    @Query("SELECT * FROM chapters WHERE bookId = :bookId ORDER BY chapterIndex ASC")
    fun getChaptersByBookId(bookId: Long): Flow<List<ChapterEntity>>

    /**
     * 获取某本书的所有章节列表 (非Flow版本)
     */
    @Query("SELECT * FROM chapters WHERE bookId = :bookId ORDER BY chapterIndex ASC")
    suspend fun getChapterList(bookId: Long): List<ChapterEntity>

    /**
     * 获取单个章节
     */
    @Query("SELECT * FROM chapters WHERE bookId = :bookId AND chapterIndex = :chapterIndex")
    suspend fun getChapter(bookId: Long, chapterIndex: Int): ChapterEntity?

    /**
     * 获取已缓存的章节数量
     */
    @Query("SELECT COUNT(*) FROM chapters WHERE bookId = :bookId AND isCached = 1")
    suspend fun getCachedChapterCount(bookId: Long): Int

    /**
     * 获取总章节数
     */
    @Query("SELECT COUNT(*) FROM chapters WHERE bookId = :bookId")
    suspend fun getTotalChapterCount(bookId: Long): Int

    /**
     * 更新章节缓存状态和内容
     */
    @Query("UPDATE chapters SET content = :content, isCached = 1, cacheTime = :cacheTime WHERE id = :chapterId")
    suspend fun updateChapterContent(chapterId: Long, content: String, cacheTime: Long = System.currentTimeMillis())
}
