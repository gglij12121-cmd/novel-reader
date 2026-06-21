package com.reader.novel.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.reader.novel.data.local.dao.BookDao
import com.reader.novel.data.local.dao.ChapterDao
import com.reader.novel.data.local.dao.ReadProgressDao
import com.reader.novel.data.local.entity.BookEntity
import com.reader.novel.data.local.entity.ChapterEntity
import com.reader.novel.data.local.entity.ReadProgress

/**
 * Room数据库类
 * 定义数据库的表结构和版本
 *
 * 包含三张表：
 * - books: 书籍信息
 * - chapters: 章节内容 (离线缓存)
 * - read_progress: 阅读进度
 */
@Database(
    entities = [
        BookEntity::class,
        ChapterEntity::class,
        ReadProgress::class
    ],
    version = 1,
    exportSchema = false  // 不导出数据库schema
)
abstract class AppDatabase : RoomDatabase() {

    /** 书籍DAO */
    abstract fun bookDao(): BookDao

    /** 章节DAO */
    abstract fun chapterDao(): ChapterDao

    /** 阅读进度DAO */
    abstract fun readProgressDao(): ReadProgressDao
}
