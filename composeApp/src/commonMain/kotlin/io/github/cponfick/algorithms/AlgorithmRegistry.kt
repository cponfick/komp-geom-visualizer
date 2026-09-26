package io.github.cponfick.algorithms

import io.github.cponfick.state.*

object AlgorithmRegistry {
  private val descriptors = mutableListOf<AlgorithmDescriptor>()
  init {
    register(AlgorithmDescriptor("closest-pair-naive", "Closest Pair (naive)", AlgorithmCategory.DISTANCES, "Find the closest pair by checking every pair.", AlgorithmInput.POINTS, 2, setOf("points", "distance")) { PointAdapter(ClosestPairNaive()) })
    register(AlgorithmDescriptor("closest-pair-divide", "Closest Pair (divide and conquer)", AlgorithmCategory.DISTANCES, "Find the closest pair efficiently.", AlgorithmInput.POINTS, 2, setOf("points", "distance")) { PointAdapter(ClosestPairDivideAndConquer()) })
    register(AlgorithmDescriptor("quick-hull", "Quick Hull", AlgorithmCategory.HULLS, "Compute the convex hull of selected points.", AlgorithmInput.POINTS, 3, setOf("points", "hull")) { PointAdapter(QuickHull()) })
    register(AlgorithmDescriptor("bentley-ottmann", "Segment Intersections – Bentley–Ottmann", AlgorithmCategory.INTERSECTIONS, "Find all intersections among selected segments.", AlgorithmInput.SEGMENTS, 2, setOf("segments", "intersection", "overlap")) { BentleyOttmannAlgorithm() })
  }
  private fun register(descriptor: AlgorithmDescriptor) { require(descriptors.none { it.id == descriptor.id }) { "Algorithm id '${descriptor.id}' is already registered" }; descriptors += descriptor }
  fun all(): List<AlgorithmDescriptor> = descriptors.toList()
  fun byCategory(): Map<AlgorithmCategory,List<AlgorithmDescriptor>> = descriptors.groupBy { it.category }
  fun find(id:String):AlgorithmDescriptor? = descriptors.find { it.id==id }
  // Compatibility API for non-UI consumers.
  fun getAllAlgorithms(): List<GeometricAlgorithm> = descriptors.mapNotNull { it.factory() as? GeometricAlgorithm }
  fun getAlgorithmByName(name:String):GeometricAlgorithm? = getAllAlgorithms().find { it.getName()==name }
  private class PointAdapter(private val delegate:GeometricAlgorithm):GeometryAlgorithm {
    override fun execute(points:GeometrySelection.Points,segments:GeometrySelection.Segments)=delegate.execute(points.points.map { it.position })
  }
}
