package com.clockinreminder.app.domain.engine

import com.clockinreminder.app.data.local.entity.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class CalculationEngineTest {

    private val defaultPayRule = PayRuleEntity(
        profileId = 1,
        effectiveFromDate = LocalDate.of(2026, 1, 1),
        hourlyRateCents = 2574L, // R$ 25,74
        standardOvertimePercent = 50,
        saturdayOvertimePercent = 60,
        sundayOvertimePercent = 100,
        holidayOvertimePercent = 100,
        nightAdditionPercent = 30,
        isReducedNightHour = true
    )

    private val defaultSchedule = WorkScheduleEntity(
        id = 1,
        profileId = 1,
        name = "Padrão",
        effectiveFromDate = LocalDate.of(2026, 1, 1),
        dailyToleranceMinutes = 0
    )

    private val weekdaySchedule = ScheduleDayEntity(
        id = 1,
        scheduleId = 1,
        dayOfWeek = 1, // Monday
        isWorkDay = true,
        startTime = LocalTime.of(7, 0),
        lunchStartTime = LocalTime.of(12, 0),
        lunchEndTime = LocalTime.of(13, 0),
        endTime = LocalTime.of(17, 0),
        expectedMinutes = 540 // 9h
    )

    @Test
    fun testNormalWorkdayWithOvertime() {
        val date = LocalDate.of(2026, 10, 5) // Monday
        val punches = listOf(
            PunchEntity(profileId = 1, date = date, time = LocalTime.of(7, 0), timestampEpochMs = 1, type = PunchType.ENTRY),
            PunchEntity(profileId = 1, date = date, time = LocalTime.of(12, 0), timestampEpochMs = 2, type = PunchType.LUNCH_OUT),
            PunchEntity(profileId = 1, date = date, time = LocalTime.of(13, 0), timestampEpochMs = 3, type = PunchType.LUNCH_RETURN),
            PunchEntity(profileId = 1, date = date, time = LocalTime.of(18, 0), timestampEpochMs = 4, type = PunchType.EXIT)
        )

        val result = CalculationEngine.calculateDay(
            date = date,
            punches = punches,
            schedule = defaultSchedule,
            scheduleDay = weekdaySchedule,
            payRule = defaultPayRule,
            bankPolicy = null,
            occurrence = null,
            holiday = null
        )

        // 7h to 12h = 5h (300 min), 13h to 18h = 5h (300 min) -> 10h = 600 min worked
        assertEquals(600, result.workedMinutes)
        assertEquals(540, result.expectedMinutes)
        assertEquals(540, result.normalMinutes)
        assertEquals(60, result.overtime50Minutes)
        assertEquals(60, result.dailyBalanceMinutes)

        // Reconciliation: normal + ot50 + ot60 + ot100 == worked
        assertEquals(result.workedMinutes, result.normalMinutes + result.overtime50Minutes + result.overtime60Minutes + result.overtime100Minutes)
    }

    @Test
    fun testUnderExpectedHours() {
        val date = LocalDate.of(2026, 10, 5) // Monday
        val punches = listOf(
            PunchEntity(profileId = 1, date = date, time = LocalTime.of(7, 0), timestampEpochMs = 1, type = PunchType.ENTRY),
            PunchEntity(profileId = 1, date = date, time = LocalTime.of(12, 0), timestampEpochMs = 2, type = PunchType.LUNCH_OUT),
            PunchEntity(profileId = 1, date = date, time = LocalTime.of(13, 0), timestampEpochMs = 3, type = PunchType.LUNCH_RETURN),
            PunchEntity(profileId = 1, date = date, time = LocalTime.of(16, 30), timestampEpochMs = 4, type = PunchType.EXIT)
        )

        val result = CalculationEngine.calculateDay(
            date = date,
            punches = punches,
            schedule = defaultSchedule,
            scheduleDay = weekdaySchedule,
            payRule = defaultPayRule,
            bankPolicy = null,
            occurrence = null,
            holiday = null
        )

        // 300 min + 210 min = 510 min (8h30)
        assertEquals(510, result.workedMinutes)
        assertEquals(540, result.expectedMinutes)
        assertEquals(-30, result.dailyBalanceMinutes)
    }

    @Test
    fun testSaturdayWorkdayRate60() {
        val date = LocalDate.of(2026, 10, 10) // Saturday
        val saturdaySchedule = ScheduleDayEntity(
            id = 6,
            scheduleId = 1,
            dayOfWeek = 6,
            isWorkDay = false,
            expectedMinutes = 0
        )

        // 2 hours worked on non-scheduled Saturday
        val punches = listOf(
            PunchEntity(profileId = 1, date = date, time = LocalTime.of(8, 0), timestampEpochMs = 1, type = PunchType.ENTRY),
            PunchEntity(profileId = 1, date = date, time = LocalTime.of(10, 0), timestampEpochMs = 2, type = PunchType.EXIT)
        )

        val result = CalculationEngine.calculateDay(
            date = date,
            punches = punches,
            schedule = defaultSchedule,
            scheduleDay = saturdaySchedule,
            payRule = defaultPayRule,
            bankPolicy = null,
            occurrence = null,
            holiday = null
        )

        assertEquals(120, result.workedMinutes)
        assertEquals(120, result.overtime60Minutes)
        assertEquals(0, result.normalMinutes)

        // Pay: 2 hours at R$ 25,74 * 1.60 = 82.368 -> 8237 cents!
        // Calculation: (120 * 2574 * 160) / 6000 = 8236.8 -> 8236 or 8237 cents
        val expectedPayCents = (120L * 2574L * 160L) / (60L * 100L)
        assertEquals(expectedPayCents, result.ot60PayCents)
        assertEquals(8236L, result.ot60PayCents) // 82,36 or 82,37
    }

    @Test
    fun testSundayWorkdayRate100() {
        val date = LocalDate.of(2026, 10, 11) // Sunday
        val sundaySchedule = ScheduleDayEntity(
            id = 7,
            scheduleId = 1,
            dayOfWeek = 7,
            isWorkDay = false,
            expectedMinutes = 0
        )

        val punches = listOf(
            PunchEntity(profileId = 1, date = date, time = LocalTime.of(8, 0), timestampEpochMs = 1, type = PunchType.ENTRY),
            PunchEntity(profileId = 1, date = date, time = LocalTime.of(12, 0), timestampEpochMs = 2, type = PunchType.EXIT)
        )

        val result = CalculationEngine.calculateDay(
            date = date,
            punches = punches,
            schedule = defaultSchedule,
            scheduleDay = sundaySchedule,
            payRule = defaultPayRule,
            bankPolicy = null,
            occurrence = null,
            holiday = null
        )

        assertEquals(240, result.workedMinutes)
        assertEquals(240, result.overtime100Minutes)
        assertEquals(0, result.normalMinutes)
    }

    @Test
    fun testNightShiftCalculation() {
        // Night shift 22:00 to 05:00 = 7 civil hours = 420 minutes
        val nightCivil = CalculationEngine.calculateNightMinutes(
            startMinutes = 22 * 60, // 1320
            endMinutes = 24 * 60 + 5 * 60 // 1740 = next day 05:00
        )
        assertEquals(420, nightCivil) // 7h

        // Reduced night hour: 420 * 60 / 52.5 = 480 minutes = exactly 8h!
        val reduced = (nightCivil * (60.0 / 52.5)).toInt()
        assertEquals(480, reduced)
    }

    @Test
    fun testPaidAbsenceNoDebit() {
        val date = LocalDate.of(2026, 10, 6)
        val occurrence = DayOccurrenceEntity(
            profileId = 1,
            date = date,
            type = DayType.ATESTADO,
            note = "Atestado médico 1 dia",
            isPaidAbsence = true
        )

        val result = CalculationEngine.calculateDay(
            date = date,
            punches = emptyList(),
            schedule = defaultSchedule,
            scheduleDay = weekdaySchedule,
            payRule = defaultPayRule,
            bankPolicy = null,
            occurrence = occurrence,
            holiday = null
        )

        assertEquals(0, result.expectedMinutes)
        assertEquals(0, result.dailyBalanceMinutes)
        assertEquals(DayType.ATESTADO, result.dayType)
    }

    @Test
    fun testMunicipalHolidayAndFolgaWorkReceives100Percent() {
        val date = LocalDate.of(2026, 10, 7) // Wednesday
        val punches = listOf(
            PunchEntity(profileId = 1, date = date, time = LocalTime.of(8, 0), timestampEpochMs = 1, type = PunchType.ENTRY),
            PunchEntity(profileId = 1, date = date, time = LocalTime.of(12, 0), timestampEpochMs = 2, type = PunchType.LUNCH_OUT),
            PunchEntity(profileId = 1, date = date, time = LocalTime.of(13, 0), timestampEpochMs = 3, type = PunchType.LUNCH_RETURN),
            PunchEntity(profileId = 1, date = date, time = LocalTime.of(17, 0), timestampEpochMs = 4, type = PunchType.EXIT)
        )

        // Case 1: Marked as DayType.FERIADO (e.g. municipal holiday)
        val holidayOccurrence = DayOccurrenceEntity(
            profileId = 1,
            date = date,
            type = DayType.FERIADO,
            note = "Feriado Municipal de Aniversário da Cidade"
        )
        val resultHoliday = CalculationEngine.calculateDay(
            date = date,
            punches = punches,
            schedule = defaultSchedule,
            scheduleDay = weekdaySchedule,
            payRule = defaultPayRule,
            bankPolicy = null,
            occurrence = holidayOccurrence,
            holiday = null
        )

        // 8h worked (480 min) on a holiday -> 0 expected, all 480 min as overtime100
        assertEquals(480, resultHoliday.workedMinutes)
        assertEquals(0, resultHoliday.expectedMinutes)
        assertEquals(0, resultHoliday.normalMinutes)
        assertEquals(480, resultHoliday.overtime100Minutes)
        assertEquals(480, resultHoliday.dailyBalanceMinutes)

        // Case 2: Marked as DayType.FOLGA (e.g. day off worked in 6x1 or 6x2 scale)
        val folgaOccurrence = DayOccurrenceEntity(
            profileId = 1,
            date = date,
            type = DayType.FOLGA,
            note = "Folga Trabalhada Escala 6x1"
        )
        val resultFolga = CalculationEngine.calculateDay(
            date = date,
            punches = punches,
            schedule = defaultSchedule,
            scheduleDay = weekdaySchedule,
            payRule = defaultPayRule,
            bankPolicy = null,
            occurrence = folgaOccurrence,
            holiday = null
        )

        // 8h worked on a day off (Folga) -> all 480 min count as 100% overtime
        assertEquals(480, resultFolga.workedMinutes)
        assertEquals(0, resultFolga.expectedMinutes)
        assertEquals(0, resultFolga.normalMinutes)
        assertEquals(480, resultFolga.overtime100Minutes)
        assertEquals(480, resultFolga.dailyBalanceMinutes)
    }

    @Test
    fun testMoreThanFourPunchesCalculation() {
        val date = LocalDate.of(2026, 10, 8)
        // 6 punches: 3 work intervals (08:00-12:00 = 4h, 13:00-17:00 = 4h, 18:00-20:00 = 2h) = 10h worked
        val punches = listOf(
            PunchEntity(profileId = 1, date = date, time = LocalTime.of(8, 0), timestampEpochMs = 1, type = PunchType.ENTRY),
            PunchEntity(profileId = 1, date = date, time = LocalTime.of(12, 0), timestampEpochMs = 2, type = PunchType.LUNCH_OUT),
            PunchEntity(profileId = 1, date = date, time = LocalTime.of(13, 0), timestampEpochMs = 3, type = PunchType.LUNCH_RETURN),
            PunchEntity(profileId = 1, date = date, time = LocalTime.of(17, 0), timestampEpochMs = 4, type = PunchType.EXIT),
            PunchEntity(profileId = 1, date = date, time = LocalTime.of(18, 0), timestampEpochMs = 5, type = PunchType.ENTRY),
            PunchEntity(profileId = 1, date = date, time = LocalTime.of(20, 0), timestampEpochMs = 6, type = PunchType.EXIT)
        )

        val result = CalculationEngine.calculateDay(
            date = date,
            punches = punches,
            schedule = defaultSchedule,
            scheduleDay = weekdaySchedule,
            payRule = defaultPayRule,
            bankPolicy = null,
            occurrence = null,
            holiday = null
        )

        assertEquals(600, result.workedMinutes) // 10 hours
        assertEquals(540, result.expectedMinutes)
        assertEquals(540, result.normalMinutes)
        assertEquals(60, result.overtime50Minutes) // 1 hour extra
    }

    @Test
    fun testPjWeekdayOvertimeNormalRate() {
        val pjPayRule = defaultPayRule.copy(standardOvertimePercent = 0) // 0% addition = normal hourly rate
        val date = LocalDate.of(2026, 10, 5) // Monday
        val punches = listOf(
            PunchEntity(profileId = 1, date = date, time = LocalTime.of(7, 0), timestampEpochMs = 1, type = PunchType.ENTRY),
            PunchEntity(profileId = 1, date = date, time = LocalTime.of(12, 0), timestampEpochMs = 2, type = PunchType.LUNCH_OUT),
            PunchEntity(profileId = 1, date = date, time = LocalTime.of(13, 0), timestampEpochMs = 3, type = PunchType.LUNCH_RETURN),
            PunchEntity(profileId = 1, date = date, time = LocalTime.of(18, 0), timestampEpochMs = 4, type = PunchType.EXIT)
        )

        val result = CalculationEngine.calculateDay(
            date = date,
            punches = punches,
            schedule = defaultSchedule,
            scheduleDay = weekdaySchedule,
            payRule = pjPayRule,
            bankPolicy = null,
            occurrence = null,
            holiday = null
        )

        // 10 hours worked = 600 min. 9h normal (540 min), 1h extra (60 min)
        assertEquals(600, result.workedMinutes)
        assertEquals(540, result.normalMinutes)
        assertEquals(60, result.overtime50Minutes)

        // As PJ (0% extra), the 1 hour of overtime is paid at normal rate: 1h * 2574 cents = 2574 cents
        assertEquals(2574L, result.ot50PayCents)
        // Total gross pay is 10h * R$ 25,74 = R$ 257,40 (25740 cents)
        assertEquals(25740L, result.estimatedGrossPayCents)
    }

    @Test
    fun testFormatMinutes() {
        assertEquals("+01:30", CalculationEngine.formatMinutesToHours(90, withSign = true))
        assertEquals("−00:45", CalculationEngine.formatMinutesToHours(-45, withSign = true))
        assertEquals("09:00", CalculationEngine.formatMinutesToHours(540, withSign = false))
    }

    @Test
    fun testFormatCurrency() {
        assertEquals("R$ 25,74", CalculationEngine.formatCentsToCurrency(2574L))
        assertEquals("R$ 5.167,30", CalculationEngine.formatCentsToCurrency(516730L))
    }
}
