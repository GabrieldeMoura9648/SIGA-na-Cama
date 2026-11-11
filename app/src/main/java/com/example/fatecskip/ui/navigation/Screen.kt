package com.example.fatecskip.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Onboarding : Screen("onboarding", "Boas-vindas", Icons.Default.Psychology)
    object Home : Screen("home", "Meu Dia", Icons.Default.Psychology)
    object Events : Screen("events", "Agenda do Caos", Icons.Default.CalendarMonth)
    object Settings : Screen("settings", "Sobrevivência", Icons.Default.Settings)
}
