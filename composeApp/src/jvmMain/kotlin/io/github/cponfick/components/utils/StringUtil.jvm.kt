package io.github.cponfick.components.utils

actual fun formatDouble(value: Double, decimalPlaces: Int): String = "%.${decimalPlaces}f".format(value)