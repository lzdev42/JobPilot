package xyz.emuci.jobpilot

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
	Window(
		onCloseRequest = ::exitApplication,
		title = "JobPilot",
	) {
		AppInitializer.initialize()
		App()
	}
}