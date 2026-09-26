package io.github.cponfick.state

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import io.github.cponfick.kompgeom.euclidean.twod.AffineTransformationMatrix2
import io.github.cponfick.kompgeom.euclidean.twod.Vec2

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
  val algorithmResults = mutableStateMapOf<String, AlgorithmResult>()
  var isSelectionMode by mutableStateOf(false)
    private set

  private var nextPointId = 0
  private var hasLaidOut = false
  private var lastPointerPosition = Offset.Zero
  private val minPixelsPerWorldUnit = 0.1
  private val maxPixelsPerWorldUnit = 10_000.0
  private val pointHitRadiusPixels = 12.0

  private fun setTransform(transform: AffineTransformationMatrix2) {
    tempCordToScreen = transform
    cordToScreen = transform.copy()
    screenToCord = transform.inverse()
  }

  fun onSizeChanged(width: Double, height: Double) {
    if (width <= 0.0 || height <= 0.0 || (currentHeight == height && currentWidth == width)) return
    if (!hasLaidOut) {
      setTransform(AffineTransformationMatrix2.createScaling(45.0, -45.0)
        .translate(width / 2.0, height / 2.0))
      hasLaidOut = true
    } else {
      // Resize anchor: preserve the world point at the old viewport center.
      setTransform(tempCordToScreen.translate(
        (width - currentWidth) / 2.0, (height - currentHeight) / 2.0
      ))
    }
    currentWidth = width
    currentHeight = height
  }

  fun onPointerPress(position: Offset = lastPointerPosition) {
    lastPointerPosition = position
    hasMoved = false
  }

  fun onPointerMove(delta: Offset) {
    hasMoved = true
    setTransform(tempCordToScreen.translate(delta.x.toDouble(), delta.y.toDouble()))
  }

  fun onPointerRelease(position: Offset, isPrimary: Boolean) {
    lastPointerPosition = position
    if (position.x < 0 || position.x > currentWidth || position.y < 0 || position.y > currentHeight) return
    if (!isPrimary || hasMoved) return

    if (isSelectionMode) {
      val closestPointId = points.entries
        .map { (id, point) -> id to cordToScreen.apply(point) }
        .filter { (_, point) ->
          val dx = point.x - position.x
          val dy = point.y - position.y
          dx * dx + dy * dy <= pointHitRadiusPixels * pointHitRadiusPixels
        }
        .minByOrNull { (_, point) ->
          val dx = point.x - position.x
          val dy = point.y - position.y
          dx * dx + dy * dy
        }?.first
      if (closestPointId != null) {
        if (!selectedPoints.add(closestPointId)) selectedPoints.remove(closestPointId)
        algorithmResults.clear()
      }
    } else {
      points[nextPointId++] = screenToCord.apply(Vec2(position.x.toDouble(), position.y.toDouble()))
      algorithmResults.clear()
    }
  }

  fun onScroll(scrollDelta: Float, scrollFactor: Double, position: Offset = lastPointerPosition) {
    if (!scrollDelta.isFinite() || !scrollFactor.isFinite()) return
    lastPointerPosition = position
    val origin = tempCordToScreen.apply(Vec2(0.0, 0.0))
    val unit = tempCordToScreen.apply(Vec2(1.0, 0.0))
    val oldScale = unit.distance(origin)
    if (!oldScale.isFinite() || oldScale <= 0.0) return
    val requested = (1.0 + scrollDelta.toDouble() * scrollFactor).coerceIn(0.01, 100.0)
    val newScale = (oldScale * requested).coerceIn(minPixelsPerWorldUnit, maxPixelsPerWorldUnit)
    val factor = newScale / oldScale
    val anchor = Vec2(position.x.toDouble(), position.y.toDouble())
    val worldAnchor = screenToCord.apply(anchor)
    val scaled = tempCordToScreen.scale(factor, factor)
    val scaledAnchor = scaled.apply(worldAnchor)
    setTransform(scaled.translate(anchor.x - scaledAnchor.x, anchor.y - scaledAnchor.y))
  }

  fun clearPoints() {
    points.clear(); selectedPoints.clear(); algorithmResults.clear()
  }

  fun toggleSelectionMode() {
    isSelectionMode = !isSelectionMode
    if (!isSelectionMode) selectedPoints.clear()
    algorithmResults.clear()
  }

  fun clearSelection() { selectedPoints.clear(); algorithmResults.clear() }

  fun selectAllPoints() {
    selectedPoints.clear(); selectedPoints.addAll(points.keys); algorithmResults.clear()
  }

  fun executeAlgorithm(algorithm: GeometricAlgorithm) {
    val selectedPointsData = selectedPoints.sorted().mapNotNull { points[it] }
    if (selectedPointsData.size < algorithm.getMinimumPoints()) return
    try {
      algorithmResults[algorithm.getName()] = algorithm.execute(selectedPointsData)
    } catch (error: IllegalArgumentException) {
      algorithmResults[algorithm.getName()] = AlgorithmResult.Error(error.message ?: "Invalid input")
    } catch (error: IllegalStateException) {
      algorithmResults[algorithm.getName()] = AlgorithmResult.Error(error.message ?: "Algorithm failed")
    }
  }

  fun clearAlgorithmResults() { algorithmResults.clear() }
}

@Composable
fun rememberCanvasState(): CanvasState = remember { CanvasState() }

sealed class AlgorithmResult {
  data class PointPair(val point1: Vec2, val point2: Vec2, val distance: Double, val color: Color = Color.Red) : AlgorithmResult()
  data class Line(val start: Vec2, val end: Vec2, val color: Color = Color.Blue) : AlgorithmResult()
  data class Points(val points: List<Vec2>, val color: Color = Color.Green) : AlgorithmResult()
  data class Lines(val lines: List<Line>) : AlgorithmResult()
  data class Error(val message: String) : AlgorithmResult()
}

interface GeometricAlgorithm {
  fun getName(): String
  fun getMinimumPoints(): Int
  fun execute(points: List<Vec2>): AlgorithmResult
}
