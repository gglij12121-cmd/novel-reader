package com.reader.novel.data.local.dao

import androidx.room.*
import com.reader.novel.data.local.entity.ReadProgress
import kotlinx.coroutines.flow.Flow

/**
 * 阅读进度数据访问对象 (DAO)
 * 提供对read_progress表的增删改查操作
 */
@Dao
interface ReadProgressDao {

    /**
     * 插入或更新阅读进度
     * 使用REPLACE策略，如果已存在则更新
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProgress(progress: ReadProgress): Long

    /**
     * 获取某本书的阅读进度
     */
    @Query("SELECT * FROM read_progress WHERE bookId = :bookId LIMIT 1")
    suspend fun getProgress(bookId: Long): ReadProgress?

    /**
     * 获取某本书的阅读进度 (Flow版本，自动通知变化)
     */
    @Query("SELECT * FROM read_progress WHERE bookId = :bookId LIMIT 1")
    fun getProgressFlow(bookId: Long): Flow<ReadProgress?>

    /**
     * 更新章节索引
     */
    @Query("UPDATE read_progress SET chapterIndex = :chapterIndex, updateTime = :updateTime WHERE bookId = :bookId")
    suspend fun updateChapterIndex(bookId: Long, chapterIndex: Int, updateTime: Long = System.currentTimeMillis())

    /**
     * 更新阅读进度百分比
     */
    @Query("UPDATE read_progress SET progressPercent = :percent, updateTime = :updateTime WHERE bookId = :bookId")
    suspend fun updateProgressPercent(bookId: Long, percent: Float, updateTime: Long = System.currentTimeMillis())

    /**
     * 删除某本书的阅读进度
     */
    @Query("DELETE FROM read_progress WHERE bookId = :bookId")
    suspend fun deleteProgress(bookId: Long)
}
