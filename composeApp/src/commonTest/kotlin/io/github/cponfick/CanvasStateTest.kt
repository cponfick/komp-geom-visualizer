package io.github.cponfick

import io.github.cponfick.components.utils.formatDouble
import kotlin.test.Test
import kotlin.test.assertEquals

class CanvasStateTest {
  @Test
  fun formattingUsesFixedPrecision() {
    assertEquals("1.00", formatDouble(1.0))
    assertEquals("-1.25", formatDouble(-1.25))
    assertEquals("2", formatDouble(1.6, 0))
  }

  @Test
  fun formattingHandlesSmallAndLargeValues() {
    assertEquals("0.00", formatDouble(1.2e-4))
    assertEquals("1000000.00", formatDouble(1.0e6))
  }
}
