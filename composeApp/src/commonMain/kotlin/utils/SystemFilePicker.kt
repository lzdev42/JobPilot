package utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.swing.JFileChooser
import javax.swing.SwingUtilities

/**
 * 跨平台文件选择器
 * 使用Swing的JFileChooser，但确保在正确的线程中运行
 */
object SystemFilePicker {
    
    /**
     * 选择目录
     * @param title 对话框标题
     * @return 选择的目录，如果用户取消则返回null
     */
    suspend fun pickDirectory(title: String): File? = withContext(Dispatchers.IO) {
        try {
            var selectedFile: File? = null
            SwingUtilities.invokeAndWait {
                val fileChooser = JFileChooser().apply {
                    dialogTitle = title
                    fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                    isAcceptAllFileFilterUsed = false
                }
                
                val result = fileChooser.showOpenDialog(null)
                if (result == JFileChooser.APPROVE_OPTION) {
                    selectedFile = fileChooser.selectedFile
                }
            }
            
            selectedFile
        } catch (e: Exception) {
            System.err.println("选择目录失败: ${e.message}")
            null
        }
    }
    
    /**
     * 选择文件
     * @param title 对话框标题
     * @param extensions 文件扩展名列表，如 ["json", "txt"]
     * @return 选择的文件，如果用户取消则返回null
     */
    suspend fun pickFile(title: String, extensions: List<String> = emptyList()): File? = withContext(Dispatchers.IO) {
        try {
            var selectedFile: File? = null
            SwingUtilities.invokeAndWait {
                val fileChooser = JFileChooser().apply {
                    dialogTitle = title
                    fileSelectionMode = JFileChooser.FILES_ONLY
                    isAcceptAllFileFilterUsed = extensions.isEmpty()
                }
                
                // 设置文件过滤器
                if (extensions.isNotEmpty()) {
                    fileChooser.fileFilter = object : javax.swing.filechooser.FileFilter() {
                        override fun accept(f: File): Boolean {
                            return f.isDirectory || extensions.any { f.name.lowercase().endsWith(".$it") }
                        }
                        
                        override fun getDescription(): String {
                            return extensions.joinToString(", ") { "*.$it" }
                        }
                    }
                }
                
                val result = fileChooser.showOpenDialog(null)
                if (result == JFileChooser.APPROVE_OPTION) {
                    selectedFile = fileChooser.selectedFile
                }
            }
            
            selectedFile
        } catch (e: Exception) {
            System.err.println("选择文件失败: ${e.message}")
            null
        }
    }
}
