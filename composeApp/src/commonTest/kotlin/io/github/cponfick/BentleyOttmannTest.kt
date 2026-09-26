package io.github.cponfick

import io.github.cponfick.algorithms.BentleyOttmannAlgorithm
import io.github.cponfick.kompgeom.euclidean.twod.Vec2
import io.github.cponfick.state.AlgorithmResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BentleyOttmannTest {
  private val algorithm = BentleyOttmannAlgorithm()

  @Test
  fun crossingAndNonCrossingInputsAreConverted() {
    val crossing = algorithm.execute(listOf(Vec2(-1.0, -1.0), Vec2(1.0, 1.0), Vec2(-1.0, 1.0), Vec2(1.0, -1.0))) as AlgorithmResult.BentleyOttmannResult
    assertEquals(2, crossing.segments.size)
    assertEquals(1, crossing.intersections.size)
    assertEquals(AlgorithmResult.IntersectionKind.POINT, crossing.intersections.single().kind)
    assertEquals(Vec2(0.0, 0.0), crossing.intersections.single().point)

    val separate = algorithm.execute(listOf(Vec2(0.0, 0.0), Vec2(1.0, 0.0), Vec2(0.0, 1.0), Vec2(1.0, 1.0))) as AlgorithmResult.BentleyOttmannResult
    assertEquals(0, separate.intersections.size)
  }

  @Test
  fun overlapAndValidationAreReported() {
    val overlap = algorithm.execute(listOf(Vec2(0.0, 0.0), Vec2(4.0, 0.0), Vec2(2.0, 0.0), Vec2(6.0, 0.0))) as AlgorithmResult.BentleyOttmannResult
    assertEquals(AlgorithmResult.IntersectionKind.OVERLAP, overlap.intersections.single().kind)
    assertEquals(Vec2(2.0, 0.0), overlap.intersections.single().overlapStart)
    assertEquals(Vec2(4.0, 0.0), overlap.intersections.single().overlapEnd)
    assertFailsWith<IllegalArgumentException> { algorithm.execute(List(5) { Vec2(it.toDouble(), 0.0) }) }
    assertFailsWith<IllegalArgumentException> { algorithm.execute(listOf(Vec2(0.0, 0.0), Vec2(0.0, 0.0), Vec2(1.0, 0.0), Vec2(2.0, 0.0))) }
  }
}
