package components.input

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ChipInput(
    value: List<String>,
    onValueChange: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    enabled: Boolean = true
) {
    var currentInput by remember { mutableStateOf("") }

    OutlinedTextField(
        value = currentInput,
        onValueChange = { newValue ->
            if (newValue.isEmpty() && currentInput.isEmpty() && value.isNotEmpty()) {
                // 当按下退格键且输入框为空时，删除最后一个标签
                onValueChange(value.dropLast(1))
            } else if (newValue.endsWith(",") || newValue.endsWith(" ")) {
                val newKeyword = newValue.trim().trimEnd(',')
                if (newKeyword.isNotEmpty() && !value.contains(newKeyword)) {
                    onValueChange(value + newKeyword)
                }
                currentInput = ""
            } else {
                currentInput = newValue
            }
        },
        enabled = enabled,
        singleLine = true,
        modifier = modifier.fillMaxWidth(),
        textStyle = TextStyle(fontSize = 16.sp), // 直接设置输入文字大小
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(
            onDone = {
                if (currentInput.isNotEmpty()) {
                    if (!value.contains(currentInput)) {
                        onValueChange(value + currentInput)
                    }
                    currentInput = ""
                }
            }
        ),
        placeholder = if (value.isEmpty()) {
            {
                Text(
                    text = placeholder,
                    style = TextStyle(
                        fontSize = 16.sp,  // 与textStyle保持一致
                        lineHeight = 30.sp  // 设置合适的行高
                    ),
                    color = LocalContentColor.current.copy(alpha = ContentAlpha.medium)
                )
            }
        } else null,
        leadingIcon = if (value.isNotEmpty()) {
            {
                Row(
                    modifier = Modifier.padding(end = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    value.forEach { keyword ->
                        Chip(
                            text = keyword,
                            onRemove = {
                                onValueChange(value - keyword)
                            }
                        )
                    }
                }
            }
        } else null
    )
}

@Composable
private fun Chip(
    text: String,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(24.dp),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colors.primary.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.primary
            )
            TextButton(
                onClick = onRemove,
                modifier = Modifier.size(16.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    text = "×",
                    color = MaterialTheme.colors.primary,
                    style = MaterialTheme.typography.caption
                )
            }
        }
    }
}