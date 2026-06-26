package com.clatos.dialer.core.common

/**
 * Lightweight phone-number normalization used to dedupe/match contacts and
 * call-log numbers across the device address book and the CRM.
 *
 * For production, prefer libphonenumber for robust E.164 parsing with a
 * configured default region. This keeps the scaffold dependency-free.
 */
object PhoneNumberUtils {

    /** Strips formatting; keeps a leading '+' and digits only. */
    fun normalize(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        val trimmed = raw.trim()
        val hasPlus = trimmed.startsWith("+")
        val digits = trimmed.filter { it.isDigit() }
        return if (hasPlus) "+$digits" else digits
    }

    /** True when two numbers refer to the same line (compares trailing significant digits). */
    fun sameNumber(a: String?, b: String?): Boolean {
        val na = normalize(a).takeLast(SIGNIFICANT_DIGITS)
        val nb = normalize(b).takeLast(SIGNIFICANT_DIGITS)
        return na.isNotEmpty() && na == nb
    }

    private const val SIGNIFICANT_DIGITS = 9
}
