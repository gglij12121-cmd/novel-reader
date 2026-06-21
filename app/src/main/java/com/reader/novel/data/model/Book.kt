package com.reader.novel.data.model

/**
 * 书籍数据模型
 * 用于在UI层和数据层之间传递书籍信息
 *
 * @property title 书名
 * @property author 作者
 * @property coverUrl 封面图片URL
 * @property description 简介/描述
 * @property source 来源网站 (biquge/bayi/qidian)
 * @property sourceUrl 来源网站上的书籍链接
 * @property latestChapter 最新章节名称
 * @property category 分类/类型
 * @property wordCount 字数
 * @property status 连载状态 (连载中/已完结)
 */
data class Book(
    val title: String,
    val author: String,
    val coverUrl: String = "",
    val description: String = "",
    val source: String = "",
    val sourceUrl: String = "",
    val latestChapter: String = "",
    val category: String = "",
    val wordCount: String = "",
    val status: String = ""
)
