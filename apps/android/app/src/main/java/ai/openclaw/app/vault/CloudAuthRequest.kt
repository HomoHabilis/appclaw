package ai.openclaw.app.vault

import kotlinx.serialization.Serializable

/**
 * A permission request sent by the cloud OpenClaw container to the Android
 * device when it needs to perform a sensitive action.
 *
 * The cloud never holds credentials. Instead it pauses execution, emits this
 * payload over the WebSocket (or FCM), and waits for [CloudAuthResponse].
 *
 * [requestId]     — unique ID; echoed back in the response so the cloud can
 *                   match pending requests.
 * [actionKey]     — stable dot-separated action identifier, e.g. "email.read",
 *                   "finance.purchase", "calendar.write".
 * [tier]          — risk classification determined by the cloud agent.
 * [summary]       — short, human-readable description shown as the primary
 *                   line in [VaultApprovalScreen], e.g.
 *                   "OpenClaw wants to buy a flight for $200".
 * [contextDetail] — full detail paragraph with extra context the user might
 *                   need to make an informed decision.
 * [timestampMs]   — when the request was created (epoch-ms).
 */
@Serializable
data class CloudAuthRequest(
    val requestId: String,
    val actionKey: String,
    val tier: PermissionTier,
    val summary: String,
    val contextDetail: String,
    val timestampMs: Long,
)

/**
 * The device's response to a [CloudAuthRequest].
 *
 * [requestId] — mirrors [CloudAuthRequest.requestId].
 * [approved]  — true when the user authenticated and approved the action.
 * [leaseMs]   — if non-null, the cloud should treat this action as approved
 *               for this many milliseconds without re-prompting (YELLOW tier).
 */
@Serializable
data class CloudAuthResponse(
    val requestId: String,
    val approved: Boolean,
    val leaseMs: Long? = null,
)
