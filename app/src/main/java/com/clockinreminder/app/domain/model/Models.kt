package com.clockinreminder.app.domain.model

import com.clockinreminder.app.data.local.entity.DayOccurrenceEntity
import com.clockinreminder.app.data.local.entity.DayType
import com.clockinreminder.app.data.local.entity.HolidayEntity
import com.clockinreminder.app.data.local.entity.PunchEntity
import java.time.LocalDate
import java.time.YearMonth

data class DayCalculationResult(
    val date: LocalDate,
    val expectedMinutes: Int,
    val workedMinutes: Int,
    val normalMinutes: Int,
    val overtime50Minutes: Int,
    val overtime60Minutes: Int,
    val overtime100Minutes: Int,
    val customOvertimeMinutes: Int = 0,
    val nightMinutesCivil: Int,
    val nightMinutesReduced: Int,
    val dailyBalanceMinutes: Int, // (+/-) worked - expected (adjusted for paid absence)
    val bankMinutes: Int, // Amount credited or debited to bank
    val normalPayCents: Long,
    val ot50PayCents: Long,
    val ot60PayCents: Long,
    val ot100PayCents: Long,
    val nightBonusPayCents: Long,
    val estimatedGrossPayCents: Long,
    val dayType: DayType,
    val punches: List<PunchEntity> = emptyList(),
    val occurrence: DayOccurrenceEntity? = null,
    val holiday: HolidayEntity? = null,
    val inconsistencies: List<String> = emptyList(),
    val calculationNotes: String = ""
)

data class MonthSummaryResult(
    val yearMonth: YearMonth,
    val totalExpectedMinutes: Int,
    val totalWorkedMinutes: Int,
    val totalNormalMinutes: Int,
    val totalOt50Minutes: Int,
    val totalOt60Minutes: Int,
    val totalOt100Minutes: Int,
    val totalCustomOtMinutes: Int = 0,
    val totalNightMinutesCivil: Int,
    val totalNightMinutesReduced: Int,
    val totalDailyBalanceMinutes: Int,
    val startingBankMinutes: Long,
    val bankCreditsMinutes: Long,
    val bankDebitsMinutes: Long,
    val bankAdjustmentsMinutes: Long,
    val closingBankMinutes: Long,
    val totalNormalPayCents: Long,
    val totalOt50PayCents: Long,
    val totalOt60PayCents: Long,
    val totalOt100PayCents: Long,
    val totalNightBonusPayCents: Long,
    val totalGrossPayCents: Long,
    val daysWorked: Int,
    val daysOff: Int,
    val daysHoliday: Int,
    val daysAbsence: Int,
    val daysSick: Int,
    val daysVacation: Int,
    val isWeekdayOtNormal: Boolean = false,
    val dayResults: List<DayCalculationResult>
)

data class InconsistencyItem(
    val id: String,
    val date: LocalDate,
    val title: String,
    val description: String,
    val severity: InconsistencySeverity,
    val suggestedAction: String
)

enum class InconsistencySeverity {
    WARNING,
    ERROR
}

data class ExitSimulationResult(
    val scenarioName: String,
    val projectedExitTime: String,
    val targetMinutes: Int,
    val remainingMinutes: Int,
    val explanation: String
)
