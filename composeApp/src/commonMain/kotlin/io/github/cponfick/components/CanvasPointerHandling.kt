package io.github.cponfick.components

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isPressed
import androidx.compose.ui.input.pointer.isPrimary
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.onSizeChanged
import io.github.cponfick.state.CanvasState

@OptIn(ExperimentalComposeUiApi::class)
fun Modifier.canvasPointerHandling(
  canvasState: CanvasState,
  scrollFactor: Double
): Modifier = this
  .onPointerEvent(PointerEventType.Press) {
    canvasState.onPointerPress()
  }
  .onPointerEvent(PointerEventType.Move) {
    val event = it.changes.firstOrNull()
    when {
      event == null -> return@onPointerEvent
      event.pressed && it.buttons.isPressed(0) -> {
        val delta = Offset(
          event.position.x - event.previousPosition.x,
          event.position.y - event.previousPosition.y
        )
        canvasState.onPointerMove(delta)
      }
    }
  }
  .onPointerEvent(PointerEventType.Release) {
    val event = it.changes.first()
    val position = event.position
    val isPrimary = it.button?.isPrimary == true
    canvasState.onPointerRelease(position, isPrimary)
  }
  .onPointerEvent(PointerEventType.Scroll) {
    val event = it.changes.firstOrNull()
    if (event != null) {
      canvasState.onScroll(event.scrollDelta.y, scrollFactor)
    }
  }
  .onSizeChanged { size ->
    canvasState.onSizeChanged(size.width.toDouble(), size.height.toDouble())
  }
