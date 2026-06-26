package com.clatos.dialer.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhoneNumberUtilsTest {

    @Test
    fun normalize_stripsFormatting_keepsLeadingPlus() {
        assertEquals("+15551230001", PhoneNumberUtils.normalize("+1 (555) 123-0001"))
        assertEquals("5551230001", PhoneNumberUtils.normalize("555-123-0001"))
        assertEquals("", PhoneNumberUtils.normalize(null))
        assertEquals("", PhoneNumberUtils.normalize("   "))
    }

    @Test
    fun sameNumber_matchesOnSignificantDigits() {
        // Same line written with and without country code / formatting.
        assertTrue(PhoneNumberUtils.sameNumber("+1 555 123 0001", "(555) 123-0001"))
        assertTrue(PhoneNumberUtils.sameNumber("+91 98765 43210", "98765 43210"))
    }

    @Test
    fun sameNumber_rejectsDifferentNumbers() {
        assertFalse(PhoneNumberUtils.sameNumber("5551230001", "5559998888"))
        assertFalse(PhoneNumberUtils.sameNumber(null, "5551230001"))
        assertFalse(PhoneNumberUtils.sameNumber("", ""))
    }
}
