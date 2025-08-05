package io.github.cponfick.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import io.github.cponfick.kompgeom.euclidean.twod.AffineTransformationMatrix2
import io.github.cponfick.kompgeom.euclidean.twod.Vec2
import kotlinx.coroutines.delay
import kotlin.math.abs

@Composable
fun BaseCanvas(
  cordToScreen: AffineTransformationMatrix2,
  points: Map<Int, Vec2> = emptyMap(),
  canvasModifier: Modifier
) {
  val textMeasurer = rememberTextMeasurer()

  // FPS tracking state
  var frameCount by remember { mutableStateOf(0) }
  var fps by remember { mutableStateOf(0.0) }
  var fpsRate by remember { mutableStateOf(1000L) }

  LaunchedEffect(fpsRate) {
    while (true) {
      frameCount = 0
      delay(fpsRate)
      fps = frameCount.toDouble()
    }
  }


  Canvas(modifier = canvasModifier.fillMaxSize().graphicsLayer()) {
    // Increment frame count on each draw
    frameCount += 1

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
    val numberOfLinesToDraw = canvasWidth / gridSpacing
    val originOffsetX = originVec2.x % gridSpacing
    val originOffsetY = originVec2.y % gridSpacing

    for (i in 0..numberOfLinesToDraw.toInt()) {
      val x = (originOffsetX + i * gridSpacing).toFloat()
      drawLine(
        color = Color.LightGray,
        start = Offset(x, 0f),
        end = Offset(x, canvasHeight),
        strokeWidth = 0.5f
      )
    }
    for (i in 0..numberOfLinesToDraw.toInt()) {
      val y = (originOffsetY + i * gridSpacing).toFloat()
      drawLine(
        color = Color.LightGray,
        start = Offset(0f, y),
        end = Offset(canvasWidth, y),
        strokeWidth = 0.5f
      )
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


    // Draw points if any
    points.forEach { (key, point) ->
      val screenCoordinate = cordToScreen.apply(point)
      val screenX = screenCoordinate.x.toFloat()
      val screenY = screenCoordinate.y.toFloat()



      drawCircle(
        color = Color.Black,
        radius = 3f,
        center = Offset(screenX, screenY)
      )

      // only two decimal places for coordinates
      val xCord = formatDouble(point.x)
      val yCord = formatDouble(point.y)
      val text = "P$key ($xCord, $yCord)"


      val textLayoutResult = textMeasurer.measure(
        text = text,
        maxLines = 1,
        softWrap = false
      )

      val textX = screenX - textLayoutResult.size.width / 2
      val textY = screenY + 3f
      val offset = Offset(textX, textY)

      if (offset.x < 0 || offset.x > canvasWidth || offset.y < 0 || offset.y > canvasHeight) {
        return@forEach
      }

      drawText(
        textMeasurer = textMeasurer,
        topLeft = offset,
        text = text,
        softWrap = false,
        maxLines = 1
      )

    }

    // Draw FPS in bottom right corner
    val fpsText = "FPS: ${formatDouble(fps, 1)}"
    val fpsTextStyle = TextStyle(
      color = Color.Black,
      fontSize = 14.sp
    )

    val fpsTextLayoutResult = textMeasurer.measure(
      text = fpsText,
      style = fpsTextStyle,
      maxLines = 1,
      softWrap = false
    )

    val fpsTextX = canvasWidth - fpsTextLayoutResult.size.width - 10f
    val fpsTextY = canvasHeight - fpsTextLayoutResult.size.height - 10f

    drawText(
      textMeasurer = textMeasurer,
      text = fpsText,
      style = fpsTextStyle,
      topLeft = Offset(fpsTextX, fpsTextY),
      maxLines = 1,
      softWrap = false
    )
  }
}

expect fun formatDouble(value: Double, decimalPlaces: Int = 2): String