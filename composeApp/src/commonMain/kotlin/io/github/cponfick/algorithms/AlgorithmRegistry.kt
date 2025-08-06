package io.github.cponfick.algorithms

import io.github.cponfick.state.GeometryAlgorithm

object AlgorithmRegistry {
  private val algorithms = mutableListOf<GeometryAlgorithm>()
  
  init {
    registerAlgorithm(ClosestPairNaive())
    registerAlgorithm(ClosestPairDivideAndConquer())
  }
  
  fun registerAlgorithm(algorithm: GeometryAlgorithm) {
    algorithms.add(algorithm)
  }
  
  fun getAllAlgorithms(): List<GeometryAlgorithm> = algorithms.toList()
  
  fun getAlgorithmByName(name: String): GeometryAlgorithm? {
    return algorithms.find { it.getName() == name }
  }
}
