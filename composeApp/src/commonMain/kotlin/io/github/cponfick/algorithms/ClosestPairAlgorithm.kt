package io.github.cponfick.algorithms

import io.github.cponfick.kompgeom.algorithms.closestpair.ClosestPairDivideAndConquer
import io.github.cponfick.kompgeom.euclidean.twod.Vec2
import io.github.cponfick.state.AlgorithmResult
import io.github.cponfick.state.GeometryAlgorithm

class ClosestPairAlgorithm : GeometryAlgorithm {
  override fun getName(): String = "Closest Pair (Divide & Conquer)"
  
  override fun getMinimumPoints(): Int = 2
  
  override fun execute(points: List<Vec2>): AlgorithmResult {
    require(points.size >= 2) { "At least 2 points are required for closest pair algorithm" }
    
    val algorithm = ClosestPairDivideAndConquer(points)
    val result = algorithm.run()
    
    return AlgorithmResult.PointPair(
      point1 = result.result.first,
      point2 = result.result.second,
      distance = result.distance
    )
  }
}
