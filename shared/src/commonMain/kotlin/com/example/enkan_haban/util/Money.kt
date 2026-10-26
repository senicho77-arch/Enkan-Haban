package com.example.enkan_haban.util

import kotlin.math.abs
import kotlin.math.roundToLong

/** Currency shown across the app. Change in one place when the target market is final. */
const val CURRENCY_CODE = "ETB"

/** Formats minor units as e.g. "ETB 1,250" or "ETB 1,250.50". */
fun formatPrice(minorUnits: Long, currency: String = CURRENCY_CODE): String {
    val sign = if (minorUnits < 0) "-" else ""
    val absolute = abs(minorUnits)
    val major = (absolute / 100).toString().reversed().chunked(3).joinToString(",").reversed()
    val minor = absolute % 100
    val fraction = if (minor == 0L) "" else "." + minor.toString().padStart(2, '0')
    return "$sign$currency $major$fraction"
}

/** Parses user input like "1250" or "1,250.5" into minor units, or null if invalid. */
fun parsePrice(input: String): Long? {
    val value = input.replace(",", "").trim().toDoubleOrNull() ?: return null
    if (value < 0 || value.isNaN() || value.isInfinite()) return null
    return (value * 100).roundToLong()
}
