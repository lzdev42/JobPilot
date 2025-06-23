package components.dropdown

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier


@Composable
fun BossConfigDropdown(
    label: String,
    items: Map<String, String>,
    selectedValue: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = items.entries.find { it.value == selectedValue }?.key ?: "不限"
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier.clickable { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            enabled = false,
            interactionSource = interactionSource
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.width(IntrinsicSize.Min)
        ) {
            items.forEach { (itemLabel, itemValue) ->
                DropdownMenuItem(
                    onClick = {
                        onValueChange(itemValue)
                        expanded = false
                    }
                ) {
                    Text(itemLabel)
                }
            }
        }
    }
} 