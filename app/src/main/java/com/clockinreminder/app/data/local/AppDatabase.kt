package com.clockinreminder.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.clockinreminder.app.data.local.dao.*
import com.clockinreminder.app.data.local.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

@Database(
    entities = [
        ProfileEntity::class,
        WorkScheduleEntity::class,
        ScheduleDayEntity::class,
        PunchEntity::class,
        DayOccurrenceEntity::class,
        HolidayEntity::class,
        PayRuleEntity::class,
        BankPolicyEntity::class,
        BankAdjustmentEntity::class,
        AuditEventEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun profileDao(): ProfileDao
    abstract fun workScheduleDao(): WorkScheduleDao
    abstract fun punchDao(): PunchDao
    abstract fun dayOccurrenceDao(): DayOccurrenceDao
    abstract fun holidayDao(): HolidayDao
    abstract fun payRuleDao(): PayRuleDao
    abstract fun bankPolicyDao(): BankPolicyDao
    abstract fun bankAdjustmentDao(): BankAdjustmentDao
    abstract fun auditDao(): AuditDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "clockinreminder.db"
                )
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Seed national holidays
                            CoroutineScope(Dispatchers.IO).launch {
                                getInstance(context).seedDefaultHolidays()
                            }
                        }
                    })
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    suspend fun seedDefaultHolidays() {
        val currentYear = LocalDate.now().year
        val holidays = mutableListOf<HolidayEntity>()
        for (year in (currentYear - 1)..(currentYear + 2)) {
            holidays.addAll(
                listOf(
                    HolidayEntity(date = LocalDate.of(year, 1, 1), name = "Confraternização Universal", isRecurringYearly = true),
                    HolidayEntity(date = LocalDate.of(year, 4, 21), name = "Tiradentes", isRecurringYearly = true),
                    HolidayEntity(date = LocalDate.of(year, 5, 1), name = "Dia do Trabalho", isRecurringYearly = true),
                    HolidayEntity(date = LocalDate.of(year, 9, 7), name = "Independência do Brasil", isRecurringYearly = true),
                    HolidayEntity(date = LocalDate.of(year, 10, 12), name = "Nossa Senhora Aparecida", isRecurringYearly = true),
                    HolidayEntity(date = LocalDate.of(year, 11, 2), name = "Finados", isRecurringYearly = true),
                    HolidayEntity(date = LocalDate.of(year, 11, 15), name = "Proclamação da República", isRecurringYearly = true),
                    HolidayEntity(date = LocalDate.of(year, 11, 20), name = "Dia da Consciência Negra", isRecurringYearly = true),
                    HolidayEntity(date = LocalDate.of(year, 12, 25), name = "Natal", isRecurringYearly = true)
                )
            )
        }
        holidayDao().insertHolidays(holidays)
    }
}
