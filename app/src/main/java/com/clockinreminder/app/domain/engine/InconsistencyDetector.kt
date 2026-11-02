package com.clockinreminder.app.domain.engine

import com.clockinreminder.app.data.local.entity.*
import com.clockinreminder.app.domain.model.InconsistencyItem
import com.clockinreminder.app.domain.model.InconsistencySeverity
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit

object InconsistencyDetector {

    fun detectInconsistencies(
        date: LocalDate,
        punches: List<PunchEntity>,
        scheduleDay: ScheduleDayEntity?,
        occurrence: DayOccurrenceEntity?
    ): List<InconsistencyItem> {
        val issues = mutableListOf<InconsistencyItem>()
        val validPunches = punches.filter { !it.isDeleted }.sortedBy { it.time }

        // 1. Batidas duplicadas (mesmo minuto ou < 2 min de diferença)
        for (i in 0 until validPunches.size - 1) {
            val p1 = validPunches[i]
            val p2 = validPunches[i + 1]
            val diffSec = kotlin.math.abs(p2.time.toSecondOfDay() - p1.time.toSecondOfDay())
            if (diffSec < 120) {
                issues.add(
                    InconsistencyItem(
                        id = "dup_${p1.id}_${p2.id}",
                        date = date,
                        title = "Batida possivelmente duplicada",
                        description = "Duas marcações em menos de 2 minutos (${p1.time} e ${p2.time}).",
                        severity = InconsistencySeverity.WARNING,
                        suggestedAction = "Excluir uma das batidas duplicadas"
                    )
                )
            }
        }

        // 2. Duas entradas consecutivas ou duas saídas consecutivas
        for (i in 0 until validPunches.size - 1) {
            val current = validPunches[i]
            val next = validPunches[i + 1]
            val isCurrentEntry = current.type == PunchType.ENTRY || current.type == PunchType.LUNCH_RETURN
            val isNextEntry = next.type == PunchType.ENTRY || next.type == PunchType.LUNCH_RETURN

            if (isCurrentEntry && isNextEntry) {
                issues.add(
                    InconsistencyItem(
                        id = "seq_entry_${current.id}",
                        date = date,
                        title = "Duas entradas consecutivas",
                        description = "Marcação de entrada às ${current.time} seguida de outra entrada às ${next.time} sem saída intermediária.",
                        severity = InconsistencySeverity.ERROR,
                        suggestedAction = "Inserir registro de saída entre as marcações"
                    )
                )
            } else if (!isCurrentEntry && !isNextEntry) {
                issues.add(
                    InconsistencyItem(
                        id = "seq_exit_${current.id}",
                        date = date,
                        title = "Duas saídas consecutivas",
                        description = "Marcação de saída às ${current.time} seguida de outra saída às ${next.time} sem retorno intermediário.",
                        severity = InconsistencySeverity.ERROR,
                        suggestedAction = "Inserir registro de retorno de intervalo"
                    )
                )
            }
        }

        // 3. Número ímpar de batidas (sessão aberta)
        if (validPunches.isNotEmpty() && validPunches.size % 2 != 0) {
            val last = validPunches.last()
            val isToday = date == LocalDate.now()
            if (!isToday) {
                issues.add(
                    InconsistencyItem(
                        id = "open_shift_${date}",
                        date = date,
                        title = "Jornada aberta sem saída",
                        description = "A última batida foi ${last.type} às ${last.time}, sem registro de encerramento.",
                        severity = InconsistencySeverity.ERROR,
                        suggestedAction = "Adicionar saída para fechar o dia"
                    )
                )
            } else {
                // Today: check if session is excessively long (> 14h)
                val now = LocalTime.now()
                val sessionHours = (now.toSecondOfDay() - last.time.toSecondOfDay()) / 3600
                if (sessionHours >= 14) {
                    issues.add(
                        InconsistencyItem(
                            id = "long_session_${date}",
                            date = date,
                            title = "Sessão excessivamente longa (> 14h)",
                            description = "Jornada iniciada às ${last.time} continua aberta há mais de 14 horas.",
                            severity = InconsistencySeverity.WARNING,
                            suggestedAction = "Verifique se esqueceu de registrar a saída"
                        )
                    )
                }
            }
        }

        // 4. Falta de intervalo esperado para jornadas longas (> 6h)
        val intervals = CalculationEngine.extractWorkIntervals(validPunches)
        val workedMin = intervals.sumOf { it.durationMinutes }
        if (workedMin > 360 && validPunches.size == 2 && occurrence == null) {
            issues.add(
                InconsistencyItem(
                    id = "missing_lunch_${date}",
                    date = date,
                    title = "Intervalo intrajornada ausente",
                    description = "Jornada de mais de 6h (${CalculationEngine.formatMinutesToHours(workedMin)}) registrada com apenas uma entrada e uma saída contínuas.",
                    severity = InconsistencySeverity.WARNING,
                    suggestedAction = "Inserir saídas e retornos de almoço ou justificar"
                )
            )
        }

        // 5. Dia previsto sem registros de ponto após encerramento do dia
        val isPastDay = date.isBefore(LocalDate.now())
        val isScheduledWorkDay = scheduleDay?.isWorkDay == true && (scheduleDay.expectedMinutes > 0)
        if (isPastDay && isScheduledWorkDay && validPunches.isEmpty() && occurrence == null) {
            val expMin = scheduleDay?.expectedMinutes ?: 540
            issues.add(
                InconsistencyItem(
                    id = "unrecorded_workday_${date}",
                    date = date,
                    title = "Dia útil sem registros de ponto",
                    description = "Dia previsto com ${CalculationEngine.formatMinutesToHours(expMin)} de jornada sem batidas cadastradas.",
                    severity = InconsistencySeverity.WARNING,
                    suggestedAction = "Lançar batidas manuais ou justificar ausência (Folga, Férias, Falta, Atestado)"
                )
            )
        }

        // 6. Registro de ponto em dia marcado como ausência (Férias/Folga/Atestado)
        if (occurrence != null && occurrence.type.isAbsence && validPunches.isNotEmpty()) {
            issues.add(
                InconsistencyItem(
                    id = "punch_on_absence_${date}",
                    date = date,
                    title = "Marcação em dia classificado como ${occurrence.type.labelPtBr}",
                    description = "Existem ${validPunches.size} marcações de ponto em um dia marcado como ausência (${occurrence.type.labelPtBr}).",
                    severity = InconsistencySeverity.WARNING,
                    suggestedAction = "Verificar classificação do dia ou remover batidas indevidas"
                )
            )
        }

        return issues
    }
}
