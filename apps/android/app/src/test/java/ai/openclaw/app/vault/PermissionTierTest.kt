package ai.openclaw.app.vault

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PermissionTierTest {
    @Test
    fun redTierRequiresImmediateBiometric() {
        assertTrue(PermissionTier.RED.requiresImmediateBiometric)
        assertFalse(PermissionTier.YELLOW.requiresImmediateBiometric)
        assertFalse(PermissionTier.GREEN.requiresImmediateBiometric)
    }

    @Test
    fun yellowTierSupportsLease() {
        assertFalse(PermissionTier.RED.supportsLease)
        assertTrue(PermissionTier.YELLOW.supportsLease)
        assertFalse(PermissionTier.GREEN.supportsLease)
    }

    @Test
    fun greenTierIsAlwaysGranted() {
        assertFalse(PermissionTier.RED.alwaysGranted)
        assertFalse(PermissionTier.YELLOW.alwaysGranted)
        assertTrue(PermissionTier.GREEN.alwaysGranted)
    }

    @Test
    fun fromKeyIsCaseInsensitive() {
        assertEquals(PermissionTier.RED, PermissionTier.fromKey("red"))
        assertEquals(PermissionTier.RED, PermissionTier.fromKey("RED"))
        assertEquals(PermissionTier.YELLOW, PermissionTier.fromKey("Yellow"))
        assertEquals(PermissionTier.GREEN, PermissionTier.fromKey("GREEN"))
    }

    @Test
    fun fromKeyFallsBackToRedForUnknown() {
        assertEquals(PermissionTier.RED, PermissionTier.fromKey("unknown"))
        assertEquals(PermissionTier.RED, PermissionTier.fromKey(""))
    }

    @Test
    fun displayLabelsAreHumanReadable() {
        assertEquals("High Risk", PermissionTier.RED.displayLabel)
        assertEquals("Medium Risk", PermissionTier.YELLOW.displayLabel)
        assertEquals("Low Risk", PermissionTier.GREEN.displayLabel)
    }
}
