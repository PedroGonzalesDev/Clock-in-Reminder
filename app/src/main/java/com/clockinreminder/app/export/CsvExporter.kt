package com.clockinreminder.app.export

import android.content.Context
import com.clockinreminder.app.domain.engine.CalculationEngine
import com.clockinreminder.app.domain.model.MonthSummaryResult
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.time.format.DateTimeFormatter

object CsvExporter {

    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    fun exportMonthToCsv(
        context: Context,
        companyName: String,
        employeeName: String,
        summary: MonthSummaryResult
    ): File {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val filename = "espelho_ponto_${summary.yearMonth.year}_${summary.yearMonth.monthValue}.csv"
        val file = File(exportDir, filename)

        OutputStreamWriter(FileOutputStream(file), StandardCharsets.UTF_8).use { writer ->
            // Write UTF-8 BOM for Microsoft Excel compatibility
            writer.write("\uFEFF")

            // Header metadata
            writer.write("CLOCK IN REMINDER - ESPELHO DE PONTO MENSAL\n")
            writer.write("Empresa;${companyName}\n")
            writer.write("Funcionário;${employeeName}\n")
            writer.write("Competência;${summary.yearMonth.monthValue}/${summary.yearMonth.year}\n")
            writer.write("\n")

            // Table headers (without Previstas, Normais, Saldo Dia)
            writer.write("Data;Dia da Semana;Tipo;Entrada 1;Saída 1;Entrada 2;Saída 2;Entrada 3;Saída 3;Entrada 4;Saída 4;Outras Marcações;Trabalhadas;Extras Úteis;Extras 60%;Extras 100%;Noturnas (Red.);Observações\n")

            // Rows
            val dayOfWeekNames = mapOf(
                java.time.DayOfWeek.MONDAY to "Segunda",
                java.time.DayOfWeek.TUESDAY to "Terça",
                java.time.DayOfWeek.WEDNESDAY to "Quarta",
                java.time.DayOfWeek.THURSDAY to "Quinta",
                java.time.DayOfWeek.FRIDAY to "Sexta",
                java.time.DayOfWeek.SATURDAY to "Sábado",
                java.time.DayOfWeek.SUNDAY to "Domingo"
            )

            for (day in summary.dayResults) {
                val dateStr = day.date.format(dateFormatter)
                val dayOfWeekStr = dayOfWeekNames[day.date.dayOfWeek] ?: ""
                val typeStr = day.dayType.labelPtBr

                val punches = day.punches
                val e1 = punches.getOrNull(0)?.time?.toString() ?: ""
                val s1 = punches.getOrNull(1)?.time?.toString() ?: ""
                val e2 = punches.getOrNull(2)?.time?.toString() ?: ""
                val s2 = punches.getOrNull(3)?.time?.toString() ?: ""
                val e3 = punches.getOrNull(4)?.time?.toString() ?: ""
                val s3 = punches.getOrNull(5)?.time?.toString() ?: ""
                val e4 = punches.getOrNull(6)?.time?.toString() ?: ""
                val s4 = punches.getOrNull(7)?.time?.toString() ?: ""

                val otherPunches = if (punches.size > 8) {
                    punches.subList(8, punches.size).joinToString(" | ") { "${it.type}: ${it.time}" }
                } else ""

                val trab = CalculationEngine.formatMinutesToHours(day.workedMinutes)
                val ot50 = if (day.overtime50Minutes > 0) CalculationEngine.formatMinutesToHours(day.overtime50Minutes) else "—"
                val ot60 = if (day.overtime60Minutes > 0) CalculationEngine.formatMinutesToHours(day.overtime60Minutes) else "—"
                val ot100 = if (day.overtime100Minutes > 0) CalculationEngine.formatMinutesToHours(day.overtime100Minutes) else "—"
                val notRed = if (day.nightMinutesReduced > 0) CalculationEngine.formatMinutesToHours(day.nightMinutesReduced) else "—"

                val punchNotes = day.punches.mapNotNull { it.note?.trim() }.filter { it.isNotEmpty() }
                val allNotes = mutableListOf<String>()
                if (day.holiday != null) allNotes.add(day.holiday.name)
                if (day.occurrence != null && !day.occurrence.note.isNullOrBlank()) allNotes.add(day.occurrence.note)
                allNotes.addAll(punchNotes)
                val obs = allNotes.joinToString(" / ")

                writer.write("$dateStr;$dayOfWeekStr;$typeStr;$e1;$s1;$e2;$s2;$e3;$s3;$e4;$s4;$otherPunches;$trab;$ot50;$ot60;$ot100;$notRed;$obs\n")
            }

            // Summary row
            writer.write("\n")
            writer.write("TOTAIS DO MÊS;;;;\n")
            writer.write("Total Horas Trabalhadas;${CalculationEngine.formatMinutesToHours(summary.totalWorkedMinutes)}\n")
            writer.write("Total Horas Normais;${CalculationEngine.formatMinutesToHours(summary.totalNormalMinutes)};Valor Estimado;${CalculationEngine.formatCentsToCurrency(summary.totalNormalPayCents)}\n")
            val ot50Label = if (summary.isWeekdayOtNormal) "Total Horas Extras Dias Úteis (Valor Normal)" else "Total Horas Extras 50%"
            writer.write("$ot50Label;${CalculationEngine.formatMinutesToHours(summary.totalOt50Minutes)};Valor Estimado;${CalculationEngine.formatCentsToCurrency(summary.totalOt50PayCents)}\n")
            writer.write("Total Horas Extras 60%;${CalculationEngine.formatMinutesToHours(summary.totalOt60Minutes)};Valor Estimado;${CalculationEngine.formatCentsToCurrency(summary.totalOt60PayCents)}\n")
            writer.write("Total Horas Extras 100%;${CalculationEngine.formatMinutesToHours(summary.totalOt100Minutes)};Valor Estimado;${CalculationEngine.formatCentsToCurrency(summary.totalOt100PayCents)}\n")
            writer.write("Total Adicional Noturno;${CalculationEngine.formatMinutesToHours(summary.totalNightMinutesReduced)};Valor Estimado;${CalculationEngine.formatCentsToCurrency(summary.totalNightBonusPayCents)}\n")
            writer.write("TOTAL BRUTO ESTIMADO;;;${CalculationEngine.formatCentsToCurrency(summary.totalGrossPayCents)}\n")
            writer.write("\n")
            writer.write("Aviso legal: Este documento é um controle pessoal gerado pelo Clock in Reminder e não substitui os sistemas oficiais de registro de ponto do empregador.\n")
        }

        return file
    }
}
