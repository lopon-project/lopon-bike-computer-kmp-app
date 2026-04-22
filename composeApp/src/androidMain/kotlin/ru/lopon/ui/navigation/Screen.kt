package ru.lopon.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Home : Screen("home", "Главная", Icons.Outlined.Home)
    data object History : Screen("history", "История", Icons.Outlined.History)
    data object Routes : Screen("routes", "Маршруты", Icons.Outlined.Map)
    data object More : Screen("more", "Ещё", Icons.Outlined.Settings)
}
