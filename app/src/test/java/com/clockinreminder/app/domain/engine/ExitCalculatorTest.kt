package com.clockinreminder.app.domain.engine

import com.clockinreminder.app.data.local.entity.PunchEntity
import com.clockinreminder.app.data.local.entity.PunchType
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class ExitCalculatorTest {

    @Test
    fun testExitScenariosWithoutLunchYet() {
        val today = LocalDate.of(2026, 11, 2)
        // Entry at 07:00, current time 10:00 (3 hours worked so far, expected 9h = 540m)
        val punches = listOf(
            PunchEntity(profileId = 1, date = today, time = LocalTime.of(7, 0), timestampEpochMs = 1, type = PunchType.ENTRY)
        )

        val scenarios = ExitCalculator.calculateScenarios(
            currentTime = LocalTime.of(10, 0),
            punches = punches,
            expectedMinutes = 540,
            defaultLunchMinutes = 60,
            currentBankMinutes = 0L
        )

        assertTrue(scenarios.isNotEmpty())
        val normalScenario = scenarios.first()
        assertEquals("Cumprir jornada normal", normalScenario.scenarioName)
        // 3h worked so far. Remaining work = 6h. Remaining lunch = 1h. Total remaining = 7h.
        // Current 10:00 + 7h = 17:00!
        assertEquals("17:00", normalScenario.projectedExitTime)
    }

    @Test
    fun testExitScenariosWithLunchCompleted() {
        val today = LocalDate.of(2026, 11, 2)
        // Entry 07:00, Lunch out 12:00 (5h), Lunch return 13:00, current time 14:00 (1h) -> 6h worked total!
        val punches = listOf(
            PunchEntity(profileId = 1, date = today, time = LocalTime.of(7, 0), timestampEpochMs = 1, type = PunchType.ENTRY),
            PunchEntity(profileId = 1, date = today, time = LocalTime.of(12, 0), timestampEpochMs = 2, type = PunchType.LUNCH_OUT),
            PunchEntity(profileId = 1, date = today, time = LocalTime.of(13, 0), timestampEpochMs = 3, type = PunchType.LUNCH_RETURN)
        )

        val scenarios = ExitCalculator.calculateScenarios(
            currentTime = LocalTime.of(14, 0),
            punches = punches,
            expectedMinutes = 540, // 9h
            defaultLunchMinutes = 60,
            currentBankMinutes = 0L
        )

        val normalScenario = scenarios.first()
        // 6h worked so far. Remaining = 3h. No lunch remaining.
        // Current 14:00 + 3h = 17:00!
        assertEquals("17:00", normalScenario.projectedExitTime)
    }

    @Test
    fun testExitScenariosWithNegativeBank() {
        val today = LocalDate.of(2026, 11, 2)
        val punches = listOf(
            PunchEntity(profileId = 1, date = today, time = LocalTime.of(7, 0), timestampEpochMs = 1, type = PunchType.ENTRY)
        )

        // Current bank is -120 minutes (-02:00)
        val scenarios = ExitCalculator.calculateScenarios(
            currentTime = LocalTime.of(10, 0),
            punches = punches,
            expectedMinutes = 540,
            defaultLunchMinutes = 60,
            currentBankMinutes = -120L
        )

        val bankZero = scenarios.find { it.scenarioName.contains("zerar banco") }
        assertNotNull(bankZero)
        // Normal 17:00 + 2h compensation = 19:00!
        assertEquals("19:00", bankZero!!.projectedExitTime)
    }
}
