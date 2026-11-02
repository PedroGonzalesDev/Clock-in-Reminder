package com.clockinreminder.app.ui.screens.inconsistencies

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clockinreminder.app.data.local.entity.DayType
import com.clockinreminder.app.data.local.entity.PunchType
import com.clockinreminder.app.data.repository.ClockInRepository
import com.clockinreminder.app.domain.model.InconsistencyItem
import com.clockinreminder.app.domain.model.InconsistencySeverity
import com.clockinreminder.app.ui.components.DayClassificationDialog
import com.clockinreminder.app.ui.components.ManualPunchDialog
import com.clockinreminder.app.ui.theme.ControlledGreen
import com.clockinreminder.app.ui.theme.ControlledRed
import com.clockinreminder.app.ui.theme.PetrolTeal
import com.clockinreminder.app.ui.theme.WarmCopper
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter

class InconsistenciesViewModel(
    private val repository: ClockInRepository
) : ViewModel() {

    var currentYearMonth by mutableStateOf(YearMonth.now())
    var issues by mutableStateOf<List<InconsistencyItem>>(emptyList())
    var activeActionDate by mutableStateOf<LocalDate?>(null)
    var isPunchDialogOpen by mutableStateOf(false)
    var isClassifyDialogOpen by mutableStateOf(false)

    init {
        loadIssues(currentYearMonth)
    }

    fun loadIssues(ym: YearMonth) {
        currentYearMonth = ym
        viewModelScope.launch {
            issues = repository.detectInconsistenciesForMonth(ym)
        }
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
            loadIssues(currentYearMonth)
        }
    }

    fun classifyDay(date: LocalDate, type: DayType, note: String?) {
        viewModelScope.launch {
            val occ = com.clockinreminder.app.data.local.entity.DayOccurrenceEntity(
                profileId = 1L,
                date = date,
                type = type,
                note = note
            )
            repository.setOccurrence(occ)
            isClassifyDialogOpen = false
            loadIssues(currentYearMonth)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InconsistenciesScreen(
    viewModel: InconsistenciesViewModel,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Revisar Marcações", fontWeight = FontWeight.Bold) },
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
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (viewModel.issues.isEmpty()) ControlledGreen.copy(alpha = 0.1f)
                    else WarmCopper.copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (viewModel.issues.isEmpty()) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                        contentDescription = null,
                        tint = if (viewModel.issues.isEmpty()) ControlledGreen else WarmCopper,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = if (viewModel.issues.isEmpty()) "Nenhuma inconsistência encontrada!" else "${viewModel.issues.size} inconsistência(s) detectada(s)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (viewModel.issues.isEmpty()) "Todas as marcações e jornadas do mês estão consistentes."
                            else "Revise os pontos abaixo para manter relatórios e cálculos auditáveis.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Issues List
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(viewModel.issues) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item.date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            if (item.severity == InconsistencySeverity.ERROR) ControlledRed.copy(alpha = 0.15f)
                                            else WarmCopper.copy(alpha = 0.15f)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = if (item.severity == InconsistencySeverity.ERROR) "ERRO" else "AVISO",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (item.severity == InconsistencySeverity.ERROR) ControlledRed else WarmCopper
                                    )
                                }
                            }

                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Text(
                                text = item.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = {
                                        viewModel.activeActionDate = item.date
                                        if (item.title.contains("ausência") || item.title.contains("útil")) {
                                            viewModel.isClassifyDialogOpen = true
                                        } else {
                                            viewModel.isPunchDialogOpen = true
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = PetrolTeal),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(item.suggestedAction, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (viewModel.isPunchDialogOpen && viewModel.activeActionDate != null) {
        ManualPunchDialog(
            initialDate = viewModel.activeActionDate!!,
            onDismiss = { viewModel.isPunchDialogOpen = false },
            onSave = { date, time, type, note, loc ->
                viewModel.saveManualPunch(date, time, type, note, loc)
            }
        )
    }

    if (viewModel.isClassifyDialogOpen && viewModel.activeActionDate != null) {
        DayClassificationDialog(
            date = viewModel.activeActionDate!!,
            currentType = DayType.NORMAL,
            currentNote = null,
            onDismiss = { viewModel.isClassifyDialogOpen = false },
            onSave = { type, note -> viewModel.classifyDay(viewModel.activeActionDate!!, type, note) }
        )
    }
}
