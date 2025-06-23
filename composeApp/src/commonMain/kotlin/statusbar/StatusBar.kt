package statusbar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.ui.Alignment

@Composable
@Preview
fun StatusBar(viewModel: StatusBarViewModel, modifier: Modifier = Modifier) {

    Row(modifier = modifier) {
        Text("Version: ")
        Text(viewModel.applicationDisplayVersion)
        Spacer(Modifier.weight(1f))
        Text("System: ${viewModel.operatingSystemInfo} 内存占用: ${viewModel.heapUsedMb}MB / ${viewModel.heapTotalMb}MB",Modifier.padding(start = 10.dp))
    }
}