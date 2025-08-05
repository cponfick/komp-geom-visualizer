package io.github.cponfick

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isPressed
import androidx.compose.ui.input.pointer.isPrimary
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.zIndex
import io.github.cponfick.components.BaseCanvas
import io.github.cponfick.components.NavigationBar
import io.github.cponfick.kompgeom.euclidean.twod.AffineTransformationMatrix2
import io.github.cponfick.kompgeom.euclidean.twod.Vec2
import org.jetbrains.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
@Preview
fun App() {
  MaterialTheme {
    var screenToCord by remember { mutableStateOf(AffineTransformationMatrix2.IDENTITY) }
    var cordToScreen by remember { mutableStateOf(screenToCord.inverse()) }
    var currentHeight by remember { mutableStateOf(0.0) }
    var currentWidth by remember { mutableStateOf(0.0) }

    val points = remember { mutableStateMapOf<Int, Vec2>() }

    NavigationBar(
      modifier = Modifier.zIndex(1f), onClearPoints = { points.clear() })

    Column(modifier = Modifier.fillMaxSize()) {

      BaseCanvas(
        cordToScreen = cordToScreen,
        points = points,
        canvasModifier = Modifier.fillMaxSize().onPointerEvent(PointerEventType.Move) {
            val event = it.changes.firstOrNull()
            when {
              event == null -> return@onPointerEvent
              event.pressed && it.buttons.isPressed(0) -> {
                cordToScreen = cordToScreen.translate(
                  (event.position.x - event.previousPosition.x).toDouble(),
                  (event.position.y - event.previousPosition.y).toDouble(
                  )
                )
                screenToCord = cordToScreen.inverse()
              }
            }
          }.onPointerEvent(PointerEventType.Release) {
            val event = it.changes.first()
            val position = event.position
            when {
              // only if previous event was a press and the button is primary
              position.x < 0 || position.x > currentWidth || position.y < 0 || position.y > currentHeight -> return@onPointerEvent
              it.button!!.isPrimary -> {
                points[points.size] = screenToCord.apply(Vec2(position.x.toDouble(), position.y.toDouble()))
              }
            }
          }.onPointerEvent(PointerEventType.Scroll) {
            val event = it.changes.firstOrNull()
            if (event == null) return@onPointerEvent
            val scrollAmount = event.scrollDelta.y.toDouble() * scrollFactor
            cordToScreen = cordToScreen.scale(1.0 + scrollAmount, 1.0 + scrollAmount)
            screenToCord = cordToScreen.inverse()
          }.onSizeChanged {
            val newHeight = it.height.toDouble()
            val newWidth = it.width.toDouble()
            if (currentHeight != newHeight || currentWidth != newWidth) {
              cordToScreen =
                AffineTransformationMatrix2.createScaling(45.0, -45.0).translate(newWidth / 2.0, newHeight / 2.0)
              screenToCord = cordToScreen.inverse()
              currentHeight = newHeight
              currentWidth = newWidth
            }
          })
    }
  }
}

expect val scrollFactor : Double
