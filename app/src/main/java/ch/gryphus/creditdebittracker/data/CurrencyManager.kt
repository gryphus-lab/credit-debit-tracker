package ch.gryphus.creditdebittracker.data

import android.content.Context
import androidx.core.content.edit

/**
 * Persists the user's chosen display currency in SharedPreferences.
 *
 * Extends the original set with **CHF (Swiss Franc)**, which is also the
 * default - the app is used in Switzerland.
 */
object CurrencyManager {
    private const val PREFS = "currency_prefs"
    private const val KEY_CODE = "currency_code"
    private const val DEFAULT_CODE = "CHF"

    data class Currency(val code: String, val symbol: String, val name: String)

    val supported = listOf(
        Currency("CHF", "CHF ", "Swiss Franc"),
        Currency("EUR", "€", "Euro"),
        Currency("USD", "$", "US Dollar"),
        Currency("GBP", "£", "British Pound"),
        Currency("INR", "₹", "Indian Rupee"),
        Currency("JPY", "¥", "Japanese Yen"),
        Currency("AUD", "A$", "Australian Dollar"),
        Currency("CAD", "C$", "Canadian Dollar"),
    )

    fun current(context: Context): Currency {
        val code = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_CODE, DEFAULT_CODE) ?: DEFAULT_CODE
        return supported.find { it.code == code } ?: supported.first()
    }

    fun symbol(context: Context): String = current(context).symbol

    fun setCurrency(context: Context, currency: Currency) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit { putString(KEY_CODE, currency.code) }
    }
}
