# 📚 小说阅读器 (NovelReader)

一个无广告的安卓小说阅读App，支持从多个小说网站爬取数据，提供舒适的阅读体验。

## ✨ 功能特性

### 核心功能
- 🔍 **多源搜索** - 同时搜索笔趣阁、八一中文网、起点中文网
- 📖 **舒适阅读** - 支持多种翻页效果和阅读设置
- 📚 **书架管理** - 收藏喜欢的小说，记录阅读进度
- 💾 **离线缓存** - 下载章节内容，支持离线阅读

### 阅读体验
- 🌙 **夜间模式** - 保护眼睛
- 🎨 **多种背景** - 白色、护眼绿、羊皮纸等7种背景
- 📝 **字体调节** - 自定义字体大小和行间距
- 📄 **翻页效果** - 左右滑动、上下滚动、仿真翻页

## 🛠️ 技术栈

| 技术 | 用途 |
|------|------|
| Kotlin | 开发语言 |
| Jetpack Compose | UI框架 |
| Room | 本地数据库 |
| Retrofit + OkHttp | 网络请求 |
| Jsoup | HTML解析 |
| Hilt | 依赖注入 |
| Coil | 图片加载 |
| Navigation Compose | 页面导航 |

## 📁 项目结构

```
app/src/main/java/com/reader/novel/
├── data/                          # 数据层
│   ├── local/                     # 本地数据库
│   │   ├── dao/                   # 数据访问对象
│   │   └── entity/                # 数据实体
│   ├── network/                   # 网络层
│   ├── scraper/                   # 爬虫模块 ⭐
│   ├── model/                     # 数据模型
│   └── repository/                # 数据仓库
├── ui/                            # UI层
│   ├── theme/                     # 主题配置
│   ├── navigation/                # 导航路由
│   ├── screen/                    # 页面
│   │   ├── home/                  # 首页(发现)
│   │   ├── search/                # 搜索
│   │   ├── detail/                # 小说详情
│   │   ├── reader/                # 阅读器 ⭐
│   │   ├── bookshelf/             # 书架
│   │   └── settings/              # 设置
│   └── components/                # 通用组件
├── di/                            # 依赖注入
└── util/                          # 工具类
```

## 🚀 如何使用

### 环境要求
- Android Studio Hedgehog (2023.1.1) 或更高版本
- JDK 17
- Android SDK 34

### 安装步骤

1. **克隆项目**
   ```bash
   git clone <项目地址>
   ```

2. **用Android Studio打开项目**
   - 打开Android Studio
   - 选择 "Open an existing project"
   - 选择项目文件夹

3. **等待Gradle同步**
   - Android Studio会自动下载依赖
   - 首次同步可能需要几分钟

4. **运行项目**
   - 连接安卓手机或启动模拟器
   - 点击运行按钮 ▶️

### 生成APK

1. **Debug版本**
   ```bash
   ./gradlew assembleDebug
   ```
   APK位置: `app/build/outputs/apk/debug/app-debug.apk`

2. **Release版本**
   ```bash
   ./gradlew assembleRelease
   ```
   需要先配置签名信息

## 📱 页面说明

### 首页 (发现页)
- 搜索框：输入书名或作者
- 搜索结果：显示来自各网站的小说
- 快捷导航：书架、设置

### 搜索页
- 多源搜索：同时从3个网站搜索
- 结果聚合：合并去重显示

### 详情页
- 书籍信息：封面、简介、作者
- 章节目录：所有章节列表
- 操作按钮：加入书架、开始阅读

### 阅读器
- 正文显示：支持长文本滚动
- 翻页控制：上一章/下一章
- 设置面板：字体、行距、背景
- 目录抽屉：快速跳转章节

### 书架
- 书籍列表：已收藏的小说
- 继续阅读：快速回到上次阅读位置
- 管理功能：移出书架、删除

### 设置
- 外观设置：深色模式
- 阅读设置：默认字体、行距
- 数据管理：清除缓存

## ⚠️ 注意事项

### 关于爬虫
1. **网站域名变化**：小说网站的域名会经常变化，如果某个站点无法访问，需要更新 `Constants.kt` 中的URL
2. **反爬措施**：部分网站可能有反爬机制，可能需要：
   - 调整请求间隔
   - 更新User-Agent
   - 添加Cookie
3. **内容版权**：爬取的内容仅供个人学习使用

### 关于法律
- 此App仅供个人学习使用
- 爬取的内容版权归原作者和网站所有
- 不建议用于商业用途或公开分发

## 🔧 自定义配置

### 修改网站URL
编辑 `app/src/main/java/com/reader/novel/util/Constants.kt`：

```kotlin
// 修改笔趣阁URL
const val BIQUGE_BASE_URL = "https://新的域名"

// 修改八一中文网URL
const val BAYI_BASE_URL = "https://新的域名"
```

### 修改请求配置
```kotlin
// 请求超时时间（秒）
const val CONNECT_TIMEOUT = 15L

// 请求间隔（毫秒）- 增大可降低被封风险
const val REQUEST_DELAY = 1000L
```

## 📝 开发说明

### 添加新的小说网站

1. 创建新的爬虫类，继承 `BaseScraper`
2. 实现三个抽象方法：
   - `search()` - 搜索小说
   - `getBookDetail()` - 获取详情和目录
   - `getChapterContent()` - 获取章节内容
3. 在 `ScraperManager` 中注册新爬虫

示例：
```kotlin
class NewSiteScraper : BaseScraper() {
    override val sourceName = "新站点"
    override val sourceId = "newsite"

    override suspend fun search(keyword: String): SearchResult {
        // 实现搜索逻辑
    }

    override suspend fun getBookDetail(bookUrl: String): Pair<Book, List<Chapter>> {
        // 实现详情获取
    }

    override suspend fun getChapterContent(chapterUrl: String): String {
        // 实现内容获取
    }
}
```

## 🐛 常见问题

### Q: 搜索没有结果？
A: 检查网络连接，确认网站URL是否正确，可能需要更新域名。

### Q: 章节内容显示不全？
A: 部分网站可能需要登录才能查看完整内容，或者有反爬机制。

### Q: 如何更新网站URL？
A: 编辑 `Constants.kt` 文件中的URL配置。

### Q: 如何添加新的小说源？
A: 参考"添加新的小说网站"章节。

## 📄 许可证

此项目仅供学习交流使用。

## 🙏 致谢

- Jetpack Compose - UI框架
- Jsoup - HTML解析库
- Coil - 图片加载库
- Hilt - 依赖注入框架

---

**注意**: 此应用仅供个人学习使用，请勿用于商业用途。爬取的内容版权归原作者和网站所有。
