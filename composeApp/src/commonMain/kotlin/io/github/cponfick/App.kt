package io.github.cponfick

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import io.github.cponfick.components.BaseCanvas
import io.github.cponfick.components.NavigationBar
import io.github.cponfick.components.canvasPointerHandling
import io.github.cponfick.state.rememberCanvasState
import org.jetbrains.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalFoundationApi::class)
@Composable
@Preview
fun App() {
  MaterialTheme {
    val canvasState = rememberCanvasState()

    // Start the transformation update loop
    LaunchedEffect(canvasState) {
      canvasState.startTransformationLoop()
    }

    NavigationBar(
      modifier = Modifier.zIndex(1f),
      onClearPoints = { canvasState.clearPoints() },
      onShowLabelsChanged = { canvasState.showLabels = it }
    )

    Column(modifier = Modifier.fillMaxSize()) {
      BaseCanvas(
        cordToScreen = canvasState.cordToScreen,
        points = canvasState.points,
        showLabels = canvasState.showLabels,
        canvasModifier = Modifier
          .fillMaxSize()
          .graphicsLayer()
          .canvasPointerHandling(canvasState, scrollFactor)
      )
    }
  }
}

expect val scrollFactor: Double
