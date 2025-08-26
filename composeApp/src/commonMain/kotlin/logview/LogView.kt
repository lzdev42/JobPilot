package logview

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.Text
@Composable
@Preview
fun LogView(viewModel: LogViewModel,modifier: Modifier) {
    val listState = rememberLazyListState() // 用于控制滚动
    // 当有新日志添加到列表顶部时，自动滚动到顶部
    // 注意: 如果日志是添加到列表末尾，则需要滚动到 logs.size - 1
    LaunchedEffect(viewModel.logs.size) {
        if (viewModel.logs.isNotEmpty()) {
            // 因为我们是 add(0, ...)，所以新日志在索引0
            listState.animateScrollToItem(index = 0)
        }
    }
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxWidth().padding(8.dp)
    ) {
        itemsIndexed(
            items = viewModel.logs,
            key = { index, logEntry -> "$index-$logEntry" }
        ){ index, logEntry ->
            Text(
                text = logEntry,
                modifier = Modifier
                    .fillMaxWidth() // 确保文本占据整行宽度以进行换行
                    .padding(vertical = 4.dp),
                fontSize = 13.sp, // 合适的日志字体大小
                fontFamily = FontFamily.Monospace,
                // softWrap = true (Text 默认 softWrap 为 true，所以会自动换行)
                // overflow = TextOverflow.Clip (默认)
            )
            if (index < viewModel.logs.size - 1) {
                Divider(color = MaterialTheme.colors.onSurface.copy(alpha = 0.08f)) // 日志间的分割线
            }
        }
    }
}

