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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import inputview.InputType
import inputview.InputViewModel
import org.jetbrains.compose.ui.tooling.preview.Preview
import utils.AppConfig

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
    }
}