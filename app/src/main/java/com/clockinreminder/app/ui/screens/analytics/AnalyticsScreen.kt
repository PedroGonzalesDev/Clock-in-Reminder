package com.clockinreminder.app.ui.screens.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clockinreminder.app.data.repository.ClockInRepository
import com.clockinreminder.app.domain.engine.CalculationEngine
import com.clockinreminder.app.domain.model.MonthSummaryResult
import com.clockinreminder.app.ui.components.KpiCard
import com.clockinreminder.app.ui.theme.*
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

class AnalyticsViewModel(
    private val repository: ClockInRepository
) : ViewModel() {

    var currentYearMonth by mutableStateOf(YearMonth.now())
    var monthSummary by mutableStateOf<MonthSummaryResult?>(null)

    init {
        loadMonth(currentYearMonth)
    }

    fun loadMonth(ym: YearMonth) {
        currentYearMonth = ym
        viewModelScope.launch {
            monthSummary = repository.calculateMonth(ym)
        }
    }

    fun nextMonth() = loadMonth(currentYearMonth.plusMonths(1))
    fun previousMonth() = loadMonth(currentYearMonth.minusMonths(1))
}

@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel
) {
    val ym = viewModel.currentYearMonth
    val localePtBr = Locale("pt", "BR")
    val monthTitle = ym.format(DateTimeFormatter.ofPattern("MMMM 'de' yyyy", localePtBr)).replaceFirstChar { it.uppercase() }
    val summary = viewModel.monthSummary

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
                            text = "Análises de Desempenho",
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

            Spacer(modifier = Modifier.height(16.dp))

            if (summary != null) {
                // Top KPIs row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiCard(
                        title = "Trabalhado",
                        value = CalculationEngine.formatMinutesToHours(summary.totalWorkedMinutes),
                        subtitle = "Previsto: ${CalculationEngine.formatMinutesToHours(summary.totalExpectedMinutes)}",
                        modifier = Modifier.weight(1f)
                    )

                    val saldoCol = if (summary.totalDailyBalanceMinutes >= 0) ControlledGreen else ControlledRed
                    KpiCard(
                        title = "Saldo Mês",
                        value = CalculationEngine.formatMinutesToHours(summary.totalDailyBalanceMinutes, withSign = true),
                        subtitle = if (summary.totalDailyBalanceMinutes >= 0) "Superávit" else "Déficit",
                        valueColor = saldoCol,
                        borderColor = saldoCol.copy(alpha = 0.4f),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Daily Hours Worked Bar Chart
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
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "HORAS TRABALHADAS POR DIA",
                            style = MaterialTheme.typography.labelSmall,
                            color = WarmCopper,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Custom Bar Chart using Compose Layout
                        val maxMinutes = maxOf(600, summary.dayResults.maxOfOrNull { it.workedMinutes } ?: 600)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            summary.dayResults.forEach { day ->
                                val barFraction = (day.workedMinutes.toFloat() / maxMinutes.toFloat()).coerceIn(0.04f, 1f)
                                val barColor = when {
                                    day.workedMinutes == 0 -> MaterialTheme.colorScheme.surfaceVariant
                                    day.overtime100Minutes > 0 -> ControlledRed
                                    day.overtime60Minutes > 0 -> WarmCopper
                                    day.overtime50Minutes > 0 -> PetrolTealLight
                                    else -> PetrolTeal
                                }

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(0.65f)
                                            .fillMaxHeight(barFraction)
                                            .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                                            .background(barColor)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    if (day.date.dayOfMonth % 5 == 1 || day.date.dayOfMonth == summary.dayResults.size) {
                                        Text(
                                            text = day.date.dayOfMonth.toString(),
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.height(10.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Breakdown by Rate Category Card
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
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "COMPOSIÇÃO DE HORAS",
                            style = MaterialTheme.typography.labelSmall,
                            color = WarmCopper,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )

                        AnalyticsRow(label = "Horas Normais", minutes = summary.totalNormalMinutes, payCents = summary.totalNormalPayCents, color = PetrolTeal)
                        AnalyticsRow(label = "Extras 50% (Dias úteis)", minutes = summary.totalOt50Minutes, payCents = summary.totalOt50PayCents, color = PetrolTealLight)
                        AnalyticsRow(label = "Extras 60% (Sábados)", minutes = summary.totalOt60Minutes, payCents = summary.totalOt60PayCents, color = WarmCopper)
                        AnalyticsRow(label = "Extras 100% (Dom / Feriados)", minutes = summary.totalOt100Minutes, payCents = summary.totalOt100PayCents, color = ControlledRed)
                        AnalyticsRow(label = "Adicional Noturno (Reduzido)", minutes = summary.totalNightMinutesReduced, payCents = summary.totalNightBonusPayCents, color = ControlledAmber)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Attendance Statistics Card
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
                            text = "ESTATÍSTICAS DE FREQUÊNCIA",
                            style = MaterialTheme.typography.labelSmall,
                            color = WarmCopper,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Dias com Trabalho Efetivo:", style = MaterialTheme.typography.bodySmall)
                            Text("${summary.daysWorked} dias", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Folgas e Descansos (DSR):", style = MaterialTheme.typography.bodySmall)
                            Text("${summary.daysOff} dias", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Feriados:", style = MaterialTheme.typography.bodySmall)
                            Text("${summary.daysHoliday} dias", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Atestados Médicos:", style = MaterialTheme.typography.bodySmall)
                            Text("${summary.daysSick} dias", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Férias:", style = MaterialTheme.typography.bodySmall)
                            Text("${summary.daysVacation} dias", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Faltas não justificadas:", style = MaterialTheme.typography.bodySmall)
                            Text("${summary.daysAbsence} dias", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = if (summary.daysAbsence > 0) ControlledRed else MaterialTheme.colorScheme.onSurface))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
private fun AnalyticsRow(label: String, minutes: Int, payCents: Long, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.bodySmall)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(CalculationEngine.formatMinutesToHours(minutes), style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold))
            Text(CalculationEngine.formatCentsToCurrency(payCents), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = color))
        }
    }
}
