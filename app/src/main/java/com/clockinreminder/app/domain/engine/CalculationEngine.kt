package com.clockinreminder.app.domain.engine

import com.clockinreminder.app.data.local.entity.*
import com.clockinreminder.app.domain.model.DayCalculationResult
import com.clockinreminder.app.domain.model.MonthSummaryResult
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import kotlin.math.roundToInt

object CalculationEngine {

    /**
     * Represents a single contiguous work interval from an Entry to an Exit/Break.
     */
    data class WorkInterval(
        val startMinutes: Int, // Minutes from midnight of shift date (can exceed 1440 if crossing midnight)
        val endMinutes: Int
    ) {
        val durationMinutes: Int get() = (endMinutes - startMinutes).coerceAtLeast(0)
    }

    /**
     * Builds working intervals from a list of sorted punches.
     */
    fun extractWorkIntervals(punches: List<PunchEntity>): List<WorkInterval> {
        val validPunches = punches.filter { !it.isDeleted }.sortedWith(
            compareBy<PunchEntity> { it.date }.thenBy { it.time }
        )
        if (validPunches.isEmpty()) return emptyList()

        val intervals = mutableListOf<WorkInterval>()
        var openStartMinutes: Int? = null

        val baseDate = validPunches.first().date

        for (punch in validPunches) {
            val dayOffset = java.time.temporal.ChronoUnit.DAYS.between(baseDate, punch.date).toInt()
            val punchMinutes = dayOffset * 1440 + punch.time.hour * 60 + punch.time.minute

            when (punch.type) {
                PunchType.ENTRY, PunchType.LUNCH_RETURN -> {
                    if (openStartMinutes == null) {
                        openStartMinutes = punchMinutes
                    }
                }
                PunchType.LUNCH_OUT, PunchType.EXIT -> {
                    if (openStartMinutes != null) {
                        if (punchMinutes >= openStartMinutes) {
                            intervals.add(WorkInterval(openStartMinutes, punchMinutes))
                        }
                        openStartMinutes = null
                    }
                }
            }
        }

        return intervals
    }

    /**
     * Calculates night duration for an interval.
     * Standard Brazilian night shift window: 22:00 (1320 min) to 05:00 (300 min).
     */
    fun calculateNightMinutes(
        startMinutes: Int,
        endMinutes: Int,
        nightStartMin: Int = 1320, // 22:00
        nightEndMin: Int = 300     // 05:00
    ): Int {
        var nightMinutes = 0
        // Check across a range of days (-1 to +2 days from base)
        for (day in 0..2) {
            val dayStart = day * 1440
            // Night window 1: 00:00 to 05:00 of day
            val win1Start = dayStart
            val win1End = dayStart + nightEndMin
            val overlap1 = (minOf(endMinutes, win1End) - maxOf(startMinutes, win1Start)).coerceAtLeast(0)
            nightMinutes += overlap1

            // Night window 2: 22:00 to 24:00 of day
            val win2Start = dayStart + nightStartMin
            val win2End = dayStart + 1440
            val overlap2 = (minOf(endMinutes, win2End) - maxOf(startMinutes, win2Start)).coerceAtLeast(0)
            nightMinutes += overlap2
        }
        return nightMinutes
    }

    /**
     * Calculates the results for a single day.
     */
    fun calculateDay(
        date: LocalDate,
        punches: List<PunchEntity>,
        schedule: WorkScheduleEntity?,
        scheduleDay: ScheduleDayEntity?,
        payRule: PayRuleEntity,
        bankPolicy: BankPolicyEntity?,
        occurrence: DayOccurrenceEntity?,
        holiday: HolidayEntity?
    ): DayCalculationResult {
        val dayOfWeek = date.dayOfWeek
        val isSaturday = dayOfWeek == DayOfWeek.SATURDAY
        val isSunday = dayOfWeek == DayOfWeek.SUNDAY
        val isHoliday = holiday != null

        // Determine effective day type
        val effectiveDayType = when {
            occurrence != null -> occurrence.type
            isHoliday -> DayType.FERIADO
            isSunday -> DayType.DSR
            scheduleDay != null && !scheduleDay.isWorkDay -> DayType.FOLGA
            else -> DayType.NORMAL
        }

        // Expected minutes for the day
        val expectedMinutes = when {
            occurrence != null && occurrence.isPaidAbsence -> 0 // Handled as paid absence, no penalty
            occurrence != null && occurrence.type == DayType.FALTA -> scheduleDay?.expectedMinutes ?: 540
            occurrence != null && (occurrence.type == DayType.FERIADO || occurrence.type == DayType.FOLGA || occurrence.type == DayType.DSR) -> 0
            isHoliday -> 0
            effectiveDayType == DayType.FOLGA || effectiveDayType == DayType.FERIADO || effectiveDayType == DayType.DSR -> 0
            schedule?.isHoursOnly == true -> schedule.dailyHoursTargetMinutes
            scheduleDay != null && scheduleDay.isWorkDay -> scheduleDay.expectedMinutes
            else -> 0
        }

        // Calculate worked intervals
        val intervals = extractWorkIntervals(punches)
        val totalWorkedMinutes = intervals.sumOf { it.durationMinutes }

        // Night minutes
        var civilNightMinutes = 0
        val nightStartMin = payRule.nightStartHour * 60 + payRule.nightStartMinute
        val nightEndMin = payRule.nightEndHour * 60 + payRule.nightEndMinute
        for (interval in intervals) {
            civilNightMinutes += calculateNightMinutes(interval.startMinutes, interval.endMinutes, nightStartMin, nightEndMin)
        }

        // Reduced night hour: CLT 52m30s -> factor = 60.0 / 52.5 = 1.14285714
        val reducedNightMinutes = if (payRule.isReducedNightHour) {
            (civilNightMinutes * (60.0 / 52.5)).roundToInt()
        } else {
            civilNightMinutes
        }

        // Classify worked minutes into mutually exclusive categories:
        // normal, overtime50, overtime60, overtime100
        var normalMin = 0
        var ot50Min = 0
        var ot60Min = 0
        var ot100Min = 0

        when {
            // Holiday (including municipal/manual), DSR or Sunday: all worked hours are 100%
            isHoliday || effectiveDayType == DayType.FERIADO || effectiveDayType == DayType.DSR || isSunday -> {
                ot100Min = totalWorkedMinutes
            }

            // Saturday: Pedro's rule - 60% overtime rate
            isSaturday -> {
                if (scheduleDay != null && scheduleDay.isWorkDay) {
                    // Scheduled Saturday workday: normal up to expected, excess 60%
                    normalMin = minOf(totalWorkedMinutes, expectedMinutes)
                    ot60Min = (totalWorkedMinutes - expectedMinutes).coerceAtLeast(0)
                } else {
                    // Non-scheduled Saturday: 60% overtime
                    ot60Min = totalWorkedMinutes
                }
            }

            // Day off in scale (Folga trabalhada em escala 6x1, 6x2): 100%
            effectiveDayType == DayType.FOLGA -> {
                ot100Min = totalWorkedMinutes
            }

            // Normal weekday:
            else -> {
                if (expectedMinutes > 0) {
                    normalMin = minOf(totalWorkedMinutes, expectedMinutes)
                    ot50Min = (totalWorkedMinutes - expectedMinutes).coerceAtLeast(0)
                } else {
                    // Day with 0 expected hours (e.g. day off)
                    ot50Min = totalWorkedMinutes
                }
            }
        }

        // Apply daily tolerance if configured (e.g. +/- 10 min tolerance)
        val dailyTolerance = schedule?.dailyToleranceMinutes ?: 0
        if (dailyTolerance > 0 && expectedMinutes > 0 && totalWorkedMinutes > 0) {
            val diff = totalWorkedMinutes - expectedMinutes
            if (kotlin.math.abs(diff) <= dailyTolerance) {
                // Within tolerance: treat as exactly expected
                normalMin = totalWorkedMinutes
                ot50Min = 0
            }
        }

        // Saldo diário
        val dailyBalanceMinutes = when {
            occurrence != null && occurrence.type == DayType.FALTA -> -expectedMinutes
            occurrence != null && occurrence.type.isAbsence -> 0 // Paid absence (Férias, Atestado, etc.)
            effectiveDayType == DayType.FOLGA || effectiveDayType == DayType.FERIADO || effectiveDayType == DayType.DSR -> totalWorkedMinutes
            else -> totalWorkedMinutes - expectedMinutes
        }

        // Bank allocation
        val bankMinutes = if (bankPolicy != null && bankPolicy.isEnabled && bankPolicy.autoAccumulate) {
            if (bankPolicy.sendOvertimeToBank) {
                dailyBalanceMinutes
            } else {
                // If overtime is paid out, only negative balance or non-overtime balance goes to bank
                if (dailyBalanceMinutes < 0) dailyBalanceMinutes else 0
            }
        } else {
            0
        }

        // Financial calculations in cents
        val hourlyRate = payRule.hourlyRateCents

        val normalPayCents = (normalMin * hourlyRate) / 60L
        val ot50PayCents = (ot50Min * hourlyRate * (100 + payRule.standardOvertimePercent)) / (60L * 100L)
        val ot60PayCents = (ot60Min * hourlyRate * (100 + payRule.saturdayOvertimePercent)) / (60L * 100L)
        val ot100PayCents = (ot100Min * hourlyRate * (100 + payRule.holidayOvertimePercent)) / (60L * 100L)

        // Night addition bonus
        val nightDurationForPay = if (payRule.isReducedNightHour) reducedNightMinutes else civilNightMinutes
        val nightBonusPayCents = (nightDurationForPay * hourlyRate * payRule.nightAdditionPercent) / (60L * 100L)

        val totalGrossPayCents = normalPayCents + ot50PayCents + ot60PayCents + ot100PayCents + nightBonusPayCents

        // Inconsistency checks for this day
        val inconsistencies = mutableListOf<String>()
        if (punches.size % 2 != 0) {
            inconsistencies.add("Número ímpar de marcações (marcação pendente)")
        }
        if (expectedMinutes > 0 && totalWorkedMinutes == 0 && occurrence == null && date.isBefore(LocalDate.now())) {
            inconsistencies.add("Jornada prevista sem registros de ponto")
        }

        return DayCalculationResult(
            date = date,
            expectedMinutes = expectedMinutes,
            workedMinutes = totalWorkedMinutes,
            normalMinutes = normalMin,
            overtime50Minutes = ot50Min,
            overtime60Minutes = ot60Min,
            overtime100Minutes = ot100Min,
            customOvertimeMinutes = 0,
            nightMinutesCivil = civilNightMinutes,
            nightMinutesReduced = reducedNightMinutes,
            dailyBalanceMinutes = dailyBalanceMinutes,
            bankMinutes = bankMinutes,
            normalPayCents = normalPayCents,
            ot50PayCents = ot50PayCents,
            ot60PayCents = ot60PayCents,
            ot100PayCents = ot100PayCents,
            nightBonusPayCents = nightBonusPayCents,
            estimatedGrossPayCents = totalGrossPayCents,
            dayType = effectiveDayType,
            punches = punches.filter { !it.isDeleted },
            occurrence = occurrence,
            holiday = holiday,
            inconsistencies = inconsistencies
        )
    }

    /**
     * Calculates the monthly summary across all days of the month.
     */
    fun calculateMonth(
        yearMonth: YearMonth,
        dayResults: List<DayCalculationResult>,
        startingBankMinutes: Long,
        adjustments: List<BankAdjustmentEntity>,
        isWeekdayOtNormal: Boolean = false
    ): MonthSummaryResult {
        var totalExpected = 0
        var totalWorked = 0
        var totalNormal = 0
        var totalOt50 = 0
        var totalOt60 = 0
        var totalOt100 = 0
        var totalCustomOt = 0
        var totalNightCivil = 0
        var totalNightReduced = 0
        var totalDailyBalance = 0

        var normalPay = 0L
        var ot50Pay = 0L
        var ot60Pay = 0L
        var ot100Pay = 0L
        var nightBonusPay = 0L
        var grossPay = 0L

        var daysWorked = 0
        var daysOff = 0
        var daysHoliday = 0
        var daysAbsence = 0
        var daysSick = 0
        var daysVacation = 0

        for (day in dayResults) {
            totalExpected += day.expectedMinutes
            totalWorked += day.workedMinutes
            totalNormal += day.normalMinutes
            totalOt50 += day.overtime50Minutes
            totalOt60 += day.overtime60Minutes
            totalOt100 += day.overtime100Minutes
            totalCustomOt += day.customOvertimeMinutes
            totalNightCivil += day.nightMinutesCivil
            totalNightReduced += day.nightMinutesReduced
            totalDailyBalance += day.dailyBalanceMinutes

            normalPay += day.normalPayCents
            ot50Pay += day.ot50PayCents
            ot60Pay += day.ot60PayCents
            ot100Pay += day.ot100PayCents
            nightBonusPay += day.nightBonusPayCents
            grossPay += day.estimatedGrossPayCents

            if (day.workedMinutes > 0) daysWorked++
            when (day.dayType) {
                DayType.FOLGA, DayType.DSR -> daysOff++
                DayType.FERIADO -> daysHoliday++
                DayType.FALTA -> daysAbsence++
                DayType.ATESTADO -> daysSick++
                DayType.FERIAS -> daysVacation++
                else -> {}
            }
        }

        // Bank calculations
        var bankCredits = 0L
        var bankDebits = 0L
        for (day in dayResults) {
            if (day.bankMinutes > 0) bankCredits += day.bankMinutes
            else if (day.bankMinutes < 0) bankDebits += kotlin.math.abs(day.bankMinutes)
        }

        val adjustmentTotal = adjustments.sumOf { it.adjustmentMinutes }
        val closingBank = startingBankMinutes + bankCredits - bankDebits + adjustmentTotal

        return MonthSummaryResult(
            yearMonth = yearMonth,
            totalExpectedMinutes = totalExpected,
            totalWorkedMinutes = totalWorked,
            totalNormalMinutes = totalNormal,
            totalOt50Minutes = totalOt50,
            totalOt60Minutes = totalOt60,
            totalOt100Minutes = totalOt100,
            totalCustomOtMinutes = totalCustomOt,
            totalNightMinutesCivil = totalNightCivil,
            totalNightMinutesReduced = totalNightReduced,
            totalDailyBalanceMinutes = totalDailyBalance,
            startingBankMinutes = startingBankMinutes,
            bankCreditsMinutes = bankCredits,
            bankDebitsMinutes = bankDebits,
            bankAdjustmentsMinutes = adjustmentTotal,
            closingBankMinutes = closingBank,
            totalNormalPayCents = normalPay,
            totalOt50PayCents = ot50Pay,
            totalOt60PayCents = ot60Pay,
            totalOt100PayCents = ot100Pay,
            totalNightBonusPayCents = nightBonusPay,
            totalGrossPayCents = grossPay,
            daysWorked = daysWorked,
            daysOff = daysOff,
            daysHoliday = daysHoliday,
            daysAbsence = daysAbsence,
            daysSick = daysSick,
            daysVacation = daysVacation,
            isWeekdayOtNormal = isWeekdayOtNormal,
            dayResults = dayResults
        )
    }

    /**
     * Formats integer minutes into "+HH:mm" or "-HH:mm" or "HH:mm".
     */
    fun formatMinutesToHours(minutes: Int, withSign: Boolean = false): String {
        val sign = if (minutes < 0) "−" else if (withSign && minutes > 0) "+" else ""
        val absMinutes = kotlin.math.abs(minutes)
        val h = absMinutes / 60
        val m = absMinutes % 60
        return "%s%02d:%02d".format(sign, h, m)
    }

    /**
     * Formats cents into "R$ 1.234,56".
     */
    fun formatCentsToCurrency(cents: Long): String {
        val sign = if (cents < 0) "- " else ""
        val absCents = kotlin.math.abs(cents)
        val reais = absCents / 100
        val centavos = absCents % 100
        val formattedReais = "%,d".format(reais).replace(',', '.')
        return "%sR$ %s,%02d".format(sign, formattedReais, centavos)
    }
}
