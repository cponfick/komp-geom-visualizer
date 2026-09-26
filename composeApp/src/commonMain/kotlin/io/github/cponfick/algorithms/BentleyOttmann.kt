package io.github.cponfick.algorithms

import io.github.cponfick.kompgeom.algorithms.intersection.BentleyOttmann
import io.github.cponfick.kompgeom.core.shapes.IntersectionType
import io.github.cponfick.kompgeom.euclidean.twod.Seg2
import io.github.cponfick.kompgeom.euclidean.twod.Vec2
import io.github.cponfick.state.*

class BentleyOttmannAlgorithm : GeometricAlgorithm, GeometryAlgorithm {
  override fun getName() = "Segment Intersections – Bentley–Ottmann"
  override fun getMinimumPoints() = 4

  override fun execute(points: List<Vec2>): AlgorithmResult {
    require(points.size >= 4) { "Bentley–Ottmann requires at least two segments." }
    require(points.size % 2 == 0) { "Legacy point input requires an even number of points." }
    return executeGeometry(points.chunked(2).mapIndexed { i, p -> IdentifiedSegment(i, p[0], p[1]) })
  }
  override fun execute(points: GeometrySelection.Points, segments: GeometrySelection.Segments): AlgorithmResult = executeGeometry(segments.segments)

  private fun executeGeometry(input: List<IdentifiedSegment>): AlgorithmResult {
    require(input.size >= 2) { "Bentley–Ottmann requires at least two selected segments." }
    input.forEach { require(it.start != it.end) { "Segment ${it.id} has identical endpoints." } }
    val geometry = BentleyOttmann(input.map { Seg2(it.start, it.end) }).execute()
    val result = geometry.map { item ->
      val data = item.intersection
      when (data.type) {
        IntersectionType.POINT -> AlgorithmResult.SegmentIntersectionResult(input[item.first].id, input[item.second].id, AlgorithmResult.IntersectionKind.POINT, point = requireNotNull(data.point))
        IntersectionType.OVERLAP -> { val overlap=requireNotNull(data.segment); AlgorithmResult.SegmentIntersectionResult(input[item.first].id,input[item.second].id,AlgorithmResult.IntersectionKind.OVERLAP,overlapStart=overlap.first,overlapEnd=overlap.second) }
        IntersectionType.NONE -> error("Bentley–Ottmann returned an empty intersection.")
      }
    }
    return AlgorithmResult.BentleyOttmannResult(input.map { AlgorithmResult.SegmentOverlay(it.id,it.start,it.end) },result)
  }
}
