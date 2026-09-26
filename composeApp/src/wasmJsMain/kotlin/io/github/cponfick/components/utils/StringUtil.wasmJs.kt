package io.github.cponfick.components.utils

import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.round

actual fun formatDouble(value: Double, decimalPlaces: Int): String {
  if (!value.isFinite()) return value.toString()
  val places = decimalPlaces.coerceAtLeast(0)
  val scale = 10.0.pow(places)
  val rounded = round(value * scale) / scale
  val raw = abs(rounded).toString().lowercase()
  // Expand exponent notation before padding, keeping the policy identical to JVM.
  val expanded = if ('e' in raw) expandExponent(raw) else raw
  val parts = expanded.split('.')
  val fraction = (parts.getOrNull(1) ?: "").padEnd(places, '0').take(places)
  return (if (rounded < 0) "-" else "") + parts[0] + if (places > 0) "." + fraction else ""
}

private fun expandExponent(value: String): String {
  val pieces = value.split('e')
  val digits = pieces[0].replace(".", "")
  val decimalIndex = (pieces[0].substringBefore('.').length + pieces[1].toInt())
  return when {
    decimalIndex <= 0 -> "0." + "0".repeat(-decimalIndex) + digits
    decimalIndex >= digits.length -> digits + "0".repeat(decimalIndex - digits.length)
    else -> digits.substring(0, decimalIndex) + "." + digits.substring(decimalIndex)
  }
}
