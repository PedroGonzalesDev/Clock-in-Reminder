package com.clockinreminder.app.domain.engine

import com.clockinreminder.app.data.local.entity.PunchEntity
import com.clockinreminder.app.data.local.entity.PunchType
import com.clockinreminder.app.domain.model.ExitSimulationResult
import java.time.LocalTime
import java.time.format.DateTimeFormatter

object ExitCalculator {

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    /**
     * Calculates projected exit times for today.
     * @param currentTime Current local time
     * @param punches Today's registered punches
     * @param expectedMinutes Expected daily work minutes (e.g. 540 min = 9h)
     * @param defaultLunchMinutes Expected lunch break duration (e.g. 60 min)
     * @param currentBankMinutes Accumulated bank balance (can be positive or negative)
     */
    fun calculateScenarios(
        currentTime: LocalTime,
        punches: List<PunchEntity>,
        expectedMinutes: Int,
        defaultLunchMinutes: Int = 60,
        currentBankMinutes: Long = 0L
    ): List<ExitSimulationResult> {
        val validPunches = punches.filter { !it.isDeleted }.sortedBy { it.time }

        // Determine currently elapsed worked minutes and break status
        var completedWorkedMinutes = 0
        var openStart: LocalTime? = null
        var lunchTakenMinutes = 0
        var lunchStartTime: LocalTime? = null

        for (p in validPunches) {
            when (p.type) {
                PunchType.ENTRY -> {
                    openStart = p.time
                }
                PunchType.LUNCH_OUT -> {
                    if (openStart != null) {
                        completedWorkedMinutes += (p.time.toSecondOfDay() - openStart.toSecondOfDay()) / 60
                        openStart = null
                    }
                    lunchStartTime = p.time
                }
                PunchType.LUNCH_RETURN -> {
                    if (lunchStartTime != null) {
                        lunchTakenMinutes += (p.time.toSecondOfDay() - lunchStartTime.toSecondOfDay()) / 60
                        lunchStartTime = null
                    }
                    openStart = p.time
                }
                PunchType.EXIT -> {
                    if (openStart != null) {
                        completedWorkedMinutes += (p.time.toSecondOfDay() - openStart.toSecondOfDay()) / 60
                        openStart = null
                    }
                }
            }
        }

        val isCurrentlyWorking = openStart != null
        val currentWorkSessionMinutes = if (isCurrentlyWorking && openStart != null) {
            maxOf(0, (currentTime.toSecondOfDay() - openStart.toSecondOfDay()) / 60)
        } else 0

        val totalWorkedSoFar = completedWorkedMinutes + currentWorkSessionMinutes

        // Remaining planned lunch break if not yet taken
        val remainingLunchMinutes = if (lunchStartTime != null) {
            // Currently at lunch
            val currentLunchElapsed = maxOf(0, (currentTime.toSecondOfDay() - lunchStartTime.toSecondOfDay()) / 60)
            maxOf(0, defaultLunchMinutes - currentLunchElapsed)
        } else if (lunchTakenMinutes == 0 && validPunches.isNotEmpty() && expectedMinutes > 360) {
            // Lunch not taken yet
            defaultLunchMinutes
        } else 0

        fun estimateExitTime(targetWorkMinutes: Int): Pair<LocalTime, Int> {
            val remainingWork = maxOf(0, targetWorkMinutes - totalWorkedSoFar)
            val totalMinutesToExit = remainingWork + remainingLunchMinutes
            val projectedSeconds = currentTime.toSecondOfDay() + totalMinutesToExit * 60
            val normalizedSeconds = (projectedSeconds % (24 * 3600)).toInt()
            return Pair(LocalTime.ofSecondOfDay(normalizedSeconds.toLong()), remainingWork)
        }

        val scenarios = mutableListOf<ExitSimulationResult>()

        // 1. Cumprir jornada normal
        val (normalExit, normalRemaining) = estimateExitTime(expectedMinutes)
        scenarios.add(
            ExitSimulationResult(
                scenarioName = "Cumprir jornada normal",
                projectedExitTime = normalExit.format(timeFormatter),
                targetMinutes = expectedMinutes,
                remainingMinutes = normalRemaining,
                explanation = "Meta: ${CalculationEngine.formatMinutesToHours(expectedMinutes)}. Trabalhado: ${CalculationEngine.formatMinutesToHours(totalWorkedSoFar)}. Restam ${CalculationEngine.formatMinutesToHours(normalRemaining)}."
            )
        )

        // 2. Fazer +30 min
        val (ot30Exit, ot30Remaining) = estimateExitTime(expectedMinutes + 30)
        scenarios.add(
            ExitSimulationResult(
                scenarioName = "Meta +30 minutos extras",
                projectedExitTime = ot30Exit.format(timeFormatter),
                targetMinutes = expectedMinutes + 30,
                remainingMinutes = ot30Remaining,
                explanation = "Meta total: ${CalculationEngine.formatMinutesToHours(expectedMinutes + 30)} (Jornada normal + 30m de hora extra)."
            )
        )

        // 3. Fazer +1 hora extra
        val (ot60Exit, ot60Remaining) = estimateExitTime(expectedMinutes + 60)
        scenarios.add(
            ExitSimulationResult(
                scenarioName = "Meta +1h extra",
                projectedExitTime = ot60Exit.format(timeFormatter),
                targetMinutes = expectedMinutes + 60,
                remainingMinutes = ot60Remaining,
                explanation = "Meta total: ${CalculationEngine.formatMinutesToHours(expectedMinutes + 60)} (+1 hora extra)."
            )
        )

        // 4. Zerar banco negativo (se houver débito)
        if (currentBankMinutes < 0) {
            val deficitMinutes = kotlin.math.abs(currentBankMinutes).toInt()
            val (bankZeroExit, bankZeroRemaining) = estimateExitTime(expectedMinutes + deficitMinutes)
            scenarios.add(
                ExitSimulationResult(
                    scenarioName = "Compensar e zerar banco negativo hoje",
                    projectedExitTime = bankZeroExit.format(timeFormatter),
                    targetMinutes = expectedMinutes + deficitMinutes,
                    remainingMinutes = bankZeroRemaining,
                    explanation = "Saldo do banco atual: −${CalculationEngine.formatMinutesToHours(deficitMinutes)}. Trabalhando até ${bankZeroExit.format(timeFormatter)}, você compensa todo o débito hoje."
                )
            )

            // Projeção em mais de um dia (ex.: compensar em 3 dias)
            val partialCompMinutes = deficitMinutes / 3
            val (partialExit, _) = estimateExitTime(expectedMinutes + partialCompMinutes)
            scenarios.add(
                ExitSimulationResult(
                    scenarioName = "Compensar gradualmente (1/3 do banco hoje)",
                    projectedExitTime = partialExit.format(timeFormatter),
                    targetMinutes = expectedMinutes + partialCompMinutes,
                    remainingMinutes = maxOf(0, (expectedMinutes + partialCompMinutes) - totalWorkedSoFar),
                    explanation = "Compensa +${CalculationEngine.formatMinutesToHours(partialCompMinutes)} hoje e o restante nos próximos 2 dias úteis."
                )
            )
        }

        // 5. Sair mais cedo utilizando banco positivo
        if (currentBankMinutes > 0) {
            val availableCredits = minOf(currentBankMinutes.toInt(), 120) // até 2h
            val earlyTarget = maxOf(0, expectedMinutes - availableCredits)
            val (earlyExit, earlyRemaining) = estimateExitTime(earlyTarget)
            scenarios.add(
                ExitSimulationResult(
                    scenarioName = "Sair mais cedo usando banco (+${CalculationEngine.formatMinutesToHours(availableCredits)})",
                    projectedExitTime = earlyExit.format(timeFormatter),
                    targetMinutes = earlyTarget,
                    remainingMinutes = earlyRemaining,
                    explanation = "Simulação: utilizando ${CalculationEngine.formatMinutesToHours(availableCredits)} do saldo positivo de banco, você pode sair às ${earlyExit.format(timeFormatter)} (sujeito à política da empresa)."
                )
            )
        }

        return scenarios
    }
}
