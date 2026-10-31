package com.example.enkan_haban.util

/**
 * Helpers for contacting a seller straight from an ad (Jiji-style): the number is shown
 * on the ad, one tap opens the phone's dialer, another opens WhatsApp.
 */

/** "tel:" link that opens the dialer with the number filled in. No permission needed. */
fun telUri(phone: String): String = "tel:" + phone.filter { it.isDigit() || it == '+' }

/**
 * WhatsApp link (wa.me) for the number, or null when it isn't in international form.
 * WhatsApp needs the country code, so "+256 752 556 677" works but "0752 556 677" doesn't —
 * we can't guess the country, because sellers are in Uganda, Ethiopia, Europe, Canada, …
 */
fun whatsappUri(phone: String): String? {
    val trimmed = phone.trim()
    val digits = trimmed.filter { it.isDigit() }
    val international = trimmed.startsWith("+") || trimmed.startsWith("00")
    if (!international) return null
    val number = if (trimmed.startsWith("00")) digits.drop(2) else digits
    return if (number.length in 8..15) "https://wa.me/$number" else null
}
