package ch.gryphus.creditdebittracker.data

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract

/** Resolving a picked contact's full name. */
object ContactName {

    /**
     * Pure: normalize a raw contact display name to a usable full name, or null
     * when absent/blank. Kept framework-free so it's unit-tested.
     */
    fun normalize(raw: String?): String? = raw?.trim()?.ifEmpty { null }

    /**
     * Resolve the full (display) name for a contact [uri] returned by the
     * system contact picker, using [context]'s ContentResolver. Returns null if
     * the contact can't be read or has no name. Requires READ_CONTACTS.
     */
    fun resolve(context: Context, uri: Uri): String? =
        try {
            context.contentResolver.query(
                uri,
                arrayOf(ContactsContract.Contacts.DISPLAY_NAME),
                null, null, null,
            )?.use { cursor ->
                if (!cursor.moveToFirst()) return null
                val idx = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                normalize(if (idx >= 0) cursor.getString(idx) else null)
            }
        } catch (e: SecurityException) {
            null
        }
}
