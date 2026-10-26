package ch.gryphus.creditdebittracker

import ch.gryphus.creditdebittracker.data.backup.BackupData
import ch.gryphus.creditdebittracker.data.backup.BackupEntry
import ch.gryphus.creditdebittracker.data.backup.BackupPerson
import ch.gryphus.creditdebittracker.data.backup.BackupSerializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupSerializerTest {

    private fun sample() = BackupData(
        version = 1,
        exportedAt = 1_700_000_000_000L,
        themeMode = "DARK",
        currencyCode = "CHF",
        people = listOf(
            BackupPerson(
                name = "Ada",
                entries = listOf(
                    BackupEntry("Lunch", 1234, isCredit = true, settled = false, createdAt = 1L),
                    BackupEntry("Taxi", 500, isCredit = false, settled = true, createdAt = 2L),
                ),
            ),
            BackupPerson(name = "Grace", entries = emptyList()),
        ),
    )

    @Test
    fun encode_then_decode_roundTrips() {
        val data = sample()
        val json = BackupSerializer.encode(data)
        val back = BackupSerializer.decode(json)
        assertEquals(data, back)
    }

    @Test
    fun encode_producesReadableJson() {
        val json = BackupSerializer.encode(sample())
        assertTrue(json.contains("\"currencyCode\": \"CHF\""))
        assertTrue(json.contains("\"name\": \"Ada\""))
        assertTrue(json.contains("\"amountCents\": 1234"))
    }

    @Test
    fun decode_rejectsGarbage() {
        assertNull(BackupSerializer.decode("not json"))
        assertNull(BackupSerializer.decode(""))
        assertNull(BackupSerializer.decode("{ \"unterminated\": "))
    }

    @Test
    fun decode_toleratesUnknownKeysAndMissingOptionals() {
        // Extra key + omitted optionals (settled/createdAt default).
        val json = """
            {
              "version": 1,
              "currencyCode": "EUR",
              "futureField": "ignored",
              "people": [
                { "name": "Bob", "entries": [ { "note": "x", "amountCents": 10, "isCredit": true } ] }
              ]
            }
        """.trimIndent()
        val data = BackupSerializer.decode(json)
        assertEquals("EUR", data?.currencyCode)
        assertEquals("Bob", data?.people?.first()?.name)
        val entry = data?.people?.first()?.entries?.first()
        assertEquals(10L, entry?.amountCents)
        assertEquals(false, entry?.settled) // default applied
    }

    @Test
    fun fileName_isTimestamped() {
        assertEquals("credit-debit-backup-2026-10-26.json", BackupSerializer.fileName("2026-10-26"))
    }
}
