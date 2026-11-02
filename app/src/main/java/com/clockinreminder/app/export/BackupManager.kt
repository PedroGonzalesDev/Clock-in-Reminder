package com.clockinreminder.app.export

import android.content.Context
import com.clockinreminder.app.data.local.AppDatabase
import com.clockinreminder.app.data.local.entity.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.LocalTime

object BackupManager {

    suspend fun exportBackupJson(context: Context, db: AppDatabase): File {
        val exportDir = File(context.cacheDir, "backups").apply { mkdirs() }
        val filename = "clockinreminder_backup_${System.currentTimeMillis()}.json"
        val file = File(exportDir, filename)

        val root = JSONObject()
        root.put("version", 1)
        root.put("exportedAtEpochMs", System.currentTimeMillis())

        // Profile
        val profile = db.profileDao().getProfile()
        if (profile != null) {
            val profObj = JSONObject()
            profObj.put("companyName", profile.companyName)
            profObj.put("employeeName", profile.employeeName)
            profObj.put("nickname", profile.nickname ?: "")
            profObj.put("timezoneId", profile.timezoneId)
            root.put("profile", profObj)
        }

        // Punches
        val punches = db.punchDao().getPunchesForRange(1L, LocalDate.of(2000, 1, 1), LocalDate.of(2100, 1, 1))
        val punchesArr = JSONArray()
        for (p in punches) {
            val obj = JSONObject()
            obj.put("date", p.date.toString())
            obj.put("time", p.time.toString())
            obj.put("type", p.type.name)
            obj.put("source", p.source.name)
            obj.put("note", p.note ?: "")
            obj.put("location", p.locationDescription ?: "")
            punchesArr.put(obj)
        }
        root.put("punches", punchesArr)

        // Occurrences
        val occurrences = db.dayOccurrenceDao().getOccurrencesForRange(1L, LocalDate.of(2000, 1, 1), LocalDate.of(2100, 1, 1))
        val occArr = JSONArray()
        for (o in occurrences) {
            val obj = JSONObject()
            obj.put("date", o.date.toString())
            obj.put("type", o.type.name)
            obj.put("note", o.note ?: "")
            obj.put("isPaidAbsence", o.isPaidAbsence)
            occArr.put(obj)
        }
        root.put("occurrences", occArr)

        // Holidays
        val holidays = db.holidayDao().getAllHolidays()
        val holArr = JSONArray()
        for (h in holidays) {
            val obj = JSONObject()
            obj.put("date", h.date.toString())
            obj.put("name", h.name)
            obj.put("scope", h.scope)
            obj.put("isRecurringYearly", h.isRecurringYearly)
            holArr.put(obj)
        }
        root.put("holidays", holArr)

        OutputStreamWriter(FileOutputStream(file), StandardCharsets.UTF_8).use { writer ->
            writer.write(root.toString(2))
        }

        return file
    }

    suspend fun importBackupJson(db: AppDatabase, jsonString: String): Boolean {
        try {
            val root = JSONObject(jsonString)
            if (!root.has("version")) return false

            val profileId = db.profileDao().getProfile()?.id ?: 1L

            // Restore Punches
            if (root.has("punches")) {
                val arr = root.getJSONArray("punches")
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val punch = PunchEntity(
                        profileId = profileId,
                        date = LocalDate.parse(obj.getString("date")),
                        time = LocalTime.parse(obj.getString("time")),
                        timestampEpochMs = System.currentTimeMillis(),
                        type = PunchType.valueOf(obj.getString("type")),
                        source = PunchSource.valueOf(obj.optString("source", PunchSource.MANUAL.name)),
                        note = obj.optString("note").ifEmpty { null },
                        locationDescription = obj.optString("location").ifEmpty { null }
                    )
                    db.punchDao().insertPunch(punch)
                }
            }

            // Restore Occurrences
            if (root.has("occurrences")) {
                val arr = root.getJSONArray("occurrences")
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val occ = DayOccurrenceEntity(
                        profileId = profileId,
                        date = LocalDate.parse(obj.getString("date")),
                        type = DayType.valueOf(obj.getString("type")),
                        note = obj.optString("note").ifEmpty { null },
                        isPaidAbsence = obj.optBoolean("isPaidAbsence", true)
                    )
                    db.dayOccurrenceDao().insertOrUpdateOccurrence(occ)
                }
            }

            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
}
