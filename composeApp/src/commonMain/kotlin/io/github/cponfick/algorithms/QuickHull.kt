package io.github.cponfick.algorithms

import io.github.cponfick.kompgeom.euclidean.twod.Vec2
import io.github.cponfick.state.AlgorithmResult
import io.github.cponfick.state.GeometricAlgorithm

class QuickHull : GeometricAlgorithm {
  override fun getName(): String = "Quick Hull"

  override fun getMinimumPoints(): Int = 3

  override fun execute(points: List<Vec2>): AlgorithmResult {
    require(points.size >= 3) { "At least 3 points are required for Quick Hull algorithm" }

    val algorithm = io.github.cponfick.kompgeom.algorithms.convexhull.Quickhull2(points)
    val result = algorithm.execute()

    val centroid = Vec2(
      x = result.points.map { it.x }.average(),
      y = result.points.map { it.y }.average()
    )
    val sorted = result.points.sortedBy { point ->
      kotlin.math.atan2(point.y - centroid.y, point.x - centroid.x)
    }

    return AlgorithmResult.Lines(
      lines = sorted.zip(sorted.drop(1) + sorted.first()).map { (start, end) ->
        AlgorithmResult.Line(start = start, end = end)
      }
    )
  }
}