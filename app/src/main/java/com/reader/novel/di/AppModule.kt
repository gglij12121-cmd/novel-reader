package com.reader.novel.di

import android.content.Context
import androidx.room.Room
import com.reader.novel.data.local.AppDatabase
import com.reader.novel.data.local.dao.BookDao
import com.reader.novel.data.local.dao.ChapterDao
import com.reader.novel.data.local.dao.ReadProgressDao
import com.reader.novel.data.scraper.ScraperManager
import com.reader.novel.util.Constants
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt依赖注入模块
 *
 * 使用Hilt管理依赖关系：
 * - 数据库实例
 * - DAO接口
 * - 爬虫管理器
 *
 * SingletonComponent: 整个App生命周期内只有一个实例
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /**
     * 提供Room数据库实例
     *
     * 使用单例模式，整个App只有一个数据库实例
     */
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            Constants.DATABASE_NAME
        )
            .fallbackToDestructiveMigration()  // 版本升级时重建数据库
            .build()
    }

    /**
     * 提供书籍DAO
     */
    @Provides
    @Singleton
    fun provideBookDao(database: AppDatabase): BookDao {
        return database.bookDao()
    }

    /**
     * 提供章节DAO
     */
    @Provides
    @Singleton
    fun provideChapterDao(database: AppDatabase): ChapterDao {
        return database.chapterDao()
    }

    /**
     * 提供阅读进度DAO
     */
    @Provides
    @Singleton
    fun provideReadProgressDao(database: AppDatabase): ReadProgressDao {
        return database.readProgressDao()
    }

    /**
     * 提供爬虫管理器
     */
    @Provides
    @Singleton
    fun provideScraperManager(): ScraperManager {
        return ScraperManager()
    }
}
