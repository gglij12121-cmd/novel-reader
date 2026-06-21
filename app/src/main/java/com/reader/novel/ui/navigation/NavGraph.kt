package com.reader.novel.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.reader.novel.ui.screen.bookshelf.BookshelfScreen
import com.reader.novel.ui.screen.detail.DetailScreen
import com.reader.novel.ui.screen.home.HomeScreen
import com.reader.novel.ui.screen.reader.ReaderScreen
import com.reader.novel.ui.screen.search.SearchScreen
import com.reader.novel.ui.screen.settings.SettingsScreen

/**
 * 导航路由定义
 * 定义App中所有页面的路由
 */
object Routes {
    const val HOME = "home"
    const val SEARCH = "search"
    const val DETAIL = "detail/{bookUrl}/{source}"
    const val READER = "reader/{bookId}/{chapterIndex}"
    const val BOOKSHELF = "bookshelf"
    const val SETTINGS = "settings"

    /**
     * 创建详情页路由
     */
    fun detail(bookUrl: String, source: String): String {
        val encodedUrl = java.net.URLEncoder.encode(bookUrl, "UTF-8")
        return "detail/$encodedUrl/$source"
    }

    /**
     * 创建阅读器路由
     */
    fun reader(bookId: Long, chapterIndex: Int): String {
        return "reader/$bookId/$chapterIndex"
    }
}

/**
 * 导航图配置
 *
 * @param navController 导航控制器
 * @param startDestination 起始页面
 */
@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String = Routes.HOME
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // 首页 (发现)
        composable(Routes.HOME) {
            HomeScreen(
                onNavigateToSearch = {
                    navController.navigate(Routes.SEARCH)
                },
                onNavigateToDetail = { bookUrl, source ->
                    navController.navigate(Routes.detail(bookUrl, source))
                },
                onNavigateToBookshelf = {
                    navController.navigate(Routes.BOOKSHELF)
                },
                onNavigateToSettings = {
                    navController.navigate(Routes.SETTINGS)
                }
            )
        }

        // 搜索页
        composable(Routes.SEARCH) {
            SearchScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDetail = { bookUrl, source ->
                    navController.navigate(Routes.detail(bookUrl, source))
                }
            )
        }

        // 详情页
        composable(
            route = Routes.DETAIL,
            arguments = listOf(
                navArgument("bookUrl") { type = NavType.StringType },
                navArgument("source") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val encodedUrl = backStackEntry.arguments?.getString("bookUrl") ?: ""
            val bookUrl = java.net.URLDecoder.decode(encodedUrl, "UTF-8")
            val source = backStackEntry.arguments?.getString("source") ?: ""

            DetailScreen(
                bookUrl = bookUrl,
                source = source,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToReader = { bookId, chapterIndex ->
                    navController.navigate(Routes.reader(bookId, chapterIndex))
                }
            )
        }

        // 阅读器
        composable(
            route = Routes.READER,
            arguments = listOf(
                navArgument("bookId") { type = NavType.LongType },
                navArgument("chapterIndex") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val bookId = backStackEntry.arguments?.getLong("bookId") ?: 0L
            val chapterIndex = backStackEntry.arguments?.getInt("chapterIndex") ?: 0

            ReaderScreen(
                bookId = bookId,
                initialChapterIndex = chapterIndex,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 书架
        composable(Routes.BOOKSHELF) {
            BookshelfScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDetail = { bookUrl, source ->
                    navController.navigate(Routes.detail(bookUrl, source))
                }
            )
        }

        // 设置
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
