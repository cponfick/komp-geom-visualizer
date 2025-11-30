package io.github.cponfick.algorithms

import io.github.cponfick.state.GeometricAlgorithm

object AlgorithmRegistry {
  private val algorithms = mutableListOf<GeometricAlgorithm>()

  init {
    registerAlgorithm(ClosestPairNaive())
    registerAlgorithm(ClosestPairDivideAndConquer())
    registerAlgorithm(QuickHull())
  }

  fun registerAlgorithm(algorithm: GeometricAlgorithm) {
    algorithms.add(algorithm)
  }

  fun getAllAlgorithms(): List<GeometricAlgorithm> = algorithms.toList()

  fun getAlgorithmByName(name: String): GeometricAlgorithm? {
    return algorithms.find { it.getName() == name }
  }
}
