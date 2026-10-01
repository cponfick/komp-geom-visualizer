package io.github.cponfick.components

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.isPressed
import androidx.compose.ui.input.pointer.isPrimary
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.onSizeChanged
import io.github.cponfick.state.CanvasState
import kotlin.math.hypot

@OptIn(ExperimentalComposeUiApi::class)
fun Modifier.canvasPointerHandling(
  canvasState: CanvasState,
  scrollFactor: Double
): Modifier {
  val activePointers = mutableMapOf<PointerId, Offset>()
  var wasMultiTouch = false

  fun centroid(first: Offset, second: Offset) = Offset(
    (first.x + second.x) / 2f,
    (first.y + second.y) / 2f
  )

  fun distance(first: Offset, second: Offset): Float =
    hypot(second.x - first.x, second.y - first.y)

  fun updatePointers(eventChanges: List<androidx.compose.ui.input.pointer.PointerInputChange>) {
    eventChanges.forEach { change ->
      if (change.pressed) activePointers[change.id] = change.position
      else activePointers.remove(change.id)
    }
  }

  return this
    .onPointerEvent(PointerEventType.Press) { event ->
      event.changes.forEach { change ->
        if (change.pressed) activePointers[change.id] = change.position
      }

      if (activePointers.size == 1) {
        val pointer = event.changes.firstOrNull() ?: return@onPointerEvent
        canvasState.onPointerPress(pointer.position)
      } else if (activePointers.size >= 2) {
        // A second finger changes a possible tap/segment gesture into a
        // navigation gesture. It must never create a point on release.
        wasMultiTouch = true
        canvasState.cancelPointerGesture()
      }
    }
    .onPointerEvent(PointerEventType.Move) { event ->
      val previous = activePointers.toMap()
      updatePointers(event.changes)

      if (activePointers.size >= 2) {
        val pointers = activePointers.entries.take(2)
        val first = pointers[0].value
        val second = pointers[1].value
        val previousFirst = previous[pointers[0].key] ?: first
        val previousSecond = previous[pointers[1].key] ?: second
        val previousCenter = centroid(previousFirst, previousSecond)
        val currentCenter = centroid(first, second)
        val previousDistance = distance(previousFirst, previousSecond)
        val currentDistance = distance(first, second)
        if (previousDistance > 0f && currentDistance > 0f) {
          canvasState.onPinchZoom(
            previousCenter,
            currentCenter,
            (currentDistance / previousDistance).toDouble()
          )
        }
        wasMultiTouch = true
      } else {
        val change = event.changes.firstOrNull() ?: return@onPointerEvent
        when {
          !change.pressed -> return@onPointerEvent
          change.type == PointerType.Touch || event.buttons.isPressed(0) -> {
            val delta = Offset(
              change.position.x - change.previousPosition.x,
              change.position.y - change.previousPosition.y
            )
            if (!wasMultiTouch) canvasState.onPointerMove(delta)
          }
          else -> canvasState.updateHover(change.position)
        }
      }
    }
    .onPointerEvent(PointerEventType.Release) { event ->
      val change = event.changes.firstOrNull() ?: return@onPointerEvent
      val isTouch = change.type == PointerType.Touch

      if (activePointers.size > 1 || wasMultiTouch) {
        activePointers.remove(change.id)
        if (activePointers.isEmpty()) wasMultiTouch = false
        return@onPointerEvent
      }

      activePointers.remove(change.id)
      val isPrimary = isTouch || event.button?.isPrimary == true
      canvasState.onPointerRelease(change.position, isPrimary)
      canvasState.updateHover(change.position)
    }
    .onPointerEvent(PointerEventType.Scroll) { event ->
      val change = event.changes.firstOrNull()
      if (change != null) {
        canvasState.onScroll(change.scrollDelta.y, scrollFactor, change.position)
      }
    }
    .onSizeChanged { size ->
      canvasState.onSizeChanged(size.width.toDouble(), size.height.toDouble())
    }
}
