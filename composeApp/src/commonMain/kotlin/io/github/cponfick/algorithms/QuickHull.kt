package io.github.cponfick.algorithms

import io.github.cponfick.kompgeom.euclidean.twod.Vec2
import io.github.cponfick.state.AlgorithmResult
import io.github.cponfick.state.GeometricAlgorithm

class QuickHull : GeometricAlgorithm {
  override fun getName(): String = "Quick Hull"

  override fun getMinimumPoints(): Int = 3

  override fun execute(points: List<Vec2>): AlgorithmResult {
    require(points.size >= 3) { "At least 3 points are required for Quick Hull algorithm" }

    val distinct = points.distinctBy { it.x to it.y }
    if (distinct.size < 3) return AlgorithmResult.Lines(emptyList())
    val algorithm = io.github.cponfick.kompgeom.algorithms.convexhull.Quickhull2(distinct)
    val hull = algorithm.execute().points.distinctBy { it.x to it.y }
    if (hull.size < 2) return AlgorithmResult.Lines(emptyList())
    if (hull.size == 2) return AlgorithmResult.Lines(listOf(AlgorithmResult.Line(hull[0], hull[1])))
    val centroid = Vec2(hull.map { it.x }.average(), hull.map { it.y }.average())
    val sorted = hull.sortedBy { kotlin.math.atan2(it.y - centroid.y, it.x - centroid.x) }
    return AlgorithmResult.Lines(sorted.zip(sorted.drop(1) + sorted.first()).map { (start, end) ->
      AlgorithmResult.Line(start, end)
    })
  }
}