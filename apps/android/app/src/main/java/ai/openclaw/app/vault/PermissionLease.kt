package ai.openclaw.app.vault

import kotlinx.serialization.Serializable

/**
 * A time-bound grant for a specific action key.
 *
 * Leases are only supported for YELLOW-tier actions. RED-tier actions always
 * require a fresh biometric prompt; GREEN-tier actions never need one.
 *
 * [actionKey]     — stable identifier for the action (e.g. "email.read").
 * [tier]          — the risk tier assigned at grant time.
 * [grantedAtMs]   — epoch-ms when the lease was created.
 * [expiresAtMs]   — epoch-ms when the lease expires.
 * [approvalCount] — cumulative number of times this action has been approved;
 *                   used to drive the progressive-trust suggestion in
 *                   AgentAnalyticsDashboard.
 */
@Serializable
data class PermissionLease(
    val actionKey: String,
    val tier: PermissionTier,
    val grantedAtMs: Long,
    val expiresAtMs: Long,
    val approvalCount: Int = 0,
) {
    val isExpired: Boolean get() = System.currentTimeMillis() > expiresAtMs
    val remainingMs: Long get() = (expiresAtMs - System.currentTimeMillis()).coerceAtLeast(0L)

    /** Clock-injectable expiry check; used by [PermissionLeaseManager] for testability. */
    fun isActive(nowMs: Long): Boolean = nowMs <= expiresAtMs
}

/** Standard lease durations available to the user. */
enum class LeaseDuration(val label: String, val durationMs: Long) {
    ONE_HOUR("1 hour", 60 * 60 * 1_000L),
    FOUR_HOURS("4 hours", 4 * 60 * 60 * 1_000L),
    ONE_DAY("1 day", 24 * 60 * 60 * 1_000L),
    SEVEN_DAYS("7 days", 7 * 24 * 60 * 60 * 1_000L),
    THIRTY_DAYS("30 days", 30 * 24 * 60 * 60 * 1_000L),
}
