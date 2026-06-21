package com.reader.novel.util

/**
 * 常量定义
 * 包含网站URL、请求配置等常量
 *
 * 注意：小说网站的域名会经常变化
 * 如果某个站点无法访问，需要更新对应的URL
 */
object Constants {

    // ==================== 网站URL配置 ====================

    /**
     * 笔趣阁 - 常用镜像站
     * 特点：HTML结构简单，反爬较弱
     */
    const val BIQUGE_BASE_URL = "https://www.bbiquge.cc"
    const val BIQUGE_SEARCH_URL = "$BIQUGE_BASE_URL/search.php"
    const val BIQUGE_NAME = "笔趣阁"

    /**
     * 八一中文网 - 常用镜像站
     * 特点：结构规整，内容丰富
     */
    const val BAYI_BASE_URL = "https://www.81zw.us"
    const val BAYI_SEARCH_URL = "$BAYI_BASE_URL/search.php"
    const val BAYI_NAME = "八一中文网"

    /**
     * 起点中文网
     * 特点：反爬较强，部分内容需要登录
     */
    const val QIDIAN_BASE_URL = "https://www.qidian.com"
    const val QIDIAN_SEARCH_URL = "$QIDIAN_BASE_URL/search"
    const val QIDIAN_NAME = "起点中文网"

    // ==================== 网络请求配置 ====================

    /** 请求超时时间 (秒) */
    const val CONNECT_TIMEOUT = 15L
    const val READ_TIMEOUT = 15L
    const val WRITE_TIMEOUT = 15L

    /** 请求间隔 (毫秒) - 避免被封IP */
    const val REQUEST_DELAY = 1000L

    /** User-Agent - 伪装成正常浏览器 */
    const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36"

    /** 最大重试次数 */
    const val MAX_RETRY_COUNT = 3

    // ==================== 数据库配置 ====================

    const val DATABASE_NAME = "novel_reader_db"
    const val DATABASE_VERSION = 1

    // ==================== 阅读器默认配置 ====================

    /** 默认字体大小 (sp) */
    const val DEFAULT_FONT_SIZE = 18f

    /** 最小字体大小 */
    const val MIN_FONT_SIZE = 12f

    /** 最大字体大小 */
    const val MAX_FONT_SIZE = 30f

    /** 默认行间距倍数 */
    const val DEFAULT_LINE_HEIGHT = 1.5f

    /** 默认背景颜色索引 */
    const val DEFAULT_BG_COLOR_INDEX = 0

    // ==================== 来源标识 ====================

    const val SOURCE_BIQUGE = "biquge"
    const val SOURCE_BAYI = "bayi"
    const val SOURCE_QIDIAN = "qidian"
}
