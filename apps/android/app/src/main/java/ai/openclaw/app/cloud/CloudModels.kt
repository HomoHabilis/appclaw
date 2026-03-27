package ai.openclaw.app.cloud

import kotlinx.serialization.Serializable

/**
 * Response from the AppClaw backend `/spawn` endpoint.
 *
 * [wsUrl]          — WebSocket URL of the newly provisioned OpenClaw container
 *                   (e.g. `wss://claw-us-east-42.appclaw.ai/ws`).
 * [bootstrapToken] — Short-lived token used for first connection auth. The app
 *                   stores it in [SecurePrefs] and sends it in the initial
 *                   `Authorization` header just like a normal bootstrap token.
 * [containerId]    — Opaque container identifier for de-provisioning and
 *                   diagnostics.
 */
@Serializable
data class SpawnResponse(
    val wsUrl: String,
    val bootstrapToken: String,
    val containerId: String,
)

/** Status of the cloud container for the current user. */
@Serializable
data class ContainerStatus(
    val containerId: String,
    val state: String,   // "running" | "sleeping" | "stopped"
    val wsUrl: String?,
)
