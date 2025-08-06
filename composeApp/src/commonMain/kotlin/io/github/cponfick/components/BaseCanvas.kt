package io.github.cponfick.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import io.github.cponfick.components.utils.formatDouble
import io.github.cponfick.kompgeom.euclidean.twod.AffineTransformationMatrix2
import io.github.cponfick.kompgeom.euclidean.twod.Vec2
import kotlin.math.abs
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@Composable
fun BaseCanvas(
  cordToScreen: AffineTransformationMatrix2,
  showLabels: Boolean = true,
  points: Map<Int, Vec2> = emptyMap(),
  canvasModifier: Modifier
) {
  val textMeasurer = rememberTextMeasurer()

  Canvas(modifier = canvasModifier.fillMaxSize().graphicsLayer()) {
    println("Drawing canvas at ${Clock.System.now()}")
    val canvasWidth = size.width
    val canvasHeight = size.height

    // Early return if canvas dimensions are invalid
    if (canvasWidth <= 0 || canvasHeight <= 0) {
      return@Canvas
    }

    val initialOrigin = Vec2(0.0, 0.0)
    val originVec2 = cordToScreen.apply(initialOrigin)
    val origin = Offset(x = originVec2.x.toFloat(), y = originVec2.y.toFloat())

    // Draw grid lines based on initial origin
    val gridSpacing = abs(cordToScreen.apply(Vec2(1.0, 0.0)).x - originVec2.x)
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
          color = Color.LightGray,
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
          color = Color.LightGray,
          start = Offset(0f, y),
          end = Offset(canvasWidth, y),
          strokeWidth = 0.5f
        )
      }
    }

    // Draw axes
    drawLine(
      color = Color.Black,
      start = Offset(origin.x, 0f),
      end = Offset(origin.x, canvasHeight),
      strokeWidth = 2f
    )
    drawLine(
      color = Color.Black,
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

    val pointsToDraw = points.filter { (_, it) -> it.x > minX && it.x < maxX && it.y > minY && it.y < maxY }
      .map { (key, it) ->
        val transformedVec2 = cordToScreen.apply(it)
        key to Offset(transformedVec2.x.toFloat(), transformedVec2.y.toFloat())
      }

    drawPoints(
      points = pointsToDraw.map { it.second },
      pointMode = PointMode.Points,
      color = Color.Black,
      strokeWidth = 3f
    )

    if (!showLabels) return@Canvas

    pointsToDraw.forEach { (key, offset) ->
      val point = points[key]!!
      val xCord = formatDouble(point.x)
      val yCord = formatDouble(point.y)
      val text = "P$key ($xCord, $yCord)"


      drawText(
        textMeasurer = textMeasurer,
        topLeft = offset,
        text = text,
        softWrap = false,
        maxLines = 1
      )
    }
  }
}