package com.reader.novel.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 阅读进度实体类 - Room数据库表
 * 记录用户在每本书上的阅读进度
 *
 * @property id 主键，自动生成
 * @property bookId 关联的书籍ID (外键)
 * @property chapterIndex 当前阅读的章节序号
 * @property paragraphIndex 当前阅读的段落索引
 * @property scrollPosition 滚动位置 (用于滚动模式)
 * @property progressPercent 阅读进度百分比 (0-100)
 * @property updateTime 更新时间戳
 */
@Entity(
    tableName = "read_progress",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["bookId"], unique = true)]  // 每本书只有一个阅读进度
)
data class ReadProgress(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val bookId: Long,
    val chapterIndex: Int = 0,
    val paragraphIndex: Int = 0,
    val scrollPosition: Int = 0,
    val progressPercent: Float = 0f,
    val updateTime: Long = System.currentTimeMillis()
)
