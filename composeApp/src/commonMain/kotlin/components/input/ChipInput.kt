package components.input

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
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
            if (newValue.endsWith(",") || newValue.endsWith(" ")) {
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
        modifier = modifier
            .fillMaxWidth()
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyUp &&
                    event.key == Key.Backspace &&
                    currentInput.isEmpty() && value.isNotEmpty()
                ) {
                    onValueChange(value.dropLast(1))
                    return@onKeyEvent true
                }
                false
            },
        textStyle = TextStyle(fontSize = 16.sp),
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
        placeholder = { Text(placeholder) },
        leadingIcon = if (value.isNotEmpty()) {
            {
                Row(
                    modifier = Modifier.padding(start = 8.dp, end = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
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
            modifier = Modifier.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.primary
            )
            TextButton(
                onClick = onRemove,
                modifier = Modifier.size(18.dp),
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
