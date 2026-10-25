package ch.gryphus.creditdebittracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LedgerDao {

    // --- People, each with their outstanding balance -----------------------
    // Balance = sum of unsettled entries, credits positive, debits negative.
    @Query(
        """
        SELECT p.id AS personId, p.name AS name,
               COALESCE(SUM(
                 CASE WHEN e.settled = 1 THEN 0
                      WHEN e.isCredit = 1 THEN e.amountCents
                      ELSE -e.amountCents END
               ), 0) AS balanceCents
        FROM person p
        LEFT JOIN entry e ON e.personId = p.id
        GROUP BY p.id, p.name
        ORDER BY p.name COLLATE NOCASE ASC
        """
    )
    fun personsWithBalance(): Flow<List<PersonBalanceRow>>

    @Query("SELECT * FROM person WHERE id = :id")
    fun person(id: Long): Flow<Person?>

    @Query("SELECT * FROM entry WHERE personId = :personId ORDER BY createdAt DESC")
    fun entriesFor(personId: Long): Flow<List<Entry>>

    @Insert
    suspend fun insertPerson(person: Person): Long

    @Delete
    suspend fun deletePerson(person: Person)

    @Insert
    suspend fun insertEntry(entry: Entry): Long

    @Update
    suspend fun updateEntry(entry: Entry)

    @Delete
    suspend fun deleteEntry(entry: Entry)
}

/** Projection row for [LedgerDao.personsWithBalance]. */
data class PersonBalanceRow(
    val personId: Long,
    val name: String,
    val balanceCents: Long,
)
