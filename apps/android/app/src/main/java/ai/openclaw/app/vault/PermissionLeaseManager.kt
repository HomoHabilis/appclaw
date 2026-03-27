package ai.openclaw.app.vault

import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Manages the full lifecycle of [PermissionLease] records and drives the
 * progressive-trust suggestion logic.
 *
 * Thread-safety: all public methods are safe to call from any thread.
 * Persistence is backed by a [SharedPreferences] instance (pass an
 * [EncryptedSharedPreferences] for production use).
 *
 * @param prefs storage backend; callers should pass EncryptedSharedPreferences.
 * @param clock  epoch-ms supplier; injectable for testing.
 */
class PermissionLeaseManager(
    private val prefs: SharedPreferences,
    private val clock: () -> Long = { System.currentTimeMillis() },
) {
    private val json = Json { ignoreUnknownKeys = true }

    /** Number of approvals before a lease-upgrade suggestion is surfaced. */
    val progressiveTrustThreshold: Int = 10

    // ------------------------------------------------------------------
    // Decision
    // ------------------------------------------------------------------

    /**
     * Returns the [AuthDecision] the app should take for [request].
     *
     * - GREEN  → [AuthDecision.AutoApprove]
     * - RED    → [AuthDecision.RequireBiometric]
     * - YELLOW → [AuthDecision.AutoApprove] if a valid lease exists, else
     *            [AuthDecision.RequireBiometric]
     */
    fun decide(request: CloudAuthRequest): AuthDecision {
        if (request.tier.alwaysGranted) return AuthDecision.AutoApprove
        if (request.tier.requiresImmediateBiometric) return AuthDecision.RequireBiometric
        val lease = loadLease(request.actionKey)
        return if (lease != null && lease.isActive(clock())) AuthDecision.AutoApprove else AuthDecision.RequireBiometric
    }

    // ------------------------------------------------------------------
    // Lease management
    // ------------------------------------------------------------------

    /**
     * Grants a new lease for [actionKey] with the given [duration] and
     * increments the approval count.
     */
    fun grantLease(actionKey: String, tier: PermissionTier, duration: LeaseDuration): PermissionLease {
        val now = clock()
        val existing = loadLease(actionKey)
        val newCount = (existing?.approvalCount ?: 0) + 1
        val lease = PermissionLease(
            actionKey = actionKey,
            tier = tier,
            grantedAtMs = now,
            expiresAtMs = now + duration.durationMs,
            approvalCount = newCount,
        )
        saveLease(lease)
        return lease
    }

    /**
     * Records a single approval without creating a lease (used for RED-tier
     * and for YELLOW when the user chose not to grant a lease).
     */
    fun recordApproval(actionKey: String, tier: PermissionTier): PermissionLease {
        val existing = loadLease(actionKey)
        val newCount = (existing?.approvalCount ?: 0) + 1
        val now = clock()
        val lease = PermissionLease(
            actionKey = actionKey,
            tier = tier,
            grantedAtMs = now,
            expiresAtMs = now,
            approvalCount = newCount,
        )
        saveLease(lease)
        return lease
    }

    /** Revokes any active lease for [actionKey]. */
    fun revokeLease(actionKey: String) {
        prefs.edit { remove(leaseKey(actionKey)) }
    }

    /** Returns the stored lease for [actionKey], or null if none exists. */
    fun loadLease(actionKey: String): PermissionLease? {
        val raw = prefs.getString(leaseKey(actionKey), null) ?: return null
        return try {
            json.decodeFromString<PermissionLease>(raw)
        } catch (_: Exception) {
            null
        }
    }

    // ------------------------------------------------------------------
    // Progressive trust
    // ------------------------------------------------------------------

    /**
     * Returns true if the user has approved [actionKey] enough times that
     * the app should suggest upgrading to a longer lease.
     */
    fun shouldSuggestLeaseUpgrade(actionKey: String): Boolean {
        val lease = loadLease(actionKey) ?: return false
        return lease.approvalCount >= progressiveTrustThreshold
    }

    /**
     * Returns all action keys that have hit the progressive-trust threshold
     * but are not already on a 30-day lease.
     */
    fun pendingUpgradeSuggestions(): List<String> {
        return prefs.all.keys
            .filter { it.startsWith(LEASE_PREFIX) }
            .mapNotNull { prefKey ->
                val raw = prefs.getString(prefKey, null) ?: return@mapNotNull null
                try {
                    json.decodeFromString<PermissionLease>(raw)
                } catch (_: Exception) {
                    null
                }
            }
            .filter { lease ->
                lease.approvalCount >= progressiveTrustThreshold &&
                    lease.expiresAtMs - lease.grantedAtMs < LeaseDuration.THIRTY_DAYS.durationMs
            }
            .map { it.actionKey }
    }

    /** Returns all stored lease records regardless of expiry. */
    fun allLeases(): List<PermissionLease> =
        prefs.all.keys
            .filter { it.startsWith(LEASE_PREFIX) }
            .mapNotNull { prefKey ->
                val raw = prefs.getString(prefKey, null) ?: return@mapNotNull null
                try {
                    json.decodeFromString<PermissionLease>(raw)
                } catch (_: Exception) {
                    null
                }
            }

    // ------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------

    private fun saveLease(lease: PermissionLease) {
        prefs.edit { putString(leaseKey(lease.actionKey), json.encodeToString(lease)) }
    }

    private fun leaseKey(actionKey: String) = "$LEASE_PREFIX$actionKey"

    companion object {
        private const val LEASE_PREFIX = "vault.lease."
    }
}

/** The authorization decision returned by [PermissionLeaseManager.decide]. */
enum class AuthDecision {
    /** Action is safe to proceed without user interaction. */
    AutoApprove,

    /** Must show [VaultApprovalScreen] and trigger biometric authentication. */
    RequireBiometric,
}
