package ch.gryphus.creditdebittracker

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ch.gryphus.creditdebittracker.data.CurrencyManager
import ch.gryphus.creditdebittracker.data.Entry
import ch.gryphus.creditdebittracker.data.LedgerRepository
import ch.gryphus.creditdebittracker.data.Person
import ch.gryphus.creditdebittracker.data.PersonWithBalance
import ch.gryphus.creditdebittracker.data.ThemeMode
import ch.gryphus.creditdebittracker.data.ThemePreference
import ch.gryphus.creditdebittracker.data.backup.BackupSerializer
import ch.gryphus.creditdebittracker.data.backup.DriveBackup
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LedgerViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = LedgerRepository(app)
    val drive = DriveBackup(app)

    // One-shot user-facing messages (backup results). Collected by the UI.
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages

    private val _currencySymbol = MutableStateFlow(CurrencyManager.symbol(app))
    val currencySymbol: StateFlow<String> = _currencySymbol

    private val _themeMode = MutableStateFlow(ThemePreference.current(app))
    val themeMode: StateFlow<ThemeMode> = _themeMode

    val persons: StateFlow<List<PersonWithBalance>> =
        repo.personsWithBalance.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Total owed to you (sum of positive balances), in cents. */
    val totalCredit: StateFlow<Long> =
        persons.map { list -> list.filter { it.balanceCents > 0 }.sumOf { it.balanceCents } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    /** Total you owe (magnitude of negative balances), in cents. */
    val totalDebit: StateFlow<Long> =
        persons.map { list -> list.filter { it.balanceCents < 0 }.sumOf { -it.balanceCents } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    fun person(id: Long): Flow<Person?> = repo.person(id)
    fun entriesFor(id: Long): Flow<List<Entry>> = repo.entriesFor(id)

    fun setCurrency(currency: CurrencyManager.Currency) {
        CurrencyManager.setCurrency(getApplication(), currency)
        _currencySymbol.value = currency.symbol
    }

    fun setThemeMode(mode: ThemeMode) {
        ThemePreference.setMode(getApplication(), mode)
        _themeMode.value = mode
    }

    fun addPerson(name: String) = viewModelScope.launch { repo.addPerson(name) }
    fun deletePerson(person: Person) = viewModelScope.launch { repo.deletePerson(person) }

    fun addEntry(personId: Long, note: String, amountCents: Long, isCredit: Boolean) =
        viewModelScope.launch { repo.addEntry(personId, note, amountCents, isCredit) }

    fun toggleSettled(entry: Entry) = viewModelScope.launch { repo.toggleSettled(entry) }
    fun deleteEntry(entry: Entry) = viewModelScope.launch { repo.deleteEntry(entry) }

    // --- Backup / restore --------------------------------------------------

    /** Build the backup JSON (the UI writes it to the user-picked file). */
    suspend fun buildBackupJson(): String = BackupSerializer.encode(repo.exportBackup())

    /** Suggested file name for a new local backup, stamped with today's date. */
    fun suggestedFileName(): String {
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            .format(java.util.Date())
        return BackupSerializer.fileName(today)
    }

    /** Restore from backup JSON text (local file or Drive). Refreshes settings. */
    private suspend fun applyBackupText(text: String): Boolean {
        val data = BackupSerializer.decode(text) ?: return false
        repo.importBackup(data)
        _currencySymbol.value = CurrencyManager.symbol(getApplication())
        _themeMode.value = ThemePreference.current(getApplication())
        return true
    }

    fun exportToStream(write: (String) -> Unit) = viewModelScope.launch {
        runCatching { write(buildBackupJson()) }
            .onSuccess { _messages.emit("Backup saved.") }
            .onFailure { _messages.emit("Backup failed: ${it.message}") }
    }

    fun importFromText(text: String) = viewModelScope.launch {
        runCatching { applyBackupText(text) }
            .onSuccess { ok -> _messages.emit(if (ok) "Backup restored." else "Not a valid backup file.") }
            .onFailure { _messages.emit("Restore failed: ${it.message}") }
    }

    fun backupToDrive(account: GoogleSignInAccount) = viewModelScope.launch {
        val json = buildBackupJson()
        drive.upload(account, json)
            .onSuccess { _messages.emit("Backed up to Google Drive.") }
            .onFailure { _messages.emit("Drive backup failed: ${it.message}") }
    }

    fun restoreFromDrive(account: GoogleSignInAccount) = viewModelScope.launch {
        drive.download(account)
            .onSuccess { json ->
                if (json == null) {
                    _messages.emit("No backup found on Google Drive.")
                } else if (applyBackupText(json)) {
                    _messages.emit("Restored from Google Drive.")
                } else {
                    _messages.emit("Drive backup was unreadable.")
                }
            }
            .onFailure { _messages.emit("Drive restore failed: ${it.message}") }
    }
}
