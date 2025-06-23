package inputview

import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import utils.AppConfig

enum class InputType {
    SETTING, // 设置
    RESUME, // 简历
    PROMPT // 提示
}

class InputViewModel(val inputType: InputType): ViewModel() {
    private val _textFieldValue = MutableStateFlow("")
    // 公开的 StateFlow，UI 只能观察，不能直接修改
    val textFieldValue: StateFlow<String> = _textFieldValue.asStateFlow()

    init {
        loadInitialValue()
    }

    // 加载初始值
    private fun loadInitialValue() {
        val initialValue = when (inputType) {
            InputType.SETTING -> {
                // 如果有设置相关的配置，在这里读取
                "" // 或者从 AppConfig 读取相应的设置值
            }
            InputType.RESUME -> {
                AppConfig.resume
            }
            InputType.PROMPT -> {
                AppConfig.prompt
            }
        }
        _textFieldValue.value = initialValue
    }

    // 重新加载配置值（在设置更新后调用）
    fun reloadValue() {
        loadInitialValue()
    }

    // 当 TextField 内容改变时，UI 会调用这个函数来更新 ViewModel 中的状态
    fun onTextFieldValueChanged(newValue: String) {
        _textFieldValue.value = newValue
    }

    // 你可以在 ViewModel 中添加与这个文本相关的其他逻辑
    fun submitText() {
        println("Submitted text: ${_textFieldValue.value}")
        // 例如：进行网络请求，保存到数据库等
        when (inputType) {
            InputType.SETTING -> {
                // 处理设置相关的逻辑
                println("Handling setting input: ${_textFieldValue.value}")
                AppConfig.geminiAppKey = _textFieldValue.value
            }
            InputType.RESUME -> {
                // 处理简历相关的逻辑
                println("Handling resume input: ${_textFieldValue.value}")
                AppConfig.resume = _textFieldValue.value
            }
            InputType.PROMPT -> {
                // 处理提示相关的逻辑
                println("Handling prompt input: ${_textFieldValue.value}")
                AppConfig.prompt = _textFieldValue.value
            }

        }
    }
}