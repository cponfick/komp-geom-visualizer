package io.github.cponfick.components

actual fun formatDouble(value: Double, decimalPlaces: Int): String = "%.${decimalPlaces}f".format(value)