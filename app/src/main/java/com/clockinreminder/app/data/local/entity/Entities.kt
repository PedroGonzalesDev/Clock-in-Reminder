package com.clockinreminder.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalTime

enum class PunchType {
    ENTRY,          // Entrada
    LUNCH_OUT,      // Saída para intervalo
    LUNCH_RETURN,   // Retorno do intervalo
    EXIT            // Saída
}

enum class PunchSource {
    AUTOMATIC,
    MANUAL
}

enum class DayType(val labelPtBr: String, val isAbsence: Boolean = false) {
    NORMAL("Normal"),
    FERIADO("Feriado"),
    FOLGA("Folga"),
    FALTA("Falta", isAbsence = true),
    ATESTADO("Atestado Médico", isAbsence = true),
    FERIAS("Férias", isAbsence = true),
    SOBREAVISO("Sobreaviso"),
    COMPENSACAO("Compensação"),
    LICENCA("Licença", isAbsence = true),
    DSR("Descanso Semanal Remunerado"),
    PERSONALIZADO("Personalizado")
}

enum class CalculationMode {
    AUTOMATIC,
    MANUAL
}

enum class AuditAction {
    CREATE,
    UPDATE,
    DELETE
}

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val companyName: String,
    val employeeName: String,
    val nickname: String? = null,
    val timezoneId: String,
    val createdAtEpochMs: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "work_schedules",
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("profileId")]
)
data class WorkScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    val name: String = "Padrão",
    val effectiveFromDate: LocalDate,
    val isHoursOnly: Boolean = false,
    val dailyHoursTargetMinutes: Int = 540, // e.g. 9 hours = 540 min
    val weeklyToleranceMinutes: Int = 10,
    val dailyToleranceMinutes: Int = 10
)

@Entity(
    tableName = "schedule_days",
    foreignKeys = [
        ForeignKey(
            entity = WorkScheduleEntity::class,
            parentColumns = ["id"],
            childColumns = ["scheduleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("scheduleId")]
)
data class ScheduleDayEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val scheduleId: Long,
    val dayOfWeek: Int, // 1 = Monday .. 7 = Sunday (java.time.DayOfWeek.value)
    val isWorkDay: Boolean,
    val isVariable: Boolean = false,
    val startTime: LocalTime? = LocalTime.of(7, 0),
    val lunchStartTime: LocalTime? = LocalTime.of(12, 0),
    val lunchEndTime: LocalTime? = LocalTime.of(13, 0),
    val endTime: LocalTime? = LocalTime.of(17, 0),
    val expectedMinutes: Int = 540 // 9h default
)

@Entity(
    tableName = "punches",
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("profileId"), Index("date"), Index("timestampEpochMs")]
)
data class PunchEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    val date: LocalDate, // Logical date of the shift
    val time: LocalTime,
    val timestampEpochMs: Long,
    val type: PunchType,
    val source: PunchSource = PunchSource.AUTOMATIC,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationDescription: String? = null,
    val note: String? = null,
    val isDeleted: Boolean = false,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val updatedAtEpochMs: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "day_occurrences",
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("profileId"), Index(value = ["profileId", "date"], unique = true)]
)
data class DayOccurrenceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    val date: LocalDate,
    val type: DayType,
    val note: String? = null,
    val attachmentUri: String? = null,
    val isPaidAbsence: Boolean = true
)

@Entity(
    tableName = "holidays",
    indices = [Index("date")]
)
data class HolidayEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val name: String,
    val isRecurringYearly: Boolean = true,
    val scope: String = "Nacional" // Nacional, Estadual, Municipal, Facultativo
)

@Entity(
    tableName = "pay_rules",
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("profileId"), Index("effectiveFromDate")]
)
data class PayRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    val effectiveFromDate: LocalDate,
    val hourlyRateCents: Long = 2574L, // R$ 25,74 = 2574 centavos
    val calculationMode: CalculationMode = CalculationMode.AUTOMATIC,
    val standardOvertimePercent: Int = 50,
    val saturdayOvertimePercent: Int = 60,
    val sundayOvertimePercent: Int = 100,
    val holidayOvertimePercent: Int = 100,
    val nightAdditionPercent: Int = 30, // 30% conforme planilha de referência
    val nightStartHour: Int = 22,
    val nightStartMinute: Int = 0,
    val nightEndHour: Int = 5,
    val nightEndMinute: Int = 0,
    val isReducedNightHour: Boolean = true, // 52m30s
    val outsideScheduleIsOvertime: Boolean = true,
    val additiveNightBonus: Boolean = true // Aditivos sobre valor-base: (1 + 0.5 + 0.3)
)

@Entity(
    tableName = "bank_policies",
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("profileId"), Index("effectiveFromDate")]
)
data class BankPolicyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    val effectiveFromDate: LocalDate,
    val isEnabled: Boolean = true,
    val autoAccumulate: Boolean = true,
    val conversionRatio: Double = 1.0,
    val initialBalanceMinutes: Long = 0L,
    val sendOvertimeToBank: Boolean = false // Se false, overtime é pago; se true, vai para banco
)

@Entity(
    tableName = "bank_adjustments",
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("profileId"), Index("date")]
)
data class BankAdjustmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    val date: LocalDate,
    val adjustmentMinutes: Long, // + or -
    val reason: String,
    val authorizedBy: String? = null,
    val createdAtEpochMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "audit_events")
data class AuditEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val entityType: String,
    val entityId: Long,
    val action: AuditAction,
    val beforeJson: String? = null,
    val afterJson: String? = null,
    val reason: String? = null,
    val timestampEpochMs: Long = System.currentTimeMillis()
)
