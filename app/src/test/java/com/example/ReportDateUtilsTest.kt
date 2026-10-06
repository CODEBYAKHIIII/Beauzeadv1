package com.example

import com.example.data.ReportDateUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale

class ReportDateUtilsTest {
    @Test
    fun dueDateIsNotOverdueUntilTheFollowingDay() {
        val today = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.US)
            .parse("05 Oct 2026 12:00")!!

        assertFalse(ReportDateUtils.isBeforeToday("05 Oct 2026", today))
        assertTrue(ReportDateUtils.isBeforeToday("04 Oct 2026", today))
        assertFalse(ReportDateUtils.isBeforeToday("invalid date", today))
    }

    @Test
    fun monthRangeIncludesBothEndsAcrossYearBoundary() {
        val start = SimpleDateFormat("dd MMM yyyy", Locale.US).parse("20 Dec 2025")!!
        val end = SimpleDateFormat("dd MMM yyyy", Locale.US).parse("03 Feb 2026")!!

        assertEquals(
            listOf("2025-12", "2026-01", "2026-02"),
            ReportDateUtils.monthKeys(start, end)
        )
    }
}
