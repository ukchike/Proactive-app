package com.unifiedproductivity.app.ui.settings

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unifiedproductivity.app.sync.DriveSyncManager
import com.unifiedproductivity.app.ui.theme.ThemeMode
import com.unifiedproductivity.app.ui.theme.ThemePreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val drive: DriveSyncManager,
    private val themePreferences: ThemePreferences
) : ViewModel() {

    val accountEmail: StateFlow<String?> = drive.accountEmail
    val status: StateFlow<String?> = drive.status
    val themeMode: StateFlow<ThemeMode> = themePreferences.mode

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    fun setThemeMode(mode: ThemeMode) = themePreferences.setMode(mode)

    fun signInIntent(): Intent = drive.signInIntent()

    fun onSignInResult(data: Intent?) = drive.handleSignInResult(data)

    fun signOut() = drive.signOut()

    fun syncNow() = run { drive.syncNow() }

    fun backupNow() = run { drive.backupNow() }

    fun restore() = run { drive.restore() }

    /** Run a suspending Drive action while flipping the [busy] flag for the UI. */
    private fun run(action: suspend () -> Boolean) {
        if (_busy.value) return
        viewModelScope.launch {
            _busy.value = true
            try {
                action()
            } finally {
                _busy.value = false
            }
        }
    }
}
