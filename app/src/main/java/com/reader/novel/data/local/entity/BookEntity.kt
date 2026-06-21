package com.reader.novel.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 书籍实体类 - Room数据库表
 * 用于存储书籍信息到本地数据库
 *
 * 主要用途：
 * 1. 书架上的书籍
 * 2. 阅读历史记录
 *
 * @property id 主键，自动生成
 * @property title 书名
 * @property author 作者
 * @property coverUrl 封面图片URL
 * @property description 简介
 * @property source 来源网站标识 (biquge/bayi/qidian)
 * @property sourceUrl 来源网站上的书籍链接
 * @property latestChapter 最新章节名称
 * @property category 分类
 * @property status 连载状态
 * @property addedToShelf 加入书架的时间戳
 * @property lastReadTime 最后阅读时间戳
 * @property isOnBookshelf 是否在书架上
 */
@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val author: String,
    val coverUrl: String = "",
    val description: String = "",
    val source: String = "",
    val sourceUrl: String = "",
    val latestChapter: String = "",
    val category: String = "",
    val status: String = "",
    val addedToShelf: Long = System.currentTimeMillis(),
    val lastReadTime: Long = System.currentTimeMillis(),
    val isOnBookshelf: Boolean = true
)
