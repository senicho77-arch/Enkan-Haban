package com.example.enkan_haban.util

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Currencies the app can show prices in.
 *
 * All prices are stored once, in US cents, and converted only for display. That keeps
 * one source of truth: a seller in Kampala and a buyer in Canada see the same item,
 * each in the currency they picked.
 *
 * [unitsPerUsd] is a fixed rate for now (≈ 3,670 UGX per USD, October 2026).
 * Before real payments go live, load it daily from an exchange-rate API.
 */
enum class Currency(
    val code: String,
    /** Prefix shown before the amount. */
    val symbol: String,
    /** Tigrinya name for the currency picker. */
    val localName: String,
    val unitsPerUsd: Double,
    /** How many decimals to show; the shilling has no coins in use. */
    val decimals: Int,
    /** Rounds converted amounts to tidy prices (UGX 49,985 → UGX 50,000). */
    val roundTo: Long,
) {
    UGX(code = "UGX", symbol = "UGX ", localName = "ሽሊንግ ዩጋንዳ", unitsPerUsd = 3_670.0, decimals = 0, roundTo = 100),
    USD(code = "USD", symbol = "$", localName = "ዶላር", unitsPerUsd = 1.0, decimals = 2, roundTo = 1),
}

/**
 * The currency the user picked. Backed by Compose state, so every price on screen
 * updates instantly when it changes. Shilling is the default for the Kampala launch.
 */
object CurrencySettings {
    var current: Currency by mutableStateOf(Currency.UGX)
}

/** Converts a stored USD-cent amount to the smallest unit shown for [currency]. */
fun convertFromUsdCents(usdCents: Long, currency: Currency): Long = when (currency) {
    Currency.USD -> usdCents
    else -> {
        val raw = (usdCents / 100.0 * currency.unitsPerUsd).roundToLong()
        if (currency.roundTo > 1) ((raw + currency.roundTo / 2) / currency.roundTo) * currency.roundTo else raw
    }
}

/**
 * Formats a price stored in US cents, e.g. "UGX 1,395,000" or "$380" / "$12.05".
 * Reads [CurrencySettings.current] by default, so composables recompose on a switch.
 */
fun formatPrice(usdCents: Long, currency: Currency = CurrencySettings.current): String =
    formatConverted(convertFromUsdCents(usdCents, currency), currency)

/**
 * Formats a cart total as the sum of each line's displayed price, so the subtotal always
 * matches what the shopper adds up on screen (rounding per item, not on the total).
 * Each pair is (unit price in US cents, quantity).
 */
fun formatTotal(lines: List<Pair<Long, Int>>, currency: Currency = CurrencySettings.current): String =
    formatConverted(lines.sumOf { (usdCents, quantity) -> convertFromUsdCents(usdCents, currency) * quantity }, currency)

/** Formats an amount already in [currency]'s smallest shown unit (cents for USD, shillings for UGX). */
private fun formatConverted(units: Long, currency: Currency): String {
    val sign = if (units < 0) "-" else ""
    val amount = abs(units)
    return if (currency.decimals == 2) {
        val minor = amount % 100
        val fraction = if (minor == 0L) "" else "." + minor.toString().padStart(2, '0')
        "$sign${currency.symbol}${groupThousands(amount / 100)}$fraction"
    } else {
        "$sign${currency.symbol}${groupThousands(amount)}"
    }
}

/**
 * Parses what a seller typed, in [currency], into US cents for storage.
 * "50,000" in UGX → 1362 cents; "12.5" in USD → 1250 cents. Null if invalid.
 */
fun parsePrice(input: String, currency: Currency = CurrencySettings.current): Long? {
    val value = input.replace(",", "").trim().toDoubleOrNull() ?: return null
    if (value < 0 || value.isNaN() || value.isInfinite()) return null
    return (value / currency.unitsPerUsd * 100).roundToLong()
}

private fun groupThousands(value: Long): String =
    value.toString().reversed().chunked(3).joinToString(",").reversed()
