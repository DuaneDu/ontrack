package net.nemerosa.ontrack.extension.chart.support

import net.nemerosa.ontrack.common.truncate
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals

internal class IntervalTest {

    @Test
    fun `Last day`() {
        val interval = Interval.parse("1d", ref)
        assertEquals(ref.minusDays(1), interval.start)
        assertEquals(ref, interval.end)
    }

    @Test
    fun `Last 3 days`() {
        val interval = Interval.parse("3d", ref)
        assertEquals(ref.minusDays(3), interval.start)
        assertEquals(ref, interval.end)
    }

    @Test
    fun `Last week`() {
        val interval = Interval.parse("1w", ref)
        assertEquals(ref.minusWeeks(1), interval.start)
        assertEquals(ref, interval.end)
    }

    @Test
    fun `Last 4 weeks`() {
        val interval = Interval.parse("4w", ref)
        assertEquals(ref.minusWeeks(4), interval.start)
        assertEquals(ref, interval.end)
    }

    @Test
    fun `Last month`() {
        val interval = Interval.parse("1m", ref)
        assertEquals(ref.minusMonths(1), interval.start)
        assertEquals(ref, interval.end)
    }

    @Test
    fun `Last 3 months`() {
        val interval = Interval.parse("3m", ref)
        assertEquals(ref.minusMonths(3), interval.start)
        assertEquals(ref, interval.end)
    }

    @Test
    fun `Last year`() {
        val interval = Interval.parse("1y", ref)
        assertEquals(ref.minusYears(1), interval.start)
        assertEquals(ref, interval.end)
    }

    @Test
    fun `Last 2 years`() {
        val interval = Interval.parse("2y", ref)
        assertEquals(ref.minusYears(2), interval.start)
        assertEquals(ref, interval.end)
    }

    private val ref = LocalDateTime.of(2022, 5, 12, 8, 52, 30, 0)

}