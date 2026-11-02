package com.clockinreminder.app.data.repository

import com.clockinreminder.app.data.local.entity.*
import com.clockinreminder.app.domain.model.DayCalculationResult
import com.clockinreminder.app.domain.model.InconsistencyItem
import com.clockinreminder.app.domain.model.MonthSummaryResult
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

interface ClockInRepository {
    fun getProfileFlow(): Flow<ProfileEntity?>
    suspend fun getProfile(): ProfileEntity?
    suspend fun saveProfile(profile: ProfileEntity): Long

    suspend fun getEffectiveSchedule(date: LocalDate): WorkScheduleEntity?
    suspend fun getScheduleDays(scheduleId: Long): List<ScheduleDayEntity>
    suspend fun saveScheduleWithDays(schedule: WorkScheduleEntity, days: List<ScheduleDayEntity>): Long

    fun getPunchesForDateFlow(date: LocalDate): Flow<List<PunchEntity>>
    suspend fun getPunchesForDate(date: LocalDate): List<PunchEntity>
    fun getLatestPunchFlow(): Flow<PunchEntity?>
    suspend fun registerPunch(
        date: LocalDate,
        time: LocalTime,
        type: PunchType,
        source: PunchSource = PunchSource.AUTOMATIC,
        latitude: Double? = null,
        longitude: Double? = null,
        locationDescription: String? = null,
        note: String? = null
    ): Long
    suspend fun updatePunch(punch: PunchEntity, editReason: String)
    suspend fun deletePunch(punchId: Long, deleteReason: String)

    fun getOccurrenceFlow(date: LocalDate): Flow<DayOccurrenceEntity?>
    suspend fun getOccurrence(date: LocalDate): DayOccurrenceEntity?
    suspend fun setOccurrence(occurrence: DayOccurrenceEntity)
    suspend fun deleteOccurrence(occurrence: DayOccurrenceEntity)

    fun getAllHolidaysFlow(): Flow<List<HolidayEntity>>
    suspend fun addHoliday(holiday: HolidayEntity): Long
    suspend fun deleteHoliday(holiday: HolidayEntity)

    suspend fun getEffectivePayRule(date: LocalDate): PayRuleEntity
    fun getPayRuleFlow(): Flow<PayRuleEntity?>
    suspend fun savePayRule(rule: PayRuleEntity): Long

    suspend fun getEffectiveBankPolicy(date: LocalDate): BankPolicyEntity?
    fun getBankPolicyFlow(): Flow<BankPolicyEntity?>
    suspend fun saveBankPolicy(policy: BankPolicyEntity): Long
    suspend fun addBankAdjustment(adjustment: BankAdjustmentEntity): Long
    fun getAllBankAdjustmentsFlow(): Flow<List<BankAdjustmentEntity>>

    suspend fun calculateDay(date: LocalDate): DayCalculationResult
    suspend fun calculateMonth(yearMonth: YearMonth): MonthSummaryResult
    suspend fun detectInconsistenciesForMonth(yearMonth: YearMonth): List<InconsistencyItem>
}
