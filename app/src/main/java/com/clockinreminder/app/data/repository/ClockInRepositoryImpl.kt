package com.clockinreminder.app.data.repository

import com.clockinreminder.app.data.local.AppDatabase
import com.clockinreminder.app.data.local.entity.*
import com.clockinreminder.app.domain.engine.CalculationEngine
import com.clockinreminder.app.domain.engine.InconsistencyDetector
import com.clockinreminder.app.domain.model.DayCalculationResult
import com.clockinreminder.app.domain.model.InconsistencyItem
import com.clockinreminder.app.domain.model.MonthSummaryResult
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

class ClockInRepositoryImpl(
    private val db: AppDatabase
) : ClockInRepository {

    private val profileDao = db.profileDao()
    private val scheduleDao = db.workScheduleDao()
    private val punchDao = db.punchDao()
    private val occurrenceDao = db.dayOccurrenceDao()
    private val holidayDao = db.holidayDao()
    private val payRuleDao = db.payRuleDao()
    private val bankPolicyDao = db.bankPolicyDao()
    private val bankAdjustmentDao = db.bankAdjustmentDao()
    private val auditDao = db.auditDao()

    private suspend fun getCurrentProfileId(): Long {
        return profileDao.getProfile()?.id ?: 1L
    }

    override fun getProfileFlow(): Flow<ProfileEntity?> = profileDao.getProfileFlow()

    override suspend fun getProfile(): ProfileEntity? = profileDao.getProfile()

    override suspend fun saveProfile(profile: ProfileEntity): Long {
        val existing = profileDao.getProfile()
        return if (existing == null) {
            profileDao.insertProfile(profile)
        } else {
            val updated = profile.copy(id = existing.id)
            profileDao.updateProfile(updated)
            existing.id
        }
    }

    override suspend fun getEffectiveSchedule(date: LocalDate): WorkScheduleEntity? {
        val profileId = getCurrentProfileId()
        return scheduleDao.getEffectiveSchedule(profileId, date)
    }

    override suspend fun getScheduleDays(scheduleId: Long): List<ScheduleDayEntity> {
        return scheduleDao.getScheduleDays(scheduleId)
    }

    override suspend fun saveScheduleWithDays(
        schedule: WorkScheduleEntity,
        days: List<ScheduleDayEntity>
    ): Long {
        val scheduleId = scheduleDao.insertSchedule(schedule)
        val linkedDays = days.map { it.copy(scheduleId = scheduleId) }
        scheduleDao.insertScheduleDays(linkedDays)
        return scheduleId
    }

    override fun getPunchesForDateFlow(date: LocalDate): Flow<List<PunchEntity>> {
        return punchDao.getPunchesForDateFlow(1L, date)
    }

    override suspend fun getPunchesForDate(date: LocalDate): List<PunchEntity> {
        val profileId = getCurrentProfileId()
        return punchDao.getPunchesForDate(profileId, date)
    }

    override fun getLatestPunchFlow(): Flow<PunchEntity?> {
        return punchDao.getLatestPunchFlow(1L)
    }

    override suspend fun registerPunch(
        date: LocalDate,
        time: LocalTime,
        type: PunchType,
        source: PunchSource,
        latitude: Double?,
        longitude: Double?,
        locationDescription: String?,
        note: String?
    ): Long {
        val profileId = getCurrentProfileId()
        val punch = PunchEntity(
            profileId = profileId,
            date = date,
            time = time,
            timestampEpochMs = System.currentTimeMillis(),
            type = type,
            source = source,
            latitude = latitude,
            longitude = longitude,
            locationDescription = locationDescription,
            note = note
        )
        val punchId = punchDao.insertPunch(punch)
        auditDao.insertAudit(
            AuditEventEntity(
                entityType = "PUNCH",
                entityId = punchId,
                action = AuditAction.CREATE,
                afterJson = "date=$date, time=$time, type=$type, source=$source",
                reason = "Registro de ponto"
            )
        )
        return punchId
    }

    override suspend fun updatePunch(punch: PunchEntity, editReason: String) {
        val before = punchDao.getPunchById(punch.id)
        punchDao.updatePunch(punch.copy(updatedAtEpochMs = System.currentTimeMillis()))
        auditDao.insertAudit(
            AuditEventEntity(
                entityType = "PUNCH",
                entityId = punch.id,
                action = AuditAction.UPDATE,
                beforeJson = before?.let { "time=${it.time}, type=${it.type}" },
                afterJson = "time=${punch.time}, type=${punch.type}",
                reason = editReason
            )
        )
    }

    override suspend fun deletePunch(punchId: Long, deleteReason: String) {
        val before = punchDao.getPunchById(punchId)
        punchDao.softDeletePunch(punchId)
        auditDao.insertAudit(
            AuditEventEntity(
                entityType = "PUNCH",
                entityId = punchId,
                action = AuditAction.DELETE,
                beforeJson = before?.let { "date=${it.date}, time=${it.time}, type=${it.type}" },
                reason = deleteReason
            )
        )
    }

    override fun getOccurrenceFlow(date: LocalDate): Flow<DayOccurrenceEntity?> {
        return occurrenceDao.getOccurrenceForDateFlow(1L, date)
    }

    override suspend fun getOccurrence(date: LocalDate): DayOccurrenceEntity? {
        val profileId = getCurrentProfileId()
        return occurrenceDao.getOccurrenceForDate(profileId, date)
    }

    override suspend fun setOccurrence(occurrence: DayOccurrenceEntity) {
        val profileId = getCurrentProfileId()
        occurrenceDao.insertOrUpdateOccurrence(occurrence.copy(profileId = profileId))
    }

    override suspend fun deleteOccurrence(occurrence: DayOccurrenceEntity) {
        occurrenceDao.deleteOccurrence(occurrence)
    }

    override fun getAllHolidaysFlow(): Flow<List<HolidayEntity>> = holidayDao.getAllHolidaysFlow()

    override suspend fun addHoliday(holiday: HolidayEntity): Long = holidayDao.insertHoliday(holiday)

    override suspend fun deleteHoliday(holiday: HolidayEntity) = holidayDao.deleteHoliday(holiday)

    override suspend fun getEffectivePayRule(date: LocalDate): PayRuleEntity {
        val profileId = getCurrentProfileId()
        return payRuleDao.getEffectivePayRule(profileId, date)
            ?: PayRuleEntity(profileId = profileId, effectiveFromDate = LocalDate.MIN)
    }

    override fun getPayRuleFlow(): Flow<PayRuleEntity?> = payRuleDao.getLatestPayRuleFlow(1L)

    override suspend fun savePayRule(rule: PayRuleEntity): Long {
        val profileId = getCurrentProfileId()
        return payRuleDao.insertPayRule(rule.copy(profileId = profileId))
    }

    override suspend fun getEffectiveBankPolicy(date: LocalDate): BankPolicyEntity? {
        val profileId = getCurrentProfileId()
        return bankPolicyDao.getEffectiveBankPolicy(profileId, date)
    }

    override fun getBankPolicyFlow(): Flow<BankPolicyEntity?> = bankPolicyDao.getLatestBankPolicyFlow(1L)

    override suspend fun saveBankPolicy(policy: BankPolicyEntity): Long {
        val profileId = getCurrentProfileId()
        return bankPolicyDao.insertBankPolicy(policy.copy(profileId = profileId))
    }

    override suspend fun addBankAdjustment(adjustment: BankAdjustmentEntity): Long {
        val profileId = getCurrentProfileId()
        return bankAdjustmentDao.insertAdjustment(adjustment.copy(profileId = profileId))
    }

    override fun getAllBankAdjustmentsFlow(): Flow<List<BankAdjustmentEntity>> {
        return bankAdjustmentDao.getAllAdjustmentsFlow(1L)
    }

    override suspend fun calculateDay(date: LocalDate): DayCalculationResult {
        val profileId = getCurrentProfileId()
        val punches = punchDao.getPunchesForDate(profileId, date)
        val schedule = scheduleDao.getEffectiveSchedule(profileId, date)
        val scheduleDays = schedule?.let { scheduleDao.getScheduleDays(it.id) } ?: emptyList()
        val scheduleDay = scheduleDays.find { it.dayOfWeek == date.dayOfWeek.value }
        val payRule = getEffectivePayRule(date)
        val bankPolicy = getEffectiveBankPolicy(date)
        val occurrence = occurrenceDao.getOccurrenceForDate(profileId, date)
        val holiday = holidayDao.getHolidayOnDate(date)

        return CalculationEngine.calculateDay(
            date = date,
            punches = punches,
            schedule = schedule,
            scheduleDay = scheduleDay,
            payRule = payRule,
            bankPolicy = bankPolicy,
            occurrence = occurrence,
            holiday = holiday
        )
    }

    override suspend fun calculateMonth(yearMonth: YearMonth): MonthSummaryResult {
        val profileId = getCurrentProfileId()
        val startDate = yearMonth.atDay(1)
        val endDate = yearMonth.atEndOfMonth()

        val allPunches = punchDao.getPunchesForRange(profileId, startDate, endDate)
        val allOccurrences = occurrenceDao.getOccurrencesForRange(profileId, startDate, endDate)
        val allHolidays = holidayDao.getAllHolidays()
        val schedule = scheduleDao.getEffectiveSchedule(profileId, startDate)
        val scheduleDays = schedule?.let { scheduleDao.getScheduleDays(it.id) } ?: emptyList()
        val payRule = getEffectivePayRule(startDate)
        val bankPolicy = getEffectiveBankPolicy(startDate)
        val adjustments = bankAdjustmentDao.getAdjustmentsInRange(profileId, startDate, endDate)

        val dayResults = mutableListOf<DayCalculationResult>()
        for (day in 1..yearMonth.lengthOfMonth()) {
            val date = yearMonth.atDay(day)
            val dayPunches = allPunches.filter { it.date == date }
            val dayOccurrence = allOccurrences.find { it.date == date }
            val dayHoliday = allHolidays.find { it.date == date || (it.isRecurringYearly && it.date.month == date.month && it.date.dayOfMonth == date.dayOfMonth) }
            val scheduleDay = scheduleDays.find { it.dayOfWeek == date.dayOfWeek.value }

            val result = CalculationEngine.calculateDay(
                date = date,
                punches = dayPunches,
                schedule = schedule,
                scheduleDay = scheduleDay,
                payRule = payRule,
                bankPolicy = bankPolicy,
                occurrence = dayOccurrence,
                holiday = dayHoliday
            )
            dayResults.add(result)
        }

        val startingBank = bankPolicy?.initialBalanceMinutes ?: 0L

        return CalculationEngine.calculateMonth(
            yearMonth = yearMonth,
            dayResults = dayResults,
            startingBankMinutes = startingBank,
            adjustments = adjustments
        )
    }

    override suspend fun detectInconsistenciesForMonth(yearMonth: YearMonth): List<InconsistencyItem> {
        val profileId = getCurrentProfileId()
        val startDate = yearMonth.atDay(1)
        val endDate = yearMonth.atEndOfMonth()

        val allPunches = punchDao.getPunchesForRange(profileId, startDate, endDate)
        val allOccurrences = occurrenceDao.getOccurrencesForRange(profileId, startDate, endDate)
        val schedule = scheduleDao.getEffectiveSchedule(profileId, startDate)
        val scheduleDays = schedule?.let { scheduleDao.getScheduleDays(it.id) } ?: emptyList()

        val allIssues = mutableListOf<InconsistencyItem>()
        for (day in 1..yearMonth.lengthOfMonth()) {
            val date = yearMonth.atDay(day)
            val dayPunches = allPunches.filter { it.date == date }
            val dayOccurrence = allOccurrences.find { it.date == date }
            val scheduleDay = scheduleDays.find { it.dayOfWeek == date.dayOfWeek.value }

            val issues = InconsistencyDetector.detectInconsistencies(
                date = date,
                punches = dayPunches,
                scheduleDay = scheduleDay,
                occurrence = dayOccurrence
            )
            allIssues.addAll(issues)
        }
        return allIssues
    }
}
