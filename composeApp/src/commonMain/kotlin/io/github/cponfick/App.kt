package io.github.cponfick

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import io.github.cponfick.components.AlgorithmPanel
import io.github.cponfick.components.BaseCanvas
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

    Box(modifier = Modifier) {
      // Main canvas
      BaseCanvas(
        cordToScreen = canvasState.cordToScreen,
        points = canvasState.points,
        selectedPoints = canvasState.selectedPoints,
        showLabels = canvasState.showLabels,
        algorithmResults = canvasState.algorithmResults,
        canvasModifier = Modifier
          .fillMaxSize()
          .graphicsLayer()
          .canvasPointerHandling(canvasState, scrollFactor)
      )

      AlgorithmPanel(
        canvasState = canvasState,
        modifier = Modifier
          .align(Alignment.TopStart)
          .zIndex(1f)
          .padding(16.dp)
      )
    }
  }
}

expect val scrollFactor: Double
