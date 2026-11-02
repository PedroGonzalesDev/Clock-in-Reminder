package com.clockinreminder.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.clockinreminder.app.ClockInApp
import com.clockinreminder.app.ui.screens.analytics.AnalyticsScreen
import com.clockinreminder.app.ui.screens.analytics.AnalyticsViewModel
import com.clockinreminder.app.ui.screens.calculator.ExitCalculatorScreen
import com.clockinreminder.app.ui.screens.calculator.ExitCalculatorViewModel
import com.clockinreminder.app.ui.screens.calendar.CalendarScreen
import com.clockinreminder.app.ui.screens.calendar.CalendarViewModel
import com.clockinreminder.app.ui.screens.home.HomeScreen
import com.clockinreminder.app.ui.screens.home.HomeViewModel
import com.clockinreminder.app.ui.screens.inconsistencies.InconsistenciesScreen
import com.clockinreminder.app.ui.screens.inconsistencies.InconsistenciesViewModel
import com.clockinreminder.app.ui.screens.onboarding.OnboardingScreen
import com.clockinreminder.app.ui.screens.onboarding.OnboardingViewModel
import com.clockinreminder.app.ui.screens.reports.ReportsScreen
import com.clockinreminder.app.ui.screens.reports.ReportsViewModel
import com.clockinreminder.app.ui.screens.settings.SettingsScreen
import com.clockinreminder.app.ui.screens.settings.SettingsViewModel
import com.clockinreminder.app.ui.theme.PetrolTeal

@Composable
fun AppNavigation(app: ClockInApp) {
    val navController = rememberNavController()
    val repository = app.repository
    val db = app.database

    val profileState by repository.getProfileFlow().collectAsState(initial = null)
    val hasCheckedProfile = remember { mutableStateOf(false) }

    LaunchedEffect(profileState) {
        if (!hasCheckedProfile.value) {
            hasCheckedProfile.value = true
        }
    }

    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStack?.destination?.route

    val showBottomBar = currentRoute in Screen.bottomNavItems.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp
                ) {
                    Screen.bottomNavItems.forEach { screen ->
                        val selected = currentRoute == screen.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (currentRoute != screen.route) {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) screen.selectedIcon else screen.icon,
                                    contentDescription = screen.title
                                )
                            },
                            label = {
                                Text(
                                    text = screen.title,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                    )
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = PetrolTeal,
                                selectedTextColor = PetrolTeal,
                                indicatorColor = PetrolTeal.copy(alpha = 0.12f)
                            )
                        )
                    }
                }
            }
        }
    ) { padding ->
        val startDestination = if (profileState == null) Screen.Onboarding.route else Screen.Home.route

        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(padding)
        ) {
            composable(Screen.Onboarding.route) {
                val vm = remember { OnboardingViewModel(repository) }
                OnboardingScreen(
                    viewModel = vm,
                    onFinished = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Home.route) {
                val vm = remember { HomeViewModel(repository) }
                HomeScreen(
                    viewModel = vm,
                    onNavigateToCalculator = { navController.navigate(Screen.ExitCalculator.route) },
                    onNavigateToInconsistencies = { navController.navigate(Screen.Inconsistencies.route) }
                )
            }

            composable(Screen.Calendar.route) {
                val vm = remember { CalendarViewModel(repository) }
                CalendarScreen(viewModel = vm)
            }

            composable(Screen.Reports.route) {
                val vm = remember { ReportsViewModel(repository) }
                ReportsScreen(viewModel = vm)
            }

            composable(Screen.Analytics.route) {
                val vm = remember { AnalyticsViewModel(repository) }
                AnalyticsScreen(viewModel = vm)
            }

            composable(Screen.Settings.route) {
                val vm = remember { SettingsViewModel(repository, db) }
                SettingsScreen(viewModel = vm)
            }

            composable(Screen.ExitCalculator.route) {
                val vm = remember { ExitCalculatorViewModel(repository) }
                ExitCalculatorScreen(
                    viewModel = vm,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Inconsistencies.route) {
                val vm = remember { InconsistenciesViewModel(repository) }
                InconsistenciesScreen(
                    viewModel = vm,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
