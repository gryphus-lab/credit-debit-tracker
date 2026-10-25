package ch.gryphus.creditdebittracker.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** A person you track money with. */
@Entity(tableName = "person")
data class Person(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
)

/**
 * A single ledger entry against a [Person].
 *
 * - [amountCents] is always a positive magnitude in cents.
 * - [isCredit] = true means "they owe you" (credit, positive to your balance);
 *   false means "you owe them" (debit, negative to your balance).
 * - [settled] marks the entry as paid/cleared; settled entries don't count
 *   toward outstanding balances.
 */
@Entity(
    tableName = "entry",
    foreignKeys = [
        ForeignKey(
            entity = Person::class,
            parentColumns = ["id"],
            childColumns = ["personId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("personId")],
)
data class Entry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val personId: Long,
    val note: String,
    val amountCents: Long,
    val isCredit: Boolean,
    val settled: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
) {
    /** Signed contribution to the balance; settled entries contribute nothing. */
    val signedOutstanding: Long
        get() = if (settled) 0L else if (isCredit) amountCents else -amountCents
}

/** A person plus their computed outstanding balance (positive = owed to you). */
data class PersonWithBalance(
    val person: Person,
    val balanceCents: Long,
)
