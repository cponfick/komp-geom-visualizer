package io.github.cponfick.algorithms

import io.github.cponfick.kompgeom.algorithms.closestpair.ClosestPairDivideAndConquer
import io.github.cponfick.kompgeom.euclidean.twod.Vec2
import io.github.cponfick.state.AlgorithmResult
import io.github.cponfick.state.GeometryAlgorithm

class ClosestPairDivideAndConquer : GeometryAlgorithm {
  override fun getName(): String = "Closest Pair - Divide & Conquer"
  
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

class ClosestPairNaive : GeometryAlgorithm {
  override fun getName(): String = "Closest Pair - Naive"

  override fun getMinimumPoints(): Int = 2

  override fun execute(points: List<Vec2>): AlgorithmResult {
    require(points.size >= 2) { "At least 2 points are required for closest pair algorithm" }

    val algorithm = io.github.cponfick.kompgeom.algorithms.closestpair.ClosestPairNaive(points)
    val result = algorithm.run()

    return AlgorithmResult.PointPair(
      point1 = result.result.first,
      point2 = result.result.second,
      distance = result.distance
    )
  }
}

