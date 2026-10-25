package ch.gryphus.creditdebittracker

import ch.gryphus.creditdebittracker.data.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyTest {

    @Test
    fun format_basic() {
        assertEquals("CHF 12.34", Money.format(1234, "CHF "))
        assertEquals("CHF 0.00", Money.format(0, "CHF "))
        assertEquals("CHF 5.00", Money.format(500, "CHF "))
    }

    @Test
    fun format_groupsThousands() {
        assertEquals("CHF 1,234.56", Money.format(123456, "CHF "))
        assertEquals("CHF 1,000,000.00", Money.format(100000000, "CHF "))
    }

    @Test
    fun format_negativePrefixesMinusBeforeSymbol() {
        assertEquals("-CHF 12.34", Money.format(-1234, "CHF "))
        assertEquals("-€0.99", Money.format(-99, "€"))
    }

    @Test
    fun format_otherSymbols() {
        assertEquals("$9.99", Money.format(999, "$"))
        assertEquals("₹100.00", Money.format(10000, "₹"))
    }

    @Test
    fun parse_wholeAndDecimal() {
        assertEquals(1200L, Money.parseToCents("12"))
        assertEquals(1250L, Money.parseToCents("12.5"))
        assertEquals(1234L, Money.parseToCents("12.34"))
        assertEquals(0L, Money.parseToCents("0"))
    }

    @Test
    fun parse_toleratesThousandsSeparatorsAndWhitespace() {
        assertEquals(123450L, Money.parseToCents(" 1,234.50 "))
        assertEquals(100000000L, Money.parseToCents("1,000,000"))
    }

    @Test
    fun parse_rejectsInvalid() {
        assertNull(Money.parseToCents(null))
        assertNull(Money.parseToCents(""))
        assertNull(Money.parseToCents("   "))
        assertNull(Money.parseToCents("abc"))
        assertNull(Money.parseToCents("12.345")) // too many decimals
        assertNull(Money.parseToCents("-5"))      // negatives entered via credit/debit toggle, not sign
        assertNull(Money.parseToCents("1.2.3"))
    }

    @Test
    fun parse_thenFormat_roundTrips() {
        val cents = Money.parseToCents("1,234.50")!!
        assertEquals("CHF 1,234.50", Money.format(cents, "CHF "))
    }
}
