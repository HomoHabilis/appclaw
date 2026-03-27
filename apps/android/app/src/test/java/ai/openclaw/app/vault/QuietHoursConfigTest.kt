package ai.openclaw.app.vault

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class QuietHoursConfigTest {
    /** Creates a fake clock that returns a time at [hour]:00 on an arbitrary date. */
    private fun clockAt(hour: Int): () -> Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return { cal.timeInMillis }
    }

    @Test
    fun quietHoursDisabled_neverActive() {
        val config = QuietHoursConfig(enabled = false, startHour = 22, endHour = 8, clock = clockAt(23))
        assertFalse(config.isActive())
    }

    @Test
    fun overnightSpan_insideWindow() {
        // 22:00 → 08:00 span; current time 23:00 → inside
        val config = QuietHoursConfig(enabled = true, startHour = 22, endHour = 8, clock = clockAt(23))
        assertTrue(config.isActive())
    }

    @Test
    fun overnightSpan_earlyMorning_inside() {
        // 22:00 → 08:00 span; current time 03:00 → inside
        val config = QuietHoursConfig(enabled = true, startHour = 22, endHour = 8, clock = clockAt(3))
        assertTrue(config.isActive())
    }

    @Test
    fun overnightSpan_justAfterEnd_outside() {
        // 22:00 → 08:00 span; current time 08:00 → outside (end is exclusive)
        val config = QuietHoursConfig(enabled = true, startHour = 22, endHour = 8, clock = clockAt(8))
        assertFalse(config.isActive())
    }

    @Test
    fun overnightSpan_middleOfDay_outside() {
        // 22:00 → 08:00 span; current time 14:00 → outside
        val config = QuietHoursConfig(enabled = true, startHour = 22, endHour = 8, clock = clockAt(14))
        assertFalse(config.isActive())
    }

    @Test
    fun sameDaySpan_insideWindow() {
        // 14:00 → 17:00; current time 15:00 → inside
        val config = QuietHoursConfig(enabled = true, startHour = 14, endHour = 17, clock = clockAt(15))
        assertTrue(config.isActive())
    }

    @Test
    fun sameDaySpan_outsideWindow() {
        // 14:00 → 17:00; current time 18:00 → outside
        val config = QuietHoursConfig(enabled = true, startHour = 14, endHour = 17, clock = clockAt(18))
        assertFalse(config.isActive())
    }
}
