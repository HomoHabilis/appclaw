package ai.openclaw.app.vault

import java.util.Calendar

/**
 * Determines whether the current time falls within the user's configured
 * quiet hours window.
 *
 * During quiet hours, only RED-tier requests break through immediately.
 * YELLOW-tier requests are queued for the morning briefing.
 *
 * @param enabled      Whether quiet hours are active at all.
 * @param startHour    Hour-of-day (0–23) when quiet hours begin, e.g. 22 = 10 pm.
 * @param endHour      Hour-of-day (0–23) when quiet hours end, e.g. 8 = 8 am.
 * @param clock        Returns epoch-ms; injectable for testing.
 */
class QuietHoursConfig(
    val enabled: Boolean,
    val startHour: Int,
    val endHour: Int,
    private val clock: () -> Long = { System.currentTimeMillis() },
) {
    /**
     * Returns `true` if the current time is within the quiet window.
     *
     * Handles overnight spans (e.g. start=22, end=8).
     */
    fun isActive(): Boolean {
        if (!enabled) return false
        val cal = Calendar.getInstance().apply { timeInMillis = clock() }
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        return if (startHour <= endHour) {
            // Same-day span (e.g. 14:00 → 17:00)
            hour in startHour until endHour
        } else {
            // Overnight span (e.g. 22:00 → 08:00)
            hour >= startHour || hour < endHour
        }
    }
}
