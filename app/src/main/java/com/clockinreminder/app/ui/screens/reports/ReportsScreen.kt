package com.clockinreminder.app.ui.screens.reports

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clockinreminder.app.data.repository.ClockInRepository
import com.clockinreminder.app.domain.engine.CalculationEngine
import com.clockinreminder.app.domain.model.MonthSummaryResult
import com.clockinreminder.app.export.CsvExporter
import com.clockinreminder.app.export.ExcelExporter
import com.clockinreminder.app.export.PdfExporter
import com.clockinreminder.app.ui.theme.*
import kotlinx.coroutines.launch
import java.io.File
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

class ReportsViewModel(
    private val repository: ClockInRepository
) : ViewModel() {

    var currentYearMonth by mutableStateOf(YearMonth.now())
    var monthSummary by mutableStateOf<MonthSummaryResult?>(null)
    var companyName by mutableStateOf("Empresa")
    var employeeName by mutableStateOf("Funcionário")
    var exportStatusMessage by mutableStateOf<String?>(null)

    init {
        viewModelScope.launch {
            val prof = repository.getProfile()
            if (prof != null) {
                companyName = prof.companyName
                employeeName = prof.employeeName
            }
            loadMonth(currentYearMonth)
        }
    }

    fun loadMonth(ym: YearMonth) {
        currentYearMonth = ym
        viewModelScope.launch {
            monthSummary = repository.calculateMonth(ym)
        }
    }

    fun nextMonth() = loadMonth(currentYearMonth.plusMonths(1))
    fun previousMonth() = loadMonth(currentYearMonth.minusMonths(1))

    fun exportPdf(context: Context) {
        val summary = monthSummary ?: return
        val file = PdfExporter.exportMonthToPdf(context, companyName, employeeName, summary)
        shareFile(context, file, "application/pdf", "Espelho de Ponto PDF")
    }

    fun exportExcel(context: Context) {
        val summary = monthSummary ?: return
        val file = ExcelExporter.exportMonthToExcel(context, companyName, employeeName, summary)
        shareFile(context, file, "application/vnd.ms-excel", "Espelho de Ponto Excel")
    }

    fun exportCsv(context: Context) {
        val summary = monthSummary ?: return
        val file = CsvExporter.exportMonthToCsv(context, companyName, employeeName, summary)
        shareFile(context, file, "text/csv", "Espelho de Ponto CSV")
    }

    private fun shareFile(context: Context, file: File, mimeType: String, title: String) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, title)
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(Intent.createChooser(intent, "Compartilhar $title"))
        } catch (e: Exception) {
            e.printStackTrace()
            exportStatusMessage = "Erro ao exportar: ${e.message}"
        }
    }
}

@Composable
fun ReportsScreen(
    viewModel: ReportsViewModel
) {
    val context = LocalContext.current
    val ym = viewModel.currentYearMonth
    val localePtBr = Locale("pt", "BR")
    val monthTitle = ym.format(DateTimeFormatter.ofPattern("MMMM 'de' yyyy", localePtBr)).replaceFirstChar { it.uppercase() }
    val summary = viewModel.monthSummary

    val dayOfWeekAbbr = mapOf(
        java.time.DayOfWeek.MONDAY to "Seg",
        java.time.DayOfWeek.TUESDAY to "Ter",
        java.time.DayOfWeek.WEDNESDAY to "Qua",
        java.time.DayOfWeek.THURSDAY to "Qui",
        java.time.DayOfWeek.FRIDAY to "Sex",
        java.time.DayOfWeek.SATURDAY to "Sáb",
        java.time.DayOfWeek.SUNDAY to "Dom"
    )

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Header with month navigation
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Espelho de Ponto Mensal",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = monthTitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = PetrolTeal,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { viewModel.previousMonth() }) {
                            Icon(Icons.Filled.ChevronLeft, contentDescription = "Mês anterior")
                        }
                        IconButton(onClick = { viewModel.nextMonth() }) {
                            Icon(Icons.Filled.ChevronRight, contentDescription = "Próximo mês")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Export Actions Bar (PDF, XLSX, CSV)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.exportPdf(context) },
                    colors = ButtonDefaults.buttonColors(containerColor = PetrolTeal),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { viewModel.exportExcel(context) },
                    colors = ButtonDefaults.buttonColors(containerColor = WarmCopper),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.TableChart, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Excel", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = { viewModel.exportCsv(context) },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Description, contentDescription = null, modifier = Modifier.size(16.dp), tint = PetrolTeal)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("CSV", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Financial & Hours Summary Card
            if (summary != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "RESUMO FINANCEIRO ESTIMADO",
                            style = MaterialTheme.typography.labelSmall,
                            color = WarmCopper,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Horas Normais (${CalculationEngine.formatMinutesToHours(summary.totalNormalMinutes)}):", style = MaterialTheme.typography.bodySmall)
                            Text(CalculationEngine.formatCentsToCurrency(summary.totalNormalPayCents), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                        }
                        if (summary.totalOt50Minutes > 0) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Extras 50% (${CalculationEngine.formatMinutesToHours(summary.totalOt50Minutes)}):", style = MaterialTheme.typography.bodySmall)
                                Text(CalculationEngine.formatCentsToCurrency(summary.totalOt50PayCents), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                        if (summary.totalOt60Minutes > 0) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Extras 60% Sábado (${CalculationEngine.formatMinutesToHours(summary.totalOt60Minutes)}):", style = MaterialTheme.typography.bodySmall)
                                Text(CalculationEngine.formatCentsToCurrency(summary.totalOt60PayCents), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                        if (summary.totalOt100Minutes > 0) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Extras 100% Dom/Fer (${CalculationEngine.formatMinutesToHours(summary.totalOt100Minutes)}):", style = MaterialTheme.typography.bodySmall)
                                Text(CalculationEngine.formatCentsToCurrency(summary.totalOt100PayCents), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                        if (summary.totalNightMinutesReduced > 0) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Adicional Noturno (${CalculationEngine.formatMinutesToHours(summary.totalNightMinutesReduced)}):", style = MaterialTheme.typography.bodySmall)
                                Text(CalculationEngine.formatCentsToCurrency(summary.totalNightBonusPayCents), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                            }
                        }

                        Divider(modifier = Modifier.padding(vertical = 4.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("TOTAL BRUTO ESTIMADO:", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = PetrolTeal)
                            Text(CalculationEngine.formatCentsToCurrency(summary.totalGrossPayCents), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = PetrolTeal)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Saldo de Banco no Mês:", style = MaterialTheme.typography.bodySmall)
                            val bankCol = if (summary.closingBankMinutes >= 0) ControlledGreen else ControlledRed
                            Text(CalculationEngine.formatMinutesToHours(summary.closingBankMinutes.toInt(), withSign = true), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = bankCol)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Detailed Monthly Table Preview
            Text(
                text = "DETALHAMENTO DIÁRIO (PRÉVIA)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Horizontally Scrollable Table
            if (summary != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .horizontalScroll(rememberScrollState())
                ) {
                    Column {
                        // Table Header
                        Row(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(vertical = 10.dp, horizontal = 8.dp)
                        ) {
                            val cols = listOf("Data", "Dia", "Tipo", "E1", "S1", "E2", "S2", "E3", "S3", "E4", "S4", "Trab.", "Ex.50%", "Ex.60%", "Ex.100%", "Not.", "Observações")
                            val colWidths = listOf(52, 36, 64, 44, 44, 44, 44, 44, 44, 44, 44, 52, 52, 52, 54, 48, 160)

                            cols.forEachIndexed { i, title ->
                                val align = if (i in 3..15) androidx.compose.ui.text.style.TextAlign.Center else androidx.compose.ui.text.style.TextAlign.Start
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, textAlign = align),
                                    modifier = Modifier.width(colWidths[i].dp)
                                )
                            }
                        }

                        // Table Rows
                        summary.dayResults.forEachIndexed { idx, day ->
                            val punches = day.punches
                            val e1 = punches.getOrNull(0)?.time?.toString() ?: "—"
                            val s1 = punches.getOrNull(1)?.time?.toString() ?: "—"
                            val e2 = punches.getOrNull(2)?.time?.toString() ?: "—"
                            val s2 = punches.getOrNull(3)?.time?.toString() ?: "—"
                            val e3 = punches.getOrNull(4)?.time?.toString() ?: "—"
                            val s3 = punches.getOrNull(5)?.time?.toString() ?: "—"
                            val e4 = punches.getOrNull(6)?.time?.toString() ?: "—"
                            val s4 = punches.getOrNull(7)?.time?.toString() ?: "—"

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

                            Row(
                                modifier = Modifier
                                    .background(if (idx % 2 == 1) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f) else Color.Transparent)
                                    .padding(vertical = 8.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(day.date.format(DateTimeFormatter.ofPattern("dd/MM")), style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace), modifier = Modifier.width(52.dp))
                                Text(dayOfWeekAbbr[day.date.dayOfWeek] ?: "", style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(36.dp))
                                Text(day.dayType.labelPtBr.take(8), style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp), modifier = Modifier.width(64.dp))

                                val centerAlign = androidx.compose.ui.text.style.TextAlign.Center
                                Text(e1, style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontFamily = FontFamily.Monospace, textAlign = centerAlign), modifier = Modifier.width(44.dp))
                                Text(s1, style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontFamily = FontFamily.Monospace, textAlign = centerAlign), modifier = Modifier.width(44.dp))
                                Text(e2, style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontFamily = FontFamily.Monospace, textAlign = centerAlign), modifier = Modifier.width(44.dp))
                                Text(s2, style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontFamily = FontFamily.Monospace, textAlign = centerAlign), modifier = Modifier.width(44.dp))
                                Text(e3, style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontFamily = FontFamily.Monospace, textAlign = centerAlign), modifier = Modifier.width(44.dp))
                                Text(s3, style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontFamily = FontFamily.Monospace, textAlign = centerAlign), modifier = Modifier.width(44.dp))
                                Text(e4, style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontFamily = FontFamily.Monospace, textAlign = centerAlign), modifier = Modifier.width(44.dp))
                                Text(s4, style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontFamily = FontFamily.Monospace, textAlign = centerAlign), modifier = Modifier.width(44.dp))

                                Text(CalculationEngine.formatMinutesToHours(day.workedMinutes), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, textAlign = centerAlign), modifier = Modifier.width(52.dp))
                                Text(if (day.overtime50Minutes > 0) CalculationEngine.formatMinutesToHours(day.overtime50Minutes) else "—", style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, textAlign = centerAlign), modifier = Modifier.width(52.dp))
                                Text(if (day.overtime60Minutes > 0) CalculationEngine.formatMinutesToHours(day.overtime60Minutes) else "—", style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, textAlign = centerAlign), modifier = Modifier.width(52.dp))
                                Text(if (day.overtime100Minutes > 0) CalculationEngine.formatMinutesToHours(day.overtime100Minutes) else "—", style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, textAlign = centerAlign), modifier = Modifier.width(54.dp))
                                Text(if (day.nightMinutesReduced > 0) CalculationEngine.formatMinutesToHours(day.nightMinutesReduced) else "—", style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, textAlign = centerAlign), modifier = Modifier.width(48.dp))

                                Text(obs, style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp), modifier = Modifier.width(160.dp), maxLines = 1)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
