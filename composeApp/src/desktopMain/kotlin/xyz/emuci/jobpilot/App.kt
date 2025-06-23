package xyz.emuci.jobpilot

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import logview.LogView
import logview.LogViewModel
import mainpage.MainPage
import org.jetbrains.compose.ui.tooling.preview.Preview

import statusbar.StatusBarViewModel
import statusbar.StatusBar

@Composable
@Preview
fun App() {
    MaterialTheme {
        val scope = rememberCoroutineScope()
        var showContent by remember { mutableStateOf(false) }
        var content by remember { mutableStateOf("") }

        Column {
            MainPage(Modifier.weight(1f).fillMaxWidth().padding(10.dp))
            StatusBar(StatusBarViewModel(), Modifier.padding(10.dp))
        }
    }
}