package ch.gryphus.creditdebittracker.data.backup

import kotlinx.serialization.json.Json

/**
 * Pure JSON (de)serialization for [BackupData]. Framework-free and unit-tested.
 *
 * [decode] is tolerant of unknown keys and missing optional fields so a backup
 * written by a newer or slightly different app version still restores what it
 * can, rather than failing outright.
 */
object BackupSerializer {

    val BACKUP_FILENAME_PREFIX = "credit-debit-backup"
    const val MIME_TYPE = "application/json"

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /** Serialize a snapshot to pretty JSON. */
    fun encode(data: BackupData): String = json.encodeToString(BackupData.serializer(), data)

    /**
     * Parse a backup document. Returns null if the text isn't valid backup JSON
     * (so callers can show a clean "not a valid backup" message).
     */
    fun decode(text: String): BackupData? =
        runCatching { json.decodeFromString(BackupData.serializer(), text) }.getOrNull()

    /** A timestamped file name for a new backup, e.g. credit-debit-backup-2026-10-26.json. */
    fun fileName(dateStamp: String): String = "$BACKUP_FILENAME_PREFIX-$dateStamp.json"
}
