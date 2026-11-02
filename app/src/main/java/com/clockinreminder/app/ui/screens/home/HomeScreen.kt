package com.clockinreminder.app.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clockinreminder.app.data.local.entity.DayType
import com.clockinreminder.app.data.local.entity.ProfileEntity
import com.clockinreminder.app.data.local.entity.PunchEntity
import com.clockinreminder.app.data.local.entity.PunchSource
import com.clockinreminder.app.data.local.entity.PunchType
import com.clockinreminder.app.data.repository.ClockInRepository
import com.clockinreminder.app.domain.engine.CalculationEngine
import com.clockinreminder.app.domain.model.DayCalculationResult
import com.clockinreminder.app.ui.components.*
import com.clockinreminder.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class HomeViewModel(
    private val repository: ClockInRepository
) : ViewModel() {

    var selectedDate by mutableStateOf(LocalDate.now())
    var profile by mutableStateOf<ProfileEntity?>(null)
    var punches by mutableStateOf<List<PunchEntity>>(emptyList())
    var calculationResult by mutableStateOf<DayCalculationResult?>(null)
    var bankBalanceMinutes by mutableStateOf(0L)

    var currentTime by mutableStateOf(LocalTime.now())
    var isPunchDialogOpen by mutableStateOf(false)
    var editingPunch by mutableStateOf<PunchEntity?>(null)
    var isClassifyDialogOpen by mutableStateOf(false)

    init {
        // Ticking clock
        viewModelScope.launch {
            while (true) {
                currentTime = LocalTime.now()
                delay(1000)
            }
        }

        // Profile flow
        viewModelScope.launch {
            repository.getProfileFlow().collectLatest {
                profile = it
                loadDataForDate(selectedDate)
            }
        }
    }

    fun loadDataForDate(date: LocalDate) {
        selectedDate = date
        viewModelScope.launch {
            punches = repository.getPunchesForDate(date)
            calculationResult = repository.calculateDay(date)
            val monthSummary = repository.calculateMonth(java.time.YearMonth.from(date))
            bankBalanceMinutes = monthSummary.closingBankMinutes
        }
    }

    fun goToPreviousDay() {
        loadDataForDate(selectedDate.minusDays(1))
    }

    fun goToNextDay() {
        loadDataForDate(selectedDate.plusDays(1))
    }

    fun goToToday() {
        loadDataForDate(LocalDate.now())
    }

    fun registerContextualPunch() {
        val nextType = getNextPunchType()
        viewModelScope.launch {
            repository.registerPunch(
                date = selectedDate,
                time = LocalTime.now().withSecond(0).withNano(0),
                type = nextType,
                source = PunchSource.AUTOMATIC,
                locationDescription = "Registrado no dispositivo"
            )
            loadDataForDate(selectedDate)
        }
    }

    fun saveManualPunch(date: LocalDate, time: LocalTime, type: PunchType, note: String?, location: String?) {
        viewModelScope.launch {
            if (editingPunch == null) {
                repository.registerPunch(
                    date = date,
                    time = time,
                    type = type,
                    source = PunchSource.MANUAL,
                    note = note,
                    locationDescription = location
                )
            } else {
                repository.updatePunch(
                    editingPunch!!.copy(
                        time = time,
                        type = type,
                        note = note,
                        locationDescription = location
                    ),
                    editReason = "Ajuste manual pelo usuário"
                )
            }
            isPunchDialogOpen = false
            editingPunch = null
            loadDataForDate(selectedDate)
        }
    }

    fun deletePunch(punchId: Long) {
        viewModelScope.launch {
            repository.deletePunch(punchId, "Exclusão manual pelo usuário")
            isPunchDialogOpen = false
            editingPunch = null
            loadDataForDate(selectedDate)
        }
    }

    fun classifyDay(type: DayType, note: String?) {
        viewModelScope.launch {
            val occurrence = com.clockinreminder.app.data.local.entity.DayOccurrenceEntity(
                profileId = 1L,
                date = selectedDate,
                type = type,
                note = note
            )
            repository.setOccurrence(occurrence)
            isClassifyDialogOpen = false
            loadDataForDate(selectedDate)
        }
    }

    fun getNextPunchType(): PunchType {
        val last = punches.lastOrNull { !it.isDeleted }
        return when (last?.type) {
            null -> PunchType.ENTRY
            PunchType.ENTRY -> PunchType.LUNCH_OUT
            PunchType.LUNCH_OUT -> PunchType.LUNCH_RETURN
            PunchType.LUNCH_RETURN -> PunchType.EXIT
            PunchType.EXIT -> PunchType.ENTRY
        }
    }

    fun getCurrentWorkState(): String {
        val last = punches.lastOrNull { !it.isDeleted }
        return when (last?.type) {
            null -> "Fora do expediente"
            PunchType.ENTRY, PunchType.LUNCH_RETURN -> "Trabalhando"
            PunchType.LUNCH_OUT -> "Em intervalo"
            PunchType.EXIT -> "Jornada concluída"
        }
    }

    fun getActiveSessionSeconds(): Long {
        val last = punches.lastOrNull { !it.isDeleted } ?: return 0L
        if (last.type == PunchType.ENTRY || last.type == PunchType.LUNCH_RETURN) {
            val startSec = last.time.toSecondOfDay()
            val nowSec = currentTime.toSecondOfDay()
            return maxOf(0, (nowSec - startSec).toLong())
        }
        return 0L
    }
}

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToCalculator: () -> Unit,
    onNavigateToInconsistencies: () -> Unit
) {
    val date = viewModel.selectedDate
    val isToday = date == LocalDate.now()
    val localePtBr = Locale("pt", "BR")
    val dayOfWeekName = date.dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, localePtBr).replaceFirstChar { it.uppercase() }
    val formattedDate = date.format(DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy", localePtBr))

    val result = viewModel.calculationResult
    val workState = viewModel.getCurrentWorkState()

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Header Top Bar
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            val greeting = viewModel.profile?.employeeName?.let { "Olá, ${it.split(" ").first()}" } ?: "Controle de Ponto"
                            Text(
                                text = greeting,
                                style = MaterialTheme.typography.bodySmall,
                                color = WarmCopper,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = dayOfWeekName,
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = formattedDate,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Day navigation controls
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { viewModel.goToPreviousDay() }) {
                                Icon(Icons.Filled.ChevronLeft, contentDescription = "Dia anterior")
                            }
                            if (!isToday) {
                                TextButton(onClick = { viewModel.goToToday() }) {
                                    Text("Hoje", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = PetrolTeal)
                                }
                            }
                            IconButton(onClick = { viewModel.goToNextDay() }) {
                                Icon(Icons.Filled.ChevronRight, contentDescription = "Próximo dia")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main Shift Instrument Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Status Badge Pill
                    val (statusColor, statusBg) = when (workState) {
                        "Trabalhando" -> Pair(ControlledGreen, ControlledGreen.copy(alpha = 0.15f))
                        "Em intervalo" -> Pair(WarmCopper, WarmCopper.copy(alpha = 0.15f))
                        "Jornada concluída" -> Pair(PetrolTeal, PetrolTeal.copy(alpha = 0.15f))
                        else -> Pair(MaterialTheme.colorScheme.onSurfaceVariant, MaterialTheme.colorScheme.surfaceVariant)
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(statusBg)
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(statusColor)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = workState.uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = statusColor,
                                letterSpacing = 0.6.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Big Real-time Clock
                    val time = viewModel.currentTime
                    Text(
                        text = "%02d:%02d:%02d".format(time.hour, time.minute, time.second),
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 42.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Active session elapsed counter
                    val sessionSec = viewModel.getActiveSessionSeconds()
                    if (sessionSec > 0) {
                        val sh = sessionSec / 3600
                        val sm = (sessionSec % 3600) / 60
                        val ss = sessionSec % 60
                        Text(
                            text = "Sessão atual: %02d:%02d:%02d".format(sh, sm, ss),
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            color = PetrolTeal
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Primary Contextual Punch Button
                    val nextType = viewModel.getNextPunchType()
                    val (btnLabel, btnSub) = when (nextType) {
                        PunchType.ENTRY -> Pair("Registrar Entrada", "Iniciar primeiro período de trabalho")
                        PunchType.LUNCH_OUT -> Pair("Iniciar Intervalo", "Pausa para refeição / descanso")
                        PunchType.LUNCH_RETURN -> Pair("Voltar do Intervalo", "Retomar expediente de trabalho")
                        PunchType.EXIT -> Pair("Registrar Saída", "Encerrar jornada do dia")
                    }

                    ContextualPunchButton(
                        label = btnLabel,
                        sublabel = btnSub,
                        onClick = { viewModel.registerContextualPunch() }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3 Key KPI Badges
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Horas Trabalhadas Hoje
                val trabMin = result?.workedMinutes ?: 0
                KpiCard(
                    title = "Trabalhadas",
                    value = CalculationEngine.formatMinutesToHours(trabMin),
                    subtitle = "Meta: ${CalculationEngine.formatMinutesToHours(result?.expectedMinutes ?: 540)}",
                    valueColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                // Saldo de Hoje
                val saldoMin = result?.dailyBalanceMinutes ?: 0
                val saldoColor = if (saldoMin >= 0) ControlledGreen else ControlledRed
                KpiCard(
                    title = "Saldo Hoje",
                    value = CalculationEngine.formatMinutesToHours(saldoMin, withSign = true),
                    subtitle = if (saldoMin >= 0) "Crédito" else "Débito",
                    valueColor = saldoColor,
                    borderColor = saldoColor.copy(alpha = 0.4f),
                    modifier = Modifier.weight(1f)
                )

                // Banco Acumulado
                val bankMin = viewModel.bankBalanceMinutes.toInt()
                val bankColor = if (bankMin >= 0) ControlledGreen else ControlledRed
                KpiCard(
                    title = "Banco Total",
                    value = CalculationEngine.formatMinutesToHours(bankMin, withSign = true),
                    subtitle = "Saldo fechamento",
                    valueColor = bankColor,
                    borderColor = bankColor.copy(alpha = 0.4f),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Secondary metrics pill bar (Extras 50/60/100, Noturnas, Restante)
            if (result != null && (result.overtime50Minutes > 0 || result.overtime60Minutes > 0 || result.overtime100Minutes > 0 || result.nightMinutesReduced > 0)) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (result.overtime50Minutes > 0) {
                            Text("Extra 50%: ${CalculationEngine.formatMinutesToHours(result.overtime50Minutes)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = WarmCopper)
                        }
                        if (result.overtime60Minutes > 0) {
                            Text("Extra 60%: ${CalculationEngine.formatMinutesToHours(result.overtime60Minutes)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = WarmCopper)
                        }
                        if (result.overtime100Minutes > 0) {
                            Text("Extra 100%: ${CalculationEngine.formatMinutesToHours(result.overtime100Minutes)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = ControlledRed)
                        }
                        if (result.nightMinutesReduced > 0) {
                            Text("Noturno: ${CalculationEngine.formatMinutesToHours(result.nightMinutesReduced)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = PetrolTeal)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Quick Actions Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onNavigateToCalculator,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Filled.Calculate, contentDescription = null, modifier = Modifier.size(16.dp), tint = PetrolTeal)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Calculadora", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = {
                        viewModel.editingPunch = null
                        viewModel.isPunchDialogOpen = true
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = PetrolTeal)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Manual", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = { viewModel.isClassifyDialogOpen = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Filled.EventNote, contentDescription = null, modifier = Modifier.size(16.dp), tint = WarmCopper)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ocorrência", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Timeline Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "LINHA DO TEMPO DO DIA",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = "${viewModel.punches.size} registros",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (viewModel.punches.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Filled.AccessTime, contentDescription = null, modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Nenhuma batida registrada para este dia",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    viewModel.punches.forEachIndexed { index, punch ->
                        PunchTimelineItem(
                            punch = punch,
                            isLast = index == viewModel.punches.size - 1,
                            onEdit = {
                                viewModel.editingPunch = punch
                                viewModel.isPunchDialogOpen = true
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }

    // Dialogs
    if (viewModel.isPunchDialogOpen) {
        ManualPunchDialog(
            initialDate = viewModel.selectedDate,
            editingPunch = viewModel.editingPunch,
            onDismiss = { viewModel.isPunchDialogOpen = false },
            onSave = { date, time, type, note, loc ->
                viewModel.saveManualPunch(date, time, type, note, loc)
            },
            onDelete = { id -> viewModel.deletePunch(id) }
        )
    }

    if (viewModel.isClassifyDialogOpen) {
        DayClassificationDialog(
            date = viewModel.selectedDate,
            currentType = result?.dayType ?: DayType.NORMAL,
            currentNote = result?.occurrence?.note,
            onDismiss = { viewModel.isClassifyDialogOpen = false },
            onSave = { type, note -> viewModel.classifyDay(type, note) }
        )
    }
}
