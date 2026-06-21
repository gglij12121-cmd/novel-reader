package com.reader.novel.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 日志管理器
 * 用于收集和显示调试日志
 */
object LogManager {
    private val logs = mutableListOf<String>()
    private const val MAX_LOGS = 100

    /**
     * 添加日志
     */
    fun addLog(message: String) {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
            .format(java.util.Date())
        logs.add(0, "[$timestamp] $message")

        // 限制日志数量
        if (logs.size > MAX_LOGS) {
            logs.removeAt(logs.size - 1)
        }
    }

    /**
     * 获取所有日志
     */
    fun getLogs(): List<String> = logs.toList()

    /**
     * 清除日志
     */
    fun clearLogs() {
        logs.clear()
    }
}

/**
 * 调试日志对话框
 */
@Composable
fun DebugLogDialog(
    onDismiss: () -> Unit
) {
    val logs = remember { LogManager.getLogs() }
    val clipboardManager = LocalClipboardManager.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "调试日志",
                    style = MaterialTheme.typography.titleMedium
                )
                Row {
                    // 复制按钮
                    IconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(logs.joinToString("\n")))
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "复制"
                        )
                    }
                    // 清除按钮
                    TextButton(
                        onClick = { LogManager.clearLogs() }
                    ) {
                        Text("清除")
                    }
                    // 关闭按钮
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "关闭"
                        )
                    }
                }
            }
        },
        text = {
            if (logs.isEmpty()) {
                Text(
                    text = "暂无日志\n\n请先搜索小说，然后查看此处的日志输出",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                ) {
                    items(logs) { log ->
                        Text(
                            text = log,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            ),
                            color = if (log.contains("失败") || log.contains("错误")) {
                                MaterialTheme.colorScheme.error
                            } else if (log.contains("成功") || log.contains("找到")) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}
