package ch.gryphus.creditdebittracker.data

import android.content.Context
import androidx.core.content.edit

/** User's chosen theme: follow the system, or force light / dark. */
enum class ThemeMode(val label: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark"),
    ;

    companion object {
        /** Parse a stored name back to a mode, defaulting to [SYSTEM]. */
        fun fromName(name: String?): ThemeMode =
            entries.firstOrNull { it.name == name } ?: SYSTEM
    }
}

/** Persists the chosen [ThemeMode] in SharedPreferences. */
object ThemePreference {
    private const val PREFS = "theme_prefs"
    private const val KEY_MODE = "theme_mode"

    fun current(context: Context): ThemeMode {
        val name = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_MODE, null)
        return ThemeMode.fromName(name)
    }

    fun setMode(context: Context, mode: ThemeMode) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit { putString(KEY_MODE, mode.name) }
    }
}
