package com.unifiedproductivity.app.sync

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.unifiedproductivity.app.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Orchestrates Google Drive sync: Google Sign-In for a `drive.appdata` OAuth token,
 * then push/pull of the JSON snapshots with last-write-wins merging.
 *
 * Setup note: this needs an OAuth client registered in a Google Cloud project (the
 * app's package name + signing SHA-1, with the Drive API enabled). No secret is
 * committed — Google Sign-In uses the signing certificate, not a client secret.
 */
class DriveSyncManager(
    private val context: Context,
    db: AppDatabase
) {
    private val backupManager = BackupManager(db)
    private val driveClient = DriveClient()

    private val appDataScope = Scope(DRIVE_APPDATA_SCOPE)

    private val signInOptions: GoogleSignInOptions =
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(appDataScope)
            .build()

    private val signInClient: GoogleSignInClient = GoogleSignIn.getClient(context, signInOptions)

    private val _accountEmail = MutableStateFlow(GoogleSignIn.getLastSignedInAccount(context)?.email)
    val accountEmail: StateFlow<String?> = _accountEmail.asStateFlow()

    private val _status = MutableStateFlow<String?>(null)
    val status: StateFlow<String?> = _status.asStateFlow()

    val isSignedIn: Boolean get() = _accountEmail.value != null

    fun signInIntent(): Intent = signInClient.signInIntent

    /** Process the result of the sign-in activity launched from Settings. */
    fun handleSignInResult(data: Intent?) {
        try {
            val account = GoogleSignIn.getSignedInAccountFromIntent(data)
                .getResult(ApiException::class.java)
            _accountEmail.value = account?.email
            _status.value = "Signed in as ${account?.email}"
        } catch (e: ApiException) {
            _status.value = "Sign-in failed (code ${e.statusCode})"
        }
    }

    fun signOut() {
        signInClient.signOut()
        _accountEmail.value = null
        _status.value = "Signed out"
    }

    /** Two-way sync: pull remote, merge (last-write-wins), push the merged result. */
    suspend fun syncNow(): Boolean = withContext(Dispatchers.IO) {
        try {
            val token = accessToken() ?: run {
                _status.value = "Sign in to sync"
                return@withContext false
            }
            val remoteIndex = driveClient.listFiles(token)
            val remoteContents = SYNC_FILES.associateWith { name ->
                remoteIndex[name]?.let { id -> driveClient.download(token, id) }
            }
            val merged = backupManager.mergeAndApply(remoteContents)
            merged.forEach { (name, content) ->
                driveClient.upsert(token, name, content, remoteIndex[name])
            }
            _status.value = "Synced ${timestamp()}"
            true
        } catch (e: UserRecoverableAuthException) {
            _status.value = "Approve Drive access in the Google permission prompt, then retry"
            false
        } catch (e: Exception) {
            _status.value = "Sync failed: ${e.message}"
            false
        }
    }

    /** Push a timestamped, read-only backup copy of everything. */
    suspend fun backupNow(): Boolean = withContext(Dispatchers.IO) {
        try {
            val token = accessToken() ?: run {
                _status.value = "Sign in to back up"
                return@withContext false
            }
            val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            backupManager.exportSnapshots().forEach { (name, content) ->
                val backupName = "backup_${stamp}_$name"
                driveClient.upsert(token, backupName, content, existingId = null)
            }
            _status.value = "Backed up ${timestamp()}"
            true
        } catch (e: Exception) {
            _status.value = "Backup failed: ${e.message}"
            false
        }
    }

    /** Pull the current remote snapshots and apply them locally. */
    suspend fun restore(): Boolean = withContext(Dispatchers.IO) {
        try {
            val token = accessToken() ?: run {
                _status.value = "Sign in to restore"
                return@withContext false
            }
            val remoteIndex = driveClient.listFiles(token)
            val remoteContents = SYNC_FILES.associateWith { name ->
                remoteIndex[name]?.let { id -> driveClient.download(token, id) }
            }
            backupManager.restoreFrom(remoteContents)
            _status.value = "Restored ${timestamp()}"
            true
        } catch (e: Exception) {
            _status.value = "Restore failed: ${e.message}"
            false
        }
    }

    /** Fetch a fresh OAuth access token for the signed-in account, or null. */
    private fun accessToken(): String? {
        val account = GoogleSignIn.getLastSignedInAccount(context)?.account ?: return null
        return GoogleAuthUtil.getToken(context, account, "oauth2:$DRIVE_APPDATA_SCOPE")
    }

    private fun timestamp(): String =
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

    companion object {
        private const val DRIVE_APPDATA_SCOPE = "https://www.googleapis.com/auth/drive.appdata"
        private val SYNC_FILES = listOf(
            BackupManager.NOTES_FILE,
            BackupManager.REMINDERS_FILE,
            BackupManager.CALENDAR_FILE
        )
    }
}
