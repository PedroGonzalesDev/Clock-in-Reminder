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

            // Table headers
            writer.write("Data;Dia da Semana;Tipo;Entrada 1;Saída 1;Entrada 2;Saída 2;Outras Marcações;Previstas;Trabalhadas;Normais;Extras 50%;Extras 60%;Extras 100%;Noturnas (Civil);Noturnas (Red.);Saldo Dia;Observações\n")

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
                val otherPunches = if (punches.size > 4) {
                    punches.subList(4, punches.size).joinToString(" | ") { "${it.type}: ${it.time}" }
                } else ""

                val prev = CalculationEngine.formatMinutesToHours(day.expectedMinutes)
                val trab = CalculationEngine.formatMinutesToHours(day.workedMinutes)
                val norm = CalculationEngine.formatMinutesToHours(day.normalMinutes)
                val ot50 = CalculationEngine.formatMinutesToHours(day.overtime50Minutes)
                val ot60 = CalculationEngine.formatMinutesToHours(day.overtime60Minutes)
                val ot100 = CalculationEngine.formatMinutesToHours(day.overtime100Minutes)
                val notCivil = CalculationEngine.formatMinutesToHours(day.nightMinutesCivil)
                val notRed = CalculationEngine.formatMinutesToHours(day.nightMinutesReduced)
                val saldo = CalculationEngine.formatMinutesToHours(day.dailyBalanceMinutes, withSign = true)
                val obs = day.occurrence?.note ?: day.holiday?.name ?: ""

                writer.write("$dateStr;$dayOfWeekStr;$typeStr;$e1;$s1;$e2;$s2;$otherPunches;$prev;$trab;$norm;$ot50;$ot60;$ot100;$notCivil;$notRed;$saldo;$obs\n")
            }

            // Summary row
            writer.write("\n")
            writer.write("TOTAIS DO MÊS;;;;\n")
            writer.write("Total Horas Previstas;${CalculationEngine.formatMinutesToHours(summary.totalExpectedMinutes)}\n")
            writer.write("Total Horas Trabalhadas;${CalculationEngine.formatMinutesToHours(summary.totalWorkedMinutes)}\n")
            writer.write("Total Horas Normais;${CalculationEngine.formatMinutesToHours(summary.totalNormalMinutes)};Valor Estimado;${CalculationEngine.formatCentsToCurrency(summary.totalNormalPayCents)}\n")
            writer.write("Total Horas Extras 50%;${CalculationEngine.formatMinutesToHours(summary.totalOt50Minutes)};Valor Estimado;${CalculationEngine.formatCentsToCurrency(summary.totalOt50PayCents)}\n")
            writer.write("Total Horas Extras 60%;${CalculationEngine.formatMinutesToHours(summary.totalOt60Minutes)};Valor Estimado;${CalculationEngine.formatCentsToCurrency(summary.totalOt60PayCents)}\n")
            writer.write("Total Horas Extras 100%;${CalculationEngine.formatMinutesToHours(summary.totalOt100Minutes)};Valor Estimado;${CalculationEngine.formatCentsToCurrency(summary.totalOt100PayCents)}\n")
            writer.write("Total Adicional Noturno;${CalculationEngine.formatMinutesToHours(summary.totalNightMinutesReduced)};Valor Estimado;${CalculationEngine.formatCentsToCurrency(summary.totalNightBonusPayCents)}\n")
            writer.write("TOTAL BRUTO ESTIMADO;;;${CalculationEngine.formatCentsToCurrency(summary.totalGrossPayCents)}\n")
            writer.write("Saldo de Banco Inicial;${CalculationEngine.formatMinutesToHours(summary.startingBankMinutes.toInt(), withSign = true)}\n")
            writer.write("Saldo de Banco Final;${CalculationEngine.formatMinutesToHours(summary.closingBankMinutes.toInt(), withSign = true)}\n")
            writer.write("\n")
            writer.write("Aviso legal: Este documento é um controle pessoal gerado pelo Clock in Reminder e não substitui os sistemas oficiais de registro de ponto do empregador.\n")
        }

        return file
    }
}
