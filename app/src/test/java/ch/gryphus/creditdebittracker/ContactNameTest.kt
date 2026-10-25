package ch.gryphus.creditdebittracker

import ch.gryphus.creditdebittracker.data.ContactName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ContactNameTest {

    @Test
    fun normalize_keepsRealNames() {
        assertEquals("Ada Lovelace", ContactName.normalize("Ada Lovelace"))
        assertEquals("Ada Lovelace", ContactName.normalize("  Ada Lovelace  "))
    }

    @Test
    fun normalize_blankOrNullBecomesNull() {
        assertNull(ContactName.normalize(null))
        assertNull(ContactName.normalize(""))
        assertNull(ContactName.normalize("   "))
    }
}
