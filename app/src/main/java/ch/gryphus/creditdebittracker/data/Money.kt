package ch.gryphus.creditdebittracker.data

import java.util.Locale

/**
 * Pure, framework-free money helpers. Amounts are stored as integer **cents**
 * (rappen for CHF) to avoid floating-point rounding errors; all parsing and
 * formatting lives here so it can be unit-tested without Android.
 */
object Money {

    /** Format [cents] as a currency string, e.g. 123456 -> "CHF 1234.56". */
    fun format(cents: Long, symbol: String): String {
        val negative = cents < 0
        val abs = kotlin.math.abs(cents)
        val whole = abs / 100
        val frac = (abs % 100).toInt()
        val body = String.format(Locale.US, "%,d.%02d", whole, frac)
        return if (negative) "-$symbol$body" else "$symbol$body"
    }

    /**
     * Parse a user-typed amount (e.g. "12", "12.5", "1,234.50") into cents.
     * Returns null if the input isn't a valid non-negative amount. Thousands
     * separators are tolerated; at most two fractional digits are kept.
     */
    fun parseToCents(input: String?): Long? {
        if (input.isNullOrBlank()) return null
        val cleaned = input.trim().replace(",", "")
        if (!Regex("""^\d+(\.\d{0,2})?$""").matches(cleaned)) return null
        val parts = cleaned.split(".")
        val whole = parts[0].toLongOrNull() ?: return null
        val frac = when {
            parts.size == 1 -> 0
            else -> parts[1].padEnd(2, '0').take(2).toIntOrNull() ?: return null
        }
        return whole * 100 + frac
    }
}
