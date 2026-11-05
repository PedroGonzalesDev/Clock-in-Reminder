package com.clockinreminder.app.ui.screens.calendar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clockinreminder.app.data.local.entity.DayType
import com.clockinreminder.app.data.local.entity.PunchType
import com.clockinreminder.app.data.repository.ClockInRepository
import com.clockinreminder.app.domain.engine.CalculationEngine
import com.clockinreminder.app.domain.model.DayCalculationResult
import com.clockinreminder.app.domain.model.MonthSummaryResult
import com.clockinreminder.app.ui.components.DayClassificationDialog
import com.clockinreminder.app.ui.components.ManualPunchDialog
import com.clockinreminder.app.ui.components.PunchTimelineItem
import com.clockinreminder.app.ui.theme.*
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

class CalendarViewModel(
    private val repository: ClockInRepository
) : ViewModel() {

    var currentYearMonth by mutableStateOf(YearMonth.now())
    var monthSummary by mutableStateOf<MonthSummaryResult?>(null)
    var selectedDayResult by mutableStateOf<DayCalculationResult?>(null)

    var isPunchDialogOpen by mutableStateOf(false)
    var isClassifyDialogOpen by mutableStateOf(false)

    init {
        loadMonth(currentYearMonth)
    }

    fun loadMonth(ym: YearMonth) {
        currentYearMonth = ym
        viewModelScope.launch {
            monthSummary = repository.calculateMonth(ym)
            // If a day was selected, refresh it
            selectedDayResult?.let { old ->
                selectedDayResult = monthSummary?.dayResults?.find { it.date == old.date }
            }
        }
    }

    fun nextMonth() = loadMonth(currentYearMonth.plusMonths(1))
    fun previousMonth() = loadMonth(currentYearMonth.minusMonths(1))
    fun goToCurrentMonth() = loadMonth(YearMonth.now())

    fun selectDay(result: DayCalculationResult) {
        selectedDayResult = result
    }

    fun saveManualPunch(date: LocalDate, time: LocalTime, type: PunchType, note: String?, loc: String?) {
        viewModelScope.launch {
            repository.registerPunch(
                date = date,
                time = time,
                type = type,
                source = com.clockinreminder.app.data.local.entity.PunchSource.MANUAL,
                note = note,
                locationDescription = loc
            )
            isPunchDialogOpen = false
            loadMonth(currentYearMonth)
        }
    }

    fun deletePunch(id: Long) {
        viewModelScope.launch {
            repository.deletePunch(id, "Exclusão manual no calendário")
            isPunchDialogOpen = false
            loadMonth(currentYearMonth)
        }
    }

    fun classifyDay(type: DayType, note: String?) {
        val selected = selectedDayResult ?: return
        viewModelScope.launch {
            val occ = com.clockinreminder.app.data.local.entity.DayOccurrenceEntity(
                profileId = 1L,
                date = selected.date,
                type = type,
                note = note
            )
            repository.setOccurrence(occ)
            isClassifyDialogOpen = false
            loadMonth(currentYearMonth)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel
) {
    val ym = viewModel.currentYearMonth
    val localePtBr = Locale("pt", "BR")
    val monthTitle = ym.format(DateTimeFormatter.ofPattern("MMMM 'de' yyyy", localePtBr)).replaceFirstChar { it.uppercase() }

    val daysInMonth = ym.lengthOfMonth()
    val firstDayOfMonth = ym.atDay(1)
    val startDayOfWeek = firstDayOfMonth.dayOfWeek.value % 7 // 0 = Sunday .. 6 = Saturday

    val daysGrid = remember(ym, viewModel.monthSummary) {
        val list = mutableListOf<DayCalculationResult?>()
        // Leading blank cells
        for (i in 0 until startDayOfWeek) {
            list.add(null)
        }
        val dayMap = viewModel.monthSummary?.dayResults?.associateBy { it.date.dayOfMonth } ?: emptyMap()
        for (day in 1..daysInMonth) {
            list.add(dayMap[day])
        }
        list
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Month Header Controls
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
                            text = monthTitle,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        viewModel.monthSummary?.let { sum ->
                            Text(
                                text = "Total trabalhado: ${CalculationEngine.formatMinutesToHours(sum.totalWorkedMinutes)}  |  Saldo: ${CalculationEngine.formatMinutesToHours(sum.totalDailyBalanceMinutes, withSign = true)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { viewModel.previousMonth() }) {
                            Icon(Icons.Filled.ChevronLeft, contentDescription = "Mês anterior")
                        }
                        if (ym != YearMonth.now()) {
                            TextButton(onClick = { viewModel.goToCurrentMonth() }) {
                                Text("Hoje", fontWeight = FontWeight.Bold, color = PetrolTeal)
                            }
                        }
                        IconButton(onClick = { viewModel.nextMonth() }) {
                            Icon(Icons.Filled.ChevronRight, contentDescription = "Próximo mês")
                        }
                    }
                }
            }

            // Days of week header (DOM, SEG, TER, QUA, QUI, SEX, SÁB)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                val headers = listOf("DOM", "SEG", "TER", "QUA", "QUI", "SEX", "SÁB")
                headers.forEach { h ->
                    Text(
                        text = h,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Calendar Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 6.dp)
            ) {
                items(daysGrid) { dayResult ->
                    if (dayResult == null) {
                        Box(modifier = Modifier.aspectRatio(1f))
                    } else {
                        CalendarDayCell(
                            result = dayResult,
                            isSelected = viewModel.selectedDayResult?.date == dayResult.date,
                            onClick = { viewModel.selectDay(dayResult) }
                        )
                    }
                }
            }

            // Bottom inspection sheet / panel when day is selected
            viewModel.selectedDayResult?.let { selected ->
                DayDetailSheet(
                    day = selected,
                    onClose = { viewModel.selectedDayResult = null },
                    onClassify = { viewModel.isClassifyDialogOpen = true },
                    onAddPunch = { viewModel.isPunchDialogOpen = true }
                )
            }
        }
    }

    // Dialogs
    if (viewModel.isPunchDialogOpen && viewModel.selectedDayResult != null) {
        val selDay = viewModel.selectedDayResult!!
        val firstPunch = selDay.punches.firstOrNull()
        val lastPunch = selDay.punches.lastOrNull()
        val dayLocation = firstPunch?.locationDescription ?: lastPunch?.locationDescription ?: "Escritório"
        val nextType = when (lastPunch?.type) {
            null -> com.clockinreminder.app.data.local.entity.PunchType.ENTRY
            com.clockinreminder.app.data.local.entity.PunchType.ENTRY -> com.clockinreminder.app.data.local.entity.PunchType.LUNCH_OUT
            com.clockinreminder.app.data.local.entity.PunchType.LUNCH_OUT -> com.clockinreminder.app.data.local.entity.PunchType.LUNCH_RETURN
            com.clockinreminder.app.data.local.entity.PunchType.LUNCH_RETURN -> com.clockinreminder.app.data.local.entity.PunchType.EXIT
            com.clockinreminder.app.data.local.entity.PunchType.EXIT -> com.clockinreminder.app.data.local.entity.PunchType.ENTRY
        }
        ManualPunchDialog(
            initialDate = selDay.date,
            suggestedType = nextType,
            suggestedLocation = dayLocation,
            onDismiss = { viewModel.isPunchDialogOpen = false },
            onSave = { date, time, type, note, loc ->
                viewModel.saveManualPunch(date, time, type, note, loc)
            }
        )
    }

    if (viewModel.isClassifyDialogOpen && viewModel.selectedDayResult != null) {
        val selected = viewModel.selectedDayResult!!
        DayClassificationDialog(
            date = selected.date,
            currentType = selected.dayType,
            currentNote = selected.occurrence?.note,
            onDismiss = { viewModel.isClassifyDialogOpen = false },
            onSave = { type, note -> viewModel.classifyDay(type, note) }
        )
    }
}

@Composable
private fun CalendarDayCell(
    result: DayCalculationResult,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val isToday = result.date == LocalDate.now()
    val isWeekend = result.date.dayOfWeek == DayOfWeek.SATURDAY || result.date.dayOfWeek == DayOfWeek.SUNDAY

    val cellBorder = when {
        isSelected -> BorderStroke(2.dp, PetrolTeal)
        isToday -> BorderStroke(1.5.dp, WarmCopper)
        else -> BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
    }

    Card(
        modifier = Modifier
            .padding(2.dp)
            .aspectRatio(0.95f)
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        border = cellBorder,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) PetrolTeal.copy(alpha = 0.12f)
            else if (isToday) WarmCopper.copy(alpha = 0.08f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(3.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Day number + Type indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = result.date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = if (isToday) WarmCopper else if (isWeekend) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )

                // Small badge for absence or holiday
                if (result.dayType != DayType.NORMAL) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(
                                when (result.dayType) {
                                    DayType.FERIADO -> ControlledAmber
                                    DayType.ATESTADO -> PetrolTeal
                                    DayType.FERIAS -> WarmCopper
                                    DayType.FALTA -> ControlledRed
                                    else -> MaterialTheme.colorScheme.outline
                                }
                            )
                    )
                }
            }

            // Worked hours if any
            if (result.workedMinutes > 0) {
                Text(
                    text = CalculationEngine.formatMinutesToHours(result.workedMinutes),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            } else if (result.dayType != DayType.NORMAL) {
                Text(
                    text = result.dayType.labelPtBr.take(4),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 8.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Saldo Pill
            if (result.dailyBalanceMinutes != 0 && result.expectedMinutes > 0) {
                val saldoText = CalculationEngine.formatMinutesToHours(result.dailyBalanceMinutes, withSign = true)
                val saldoColor = if (result.dailyBalanceMinutes > 0) ControlledGreen else ControlledRed
                Text(
                    text = saldoText,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.5.sp, fontFamily = FontFamily.Monospace),
                    color = saldoColor,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun DayDetailSheet(
    day: DayCalculationResult,
    onClose: () -> Unit,
    onClassify: () -> Unit,
    onAddPunch: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = day.date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Classificação: ${day.dayType.labelPtBr}  |  Saldo: ${CalculationEngine.formatMinutesToHours(day.dailyBalanceMinutes, withSign = true)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Filled.Close, contentDescription = "Fechar")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Punches timeline summary
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Marcações (${day.punches.size}):", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                val punchStr = if (day.punches.isEmpty()) "Nenhuma batida" else day.punches.joinToString("  •  ") { "${it.type.name.take(3)}: ${it.time}" }
                Text(punchStr, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onClassify,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.EventNote, contentDescription = null, modifier = Modifier.size(16.dp), tint = WarmCopper)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ocorrência", fontSize = 12.sp)
                }

                Button(
                    onClick = onAddPunch,
                    colors = ButtonDefaults.buttonColors(containerColor = PetrolTeal),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Adicionar Batida", fontSize = 12.sp)
                }
            }
        }
    }
}
