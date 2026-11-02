package com.clockinreminder.app.domain.engine

import com.clockinreminder.app.data.local.entity.DayType
import com.clockinreminder.app.data.local.entity.PunchEntity
import com.clockinreminder.app.data.local.entity.PunchType
import com.clockinreminder.app.data.local.entity.ScheduleDayEntity
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class InconsistencyDetectorTest {

    @Test
    fun testDetectConsecutiveEntries() {
        val date = LocalDate.of(2026, 10, 5)
        val punches = listOf(
            PunchEntity(id = 1, profileId = 1, date = date, time = LocalTime.of(8, 0), timestampEpochMs = 1, type = PunchType.ENTRY),
            PunchEntity(id = 2, profileId = 1, date = date, time = LocalTime.of(8, 30), timestampEpochMs = 2, type = PunchType.ENTRY)
        )

        val issues = InconsistencyDetector.detectInconsistencies(
            date = date,
            punches = punches,
            scheduleDay = null,
            occurrence = null
        )

        assertTrue(issues.any { it.title.contains("consecutivas") })
    }

    @Test
    fun testDetectDuplicatePunches() {
        val date = LocalDate.of(2026, 10, 5)
        val punches = listOf(
            PunchEntity(id = 1, profileId = 1, date = date, time = LocalTime.of(8, 0), timestampEpochMs = 1, type = PunchType.ENTRY),
            PunchEntity(id = 2, profileId = 1, date = date, time = LocalTime.of(8, 1), timestampEpochMs = 2, type = PunchType.LUNCH_OUT)
        )

        val issues = InconsistencyDetector.detectInconsistencies(
            date = date,
            punches = punches,
            scheduleDay = null,
            occurrence = null
        )

        assertTrue(issues.any { it.title.contains("duplicada") })
    }

    @Test
    fun testDetectUnrecordedWorkday() {
        val pastDate = LocalDate.now().minusDays(3)
        val scheduleDay = ScheduleDayEntity(
            scheduleId = 1,
            dayOfWeek = pastDate.dayOfWeek.value,
            isWorkDay = true,
            expectedMinutes = 540
        )

        val issues = InconsistencyDetector.detectInconsistencies(
            date = pastDate,
            punches = emptyList(),
            scheduleDay = scheduleDay,
            occurrence = null
        )

        assertTrue(issues.any { it.title.contains("sem registros") })
    }
}
