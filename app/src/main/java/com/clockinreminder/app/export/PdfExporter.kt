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
            paint.color = Color.parseColor("#142431")
            canvas.drawRect(margin, y, pageWidth - margin, y + 42f, paint)

            paint.color = Color.WHITE
            paint.textSize = 14f
            paint.isFakeBoldText = true
            canvas.drawText("CLOCK IN REMINDER — CONTROLE DE PONTO MENSAL", margin + 12f, y + 18f, paint)

            paint.textSize = 9f
            paint.isFakeBoldText = false
            paint.color = Color.parseColor("#D58D63")
            canvas.drawText("Empresa: $companyName  |  Funcionário: $employeeName  |  Competência: ${summary.yearMonth.monthValue}/${summary.yearMonth.year}", margin + 12f, y + 34f, paint)

            y += 54f

            // Table Header Bar
            paint.color = Color.parseColor("#187B77")
            canvas.drawRect(margin, y, pageWidth - margin, y + 18f, paint)

            paint.color = Color.WHITE
            paint.textSize = 7.5f
            paint.isFakeBoldText = true

            val colX = floatArrayOf(
                margin + 4f,    // Data
                margin + 34f,   // Dia
                margin + 62f,   // E1
                margin + 92f,   // S1
                margin + 122f,  // E2
                margin + 152f,  // S2
                margin + 182f,  // Prev
                margin + 214f,  // Trab
                margin + 246f,  // Norm
                margin + 278f,  // Ex50
                margin + 310f,  // Ex60
                margin + 342f,  // Ex100
                margin + 376f,  // Not
                margin + 410f,  // Saldo
                margin + 448f   // Obs
            )

            val headers = arrayOf("Data", "Dia", "Ent. 1", "Saí. 1", "Ent. 2", "Saí. 2", "Prev.", "Trab.", "Norm.", "Ex.50%", "Ex.60%", "Ex.100%", "Not.", "Saldo", "Observações")
            for (i in headers.indices) {
                canvas.drawText(headers[i], colX[i], y + 12f, paint)
            }

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

                // Row background striping
                if (i % 2 == 1) {
                    paint.color = Color.parseColor("#F6F4EE")
                    canvas.drawRect(margin, y - 2f, pageWidth - margin, y + rowHeight - 2f, paint)
                }

                // Row border bottom
                paint.color = Color.parseColor("#DFDACF")
                paint.strokeWidth = 0.5f
                canvas.drawLine(margin, y + rowHeight - 2f, pageWidth - margin, y + rowHeight - 2f, paint)

                paint.color = Color.parseColor("#121B22")
                paint.textSize = 7.5f

                val dateStr = day.date.format(dateFormatter)
                val dayStr = dayOfWeekAbbr[day.date.dayOfWeek] ?: ""
                val prevStr = CalculationEngine.formatMinutesToHours(day.expectedMinutes)
                val trabStr = CalculationEngine.formatMinutesToHours(day.workedMinutes)
                val normStr = CalculationEngine.formatMinutesToHours(day.normalMinutes)
                val ot50Str = if (day.overtime50Minutes > 0) CalculationEngine.formatMinutesToHours(day.overtime50Minutes) else "—"
                val ot60Str = if (day.overtime60Minutes > 0) CalculationEngine.formatMinutesToHours(day.overtime60Minutes) else "—"
                val ot100Str = if (day.overtime100Minutes > 0) CalculationEngine.formatMinutesToHours(day.overtime100Minutes) else "—"
                val notStr = if (day.nightMinutesReduced > 0) CalculationEngine.formatMinutesToHours(day.nightMinutesReduced) else "—"
                val saldoStr = CalculationEngine.formatMinutesToHours(day.dailyBalanceMinutes, withSign = true)
                val obs = (day.occurrence?.note ?: day.holiday?.name ?: "").take(18)

                canvas.drawText(dateStr, colX[0], y + 9f, paint)
                canvas.drawText(dayStr, colX[1], y + 9f, paint)
                canvas.drawText(e1, colX[2], y + 9f, paint)
                canvas.drawText(s1, colX[3], y + 9f, paint)
                canvas.drawText(e2, colX[4], y + 9f, paint)
                canvas.drawText(s2, colX[5], y + 9f, paint)
                canvas.drawText(prevStr, colX[6], y + 9f, paint)
                canvas.drawText(trabStr, colX[7], y + 9f, paint)
                canvas.drawText(normStr, colX[8], y + 9f, paint)
                canvas.drawText(ot50Str, colX[9], y + 9f, paint)
                canvas.drawText(ot60Str, colX[10], y + 9f, paint)
                canvas.drawText(ot100Str, colX[11], y + 9f, paint)
                canvas.drawText(notStr, colX[12], y + 9f, paint)

                // Saldo color
                if (day.dailyBalanceMinutes > 0) {
                    paint.color = Color.parseColor("#2E7D32")
                } else if (day.dailyBalanceMinutes < 0) {
                    paint.color = Color.parseColor("#C74B4B")
                }
                canvas.drawText(saldoStr, colX[13], y + 9f, paint)

                paint.color = Color.parseColor("#5A6975")
                canvas.drawText(obs, colX[14], y + 9f, paint)

                y += rowHeight
            }

            dayIndex = endDayIndex

            // If last page, draw Summary Card & Legal note
            if (pageNumber == totalPages) {
                y += 10f

                // Summary Box
                paint.color = Color.parseColor("#EFECE3")
                canvas.drawRect(margin, y, pageWidth - margin, y + 115f, paint)

                paint.color = Color.parseColor("#187B77")
                paint.strokeWidth = 1.5f
                paint.style = Paint.Style.STROKE
                canvas.drawRect(margin, y, pageWidth - margin, y + 115f, paint)
                paint.style = Paint.Style.FILL

                paint.color = Color.parseColor("#142431")
                paint.textSize = 9.5f
                paint.isFakeBoldText = true
                canvas.drawText("RESUMO GERAL E MEMÓRIA DE CÁLCULO ESTIMADA", margin + 10f, y + 16f, paint)

                paint.isFakeBoldText = false
                paint.textSize = 8f

                val col1X = margin + 10f
                val col2X = margin + 270f

                canvas.drawText("Total Horas Previstas: ${CalculationEngine.formatMinutesToHours(summary.totalExpectedMinutes)}", col1X, y + 32f, paint)
                canvas.drawText("Total Horas Trabalhadas: ${CalculationEngine.formatMinutesToHours(summary.totalWorkedMinutes)}", col1X, y + 46f, paint)
                canvas.drawText("Remuneração Normal: ${CalculationEngine.formatMinutesToHours(summary.totalNormalMinutes)} = ${CalculationEngine.formatCentsToCurrency(summary.totalNormalPayCents)}", col1X, y + 60f, paint)
                canvas.drawText("Extras 50%: ${CalculationEngine.formatMinutesToHours(summary.totalOt50Minutes)} = ${CalculationEngine.formatCentsToCurrency(summary.totalOt50PayCents)}", col1X, y + 74f, paint)
                canvas.drawText("Extras 60% (Sáb): ${CalculationEngine.formatMinutesToHours(summary.totalOt60Minutes)} = ${CalculationEngine.formatCentsToCurrency(summary.totalOt60PayCents)}", col1X, y + 88f, paint)
                canvas.drawText("Extras 100% (Dom/Fer): ${CalculationEngine.formatMinutesToHours(summary.totalOt100Minutes)} = ${CalculationEngine.formatCentsToCurrency(summary.totalOt100PayCents)}", col1X, y + 102f, paint)

                canvas.drawText("Adicional Noturno (Red.): ${CalculationEngine.formatMinutesToHours(summary.totalNightMinutesReduced)} = ${CalculationEngine.formatCentsToCurrency(summary.totalNightBonusPayCents)}", col2X, y + 32f, paint)
                canvas.drawText("Saldo Banco Inicial: ${CalculationEngine.formatMinutesToHours(summary.startingBankMinutes.toInt(), withSign = true)}", col2X, y + 46f, paint)
                canvas.drawText("Saldo Banco Final: ${CalculationEngine.formatMinutesToHours(summary.closingBankMinutes.toInt(), withSign = true)}", col2X, y + 60f, paint)

                paint.color = Color.parseColor("#142431")
                paint.isFakeBoldText = true
                paint.textSize = 10f
                canvas.drawText("TOTAL ESTIMADO: ${CalculationEngine.formatCentsToCurrency(summary.totalGrossPayCents)}", col2X, y + 85f, paint)

                y += 128f

                // Disclaimer footer
                paint.color = Color.parseColor("#5A6975")
                paint.textSize = 6.5f
                paint.isFakeBoldText = false
                canvas.drawText("Aviso: Documento para controle pessoal do trabalhador, gerado por Clock in Reminder. Não substitui sistemas oficiais de folha nem registros do empregador.", margin, y, paint)
            }

            // Page Number Footer
            paint.color = Color.parseColor("#9FB0C0")
            paint.textSize = 7.5f
            canvas.drawText("Página $pageNumber de $totalPages", pageWidth - margin - 60f, pageHeight - 15f, paint)

            doc.finishPage(page)
        }

        FileOutputStream(file).use { out ->
            doc.writeTo(out)
        }
        doc.close()

        return file
    }
}
