package com.oskar.retrolauncher

import com.oskar.retrolauncher.data.prefs.Units
import com.oskar.retrolauncher.util.formatDistance
import com.oskar.retrolauncher.util.formatDuration
import com.oskar.retrolauncher.util.formatDurationShort
import com.oskar.retrolauncher.util.formatRelativeDay
import com.oskar.retrolauncher.util.formatSpeed
import com.oskar.retrolauncher.util.metersPerSecondToDisplay
import com.oskar.retrolauncher.util.metersToDisplay
import com.oskar.retrolauncher.util.tempCToDisplay
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

class FormatExtTest {

    @Test
    fun `metric speed converts m s to km h`() {
        assertEquals(36.0, 10.0.metersPerSecondToDisplay(Units.METRIC), 0.0001)
    }

    @Test
    fun `imperial speed converts m s to mph`() {
        assertEquals(22.3694, 10.0.metersPerSecondToDisplay(Units.IMPERIAL), 0.0001)
    }

    @Test
    fun `metric temp passes through`() {
        assertEquals(20.0, 20.0.tempCToDisplay(Units.METRIC), 0.0001)
    }

    @Test
    fun `imperial temp converts C to F`() {
        assertEquals(68.0, 20.0.tempCToDisplay(Units.IMPERIAL), 0.0001)
        assertEquals(32.0, 0.0.tempCToDisplay(Units.IMPERIAL), 0.0001)
    }

    @Test
    fun `metric distance picks meters under 1 km`() {
        assertEquals("250m", 250.0.metersToDisplay(Units.METRIC))
    }

    @Test
    fun `metric distance picks km at or above 1 km`() {
        assertEquals("1.5km", 1500.0.metersToDisplay(Units.METRIC))
    }

    @Test
    fun `formatDurationShort renders hours minutes`() {
        val ms = (3 * 3600 + 12 * 60 + 5) * 1000L
        assertEquals("3h12m", ms.formatDurationShort())
    }

    @Test
    fun `formatDurationShort renders minutes seconds`() {
        val ms = (4 * 60 + 7) * 1000L
        assertEquals("4m07s", ms.formatDurationShort())
    }

    @Test
    fun `formatDurationShort renders seconds only`() {
        assertEquals("9s", 9_000L.formatDurationShort())
    }

    // ----- T1.10 ACs: formatDistance / formatDuration / formatSpeed -----

    @Test
    fun `formatDistance metric under 1 km rounds to whole meters with space (AC1 example 1)`() {
        assertEquals("123 m", formatDistance(123.456, Units.METRIC))
    }

    @Test
    fun `formatDistance metric at or above 1 km uses one decimal km (AC1 example 2)`() {
        assertEquals("1.2 km", formatDistance(1234.5, Units.METRIC))
    }

    @Test
    fun `formatDistance metric zero is the edge case`() {
        assertEquals("0 m", formatDistance(0.0, Units.METRIC))
    }

    @Test
    fun `formatDistance imperial picks ft under 0_1 mi and mi above`() {
        assertEquals("328 ft", formatDistance(100.0, Units.IMPERIAL))
        assertEquals("0.7 mi", formatDistance(1100.0, Units.IMPERIAL))
    }

    @Test
    fun `formatDuration formats hours minutes seconds with leading zeros (AC2)`() {
        assertEquals("01:02:05", formatDuration(3725L))
    }

    @Test
    fun `formatDuration zero is the edge case`() {
        assertEquals("00:00:00", formatDuration(0L))
    }

    @Test
    fun `formatDuration spans into double-digit hours`() {
        assertEquals("12:34:56", formatDuration(12L * 3600 + 34 * 60 + 56))
    }

    @Test
    fun `formatDuration negative input clamps to zero rather than emit minus signs`() {
        assertEquals("00:00:00", formatDuration(-42L))
    }

    @Test
    fun `formatSpeed imperial converts m s to mph (AC3)`() {
        assertEquals("45.9 mph", formatSpeed(20.5, Units.IMPERIAL))
    }

    @Test
    fun `formatSpeed metric converts m s to km h`() {
        assertEquals("72.0 km/h", formatSpeed(20.0, Units.METRIC))
    }

    @Test
    fun `formatSpeed zero is the edge case`() {
        assertEquals("0.0 km/h", formatSpeed(0.0, Units.METRIC))
        assertEquals("0.0 mph", formatSpeed(0.0, Units.IMPERIAL))
    }

    // ----- T1.27 AC2: formatRelativeDay returns Today / Yesterday / MMM dd -----

    private fun startOfDayMs(year: Int, month0: Int, day: Int, tz: TimeZone): Long =
        Calendar.getInstance(tz).apply {
            clear()
            set(year, month0, day, 0, 0, 0)
        }.timeInMillis

    @Test
    fun `formatRelativeDay returns Today when same calendar day as now`() {
        val tz = TimeZone.getTimeZone("UTC")
        val today = startOfDayMs(2026, Calendar.MAY, 9, tz)
        val now = today + TimeUnit.HOURS.toMillis(14)
        assertEquals("Today", today.formatRelativeDay(now, tz, Locale.US))
    }

    @Test
    fun `formatRelativeDay returns Yesterday for the previous calendar day`() {
        val tz = TimeZone.getTimeZone("UTC")
        val today = startOfDayMs(2026, Calendar.MAY, 9, tz)
        val yesterday = today - TimeUnit.DAYS.toMillis(1)
        assertEquals("Yesterday", yesterday.formatRelativeDay(today, tz, Locale.US))
    }

    @Test
    fun `formatRelativeDay returns MMM dd for older dates`() {
        val tz = TimeZone.getTimeZone("UTC")
        val today = startOfDayMs(2026, Calendar.MAY, 9, tz)
        val twoDaysAgo = startOfDayMs(2026, Calendar.MAY, 7, tz)
        assertEquals("May 07", twoDaysAgo.formatRelativeDay(today, tz, Locale.US))
    }

    @Test
    fun `formatRelativeDay returns MMM dd for future dates`() {
        val tz = TimeZone.getTimeZone("UTC")
        val today = startOfDayMs(2026, Calendar.MAY, 9, tz)
        val tomorrow = startOfDayMs(2026, Calendar.MAY, 10, tz)
        assertEquals("May 10", tomorrow.formatRelativeDay(today, tz, Locale.US))
    }
}
