package com.reader.novel.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 章节实体类 - Room数据库表
 * 用于存储章节内容到本地数据库 (离线缓存)
 *
 * @property id 主键，自动生成
 * @property bookId 关联的书籍ID (外键)
 * @property chapterIndex 章节序号
 * @property title 章节标题
 * @property url 章节链接
 * @property content 章节正文内容
 * @property isCached 是否已缓存
 * @property cacheTime 缓存时间戳
 */
@Entity(
    tableName = "chapters",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE  // 删除书籍时同步删除章节
        )
    ],
    indices = [Index(value = ["bookId"])]  // 为bookId创建索引，加速查询
)
data class ChapterEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val bookId: Long,
    val chapterIndex: Int,
    val title: String,
    val url: String,
    val content: String = "",
    val isCached: Boolean = false,
    val cacheTime: Long = System.currentTimeMillis()
)
