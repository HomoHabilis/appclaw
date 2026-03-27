package ai.openclaw.app.vault

import kotlinx.serialization.Serializable

/**
 * Risk classification for cloud-agent actions.
 *
 * RED   — financial, destructive, or wide-blast actions. Always requires an
 *         immediate biometric prompt. Leases are not supported.
 * YELLOW — read-heavy or moderate-impact actions (email, calendar, DMs).
 *         Lease-based: the user picks a duration (1 h – 30 d) after the first
 *         approval and the action runs autonomously until the lease expires.
 * GREEN  — low-impact, fully autonomous actions (web search, draft notes).
 *         Always granted without user interaction.
 */
@Serializable
enum class PermissionTier(val displayLabel: String) {
    RED("High Risk"),
    YELLOW("Medium Risk"),
    GREEN("Low Risk"),
    ;

    val requiresImmediateBiometric: Boolean get() = this == RED
    val supportsLease: Boolean get() = this == YELLOW
    val alwaysGranted: Boolean get() = this == GREEN

    companion object {
        fun fromKey(key: String): PermissionTier =
            entries.firstOrNull { it.name.equals(key, ignoreCase = true) } ?: RED
    }
}
