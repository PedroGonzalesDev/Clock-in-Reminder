package com.clockinreminder.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.clockinreminder.app.data.local.entity.DayType
import com.clockinreminder.app.data.local.entity.PunchEntity
import com.clockinreminder.app.data.local.entity.PunchType
import com.clockinreminder.app.ui.theme.ControlledRed
import com.clockinreminder.app.ui.theme.PetrolTeal
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun ManualPunchDialog(
    initialDate: LocalDate,
    editingPunch: PunchEntity? = null,
    onDismiss: () -> Unit,
    onSave: (date: LocalDate, time: LocalTime, type: PunchType, note: String?, location: String?) -> Unit,
    onDelete: ((punchId: Long) -> Unit)? = null
) {
    val initialTime = editingPunch?.time ?: LocalTime.now()
    var hourText by remember { mutableStateOf("%02d".format(initialTime.hour)) }
    var minuteText by remember { mutableStateOf("%02d".format(initialTime.minute)) }
    var selectedType by remember { mutableStateOf(editingPunch?.type ?: PunchType.ENTRY) }
    var noteText by remember { mutableStateOf(editingPunch?.note ?: "") }
    var locationTag by remember { mutableStateOf(editingPunch?.locationDescription ?: "Escritório") }

    val locationOptions = listOf("Escritório", "Home Office", "Cliente", "Em deslocamento", "Local não informado")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (editingPunch == null) "Adicionar Marcação Manual" else "Editar Marcação de Ponto",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Data: ${initialDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Time Pickers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = hourText,
                        onValueChange = { if (it.length <= 2 && it.all { c -> c.isDigit() }) hourText = it },
                        label = { Text("Hora") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Text(":", style = MaterialTheme.typography.headlineMedium)
                    OutlinedTextField(
                        value = minuteText,
                        onValueChange = { if (it.length <= 2 && it.all { c -> c.isDigit() }) minuteText = it },
                        label = { Text("Minuto") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                // Punch Type Selector
                Text("Tipo de Marcação", style = MaterialTheme.typography.labelLarge)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    val types = listOf(
                        PunchType.ENTRY to "Entrada",
                        PunchType.LUNCH_OUT to "Saída Intervalo",
                        PunchType.LUNCH_RETURN to "Retorno Intervalo",
                        PunchType.EXIT to "Saída"
                    )
                    types.forEach { (type, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedType = type }
                        ) {
                            RadioButton(
                                selected = selectedType == type,
                                onClick = { selectedType = type }
                            )
                            Text(label, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                // Location tag
                Text("Local / Situação", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    locationOptions.take(3).forEach { loc ->
                        FilterChip(
                            selected = locationTag == loc,
                            onClick = { locationTag = loc },
                            label = { Text(loc, style = MaterialTheme.typography.bodySmall) }
                        )
                    }
                }

                // Notes
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Observação / Justificativa") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val h = hourText.toIntOrNull()?.coerceIn(0, 23) ?: 8
                    val m = minuteText.toIntOrNull()?.coerceIn(0, 59) ?: 0
                    val time = LocalTime.of(h, m)
                    onSave(initialDate, time, selectedType, noteText.ifEmpty { null }, locationTag)
                },
                colors = ButtonDefaults.buttonColors(containerColor = PetrolTeal)
            ) {
                Text("Salvar")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (editingPunch != null && onDelete != null) {
                    TextButton(
                        onClick = { onDelete(editingPunch.id) },
                        colors = ButtonDefaults.textButtonColors(contentColor = ControlledRed)
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Excluir")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancelar")
                }
            }
        }
    )
}

@Composable
fun DayClassificationDialog(
    date: LocalDate,
    currentType: DayType,
    currentNote: String?,
    onDismiss: () -> Unit,
    onSave: (type: DayType, note: String?) -> Unit
) {
    var selectedType by remember { mutableStateOf(currentType) }
    var note by remember { mutableStateOf(currentNote ?: "") }

    val options = listOf(
        DayType.NORMAL,
        DayType.FOLGA,
        DayType.FERIADO,
        DayType.FALTA,
        DayType.ATESTADO,
        DayType.FERIAS,
        DayType.SOBREAVISO,
        DayType.COMPENSACAO,
        DayType.LICENCA
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Classificar Dia (${date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))})")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Escolha a ocorrência para este dia. Ausências abonadas (férias, atestado, folga) não geram saldo negativo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                options.forEach { opt ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedType = opt }
                    ) {
                        RadioButton(
                            selected = selectedType == opt,
                            onClick = { selectedType = opt }
                        )
                        Text(
                            text = opt.labelPtBr,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (selectedType == opt) FontWeight.Bold else FontWeight.Normal
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Motivo / Observação") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(selectedType, note.ifEmpty { null }) },
                colors = ButtonDefaults.buttonColors(containerColor = PetrolTeal)
            ) {
                Text("Confirmar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
