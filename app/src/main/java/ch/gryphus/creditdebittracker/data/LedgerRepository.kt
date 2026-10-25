package ch.gryphus.creditdebittracker.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Thin repository over [LedgerDao]; the single data entry point for the UI. */
class LedgerRepository(context: Context) {

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
}
