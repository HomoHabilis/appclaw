package ai.openclaw.app.vault

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class PermissionLeaseManagerTest {
    private lateinit var manager: PermissionLeaseManager
    private var fakeNow = 1_000_000L

    @Before
    fun setUp() {
        val prefs =
            RuntimeEnvironment.getApplication()
                .getSharedPreferences("vault.test", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        manager = PermissionLeaseManager(prefs = prefs, clock = { fakeNow })
    }

    // ------------------------------------------------------------------
    // decide()
    // ------------------------------------------------------------------

    @Test
    fun greenTierIsAutoApproved() {
        val request = makeRequest(PermissionTier.GREEN)
        assertEquals(AuthDecision.AutoApprove, manager.decide(request))
    }

    @Test
    fun redTierAlwaysRequiresBiometric() {
        val request = makeRequest(PermissionTier.RED)
        assertEquals(AuthDecision.RequireBiometric, manager.decide(request))
    }

    @Test
    fun yellowTierWithNoLeaseRequiresBiometric() {
        val request = makeRequest(PermissionTier.YELLOW)
        assertEquals(AuthDecision.RequireBiometric, manager.decide(request))
    }

    @Test
    fun yellowTierWithActiveLeaseIsAutoApproved() {
        val request = makeRequest(PermissionTier.YELLOW)
        manager.grantLease(request.actionKey, PermissionTier.YELLOW, LeaseDuration.ONE_HOUR)
        assertEquals(AuthDecision.AutoApprove, manager.decide(request))
    }

    @Test
    fun yellowTierWithExpiredLeaseRequiresBiometric() {
        val request = makeRequest(PermissionTier.YELLOW)
        manager.grantLease(request.actionKey, PermissionTier.YELLOW, LeaseDuration.ONE_HOUR)
        fakeNow += LeaseDuration.ONE_HOUR.durationMs + 1
        assertEquals(AuthDecision.RequireBiometric, manager.decide(request))
    }

    // ------------------------------------------------------------------
    // grantLease()
    // ------------------------------------------------------------------

    @Test
    fun grantLeaseStoresLeaseWithCorrectExpiry() {
        manager.grantLease("email.read", PermissionTier.YELLOW, LeaseDuration.FOUR_HOURS)
        val lease = manager.loadLease("email.read")
        assertNotNull(lease)
        assertEquals(fakeNow + LeaseDuration.FOUR_HOURS.durationMs, lease!!.expiresAtMs)
    }

    @Test
    fun grantLeaseIncrementsApprovalCount() {
        manager.grantLease("email.read", PermissionTier.YELLOW, LeaseDuration.ONE_HOUR)
        manager.grantLease("email.read", PermissionTier.YELLOW, LeaseDuration.ONE_HOUR)
        val lease = manager.loadLease("email.read")
        assertEquals(2, lease!!.approvalCount)
    }

    // ------------------------------------------------------------------
    // revokeLease()
    // ------------------------------------------------------------------

    @Test
    fun revokeLeaseClearsStoredLease() {
        manager.grantLease("email.read", PermissionTier.YELLOW, LeaseDuration.ONE_DAY)
        manager.revokeLease("email.read")
        assertNull(manager.loadLease("email.read"))
    }

    // ------------------------------------------------------------------
    // recordApproval()
    // ------------------------------------------------------------------

    @Test
    fun recordApprovalIncrementsCountWithoutExtendingLease() {
        manager.recordApproval("finance.purchase", PermissionTier.RED)
        manager.recordApproval("finance.purchase", PermissionTier.RED)
        val lease = manager.loadLease("finance.purchase")
        assertNotNull(lease)
        assertEquals(2, lease!!.approvalCount)
        // expiry == grantedAt — not a real lease
        assertEquals(lease.grantedAtMs, lease.expiresAtMs)
    }

    // ------------------------------------------------------------------
    // Progressive trust
    // ------------------------------------------------------------------

    @Test
    fun shouldSuggestLeaseUpgradeAfterThreshold() {
        repeat(manager.progressiveTrustThreshold) {
            manager.recordApproval("calendar.read", PermissionTier.YELLOW)
        }
        assertTrue(manager.shouldSuggestLeaseUpgrade("calendar.read"))
    }

    @Test
    fun shouldNotSuggestUpgradeBeforeThreshold() {
        repeat(manager.progressiveTrustThreshold - 1) {
            manager.recordApproval("calendar.read", PermissionTier.YELLOW)
        }
        assertFalse(manager.shouldSuggestLeaseUpgrade("calendar.read"))
    }

    @Test
    fun pendingUpgradeSuggestionsExcludesThirtyDayLeases() {
        repeat(manager.progressiveTrustThreshold) {
            manager.recordApproval("calendar.read", PermissionTier.YELLOW)
        }
        assertTrue("calendar.read" in manager.pendingUpgradeSuggestions())

        manager.grantLease("calendar.read", PermissionTier.YELLOW, LeaseDuration.THIRTY_DAYS)
        assertFalse("calendar.read" in manager.pendingUpgradeSuggestions())
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private fun makeRequest(tier: PermissionTier) =
        CloudAuthRequest(
            requestId = "req-1",
            actionKey = "test.action",
            tier = tier,
            summary = "Test action",
            contextDetail = "Details about the test action.",
            timestampMs = fakeNow,
        )
}
