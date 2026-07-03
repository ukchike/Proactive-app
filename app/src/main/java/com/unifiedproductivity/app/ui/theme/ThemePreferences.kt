package com.unifiedproductivity.app.ui.theme

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** User-selectable appearance: follow the system, or force light/dark. */
enum class ThemeMode(val label: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark")
}

/** Persists the chosen [ThemeMode] and exposes it reactively for recomposition. */
class ThemePreferences(context: Context) {

    private val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    private val _mode = MutableStateFlow(load())
    val mode: StateFlow<ThemeMode> = _mode.asStateFlow()

    fun setMode(mode: ThemeMode) {
        prefs.edit().putString(KEY, mode.name).apply()
        _mode.value = mode
    }

    private fun load(): ThemeMode =
        prefs.getString(KEY, null)?.let { saved ->
            ThemeMode.entries.firstOrNull { it.name == saved }
        } ?: ThemeMode.SYSTEM

    private companion object {
        const val KEY = "theme_mode"
    }
}
