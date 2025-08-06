package io.github.cponfick.components.utils

actual fun formatDouble(value: Double, decimalPlaces: Int): String {
  // TODO: Is this the really how to format a double in JS?
  return value.toJsNumber().toString().substringBeforeLast('.').let { integerPart ->
    val decimalPart = value.toJsNumber().toString().substringAfterLast('.')
    if (decimalPart.length > decimalPlaces) {
      "${integerPart}.${decimalPart.take(decimalPlaces)}"
    } else {
      "${integerPart}.${decimalPart.padEnd(decimalPlaces, '0')}"
    }
  }
}
