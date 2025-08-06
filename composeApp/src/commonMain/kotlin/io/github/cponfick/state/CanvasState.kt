package io.github.cponfick.state

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
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
  val selectedPoints = mutableStateSetOf<Int>()

  // Algorithm results
  val algorithmResults = mutableStateMapOf<String, AlgorithmResult>()

  var isSelectionMode by mutableStateOf(false)
    private set

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
        if (isSelectionMode) {
          // Find the closest point to click position
          val clickPoint = screenToCord.apply(Vec2(position.x.toDouble(), position.y.toDouble()))
          val closestPointId = points.entries.minByOrNull { (_, point) ->
            point.distance(clickPoint)
          }?.key

          closestPointId?.let { id ->
            if (selectedPoints.contains(id)) {
              selectedPoints.remove(id)
            } else {
              selectedPoints.add(id)
            }
          }
        } else {
          // Add new point
          points[points.size] = screenToCord.apply(
            Vec2(position.x.toDouble(), position.y.toDouble())
          )
        }
      }
    }
  }

  fun onScroll(scrollDelta: Float, scrollFactor: Double) {
    val scrollAmount = scrollDelta.toDouble() * scrollFactor
    tempCordToScreen = tempCordToScreen.scale(1.0 + scrollAmount, 1.0 + scrollAmount)
  }

  fun clearPoints() {
    points.clear()
    selectedPoints.clear()
    algorithmResults.clear()
  }

  fun toggleSelectionMode() {
    isSelectionMode = !isSelectionMode
    if (!isSelectionMode) {
      selectedPoints.clear()
    }
  }

  fun clearSelection() {
    selectedPoints.clear()
  }

  fun selectAllPoints() {
    selectedPoints.clear()
    selectedPoints.addAll(points.keys)
  }

  fun executeAlgorithm(algorithm: GeometryAlgorithm) {
    val selectedPointsData = selectedPoints.map { points[it]!! }
    if (selectedPointsData.size >= algorithm.getMinimumPoints()) {
      val result = algorithm.execute(selectedPointsData)
      algorithmResults[algorithm.getName()] = result
    }
  }

  fun clearAlgorithmResults() {
    algorithmResults.clear()
  }
}

@Composable
fun rememberCanvasState(): CanvasState {
  return remember { CanvasState() }
}

// Data classes for algorithm results
sealed class AlgorithmResult {
  data class PointPair(
    val point1: Vec2,
    val point2: Vec2,
    val distance: Double,
    val color: Color = Color.Red
  ) : AlgorithmResult()

  data class Line(
    val start: Vec2,
    val end: Vec2,
    val color: Color = Color.Blue
  ) : AlgorithmResult()

  data class Points(
    val points: List<Vec2>,
    val color: Color = Color.Green
  ) : AlgorithmResult()
}

// Interface for geometry algorithms
interface GeometryAlgorithm {
  fun getName(): String
  fun getMinimumPoints(): Int
  fun execute(points: List<Vec2>): AlgorithmResult
}
