package logview

import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.*

class LogViewModel {
    val logs: SnapshotStateList<String> = mutableStateListOf() // SnapshotStateList 是线程安全的
    // 添加日志的方法
    fun addLog(message: String) {
        // 为了演示，我们让最新的日志显示在最前面，并限制日志数量
        if (logs.size >= 100) { // 限制日志条数，防止内存无限增长
            logs.removeLast()
        }
        logs.add(0, "[${java.text.SimpleDateFormat("HH:mm:ss").format(java.util.Date())}] $message")
    }

    // 清除日志的方法
    fun clearLogs() {
        logs.clear()
    }

    // 示例：初始化时或特定操作时添加一些日志
    init {

    }
}