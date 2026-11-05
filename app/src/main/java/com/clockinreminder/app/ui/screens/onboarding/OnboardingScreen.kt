package com.clockinreminder.app.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clockinreminder.app.data.local.entity.*
import com.clockinreminder.app.data.repository.ClockInRepository
import com.clockinreminder.app.ui.theme.PetrolTeal
import com.clockinreminder.app.ui.theme.WarmCopper
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class OnboardingViewModel(
    private val repository: ClockInRepository
) : ViewModel() {

    var currentStep by mutableStateOf(1) // 1 = Identificação, 2 = Jornada, 3 = Valores & Regras, 4 = Revisão

    // Step A
    var companyName by mutableStateOf("Minha Empresa")
    var employeeName by mutableStateOf("Pedro Gonzales")
    var nickname by mutableStateOf("")
    var timezoneId by mutableStateOf(ZoneId.systemDefault().id)

    // Step B
    var isHoursOnly by mutableStateOf(false)
    var dailyTargetHoursText by mutableStateOf("09:00")
    var startTimeText by mutableStateOf("07:00")
    var lunchStartText by mutableStateOf("12:00")
    var lunchEndText by mutableStateOf("13:00")
    var endTimeText by mutableStateOf("17:00")
    var workWeekends by mutableStateOf(false)
    var toleranceMinutesText by mutableStateOf("10")

    // Step C
    var hourlyRateText by mutableStateOf("25,74")
    var standardOtPercentText by mutableStateOf("50")
    var saturdayOtPercentText by mutableStateOf("60")
    var sundayOtPercentText by mutableStateOf("100")
    var nightPercentText by mutableStateOf("30")
    var isReducedNightHour by mutableStateOf(true)
    var isBankEnabled by mutableStateOf(true)
    var sendOvertimeToBank by mutableStateOf(false)

    fun completeOnboarding(onSuccess: () -> Unit) {
        viewModelScope.launch {
            // Save Profile
            val profile = ProfileEntity(
                companyName = companyName.trim(),
                employeeName = employeeName.trim(),
                nickname = nickname.trim().ifEmpty { null },
                timezoneId = timezoneId
            )
            val profileId = repository.saveProfile(profile)

            // Save Schedule
            val effectiveDate = LocalDate.now().withDayOfMonth(1)
            val schedule = WorkScheduleEntity(
                profileId = profileId,
                name = "Padrão",
                effectiveFromDate = effectiveDate,
                isHoursOnly = isHoursOnly,
                dailyHoursTargetMinutes = parseDurationMinutes(dailyTargetHoursText, 540),
                dailyToleranceMinutes = toleranceMinutesText.toIntOrNull() ?: 10
            )

            val days = (1..7).map { dow ->
                val isWork = when (dow) {
                    in 1..5 -> true // Seg..Sex
                    else -> workWeekends // Sáb & Dom
                }
                ScheduleDayEntity(
                    scheduleId = 0,
                    dayOfWeek = dow,
                    isWorkDay = isWork,
                    startTime = parseTime(startTimeText, 7, 0),
                    lunchStartTime = parseTime(lunchStartText, 12, 0),
                    lunchEndTime = parseTime(lunchEndText, 13, 0),
                    endTime = parseTime(endTimeText, 17, 0),
                    expectedMinutes = if (isWork) parseDurationMinutes(dailyTargetHoursText, 540) else 0
                )
            }
            repository.saveScheduleWithDays(schedule, days)

            // Save Pay Rule
            val rateCents = parseCurrencyToCents(hourlyRateText, 2574L)
            val payRule = PayRuleEntity(
                profileId = profileId,
                effectiveFromDate = effectiveDate,
                hourlyRateCents = rateCents,
                standardOvertimePercent = standardOtPercentText.toIntOrNull() ?: 50,
                saturdayOvertimePercent = saturdayOtPercentText.toIntOrNull() ?: 60,
                sundayOvertimePercent = sundayOtPercentText.toIntOrNull() ?: 100,
                nightAdditionPercent = nightPercentText.toIntOrNull() ?: 30,
                isReducedNightHour = isReducedNightHour
            )
            repository.savePayRule(payRule)

            // Save Bank Policy
            val bankPolicy = BankPolicyEntity(
                profileId = profileId,
                effectiveFromDate = effectiveDate,
                isEnabled = isBankEnabled,
                autoAccumulate = true,
                sendOvertimeToBank = sendOvertimeToBank
            )
            repository.saveBankPolicy(bankPolicy)

            onSuccess()
        }
    }

    private fun parseTime(text: String, defaultH: Int, defaultM: Int): LocalTime {
        return try {
            val parts = text.split(":")
            LocalTime.of(parts[0].toInt(), parts[1].toInt())
        } catch (e: Exception) {
            LocalTime.of(defaultH, defaultM)
        }
    }

    private fun parseDurationMinutes(text: String, defaultMin: Int): Int {
        return try {
            val parts = text.split(":")
            parts[0].toInt() * 60 + parts[1].toInt()
        } catch (e: Exception) {
            defaultMin
        }
    }

    private fun parseCurrencyToCents(text: String, defaultCents: Long): Long {
        return try {
            val cleaned = text.replace("R$", "").replace(".", "").replace(",", ".").trim()
            (cleaned.toDouble() * 100).toLong()
        } catch (e: Exception) {
            defaultCents
        }
    }
}

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    onFinished: () -> Unit
) {
    Scaffold(
        bottomBar = {
            Surface(
                tonalElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (viewModel.currentStep > 1) {
                        OutlinedButton(onClick = { viewModel.currentStep-- }) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Voltar")
                        }
                    } else {
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    if (viewModel.currentStep < 4) {
                        Button(
                            onClick = { viewModel.currentStep++ },
                            colors = ButtonDefaults.buttonColors(containerColor = PetrolTeal)
                        ) {
                            Text("Avançar")
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    } else {
                        Button(
                            onClick = { viewModel.completeOnboarding(onFinished) },
                            colors = ButtonDefaults.buttonColors(containerColor = PetrolTeal)
                        ) {
                            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Começar a Registrar", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Step Indicator Header
            Text(
                text = "CONFIGURAÇÃO INICIAL",
                style = MaterialTheme.typography.labelSmall,
                color = WarmCopper,
                letterSpacing = 1.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = when (viewModel.currentStep) {
                    1 -> "Identificação do Profissional"
                    2 -> "Definição de Jornada"
                    3 -> "Valores e Regras de Cálculo"
                    else -> "Revisão e Confirmação"
                },
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )

            // Step Progress Bar
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = viewModel.currentStep / 4f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = PetrolTeal,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            when (viewModel.currentStep) {
                1 -> StepIdentification(viewModel)
                2 -> StepSchedule(viewModel)
                3 -> StepPayRules(viewModel)
                4 -> StepReview(viewModel)
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun StepIdentification(vm: OnboardingViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "Informe seus dados básicos para emissão dos relatórios de ponto e cálculo personalizado.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value = vm.employeeName,
            onValueChange = { vm.employeeName = it },
            label = { Text("Nome Completo do Funcionário *") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = vm.companyName,
            onValueChange = { vm.companyName = it },
            label = { Text("Nome da Empresa / Empregador *") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = vm.nickname,
            onValueChange = { vm.nickname = it },
            label = { Text("Apelido do Perfil (opcional)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = vm.timezoneId,
            onValueChange = { vm.timezoneId = it },
            label = { Text("Fuso Horário") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            supportingText = { Text("Padrão do dispositivo: ${ZoneId.systemDefault().id}") }
        )
    }
}

@Composable
private fun StepSchedule(vm: OnboardingViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "Configure sua rotina de trabalho habitual. Você poderá registrar marcações manuais e turnos variáveis a qualquer momento.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value = vm.dailyTargetHoursText,
            onValueChange = { vm.dailyTargetHoursText = it },
            label = { Text("Carga Horária Diária Prevista (HH:mm)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            supportingText = { Text("Exemplo: 09:00 (9 horas de trabalho por dia)") }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = vm.startTimeText,
                onValueChange = { vm.startTimeText = it },
                label = { Text("Início Entrada") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = vm.endTimeText,
                onValueChange = { vm.endTimeText = it },
                label = { Text("Fim Saída") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = vm.lunchStartText,
                onValueChange = { vm.lunchStartText = it },
                label = { Text("Saída Intervalo") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = vm.lunchEndText,
                onValueChange = { vm.lunchEndText = it },
                label = { Text("Retorno Intervalo") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Escala de Trabalho habitual:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { vm.workWeekends = false }
            ) {
                RadioButton(
                    selected = !vm.workWeekends,
                    onClick = { vm.workWeekends = false }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("Não trabalho sábado e domingo", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text("Segunda a Sexta (5x2). Fins de semana são folgas contratuais.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { vm.workWeekends = true }
            ) {
                RadioButton(
                    selected = vm.workWeekends,
                    onClick = { vm.workWeekends = true }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("Também trabalho sábado e domingo", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text("Escala 6x1, 6x2 ou flexível. Você registra folgas nos dias específicos.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        OutlinedTextField(
            value = vm.toleranceMinutesText,
            onValueChange = { vm.toleranceMinutesText = it },
            label = { Text("Tolerância Diária de Variação (minutos)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            supportingText = { Text("CLT padrão: até 10 minutos diários não são computados como falta ou extra.") }
        )
    }
}

@Composable
private fun StepPayRules(vm: OnboardingViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "Defina os parâmetros de remuneração e horas extras. Os valores são estimativas pessoais para transparência financeira.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value = vm.hourlyRateText,
            onValueChange = { vm.hourlyRateText = it },
            label = { Text("Valor-Base da Hora Normal (R$/hora)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            supportingText = { Text("Conforme planilha de referência: R$ 25,74") }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = vm.standardOtPercentText,
                onValueChange = { vm.standardOtPercentText = it },
                label = { Text("Extra Comum (%)") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = vm.saturdayOtPercentText,
                onValueChange = { vm.saturdayOtPercentText = it },
                label = { Text("Sábado (%)") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = vm.sundayOtPercentText,
                onValueChange = { vm.sundayOtPercentText = it },
                label = { Text("Dom / Fer (%)") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }

        OutlinedTextField(
            value = vm.nightPercentText,
            onValueChange = { vm.nightPercentText = it },
            label = { Text("Adicional Noturno (%)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            supportingText = { Text("Planilha de referência: 30% (padrão legal mínimo: 20%)") }
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Switch(
                checked = vm.isReducedNightHour,
                onCheckedChange = { vm.isReducedNightHour = it }
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("Hora Noturna Reduzida (52min 30s)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text("Equivale a 1,1428x da hora civil entre 22:00 e 05:00", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Switch(
                checked = vm.isBankEnabled,
                onCheckedChange = { vm.isBankEnabled = it }
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("Habilitar Banco de Horas", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text("Acumula créditos e débitos de horas para compensação futura", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun StepReview(vm: OnboardingViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("RESUMO DAS REGRAS CONFIGURADAS", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = PetrolTeal)
            Divider()
            Text("Funcionário: ${vm.employeeName}")
            Text("Empresa: ${vm.companyName}")
            Text("Jornada Diária Prevista: ${vm.dailyTargetHoursText}")
            Text("Horários Previstos: ${vm.startTimeText} às ${vm.endTimeText} (Intervalo ${vm.lunchStartText} às ${vm.lunchEndText})")
            Text("Valor da Hora Base: R$ ${vm.hourlyRateText}/h")
            Text("Adicionais: Extra Comum ${vm.standardOtPercentText}%, Sábado ${vm.saturdayOtPercentText}%, Dom/Fer ${vm.sundayOtPercentText}%, Noturno ${vm.nightPercentText}%")
            Text("Hora Noturna Reduzida: ${if (vm.isReducedNightHour) "Ativada (52m30s)" else "Desativada (60m)"}")
            Text("Banco de Horas: ${if (vm.isBankEnabled) "Ativo" else "Desativado"}")
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Todas as configurações podem ser alteradas depois na aba Ajustes com histórico de vigência.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
