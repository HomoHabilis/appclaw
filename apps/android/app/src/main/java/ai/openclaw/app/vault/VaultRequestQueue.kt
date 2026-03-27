package ai.openclaw.app.vault

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Holds YELLOW-tier [CloudAuthRequest]s that arrived during quiet hours or
 * while the user was not actively using the app.
 *
 * The queue is intentionally in-memory: batched requests are not persisted
 * across process death (the cloud will re-issue them when the agent resumes).
 *
 * The UI layer observes [pendingBatch] and shows a "morning briefing" card
 * when the list is non-empty and quiet hours have ended.
 */
class VaultRequestQueue {
    private val _pendingBatch = MutableStateFlow<List<CloudAuthRequest>>(emptyList())

    /** All YELLOW-tier requests waiting for batch approval. */
    val pendingBatch: StateFlow<List<CloudAuthRequest>> = _pendingBatch.asStateFlow()

    /** True when there are queued requests ready for the morning briefing. */
    val hasPendingBatch: Boolean get() = _pendingBatch.value.isNotEmpty()

    /**
     * Adds [request] to the queue.
     * Duplicate [requestId]s are deduplicated.
     */
    fun enqueue(request: CloudAuthRequest) {
        val current = _pendingBatch.value
        if (current.any { it.requestId == request.requestId }) return
        _pendingBatch.value = current + request
    }

    /**
     * Removes and returns the full queued batch for processing.
     * Caller is responsible for sending [CloudAuthResponse] for each entry.
     */
    fun drainBatch(): List<CloudAuthRequest> {
        val batch = _pendingBatch.value
        _pendingBatch.value = emptyList()
        return batch
    }

    /** Removes a specific request from the queue (e.g. user dismissed one). */
    fun remove(requestId: String) {
        _pendingBatch.value = _pendingBatch.value.filter { it.requestId != requestId }
    }

    /** Clears the entire queue without processing. */
    fun clear() {
        _pendingBatch.value = emptyList()
    }
}
