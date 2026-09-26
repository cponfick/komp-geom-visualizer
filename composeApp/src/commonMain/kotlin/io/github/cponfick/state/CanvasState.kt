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
  val zoomPercent: Int
    get() {
      val origin = tempCordToScreen.apply(Vec2(0.0, 0.0))
      val unit = tempCordToScreen.apply(Vec2(1.0, 0.0))
      return ((unit.distance(origin) / 45.0) * 100.0).toInt().coerceAtLeast(1)
    }
  var hoveredPointId by mutableStateOf<Int?>(null)
    private set
  var isHelpDismissed by mutableStateOf(false)
    private set

  val points = mutableStateMapOf<Int, Vec2>()
  val selectedPoints = mutableStateSetOf<Int>()
  val algorithmResults = mutableStateMapOf<String, AlgorithmResult>()
  var isSelectionMode by mutableStateOf(false)
    private set

  private data class SceneSnapshot(val points: Map<Int, Vec2>, val selected: Set<Int>)
  private val undoStack = ArrayDeque<SceneSnapshot>()
  private val redoStack = ArrayDeque<SceneSnapshot>()
  private var nextPointId = 0

  private fun snapshot() = SceneSnapshot(points.toMap(), selectedPoints.toSet())
  private fun rememberUndo() {
    undoStack.addLast(snapshot())
    if (undoStack.size > 50) undoStack.removeFirst()
    redoStack.clear()
  }
  private var hasLaidOut = false
  private var lastPointerPosition = Offset.Zero
  private var initialTransform = AffineTransformationMatrix2.IDENTITY
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
      initialTransform = tempCordToScreen
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

  fun updateHover(position: Offset) {
    hoveredPointId = hitPoint(position)
  }

  private fun hitPoint(position: Offset): Int? = points.entries
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

  fun onPointerRelease(position: Offset, isPrimary: Boolean) {
    lastPointerPosition = position
    if (position.x < 0 || position.x > currentWidth || position.y < 0 || position.y > currentHeight) return
    if (!isPrimary || hasMoved) return

    if (isSelectionMode) {
      val closestPointId = hitPoint(position)
      if (closestPointId != null) {
        rememberUndo()
        if (!selectedPoints.add(closestPointId)) selectedPoints.remove(closestPointId)
        algorithmResults.clear()
      }
    } else {
      rememberUndo()
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
    if (points.isNotEmpty() || selectedPoints.isNotEmpty()) rememberUndo()
    points.clear(); selectedPoints.clear(); algorithmResults.clear(); hoveredPointId = null
  }

  private fun restore(snapshot: SceneSnapshot) {
    points.clear(); points.putAll(snapshot.points)
    selectedPoints.clear(); selectedPoints.addAll(snapshot.selected)
    algorithmResults.clear()
    nextPointId = (points.keys.maxOrNull() ?: -1) + 1
  }

  fun undo() {
    undoStack.removeLastOrNull()?.let { previous ->
      redoStack.addLast(snapshot()); restore(previous)
    }
  }

  fun redo() {
    redoStack.removeLastOrNull()?.let { next ->
      undoStack.addLast(snapshot()); restore(next)
    }
  }

  val canUndo: Boolean get() = undoStack.isNotEmpty()
  val canRedo: Boolean get() = redoStack.isNotEmpty()

  fun resetView() {
    if (hasLaidOut) {
      setTransform(initialTransform)
      hasMoved = false
    }
  }

  fun dismissHelp() { isHelpDismissed = true }

  fun removeAlgorithmResult(name: String) { algorithmResults.remove(name) }

  fun toggleSelectionMode() {
    applySelectionMode(!isSelectionMode)
  }

  fun applySelectionMode(selectionMode: Boolean) {
    isSelectionMode = selectionMode
    if (!isSelectionMode) selectedPoints.clear()
    algorithmResults.clear()
  }

  fun clearSelection() {
    if (selectedPoints.isNotEmpty()) rememberUndo()
    selectedPoints.clear(); algorithmResults.clear()
  }

  fun selectAllPoints() {
    if (selectedPoints != points.keys) rememberUndo()
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
