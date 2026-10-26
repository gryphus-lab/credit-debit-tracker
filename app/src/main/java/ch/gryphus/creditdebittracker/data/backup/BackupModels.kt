package ch.gryphus.creditdebittracker.data.backup

import kotlinx.serialization.Serializable

/**
 * Versioned, framework-free snapshot of the whole app state. This is the single
 * on-disk/cloud backup format - the same JSON is written to a local file and
 * uploaded to Google Drive. Keeping it independent of the Room entities means
 * the storage schema can evolve without breaking old backups, and it can be
 * (de)serialized and unit-tested without Android.
 */
@Serializable
data class BackupData(
    val version: Int = CURRENT_VERSION,
    val exportedAt: Long = 0L,
    val themeMode: String = "SYSTEM",
    val currencyCode: String = "CHF",
    val people: List<BackupPerson> = emptyList(),
) {
    companion object {
        /** Bump when the backup schema changes in a non-additive way. */
        const val CURRENT_VERSION = 1
    }
}

@Serializable
data class BackupPerson(
    val name: String,
    val entries: List<BackupEntry> = emptyList(),
)

@Serializable
data class BackupEntry(
    val note: String,
    val amountCents: Long,
    val isCredit: Boolean,
    val settled: Boolean = false,
    val createdAt: Long = 0L,
)
