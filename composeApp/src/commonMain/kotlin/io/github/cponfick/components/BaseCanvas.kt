package io.github.cponfick.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import io.github.cponfick.components.utils.formatDouble
import io.github.cponfick.kompgeom.euclidean.twod.AffineTransformationMatrix2
import io.github.cponfick.kompgeom.euclidean.twod.Vec2
import io.github.cponfick.state.AlgorithmResult
import kotlin.math.abs
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@Composable
fun BaseCanvas(
  cordToScreen: AffineTransformationMatrix2,
  showLabels: Boolean = true,
  points: Map<Int, Vec2> = emptyMap(),
  selectedPoints: Set<Int> = emptySet(),
  segments: Map<Int, io.github.cponfick.state.SegmentEntity> = emptyMap(),
  selectedSegments: Set<Int> = emptySet(),
  hoveredPointId: Int? = null,
  hoveredSegmentId: Int? = null,
  segmentPreview: Pair<Vec2, Vec2>? = null,
  algorithmResults: Map<String, AlgorithmResult> = emptyMap(),
  canvasModifier: Modifier
) {
  val textMeasurer = rememberTextMeasurer()
  val gridColor = MaterialTheme.colorScheme.outlineVariant
  val axisColor = MaterialTheme.colorScheme.onSurface
  val pointColor = MaterialTheme.colorScheme.onSurface
  val selectedColor = MaterialTheme.colorScheme.primary
  val segmentHoverColor = MaterialTheme.colorScheme.tertiary
  val segmentPreviewColor = MaterialTheme.colorScheme.primary.copy(alpha = .65f)

  Canvas(modifier = canvasModifier.fillMaxSize().graphicsLayer()) {
    val canvasWidth = size.width
    val canvasHeight = size.height

    // Early return if canvas dimensions are invalid
    if (canvasWidth <= 0 || canvasHeight <= 0) {
      return@Canvas
    }

    fun drawLabel(text: String, topLeft: Offset) {
      if (!topLeft.x.isFinite() || !topLeft.y.isFinite()) return
      val maxX = (canvasWidth - 1f).coerceAtLeast(0f)
      val maxY = (canvasHeight - 1f).coerceAtLeast(0f)
      // Do not clamp labels that are completely beyond the right/bottom edge:
      // clamping would make every off-screen label render on top of each other.
      if (topLeft.x > maxX || topLeft.y > maxY) return
      val safeX = topLeft.x.coerceAtLeast(0f)
      val safeY = topLeft.y.coerceAtLeast(0f)
      drawText(
        textMeasurer = textMeasurer,
        topLeft = Offset(safeX, safeY),
        text = text,
        softWrap = false,
        maxLines = 1
      )
    }

    val initialOrigin = Vec2(0.0, 0.0)
    val originVec2 = cordToScreen.apply(initialOrigin)
    val origin = Offset(x = originVec2.x.toFloat(), y = originVec2.y.toFloat())

    // Draw grid lines based on initial origin
    val rawGridSpacing = abs(cordToScreen.apply(Vec2(1.0, 0.0)).x - originVec2.x)
    if (!rawGridSpacing.isFinite() || rawGridSpacing <= 0.0) return@Canvas
    var gridSpacing = rawGridSpacing
    while (canvasWidth / gridSpacing > 100.0) gridSpacing *= 2.0
    val originOffsetX = originVec2.x % gridSpacing
    val originOffsetY = originVec2.y % gridSpacing


    val startX = (-originOffsetX / gridSpacing).toInt()
    val endX = ((canvasWidth - originOffsetX) / gridSpacing).toInt() + 1
    val startY = (-originOffsetY / gridSpacing).toInt()
    val endY = ((canvasHeight - originOffsetY) / gridSpacing).toInt() + 1

    // Draw only visible vertical grid lines
    for (i in startX..endX) {
      val x = (originOffsetX + i * gridSpacing).toFloat()
      if (x >= 0 && x <= canvasWidth) {
        drawLine(
          color = gridColor,
          start = Offset(x, 0f),
          end = Offset(x, canvasHeight),
          strokeWidth = 0.5f
        )
      }
    }
    // Draw only visible horizontal grid lines
    for (i in startY..endY) {
      val y = (originOffsetY + i * gridSpacing).toFloat()
      if (y >= 0 && y <= canvasHeight) {
        drawLine(
          color = gridColor,
          start = Offset(0f, y),
          end = Offset(canvasWidth, y),
          strokeWidth = 0.5f
        )
      }
    }

    // Draw axes
    drawLine(
      color = axisColor,
      start = Offset(origin.x, 0f),
      end = Offset(origin.x, canvasHeight),
      strokeWidth = 2f
    )
    drawLine(
      color = axisColor,
      start = Offset(0f, origin.y),
      end = Offset(canvasWidth, origin.y),
      strokeWidth = 2f
    )

    val screenToCord = cordToScreen.inverse()
    val topLeft = screenToCord.apply(Vec2(0.0, 0.0))
    val bottomRight = screenToCord.apply(Vec2(canvasWidth.toDouble(), canvasHeight.toDouble()))

    val minX = topLeft.x
    val minY = bottomRight.y
    val maxX = bottomRight.x
    val maxY = topLeft.y

    val pointsToDraw = points.filter { (_, it) -> it.x >= minX && it.x <= maxX && it.y >= minY && it.y <= maxY }
      .map { (key, it) ->
        val transformedVec2 = cordToScreen.apply(it)
        key to Offset(transformedVec2.x.toFloat(), transformedVec2.y.toFloat())
      }

    // Scene segments are drawn below points and algorithm overlays.
    segments.values.forEach { segment ->
      val a = cordToScreen.apply(segment.start); val b = cordToScreen.apply(segment.end)
      val selected = segment.id in selectedSegments
      val hovered = segment.id == hoveredSegmentId
      drawLine(if (selected) selectedColor else if (hovered) segmentHoverColor else pointColor,
        Offset(a.x.toFloat(), a.y.toFloat()), Offset(b.x.toFloat(), b.y.toFloat()), strokeWidth = if (selected || hovered) 5f else 3f)
      if (showLabels) drawLabel("S${segment.id}", Offset(((a.x+b.x)/2).toFloat()+4f,((a.y+b.y)/2).toFloat()+4f))
    }
    segmentPreview?.let { (start,end) ->
      val a=cordToScreen.apply(start); val b=cordToScreen.apply(end)
      drawLine(segmentPreviewColor, Offset(a.x.toFloat(),a.y.toFloat()), Offset(b.x.toFloat(),b.y.toFloat()), strokeWidth=3f)
    }

    // Draw regular points
    val regularPoints = pointsToDraw.filter { (key, _) -> !selectedPoints.contains(key) }
    // Circles are used instead of PointMode.Points because the Wasm canvas
    // backend can drop very small point primitives at some zoom levels.
    regularPoints.forEach { (_, position) ->
      drawCircle(pointColor, radius = 4f, center = position)
    }

    // Draw selected points
    val selectedPointsDrawn = pointsToDraw.filter { (key, _) -> selectedPoints.contains(key) }
    selectedPointsDrawn.forEach { (_, position) ->
      drawCircle(selectedColor, radius = 5f, center = position)
    }

    hoveredPointId?.let { id ->
      pointsToDraw.firstOrNull { it.first == id }?.second?.let { position ->
        drawCircle(selectedColor, radius = 10f, center = position, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f))
      }
    }

    // Draw algorithm results
    algorithmResults.values.forEach { result ->
      when (result) {
        is AlgorithmResult.PointPair -> {
          val point1Screen = cordToScreen.apply(result.point1)
          val point2Screen = cordToScreen.apply(result.point2)
          val offset1 = Offset(point1Screen.x.toFloat(), point1Screen.y.toFloat())
          val offset2 = Offset(point2Screen.x.toFloat(), point2Screen.y.toFloat())

          // Draw line between closest pair
          drawLine(
            color = result.color,
            start = offset1,
            end = offset2,
            strokeWidth = 3f
          )

          // Emphasize the solution endpoints as well as the connecting segment.
          drawCircle(result.color, radius = 7f, center = offset1)
          drawCircle(result.color, radius = 7f, center = offset2)

          // Draw distance label at midpoint
          if (showLabels) {
            val midpoint = Offset(
              (offset1.x + offset2.x) / 2,
              (offset1.y + offset2.y) / 2
            )
            val distanceText = "d = ${formatDouble(result.distance)}"
            drawLabel(distanceText, midpoint)
          }
        }
        is AlgorithmResult.Line -> {
          val startScreen = cordToScreen.apply(result.start)
          val endScreen = cordToScreen.apply(result.end)
          drawLine(
            color = result.color,
            start = Offset(startScreen.x.toFloat(), startScreen.y.toFloat()),
            end = Offset(endScreen.x.toFloat(), endScreen.y.toFloat()),
            strokeWidth = 2f
          )
        }
        is AlgorithmResult.Lines -> {
          result.lines.forEach { line ->
            val startScreen = cordToScreen.apply(line.start)
            val endScreen = cordToScreen.apply(line.end)
            drawLine(
              color = line.color,
              start = Offset(startScreen.x.toFloat(), startScreen.y.toFloat()),
              end = Offset(endScreen.x.toFloat(), endScreen.y.toFloat()),
              strokeWidth = 2f
            )
          }
        }
        is AlgorithmResult.Error -> {
          if (showLabels) {
            drawLabel(result.message, Offset(8f, canvasHeight - 28f))
          }
        }
        is AlgorithmResult.Points -> {
          val resultPoints = result.points
            .filter { it.x >= minX && it.x <= maxX && it.y >= minY && it.y <= maxY }
            .map { cordToScreen.apply(it) }
            .map { Offset(it.x.toFloat(), it.y.toFloat()) }

          resultPoints.forEach { position ->
            drawCircle(result.color, radius = 5f, center = position)
          }
        }
        is AlgorithmResult.BentleyOttmannResult -> {
          result.segments.forEach { segment ->
            val start = cordToScreen.apply(segment.start)
            val end = cordToScreen.apply(segment.end)
            drawLine(result.segmentColor, Offset(start.x.toFloat(), start.y.toFloat()), Offset(end.x.toFloat(), end.y.toFloat()), strokeWidth = 3f)
            if (showLabels) {
              val label = cordToScreen.apply(Vec2((segment.start.x + segment.end.x) / 2.0, (segment.start.y + segment.end.y) / 2.0))
              drawLabel("S${segment.id}", Offset(label.x.toFloat() + 4f, label.y.toFloat() + 4f))
            }
          }
          result.intersections.forEach { intersection ->
            when (intersection.kind) {
              AlgorithmResult.IntersectionKind.POINT -> intersection.point?.let {
                val p = cordToScreen.apply(it)
                drawCircle(result.pointColor, 7f, Offset(p.x.toFloat(), p.y.toFloat()))
              }
              AlgorithmResult.IntersectionKind.OVERLAP -> {
                val start = intersection.overlapStart
                val end = intersection.overlapEnd
                if (start != null && end != null) {
                  val a = cordToScreen.apply(start); val b = cordToScreen.apply(end)
                  drawLine(result.overlapColor, Offset(a.x.toFloat(), a.y.toFloat()), Offset(b.x.toFloat(), b.y.toFloat()), strokeWidth = 7f)
                  if (showLabels) drawLabel("overlap S${intersection.firstSegment}/S${intersection.secondSegment}", Offset(a.x.toFloat() + 5f, a.y.toFloat() + 5f))
                }
              }
            }
          }
        }
      }
    }

    if (!showLabels) return@Canvas

    pointsToDraw.forEach { (key, offset) ->
      val point = points[key]!!
      val xCord = formatDouble(point.x)
      val yCord = formatDouble(point.y)
      val text = "P$key ($xCord, $yCord)"


      drawLabel(text, offset + Offset(4f, 4f))
    }
  }
}