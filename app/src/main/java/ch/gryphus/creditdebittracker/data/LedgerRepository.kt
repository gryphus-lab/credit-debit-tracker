package ch.gryphus.creditdebittracker.data

import android.content.Context
import ch.gryphus.creditdebittracker.data.backup.BackupData
import ch.gryphus.creditdebittracker.data.backup.BackupEntry
import ch.gryphus.creditdebittracker.data.backup.BackupPerson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Thin repository over [LedgerDao]; the single data entry point for the UI. */
class LedgerRepository(context: Context) {

    private val appContext = context.applicationContext
    private val dao = LedgerDatabase.get(context).ledgerDao()

    val personsWithBalance: Flow<List<PersonWithBalance>> =
        dao.personsWithBalance().map { rows ->
            rows.map { PersonWithBalance(Person(it.personId, it.name), it.balanceCents) }
        }

    fun person(id: Long): Flow<Person?> = dao.person(id)

    fun entriesFor(personId: Long): Flow<List<Entry>> = dao.entriesFor(personId)

    suspend fun addPerson(name: String): Long = dao.insertPerson(Person(name = name.trim()))

    suspend fun deletePerson(person: Person) = dao.deletePerson(person)

    suspend fun addEntry(
        personId: Long,
        note: String,
        amountCents: Long,
        isCredit: Boolean,
    ): Long = dao.insertEntry(
        Entry(
            personId = personId,
            note = note.trim(),
            amountCents = amountCents,
            isCredit = isCredit,
        ),
    )

    suspend fun toggleSettled(entry: Entry) = dao.updateEntry(entry.copy(settled = !entry.settled))

    suspend fun deleteEntry(entry: Entry) = dao.deleteEntry(entry)

    // --- Backup: snapshot the whole app state <-> BackupData ---------------

    /** Build a full snapshot of people + entries + settings for a backup. */
    suspend fun exportBackup(): BackupData {
        val people = dao.allPersons().map { person ->
            BackupPerson(
                name = person.name,
                entries = dao.entriesForOnce(person.id).map { e ->
                    BackupEntry(
                        note = e.note,
                        amountCents = e.amountCents,
                        isCredit = e.isCredit,
                        settled = e.settled,
                        createdAt = e.createdAt,
                    )
                },
            )
        }
        return BackupData(
            exportedAt = System.currentTimeMillis(),
            themeMode = ThemePreference.current(appContext).name,
            currencyCode = CurrencyManager.current(appContext).code,
            people = people,
        )
    }

    /**
     * Replace all current data + settings with [backup]. Atomic at the DB level
     * (see [LedgerDao.replaceAll]); settings are applied after the data swap.
     */
    suspend fun importBackup(backup: BackupData) {
        val mapped = backup.people.map { bp ->
            Person(name = bp.name) to bp.entries.map { be ->
                Entry(
                    personId = 0,
                    note = be.note,
                    amountCents = be.amountCents,
                    isCredit = be.isCredit,
                    settled = be.settled,
                    createdAt = if (be.createdAt > 0) be.createdAt else System.currentTimeMillis(),
                )
            }
        }
        dao.replaceAll(mapped)

        // Restore settings (best-effort; unknown values fall back to defaults).
        CurrencyManager.supported.find { it.code == backup.currencyCode }
            ?.let { CurrencyManager.setCurrency(appContext, it) }
        runCatching { ThemeMode.valueOf(backup.themeMode) }
            .getOrNull()
            ?.let { ThemePreference.setMode(appContext, it) }
    }
}
