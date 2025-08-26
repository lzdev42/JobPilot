package inputview


import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun InputView(viewModel: InputViewModel, modifier: Modifier = Modifier) {

    val textFromViewModel by viewModel.textFieldValue.collectAsState()
    Column(modifier = modifier){
        Button(onClick = {
            viewModel.submitText() // 调用 ViewModel 中的其他逻辑
        }) {
            Text("保存")
        }

        TextField(
            value = textFromViewModel,
            onValueChange = { newText ->
                viewModel.onTextFieldValueChanged(newText)
            },
            Modifier.fillMaxWidth(),
            label = { Text("在此输入") },
        )
    }

}