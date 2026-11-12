package com.clockinreminder.app.export

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.clockinreminder.app.domain.engine.CalculationEngine
import com.clockinreminder.app.domain.model.MonthSummaryResult
import java.io.File
import java.io.FileOutputStream
import java.time.format.DateTimeFormatter

object PdfExporter {

    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM")

    fun exportMonthToPdf(
        context: Context,
        companyName: String,
        employeeName: String,
        summary: MonthSummaryResult
    ): File {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val filename = "espelho_ponto_${summary.yearMonth.year}_${summary.yearMonth.monthValue}.pdf"
        val file = File(exportDir, filename)

        val doc = PdfDocument()

        // Page setup: A4 = 595 x 842 points
        val pageWidth = 595
        val pageHeight = 842
        val margin = 28f

        val paint = Paint().apply { isAntiAlias = true }

        val rowHeight = 15f
        val daysPerPage = 22
        val totalDays = summary.dayResults.size
        val totalPages = (totalDays + daysPerPage - 1) / daysPerPage

        var dayIndex = 0

        for (pageNumber in 1..totalPages) {
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            val page = doc.startPage(pageInfo)
            val canvas = page.canvas

            var y = margin

            // Draw Header Banner
            paint.textAlign = Paint.Align.LEFT
            paint.color = Color.parseColor("#142431")
            canvas.drawRect(margin, y, pageWidth - margin, y + 42f, paint)

            paint.color = Color.WHITE
            paint.textSize = 13f
            paint.isFakeBoldText = true
            canvas.drawText("CLOCK IN REMINDER — CONTROLE DE PONTO MENSAL", margin + 12f, y + 18f, paint)

            paint.textSize = 8.5f
            paint.isFakeBoldText = false
            paint.color = Color.parseColor("#D58D63")
            canvas.drawText("Empresa: $companyName  |  Funcionário: $employeeName  |  Competência: ${summary.yearMonth.monthValue}/${summary.yearMonth.year}", margin + 12f, y + 34f, paint)

            y += 52f

            // Table Header Bar
            paint.color = Color.parseColor("#187B77")
            canvas.drawRect(margin, y, pageWidth - margin, y + 18f, paint)

            paint.color = Color.WHITE
            paint.textSize = 7f
            paint.isFakeBoldText = true

            // Column Centers & Positions
            val dataLeft = margin + 4f
            val diaLeft = margin + 30f

            // Centers for punches E1..S4
            val e1Center = margin + 58f
            val s1Center = margin + 83f
            val e2Center = margin + 108f
            val s2Center = margin + 133f
            val e3Center = margin + 158f
            val s3Center = margin + 183f
            val e4Center = margin + 208f
            val s4Center = margin + 233f

            // Centers for hours
            val trabCenter = margin + 266f
            val ot50Center = margin + 298f
            val ot60Center = margin + 328f
            val ot100Center = margin + 360f
            val notCenter = margin + 392f

            val obsLeft = margin + 414f

            // Draw Headers
            paint.textAlign = Paint.Align.LEFT
            canvas.drawText("Data", dataLeft, y + 12f, paint)
            canvas.drawText("Dia", diaLeft, y + 12f, paint)

            paint.textAlign = Paint.Align.CENTER
            canvas.drawText("E1", e1Center, y + 12f, paint)
            canvas.drawText("S1", s1Center, y + 12f, paint)
            canvas.drawText("E2", e2Center, y + 12f, paint)
            canvas.drawText("S2", s2Center, y + 12f, paint)
            canvas.drawText("E3", e3Center, y + 12f, paint)
            canvas.drawText("S3", s3Center, y + 12f, paint)
            canvas.drawText("E4", e4Center, y + 12f, paint)
            canvas.drawText("S4", s4Center, y + 12f, paint)
            canvas.drawText("Trab.", trabCenter, y + 12f, paint)
            canvas.drawText("Ex.Útil", ot50Center, y + 12f, paint)
            canvas.drawText("Ex.60%", ot60Center, y + 12f, paint)
            canvas.drawText("Ex.100%", ot100Center, y + 12f, paint)
            canvas.drawText("Not.", notCenter, y + 12f, paint)

            paint.textAlign = Paint.Align.LEFT
            canvas.drawText("Observações", obsLeft, y + 12f, paint)

            y += 22f

            // Table Rows
            paint.isFakeBoldText = false
            val endDayIndex = minOf(dayIndex + daysPerPage, totalDays)

            val dayOfWeekAbbr = mapOf(
                java.time.DayOfWeek.MONDAY to "Seg",
                java.time.DayOfWeek.TUESDAY to "Ter",
                java.time.DayOfWeek.WEDNESDAY to "Qua",
                java.time.DayOfWeek.THURSDAY to "Qui",
                java.time.DayOfWeek.FRIDAY to "Sex",
                java.time.DayOfWeek.SATURDAY to "Sáb",
                java.time.DayOfWeek.SUNDAY to "Dom"
            )

            for (i in dayIndex until endDayIndex) {
                val day = summary.dayResults[i]
                val punches = day.punches
                val e1 = punches.getOrNull(0)?.time?.toString() ?: "—"
                val s1 = punches.getOrNull(1)?.time?.toString() ?: "—"
                val e2 = punches.getOrNull(2)?.time?.toString() ?: "—"
                val s2 = punches.getOrNull(3)?.time?.toString() ?: "—"
                val e3 = punches.getOrNull(4)?.time?.toString() ?: "—"
                val s3 = punches.getOrNull(5)?.time?.toString() ?: "—"
                val e4 = punches.getOrNull(6)?.time?.toString() ?: "—"
                val s4 = punches.getOrNull(7)?.time?.toString() ?: "—"

                // Row background striping
                if (i % 2 == 1) {
                    paint.color = Color.parseColor("#F6F4EE")
                    canvas.drawRect(margin, y - 2f, pageWidth - margin, y + rowHeight - 2f, paint)
                }

                // Row border bottom
                paint.color = Color.parseColor("#DFDACF")
                paint.strokeWidth = 0.5f
                canvas.drawLine(margin, y + rowHeight - 2f, pageWidth - margin, y + rowHeight - 2f, paint)

                val dateStr = day.date.format(dateFormatter)
                val dayStr = dayOfWeekAbbr[day.date.dayOfWeek] ?: ""
                val trabStr = CalculationEngine.formatMinutesToHours(day.workedMinutes)
                val ot50Str = if (day.overtime50Minutes > 0) CalculationEngine.formatMinutesToHours(day.overtime50Minutes) else "—"
                val ot60Str = if (day.overtime60Minutes > 0) CalculationEngine.formatMinutesToHours(day.overtime60Minutes) else "—"
                val ot100Str = if (day.overtime100Minutes > 0) CalculationEngine.formatMinutesToHours(day.overtime100Minutes) else "—"
                val notStr = if (day.nightMinutesReduced > 0) CalculationEngine.formatMinutesToHours(day.nightMinutesReduced) else "—"

                // Concatenate observations from punches separated by / plus day notes
                val punchNotes = day.punches.mapNotNull { it.note?.trim() }.filter { it.isNotEmpty() }
                val allNotes = mutableListOf<String>()
                if (day.holiday != null) allNotes.add(day.holiday.name)
                if (day.occurrence != null && !day.occurrence.note.isNullOrBlank()) allNotes.add(day.occurrence.note)
                allNotes.addAll(punchNotes)
                if (day.punches.size > 8) {
                    val extraPunches = day.punches.drop(8).joinToString(", ") { "${it.time}" }
                    allNotes.add("Outras: $extraPunches")
                }
                val obs = allNotes.joinToString(" / ")

                paint.textSize = 7f
                paint.color = Color.parseColor("#121B22")

                // Left aligned Date & Day
                paint.textAlign = Paint.Align.LEFT
                canvas.drawText(dateStr, dataLeft, y + 9f, paint)
                canvas.drawText(dayStr, diaLeft, y + 9f, paint)

                // Center aligned punches (perfect vertical column alignment)
                paint.textAlign = Paint.Align.CENTER
                canvas.drawText(e1, e1Center, y + 9f, paint)
                canvas.drawText(s1, s1Center, y + 9f, paint)
                canvas.drawText(e2, e2Center, y + 9f, paint)
                canvas.drawText(s2, s2Center, y + 9f, paint)
                canvas.drawText(e3, e3Center, y + 9f, paint)
                canvas.drawText(s3, s3Center, y + 9f, paint)
                canvas.drawText(e4, e4Center, y + 9f, paint)
                canvas.drawText(s4, s4Center, y + 9f, paint)

                // Center aligned hours
                paint.isFakeBoldText = true
                canvas.drawText(trabStr, trabCenter, y + 9f, paint)
                paint.isFakeBoldText = false
                canvas.drawText(ot50Str, ot50Center, y + 9f, paint)
                canvas.drawText(ot60Str, ot60Center, y + 9f, paint)
                canvas.drawText(ot100Str, ot100Center, y + 9f, paint)
                canvas.drawText(notStr, notCenter, y + 9f, paint)

                // Left aligned observations
                paint.textAlign = Paint.Align.LEFT
                paint.color = Color.parseColor("#5A6975")
                val truncatedObs = if (obs.length > 32) obs.take(31) + "…" else obs
                canvas.drawText(truncatedObs, obsLeft, y + 9f, paint)

                y += rowHeight
            }

            dayIndex = endDayIndex

            // If last page, draw Summary Box & Legal note
            if (pageNumber == totalPages) {
                y += 12f

                // Summary Box
                paint.color = Color.parseColor("#EFECE3")
                canvas.drawRect(margin, y, pageWidth - margin, y + 96f, paint)

                paint.color = Color.parseColor("#187B77")
                paint.strokeWidth = 1.5f
                paint.style = Paint.Style.STROKE
                canvas.drawRect(margin, y, pageWidth - margin, y + 96f, paint)
                paint.style = Paint.Style.FILL

                paint.color = Color.parseColor("#142431")
                paint.textSize = 9.5f
                paint.isFakeBoldText = true
                paint.textAlign = Paint.Align.LEFT
                canvas.drawText("RESUMO GERAL E MEMÓRIA DE CÁLCULO ESTIMADA", margin + 12f, y + 16f, paint)

                paint.isFakeBoldText = false
                paint.textSize = 8f

                val col1X = margin + 12f
                val col2X = margin + 280f

                canvas.drawText("Total Horas Trabalhadas: ${CalculationEngine.formatMinutesToHours(summary.totalWorkedMinutes)}", col1X, y + 34f, paint)
                val ot50Label = if (summary.isWeekdayOtNormal) "Extras Úteis (Normal)" else "Extras 50%"
                canvas.drawText("$ot50Label: ${CalculationEngine.formatMinutesToHours(summary.totalOt50Minutes)} = ${CalculationEngine.formatCentsToCurrency(summary.totalOt50PayCents)}", col1X, y + 50f, paint)
                canvas.drawText("Extras 60% (Sáb): ${CalculationEngine.formatMinutesToHours(summary.totalOt60Minutes)} = ${CalculationEngine.formatCentsToCurrency(summary.totalOt60PayCents)}", col1X, y + 66f, paint)
                canvas.drawText("Extras 100% (Dom/Fer/Folga): ${CalculationEngine.formatMinutesToHours(summary.totalOt100Minutes)} = ${CalculationEngine.formatCentsToCurrency(summary.totalOt100PayCents)}", col1X, y + 82f, paint)

                canvas.drawText("Adicional Noturno (Red.): ${CalculationEngine.formatMinutesToHours(summary.totalNightMinutesReduced)} = ${CalculationEngine.formatCentsToCurrency(summary.totalNightBonusPayCents)}", col2X, y + 34f, paint)
                canvas.drawText("Remuneração Normal Estimada: ${CalculationEngine.formatCentsToCurrency(summary.totalNormalPayCents)}", col2X, y + 50f, paint)

                paint.color = Color.parseColor("#187B77")
                paint.isFakeBoldText = true
                paint.textSize = 10f
                canvas.drawText("TOTAL ESTIMADO: ${CalculationEngine.formatCentsToCurrency(summary.totalGrossPayCents)}", col2X, y + 74f, paint)

                y += 108f

                // Disclaimer footer
                paint.color = Color.parseColor("#5A6975")
                paint.textSize = 6.5f
                paint.isFakeBoldText = false
                paint.textAlign = Paint.Align.LEFT
                canvas.drawText("Aviso: Documento para controle pessoal do trabalhador, gerado por Clock in Reminder. Não substitui sistemas oficiais de folha nem registros do empregador.", margin, y, paint)
            }

            // Page Number Footer
            paint.color = Color.parseColor("#9FB0C0")
            paint.textSize = 7.5f
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText("Página $pageNumber de $totalPages", pageWidth - margin, pageHeight - 15f, paint)

            doc.finishPage(page)
        }

        FileOutputStream(file).use { out ->
            doc.writeTo(out)
        }
        doc.close()

        return file
    }
}
