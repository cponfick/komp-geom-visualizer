package io.github.cponfick.state

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import io.github.cponfick.kompgeom.euclidean.twod.AffineTransformationMatrix2
import io.github.cponfick.kompgeom.euclidean.twod.Vec2
import kotlinx.coroutines.delay

@Stable
class CanvasState {
    var screenToCord by mutableStateOf(AffineTransformationMatrix2.IDENTITY)
        private set

    var cordToScreen by mutableStateOf(screenToCord.inverse())
        private set

    var tempCordToScreen by mutableStateOf(AffineTransformationMatrix2.IDENTITY)
        private set

    var currentHeight by mutableStateOf(0.0)
        private set

    var currentWidth by mutableStateOf(0.0)
        private set

    var showLabels by mutableStateOf(true)

    var hasMoved by mutableStateOf(false)
        private set

    val points = mutableStateMapOf<Int, Vec2>()

    suspend fun startTransformationLoop() {
        while (true) {
            delay(30)
            cordToScreen = tempCordToScreen.copy()
            screenToCord = cordToScreen.inverse()
        }
    }

    fun onSizeChanged(width: Double, height: Double) {
        if (currentHeight != height || currentWidth != width) {
            tempCordToScreen = AffineTransformationMatrix2.createScaling(45.0, -45.0)
                .translate(width / 2.0, height / 2.0)
            currentHeight = height
            currentWidth = width
        }
    }

    fun onPointerPress() {
        hasMoved = false
    }

    fun onPointerMove(delta: Offset) {
        hasMoved = true
        tempCordToScreen = tempCordToScreen.translate(
            delta.x.toDouble(),
            delta.y.toDouble()
        )
    }

    fun onPointerRelease(position: Offset, isPrimary: Boolean) {
        when {
            position.x < 0 || position.x > currentWidth ||
            position.y < 0 || position.y > currentHeight -> return

            isPrimary && !hasMoved -> {
                points[points.size] = screenToCord.apply(
                    Vec2(position.x.toDouble(), position.y.toDouble())
                )
            }
        }
    }

    fun onScroll(scrollDelta: Float, scrollFactor: Double) {
        val scrollAmount = scrollDelta.toDouble() * scrollFactor
        tempCordToScreen = tempCordToScreen.scale(1.0 + scrollAmount, 1.0 + scrollAmount)
    }

    fun clearPoints() {
        points.clear()
    }
}

@Composable
fun rememberCanvasState(): CanvasState {
    return remember { CanvasState() }
}
