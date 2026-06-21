package com.reader.novel

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.reader.novel.ui.navigation.NavGraph
import com.reader.novel.ui.theme.NovelReaderTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * 主Activity
 *
 * @AndroidEntryPoint 注解：
 * - 允许Hilt注入依赖到此Activity
 * - 是Hilt依赖注入的入口点之一
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            NovelReaderTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    NavGraph(navController = navController)
                }
            }
        }
    }
}
