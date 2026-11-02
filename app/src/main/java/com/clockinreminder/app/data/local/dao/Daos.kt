package com.clockinreminder.app.data.local.dao

import androidx.room.*
import com.clockinreminder.app.data.local.entity.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profiles LIMIT 1")
    fun getProfileFlow(): Flow<ProfileEntity?>

    @Query("SELECT * FROM profiles LIMIT 1")
    suspend fun getProfile(): ProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: ProfileEntity): Long

    @Update
    suspend fun updateProfile(profile: ProfileEntity)
}

@Dao
interface WorkScheduleDao {
    @Query("SELECT * FROM work_schedules WHERE profileId = :profileId ORDER BY effectiveFromDate DESC")
    fun getSchedulesFlow(profileId: Long): Flow<List<WorkScheduleEntity>>

    @Query("SELECT * FROM work_schedules WHERE profileId = :profileId AND effectiveFromDate <= :date ORDER BY effectiveFromDate DESC LIMIT 1")
    suspend fun getEffectiveSchedule(profileId: Long, date: LocalDate): WorkScheduleEntity?

    @Query("SELECT * FROM schedule_days WHERE scheduleId = :scheduleId")
    suspend fun getScheduleDays(scheduleId: Long): List<ScheduleDayEntity>

    @Query("SELECT * FROM schedule_days WHERE scheduleId = :scheduleId")
    fun getScheduleDaysFlow(scheduleId: Long): Flow<List<ScheduleDayEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: WorkScheduleEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScheduleDays(days: List<ScheduleDayEntity>)

    @Update
    suspend fun updateSchedule(schedule: WorkScheduleEntity)

    @Update
    suspend fun updateScheduleDay(day: ScheduleDayEntity)
}

@Dao
interface PunchDao {
    @Query("SELECT * FROM punches WHERE profileId = :profileId AND date = :date AND isDeleted = 0 ORDER BY time ASC")
    fun getPunchesForDateFlow(profileId: Long, date: LocalDate): Flow<List<PunchEntity>>

    @Query("SELECT * FROM punches WHERE profileId = :profileId AND date = :date AND isDeleted = 0 ORDER BY time ASC")
    suspend fun getPunchesForDate(profileId: Long, date: LocalDate): List<PunchEntity>

    @Query("SELECT * FROM punches WHERE profileId = :profileId AND date BETWEEN :startDate AND :endDate AND isDeleted = 0 ORDER BY date ASC, time ASC")
    fun getPunchesForRangeFlow(profileId: Long, startDate: LocalDate, endDate: LocalDate): Flow<List<PunchEntity>>

    @Query("SELECT * FROM punches WHERE profileId = :profileId AND date BETWEEN :startDate AND :endDate AND isDeleted = 0 ORDER BY date ASC, time ASC")
    suspend fun getPunchesForRange(profileId: Long, startDate: LocalDate, endDate: LocalDate): List<PunchEntity>

    @Query("SELECT * FROM punches WHERE profileId = :profileId AND isDeleted = 0 ORDER BY date DESC, time DESC LIMIT 1")
    fun getLatestPunchFlow(profileId: Long): Flow<PunchEntity?>

    @Query("SELECT * FROM punches WHERE profileId = :profileId AND isDeleted = 0 ORDER BY date DESC, time DESC LIMIT 1")
    suspend fun getLatestPunch(profileId: Long): PunchEntity?

    @Query("SELECT * FROM punches WHERE id = :id")
    suspend fun getPunchById(id: Long): PunchEntity?

    @Insert
    suspend fun insertPunch(punch: PunchEntity): Long

    @Update
    suspend fun updatePunch(punch: PunchEntity)

    @Query("UPDATE punches SET isDeleted = 1, updatedAtEpochMs = :timestamp WHERE id = :id")
    suspend fun softDeletePunch(id: Long, timestamp: Long = System.currentTimeMillis())
}

@Dao
interface DayOccurrenceDao {
    @Query("SELECT * FROM day_occurrences WHERE profileId = :profileId AND date = :date LIMIT 1")
    fun getOccurrenceForDateFlow(profileId: Long, date: LocalDate): Flow<DayOccurrenceEntity?>

    @Query("SELECT * FROM day_occurrences WHERE profileId = :profileId AND date = :date LIMIT 1")
    suspend fun getOccurrenceForDate(profileId: Long, date: LocalDate): DayOccurrenceEntity?

    @Query("SELECT * FROM day_occurrences WHERE profileId = :profileId AND date BETWEEN :startDate AND :endDate")
    fun getOccurrencesForRangeFlow(profileId: Long, startDate: LocalDate, endDate: LocalDate): Flow<List<DayOccurrenceEntity>>

    @Query("SELECT * FROM day_occurrences WHERE profileId = :profileId AND date BETWEEN :startDate AND :endDate")
    suspend fun getOccurrencesForRange(profileId: Long, startDate: LocalDate, endDate: LocalDate): List<DayOccurrenceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateOccurrence(occurrence: DayOccurrenceEntity): Long

    @Delete
    suspend fun deleteOccurrence(occurrence: DayOccurrenceEntity)
}

@Dao
interface HolidayDao {
    @Query("SELECT * FROM holidays ORDER BY date ASC")
    fun getAllHolidaysFlow(): Flow<List<HolidayEntity>>

    @Query("SELECT * FROM holidays ORDER BY date ASC")
    suspend fun getAllHolidays(): List<HolidayEntity>

    @Query("SELECT * FROM holidays WHERE date = :date LIMIT 1")
    suspend fun getHolidayOnDate(date: LocalDate): HolidayEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHolidays(holidays: List<HolidayEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHoliday(holiday: HolidayEntity): Long

    @Delete
    suspend fun deleteHoliday(holiday: HolidayEntity)
}

@Dao
interface PayRuleDao {
    @Query("SELECT * FROM pay_rules WHERE profileId = :profileId AND effectiveFromDate <= :date ORDER BY effectiveFromDate DESC LIMIT 1")
    suspend fun getEffectivePayRule(profileId: Long, date: LocalDate): PayRuleEntity?

    @Query("SELECT * FROM pay_rules WHERE profileId = :profileId ORDER BY effectiveFromDate DESC LIMIT 1")
    fun getLatestPayRuleFlow(profileId: Long): Flow<PayRuleEntity?>

    @Query("SELECT * FROM pay_rules WHERE profileId = :profileId ORDER BY effectiveFromDate DESC")
    fun getAllPayRulesFlow(profileId: Long): Flow<List<PayRuleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayRule(rule: PayRuleEntity): Long

    @Update
    suspend fun updatePayRule(rule: PayRuleEntity)
}

@Dao
interface BankPolicyDao {
    @Query("SELECT * FROM bank_policies WHERE profileId = :profileId AND effectiveFromDate <= :date ORDER BY effectiveFromDate DESC LIMIT 1")
    suspend fun getEffectiveBankPolicy(profileId: Long, date: LocalDate): BankPolicyEntity?

    @Query("SELECT * FROM bank_policies WHERE profileId = :profileId ORDER BY effectiveFromDate DESC LIMIT 1")
    fun getLatestBankPolicyFlow(profileId: Long): Flow<BankPolicyEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBankPolicy(policy: BankPolicyEntity): Long

    @Update
    suspend fun updateBankPolicy(policy: BankPolicyEntity)
}

@Dao
interface BankAdjustmentDao {
    @Query("SELECT * FROM bank_adjustments WHERE profileId = :profileId ORDER BY date ASC")
    fun getAllAdjustmentsFlow(profileId: Long): Flow<List<BankAdjustmentEntity>>

    @Query("SELECT * FROM bank_adjustments WHERE profileId = :profileId AND date <= :date")
    suspend fun getAdjustmentsUpToDate(profileId: Long, date: LocalDate): List<BankAdjustmentEntity>

    @Query("SELECT * FROM bank_adjustments WHERE profileId = :profileId AND date BETWEEN :startDate AND :endDate")
    suspend fun getAdjustmentsInRange(profileId: Long, startDate: LocalDate, endDate: LocalDate): List<BankAdjustmentEntity>

    @Insert
    suspend fun insertAdjustment(adjustment: BankAdjustmentEntity): Long

    @Delete
    suspend fun deleteAdjustment(adjustment: BankAdjustmentEntity)
}

@Dao
interface AuditDao {
    @Insert
    suspend fun insertAudit(event: AuditEventEntity): Long

    @Query("SELECT * FROM audit_events WHERE entityType = :type AND entityId = :id ORDER BY timestampEpochMs DESC")
    suspend fun getAuditHistory(type: String, id: Long): List<AuditEventEntity>
}
