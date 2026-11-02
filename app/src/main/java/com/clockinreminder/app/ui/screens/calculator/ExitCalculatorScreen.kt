package com.clockinreminder.app.ui.screens.calculator

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.HelpOutline
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
import com.clockinreminder.app.data.local.entity.PunchEntity
import com.clockinreminder.app.data.repository.ClockInRepository
import com.clockinreminder.app.domain.engine.CalculationEngine
import com.clockinreminder.app.domain.engine.ExitCalculator
import com.clockinreminder.app.domain.model.ExitSimulationResult
import com.clockinreminder.app.ui.theme.ControlledGreen
import com.clockinreminder.app.ui.theme.PetrolTeal
import com.clockinreminder.app.ui.theme.WarmCopper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

class ExitCalculatorViewModel(
    private val repository: ClockInRepository
) : ViewModel() {

    var punches by mutableStateOf<List<PunchEntity>>(emptyList())
    var expectedMinutes by mutableStateOf(540)
    var currentBankMinutes by mutableStateOf(0L)
    var currentTime by mutableStateOf(LocalTime.now())
    var scenarios by mutableStateOf<List<ExitSimulationResult>>(emptyList())
    var selectedScenarioIndex by mutableStateOf(0)

    init {
        loadData()
        viewModelScope.launch {
            while (true) {
                currentTime = LocalTime.now()
                recalculate()
                delay(30000) // update every 30s
            }
        }
    }

    fun loadData() {
        val today = LocalDate.now()
        viewModelScope.launch {
            punches = repository.getPunchesForDate(today)
            val schedule = repository.getEffectiveSchedule(today)
            val scheduleDays = schedule?.let { repository.getScheduleDays(it.id) } ?: emptyList()
            val day = scheduleDays.find { it.dayOfWeek == today.dayOfWeek.value }
            expectedMinutes = day?.expectedMinutes ?: 540

            val monthSummary = repository.calculateMonth(YearMonth.from(today))
            currentBankMinutes = monthSummary.closingBankMinutes

            recalculate()
        }
    }

    private fun recalculate() {
        scenarios = ExitCalculator.calculateScenarios(
            currentTime = currentTime,
            punches = punches,
            expectedMinutes = expectedMinutes,
            defaultLunchMinutes = 60,
            currentBankMinutes = currentBankMinutes
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExitCalculatorScreen(
    viewModel: ExitCalculatorViewModel,
    onBack: () -> Unit
) {
    val selected = viewModel.scenarios.getOrNull(viewModel.selectedScenarioIndex)
        ?: viewModel.scenarios.firstOrNull()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calculadora de Saída", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 18.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Main Projected Departure Banner
            if (selected != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "HORÁRIO ESTIMADO DE SAÍDA",
                            style = MaterialTheme.typography.labelSmall,
                            color = WarmCopper,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = selected.projectedExitTime,
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 48.sp
                            ),
                            color = PetrolTeal
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = selected.scenarioName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = selected.explanation,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }

            // Scenarios List Selector
            Text(
                text = "SIMULAR OUTROS CENÁRIOS",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )

            viewModel.scenarios.forEachIndexed { index, sc ->
                val isSelected = index == viewModel.selectedScenarioIndex
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.selectedScenarioIndex = index }
                        .border(
                            width = if (isSelected) 2.dp else 0.5.dp,
                            color = if (isSelected) PetrolTeal else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) PetrolTeal.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = sc.scenarioName,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Meta de trabalho: ${CalculationEngine.formatMinutesToHours(sc.targetMinutes)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            text = sc.projectedExitTime,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            ),
                            color = if (isSelected) PetrolTeal else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Calculation Memory Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.HelpOutline, contentDescription = null, modifier = Modifier.size(16.dp), tint = WarmCopper)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Como calculamos:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = "1. Contabilizamos as horas dos períodos já finalizados e da sessão em andamento.\n" +
                                "2. Deduzimos os minutos restantes para atingir a meta escolhida.\n" +
                                "3. Se o intervalo intrajornada (almoço) ainda não tiver sido concluído, somamos o tempo de pausa previsto.\n" +
                                "4. Esta projeção é dinâmica e atualizada em tempo real.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
