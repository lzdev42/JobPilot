package setting

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.AlertDialog
import androidx.compose.material.OutlinedButton
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import inputview.InputType
import inputview.InputViewModel
import org.jetbrains.compose.ui.tooling.preview.Preview
import utils.AppConfig
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import utils.SystemFilePicker
import java.io.File

@Composable
@Preview

fun SettingView(viewModel: InputViewModel, modifier: Modifier = Modifier) {
    val textFromViewModel by viewModel.textFieldValue.collectAsState()
    var debugMode by remember { mutableStateOf(AppConfig.isDebugMode) }
    
    // 投递次数限制状态
    var bossApplyLimit by remember { mutableStateOf(AppConfig.bossApplyLimit.toString()) }
    var job51ApplyLimit by remember { mutableStateOf(AppConfig.job51ApplyLimit.toString()) }
    var liepin5ApplyLimit by remember { mutableStateOf(AppConfig.liepin5ApplyLimit.toString()) }
    
    Column(modifier = modifier.padding(16.dp)) {
        // Debug开关
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("调试模式", modifier = Modifier.weight(1f))
            Switch(
                checked = debugMode,
                onCheckedChange = { enabled ->
                    debugMode = enabled
                    AppConfig.isDebugMode = enabled
                }
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Gemini AppKey设置
        TextField(
            value = textFromViewModel,
            onValueChange = { newText ->
                viewModel.onTextFieldValueChanged(newText)
            },
            Modifier.fillMaxWidth(),
            label = { Text("Gemini AppKey") },
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Boss直聘投递次数限制
        TextField(
            value = bossApplyLimit,
            onValueChange = { newValue ->
                if (newValue.all { it.isDigit() } || newValue.isEmpty()) {
                    bossApplyLimit = newValue
                }
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Boss直聘每日投递限制") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 51job投递次数限制
        TextField(
            value = job51ApplyLimit,
            onValueChange = { newValue ->
                if (newValue.all { it.isDigit() } || newValue.isEmpty()) {
                    job51ApplyLimit = newValue
                }
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("51job每日投递限制") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 猎聘投递次数限制
        TextField(
            value = liepin5ApplyLimit,
            onValueChange = { newValue ->
                if (newValue.all { it.isDigit() } || newValue.isEmpty()) {
                    liepin5ApplyLimit = newValue
                }
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("猎聘每日投递限制") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = {
                // 保存投递次数限制设置
                val bossLimit = bossApplyLimit.toIntOrNull() ?: 100
                val job51Limit = job51ApplyLimit.toIntOrNull() ?: 100
                val liepin5Limit = liepin5ApplyLimit.toIntOrNull() ?: 100
                
                AppConfig.bossApplyLimit = bossLimit
                AppConfig.job51ApplyLimit = job51Limit
                AppConfig.liepin5ApplyLimit = liepin5Limit
                AppConfig.saveUserSettings()
                
                viewModel.submitText() // 调用 ViewModel 中的其他逻辑
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("保存")
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // 配置管理部分
        Text("配置管理", modifier = Modifier.padding(bottom = 8.dp))
        
        ConfigManagementSection()
    }
}

@Composable
fun ConfigManagementSection() {
    val coroutineScope = rememberCoroutineScope()
    var showClearDialog by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }
    
    Column {
        // 导出配置按钮
        OutlinedButton(
            onClick = {
                coroutineScope.launch {
                    try {
                        val selectedDir = SystemFilePicker.pickDirectory("选择导出目录")
                        if (selectedDir != null) {
                            val success = AppConfig.exportConfigs(selectedDir)
                            statusMessage = if (success) {
                                "配置导出成功: ${selectedDir.absolutePath}"
                            } else {
                                "配置导出失败"
                            }
                        }
                    } catch (e: Exception) {
                        statusMessage = "导出失败: ${e.message}"
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("导出配置")
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 导入配置按钮
        OutlinedButton(
            onClick = {
                coroutineScope.launch {
                    try {
                        val selectedDir = SystemFilePicker.pickDirectory("选择配置目录")
                        if (selectedDir != null) {
                            val success = AppConfig.importConfigs(selectedDir)
                            statusMessage = if (success) {
                                "配置导入成功"
                            } else {
                                "配置导入失败"
                            }
                        }
                    } catch (e: Exception) {
                        statusMessage = "导入失败: ${e.message}"
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("导入配置")
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 清空配置按钮
        Button(
            onClick = { showClearDialog = true },
            modifier = Modifier.fillMaxWidth(),
            colors = androidx.compose.material.ButtonDefaults.buttonColors(
                backgroundColor = Color.Red.copy(alpha = 0.1f),
                contentColor = Color.Red
            )
        ) {
            Text("清空所有配置")
        }
        
        // 状态消息
        if (statusMessage.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = statusMessage,
                color = if (statusMessage.contains("成功")) Color.Green else Color.Red,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
    
    // 清空确认对话框
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("确认清空配置") },
            text = { Text("此操作将清空所有用户配置、黑名单和投递历史，且无法撤销。确定要继续吗？") },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            withContext(Dispatchers.IO) {
                                if (AppConfig.clearAllConfigs()) {
                                    statusMessage = "配置清空成功"
                                } else {
                                    statusMessage = "配置清空失败"
                                }
                            }
                        }
                        showClearDialog = false
                    },
                    colors = androidx.compose.material.ButtonDefaults.buttonColors(
                        backgroundColor = Color.Red
                    )
                ) {
                    Text("确认清空", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showClearDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}