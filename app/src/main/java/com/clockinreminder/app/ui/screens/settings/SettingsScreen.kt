package com.clockinreminder.app.ui.screens.settings

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clockinreminder.app.data.local.AppDatabase
import com.clockinreminder.app.data.local.entity.*
import com.clockinreminder.app.data.repository.ClockInRepository
import com.clockinreminder.app.export.BackupManager
import com.clockinreminder.app.notifications.ReminderScheduler
import com.clockinreminder.app.ui.theme.ControlledGreen
import com.clockinreminder.app.ui.theme.ControlledRed
import com.clockinreminder.app.ui.theme.PetrolTeal
import com.clockinreminder.app.ui.theme.WarmCopper
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate

class SettingsViewModel(
    private val repository: ClockInRepository,
    private val db: AppDatabase
) : ViewModel() {

    var profile by mutableStateOf<ProfileEntity?>(null)
    var payRule by mutableStateOf<PayRuleEntity?>(null)
    var bankPolicy by mutableStateOf<BankPolicyEntity?>(null)
    var statusMessage by mutableStateOf<String?>(null)

    // Edit fields
    var employeeName by mutableStateOf("")
    var companyName by mutableStateOf("")
    var hourlyRateText by mutableStateOf("25,74")
    var standardOtText by mutableStateOf("50")
    var saturdayOtText by mutableStateOf("60")
    var sundayOtText by mutableStateOf("100")
    var nightPercentText by mutableStateOf("30")
    var isReducedNightHour by mutableStateOf(true)
    var isBankEnabled by mutableStateOf(true)
    var sendOvertimeToBank by mutableStateOf(false)

    // Adjustment dialog
    var isAdjustmentDialogOpen by mutableStateOf(false)
    var isAboutDialogOpen by mutableStateOf(false)

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            val prof = repository.getProfile()
            profile = prof
            if (prof != null) {
                employeeName = prof.employeeName
                companyName = prof.companyName
            }
            val rule = repository.getEffectivePayRule(LocalDate.now())
            payRule = rule
            hourlyRateText = "%.2f".format(rule.hourlyRateCents / 100.0).replace('.', ',')
            standardOtText = rule.standardOvertimePercent.toString()
            saturdayOtText = rule.saturdayOvertimePercent.toString()
            sundayOtText = rule.sundayOvertimePercent.toString()
            nightPercentText = rule.nightAdditionPercent.toString()
            isReducedNightHour = rule.isReducedNightHour

            val bank = repository.getEffectiveBankPolicy(LocalDate.now())
            bankPolicy = bank
            if (bank != null) {
                isBankEnabled = bank.isEnabled
                sendOvertimeToBank = bank.sendOvertimeToBank
            }
        }
    }

    fun saveSettings(context: Context) {
        viewModelScope.launch {
            profile?.let {
                repository.saveProfile(it.copy(employeeName = employeeName.trim(), companyName = companyName.trim()))
            }

            val cents = parseRateToCents(hourlyRateText)
            val updatedRule = (payRule ?: PayRuleEntity(profileId = 1L, effectiveFromDate = LocalDate.now())).copy(
                hourlyRateCents = cents,
                standardOvertimePercent = standardOtText.toIntOrNull() ?: 50,
                saturdayOvertimePercent = saturdayOtText.toIntOrNull() ?: 60,
                sundayOvertimePercent = sundayOtText.toIntOrNull() ?: 100,
                nightAdditionPercent = nightPercentText.toIntOrNull() ?: 30,
                isReducedNightHour = isReducedNightHour
            )
            repository.savePayRule(updatedRule)

            val updatedBank = (bankPolicy ?: BankPolicyEntity(profileId = 1L, effectiveFromDate = LocalDate.now())).copy(
                isEnabled = isBankEnabled,
                sendOvertimeToBank = sendOvertimeToBank
            )
            repository.saveBankPolicy(updatedBank)

            // Reschedule reminders
            val schedule = repository.getEffectiveSchedule(LocalDate.now())
            val scheduleDays = schedule?.let { repository.getScheduleDays(it.id) } ?: emptyList()
            ReminderScheduler.scheduleRemindersForSchedule(context, scheduleDays)

            statusMessage = "Configurações salvas com sucesso!"
        }
    }

    fun addBankAdjustment(minutes: Long, reason: String) {
        viewModelScope.launch {
            repository.addBankAdjustment(
                BankAdjustmentEntity(
                    profileId = 1L,
                    date = LocalDate.now(),
                    adjustmentMinutes = minutes,
                    reason = reason,
                    authorizedBy = "Usuário"
                )
            )
            isAdjustmentDialogOpen = false
            statusMessage = "Ajuste de banco adicionado!"
        }
    }

    fun exportBackup(context: Context) {
        viewModelScope.launch {
            val file = BackupManager.exportBackupJson(context, db)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Backup Clock in Reminder")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(Intent.createChooser(intent, "Salvar Backup JSON"))
        }
    }

    private fun parseRateToCents(text: String): Long {
        return try {
            val cleaned = text.replace("R$", "").replace(".", "").replace(",", ".").trim()
            (cleaned.toDouble() * 100).toLong()
        } catch (e: Exception) {
            2574L
        }
    }
}

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel
) {
    val context = LocalContext.current
    var adjHoursText by remember { mutableStateOf("01:00") }
    var adjIsCredit by remember { mutableStateOf(true) }
    var adjReasonText by remember { mutableStateOf("Compensação autorizada") }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ajustes e Regras",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Button(
                        onClick = { viewModel.saveSettings(context) },
                        colors = ButtonDefaults.buttonColors(containerColor = PetrolTeal),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Salvar", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Status message snack bar if any
            viewModel.statusMessage?.let { msg ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = ControlledGreen.copy(alpha = 0.15f))
                ) {
                    Text(
                        text = msg,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = ControlledGreen,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Identification Section
            SettingsSectionHeader(title = "PERFIL E EMPRESA")
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = viewModel.employeeName,
                        onValueChange = { viewModel.employeeName = it },
                        label = { Text("Nome do Funcionário") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = viewModel.companyName,
                        onValueChange = { viewModel.companyName = it },
                        label = { Text("Nome da Empresa") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Rates and Calculation Rules Section
            SettingsSectionHeader(title = "TAXAS E MOTOR DE CÁLCULO")
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = viewModel.hourlyRateText,
                        onValueChange = { viewModel.hourlyRateText = it },
                        label = { Text("Valor da Hora Base (R$/h)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = viewModel.standardOtText,
                            onValueChange = { viewModel.standardOtText = it },
                            label = { Text("Extra 50% (%)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = viewModel.saturdayOtText,
                            onValueChange = { viewModel.saturdayOtText = it },
                            label = { Text("Sábado (%)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = viewModel.sundayOtText,
                            onValueChange = { viewModel.sundayOtText = it },
                            label = { Text("Dom/Fer (%)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    OutlinedTextField(
                        value = viewModel.nightPercentText,
                        onValueChange = { viewModel.nightPercentText = it },
                        label = { Text("Adicional Noturno (%)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Switch(
                            checked = viewModel.isReducedNightHour,
                            onCheckedChange = { viewModel.isReducedNightHour = it }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Hora Noturna Reduzida (52m30s)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text("Aplica fator 1,1428x entre 22h e 05h", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bank of Hours Section
            SettingsSectionHeader(title = "BANCO DE HORAS")
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Switch(
                            checked = viewModel.isBankEnabled,
                            onCheckedChange = { viewModel.isBankEnabled = it }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Banco de Horas Ativo", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Switch(
                            checked = viewModel.sendOvertimeToBank,
                            onCheckedChange = { viewModel.sendOvertimeToBank = it }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Enviar Horas Extras ao Banco", style = MaterialTheme.typography.bodyMedium)
                            Text("Se desmarcado, horas extras são remuneradas financeiramente", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    OutlinedButton(
                        onClick = { viewModel.isAdjustmentDialogOpen = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Tune, contentDescription = null, modifier = Modifier.size(16.dp), tint = WarmCopper)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Lançar Ajuste Manual de Banco")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Backup & Data Management
            SettingsSectionHeader(title = "DADOS E BACKUP LOCAL")
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { viewModel.exportBackup(context) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = PetrolTeal)
                    ) {
                        Icon(Icons.Filled.Backup, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Exportar Backup JSON")
                    }

                    OutlinedButton(
                        onClick = { viewModel.isAboutDialogOpen = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Info, contentDescription = null, modifier = Modifier.size(18.dp), tint = WarmCopper)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sobre o App e Memória de Cálculo")
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }

    // Adjustment Dialog
    if (viewModel.isAdjustmentDialogOpen) {
        AlertDialog(
            onDismissRequest = { viewModel.isAdjustmentDialogOpen = false },
            title = { Text("Lançar Ajuste de Banco de Horas") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = adjIsCredit, onClick = { adjIsCredit = true })
                        Text("Crédito (+)")
                        Spacer(modifier = Modifier.width(16.dp))
                        RadioButton(selected = !adjIsCredit, onClick = { adjIsCredit = false })
                        Text("Débito (−)")
                    }

                    OutlinedTextField(
                        value = adjHoursText,
                        onValueChange = { adjHoursText = it },
                        label = { Text("Quantidade de Tempo (HH:mm)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = adjReasonText,
                        onValueChange = { adjReasonText = it },
                        label = { Text("Motivo / Autorização") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parts = adjHoursText.split(":")
                        val min = (parts.getOrNull(0)?.toLongOrNull() ?: 1L) * 60 + (parts.getOrNull(1)?.toLongOrNull() ?: 0L)
                        val signedMin = if (adjIsCredit) min else -min
                        viewModel.addBankAdjustment(signedMin, adjReasonText)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PetrolTeal)
                ) {
                    Text("Lançar")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.isAdjustmentDialogOpen = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // About Dialog
    if (viewModel.isAboutDialogOpen) {
        AlertDialog(
            onDismissRequest = { viewModel.isAboutDialogOpen = false },
            title = { Text("Clock in Reminder v1.0.0") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Controle Pessoal de Ponto e Produtividade", fontWeight = FontWeight.Bold, color = PetrolTeal)
                    Text("• Funcionamento 100% offline, local e seguro.")
                    Text("• Sem login, sem anúncios e sem coleta de dados externos.")
                    Text("• Motor determinístico de cálculo de horas normais, extras 50%, 60%, 100% e adicional noturno com hora reduzida (52m30s).")
                    Text("• Base de regras e layout inspirada na planilha modelo de horas de Pedro Gonzales.")
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Aviso: Este aplicativo é um instrumento de controle pessoal do profissional e não substitui o sistema de ponto eletrônico ou homologado da empresa.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.isAboutDialogOpen = false }) {
                    Text("Entendido")
                }
            }
        )
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        color = WarmCopper,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.8.sp,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
    )
}
