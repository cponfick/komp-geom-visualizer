package io.github.cponfick

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "komp-geom-visualizer",
    ) {
        App()
    }
}