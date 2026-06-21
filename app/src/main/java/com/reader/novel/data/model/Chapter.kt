package com.reader.novel.data.model

/**
 * 章节数据模型
 * 表示小说的一个章节
 *
 * @property title 章节标题
 * @property url 章节链接
 * @property chapterIndex 章节序号 (从0开始)
 * @property content 章节正文内容 (可能为空，需要单独加载)
 * @property isCached 是否已缓存到本地
 */
data class Chapter(
    val title: String,
    val url: String,
    val chapterIndex: Int = 0,
    val content: String = "",
    val isCached: Boolean = false
)
