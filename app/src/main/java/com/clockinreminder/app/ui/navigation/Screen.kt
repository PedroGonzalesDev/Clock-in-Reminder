package com.clockinreminder.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector
) {
    object Home : Screen(
        route = "home",
        title = "Hoje",
        icon = Icons.Outlined.Schedule,
        selectedIcon = Icons.Filled.Schedule
    )

    object Calendar : Screen(
        route = "calendar",
        title = "Calendário",
        icon = Icons.Outlined.CalendarMonth,
        selectedIcon = Icons.Filled.CalendarMonth
    )

    object Reports : Screen(
        route = "reports",
        title = "Relatórios",
        icon = Icons.Outlined.Description,
        selectedIcon = Icons.Filled.Description
    )

    object Analytics : Screen(
        route = "analytics",
        title = "Análises",
        icon = Icons.Outlined.BarChart,
        selectedIcon = Icons.Filled.BarChart
    )

    object Settings : Screen(
        route = "settings",
        title = "Ajustes",
        icon = Icons.Outlined.Tune,
        selectedIcon = Icons.Filled.Tune
    )

    object Onboarding : Screen(
        route = "onboarding",
        title = "Boas-vindas",
        icon = Icons.Outlined.Person,
        selectedIcon = Icons.Filled.Person
    )

    object ExitCalculator : Screen(
        route = "exit_calculator",
        title = "Calculadora de Saída",
        icon = Icons.Outlined.Calculate,
        selectedIcon = Icons.Filled.Calculate
    )

    object Inconsistencies : Screen(
        route = "inconsistencies",
        title = "Revisar Marcações",
        icon = Icons.Outlined.WarningAmber,
        selectedIcon = Icons.Filled.Warning
    )

    companion object {
        val bottomNavItems = listOf(Home, Calendar, Reports, Analytics, Settings)
    }
}
