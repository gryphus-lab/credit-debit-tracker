// Google Sign-In (GoogleSignIn / GoogleSignInOptions / GoogleSignInAccount) is
// deprecated in favour of Credential Manager, but it remains the supported,
// working way to obtain a Drive OAuth token; migrating is a large separate
// effort. This is a deliberate choice, so suppress the deprecation (S1874)
// file-wide rather than sprinkling per-call annotations.
@file:Suppress("DEPRECATION")

package ch.gryphus.creditdebittracker.data.backup

import android.accounts.Account
import android.content.Context
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Backs up / restores the app-state JSON to Google Drive's hidden
 * **app-data folder** (scope [Scopes.DRIVE_APPFOLDER] = `drive.appdata`).
 *
 * Design notes:
 * - The app-data folder is private to this app, so the backup never clutters
 *   the user's Drive and needs no broad Drive permission.
 * - Auth is Google Sign-In; we fetch an OAuth access token with
 *   [GoogleAuthUtil] and call the Drive v3 REST API directly over
 *   [HttpURLConnection] - avoiding the heavy google-api-client libraries that
 *   fight with R8.
 * - Everything is best-effort and returns a [Result]; if Google Sign-In isn't
 *   configured (no OAuth client for this package + signing cert), sign-in
 *   simply fails and the UI reports it cleanly rather than crashing.
 */
class DriveBackup(
    private val context: Context,
    // Injected (not hardcoded) so the dispatcher is overridable/testable.
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {

    companion object {
        private const val BACKUP_NAME = "credit-debit-backup.json"
        private const val FILES_URL = "https://www.googleapis.com/drive/v3/files"
        private const val UPLOAD_URL =
            "https://www.googleapis.com/upload/drive/v3/files"
        private const val OAUTH_SCOPE = "oauth2:https://www.googleapis.com/auth/drive.appdata"
    }

    /** Sign-in options requesting the app-data Drive scope. */
    private fun signInOptions(): GoogleSignInOptions =
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(Scopes.DRIVE_APPFOLDER))
            .build()

    /** The already-signed-in account, if any (no UI). */
    fun lastAccount(): GoogleSignInAccount? = GoogleSignIn.getLastSignedInAccount(context)

    /**
     * Intent that launches the Google account chooser. The UI starts this with
     * an activity-result launcher and passes the result data back to
     * [accountFromResult]. Keeping all Google Sign-In (deprecated-API) calls in
     * this class means the UI never touches the deprecated surface directly.
     */
    fun signInIntent(): android.content.Intent =
        GoogleSignIn.getClient(context, signInOptions()).signInIntent

    /** Resolve a signed-in account from the sign-in activity result, or null. */
    fun accountFromResult(data: android.content.Intent?): GoogleSignInAccount? =
        GoogleSignIn.getSignedInAccountFromIntent(data).result

    /** Upload (create or overwrite) the backup into the Drive app-data folder. */
    suspend fun upload(account: GoogleSignInAccount, json: String): Result<Unit> =
        withContext(ioDispatcher) {
            runCatching {
                val token = accessToken(account)
                val existingId = findBackupId(token)
                if (existingId == null) createBackup(token, json) else updateBackup(token, existingId, json)
                Unit
            }
        }

    /** Download the latest backup JSON from the app-data folder, or null if none. */
    suspend fun download(account: GoogleSignInAccount): Result<String?> =
        withContext(ioDispatcher) {
            runCatching {
                val token = accessToken(account)
                val id = findBackupId(token) ?: return@runCatching null
                get("$FILES_URL/$id?alt=media", token)
            }
        }

    // --- internals ---------------------------------------------------------

    private fun accessToken(account: GoogleSignInAccount): String {
        val email = account.email ?: error("Signed-in account has no email")
        return GoogleAuthUtil.getToken(context, Account(email, "com.google"), OAUTH_SCOPE)
    }

    /** Returns the fileId of the existing backup in appDataFolder, or null. */
    private fun findBackupId(token: String): String? {
        val q = URLEncoder.encode("name = '$BACKUP_NAME'", "UTF-8")
        val url = "$FILES_URL?spaces=appDataFolder&q=$q&fields=files(id,name)"
        val body = get(url, token)
        val files = JSONObject(body).optJSONArray("files") ?: return null
        return if (files.length() > 0) files.getJSONObject(0).getString("id") else null
    }

    private fun createBackup(token: String, json: String) {
        // Multipart: metadata (parents=[appDataFolder]) + media, in one request.
        val boundary = "cdtBoundary${System.currentTimeMillis()}"
        val metadata = JSONObject()
            .put("name", BACKUP_NAME)
            .put("parents", org.json.JSONArray().put("appDataFolder"))
            .toString()
        val payload = buildString {
            append("--$boundary\r\n")
            append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
            append(metadata).append("\r\n")
            append("--$boundary\r\n")
            append("Content-Type: ${BackupSerializer.MIME_TYPE}\r\n\r\n")
            append(json).append("\r\n")
            append("--$boundary--")
        }
        post(
            "$UPLOAD_URL?uploadType=multipart&fields=id",
            token,
            payload,
            "multipart/related; boundary=$boundary",
            method = "POST",
        )
    }

    private fun updateBackup(token: String, fileId: String, json: String) {
        post(
            "$UPLOAD_URL/$fileId?uploadType=media",
            token,
            json,
            BackupSerializer.MIME_TYPE,
            method = "PATCH",
        )
    }

    private fun get(url: String, token: String): String =
        (URL(url).openConnection() as HttpURLConnection).run {
            requestMethod = "GET"
            setRequestProperty("Authorization", "Bearer $token")
            readResponse(this)
        }

    private fun post(
        url: String,
        token: String,
        body: String,
        contentType: String,
        method: String,
    ): String =
        (URL(url).openConnection() as HttpURLConnection).run {
            requestMethod = method
            doOutput = true
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Content-Type", contentType)
            outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            readResponse(this)
        }

    private fun readResponse(conn: HttpURLConnection): String {
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        if (code !in 200..299) error("Drive API error $code: $text")
        return text
    }
}
