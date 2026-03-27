package ai.openclaw.app.vault

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class VaultRequestQueueTest {
    private lateinit var queue: VaultRequestQueue

    @Before
    fun setUp() {
        queue = VaultRequestQueue()
    }

    private fun makeRequest(id: String, tier: PermissionTier = PermissionTier.YELLOW) =
        CloudAuthRequest(
            requestId = id,
            actionKey = "email.read",
            tier = tier,
            summary = "Read emails",
            contextDetail = "OpenClaw wants to read your inbox.",
            timestampMs = System.currentTimeMillis(),
        )

    @Test
    fun enqueue_addsRequest() {
        queue.enqueue(makeRequest("req-1"))
        assertEquals(1, queue.pendingBatch.value.size)
    }

    @Test
    fun enqueue_deduplicates() {
        queue.enqueue(makeRequest("req-1"))
        queue.enqueue(makeRequest("req-1"))
        assertEquals(1, queue.pendingBatch.value.size)
    }

    @Test
    fun drainBatch_returnsAllAndClears() {
        queue.enqueue(makeRequest("req-1"))
        queue.enqueue(makeRequest("req-2"))
        val batch = queue.drainBatch()
        assertEquals(2, batch.size)
        assertTrue(queue.pendingBatch.value.isEmpty())
        assertFalse(queue.hasPendingBatch)
    }

    @Test
    fun remove_removesSpecificRequest() {
        queue.enqueue(makeRequest("req-1"))
        queue.enqueue(makeRequest("req-2"))
        queue.remove("req-1")
        assertEquals(1, queue.pendingBatch.value.size)
        assertEquals("req-2", queue.pendingBatch.value.first().requestId)
    }

    @Test
    fun clear_emptiesQueue() {
        queue.enqueue(makeRequest("req-1"))
        queue.enqueue(makeRequest("req-2"))
        queue.clear()
        assertTrue(queue.pendingBatch.value.isEmpty())
    }
}
