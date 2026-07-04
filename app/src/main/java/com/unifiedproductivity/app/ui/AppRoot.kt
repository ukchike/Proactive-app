package com.unifiedproductivity.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.unifiedproductivity.app.di.AppContainer
import com.unifiedproductivity.app.ui.calendar.CalendarScreen
import com.unifiedproductivity.app.ui.calendar.CalendarViewModel
import com.unifiedproductivity.app.ui.home.HomeScreen
import com.unifiedproductivity.app.ui.home.HomeViewModel
import com.unifiedproductivity.app.ui.notes.NoteEditorScreen
import com.unifiedproductivity.app.ui.notes.NotesScreen
import com.unifiedproductivity.app.ui.notes.NotesViewModel
import com.unifiedproductivity.app.ui.reminders.RemindersScreen
import com.unifiedproductivity.app.ui.reminders.RemindersViewModel
import com.unifiedproductivity.app.ui.settings.SettingsScreen
import com.unifiedproductivity.app.ui.settings.SettingsViewModel

private sealed class TopLevel(val route: String, val label: String, val icon: ImageVector) {
    data object Home : TopLevel("home", "Home", Icons.Filled.Home)
    data object Notes : TopLevel("notes", "Notes", Icons.Filled.Description)
    data object Reminders : TopLevel("reminders", "Reminders", Icons.Filled.CheckCircle)
    data object Calendar : TopLevel("calendar", "Calendar", Icons.Filled.CalendarMonth)
    data object Settings : TopLevel("settings", "Settings", Icons.Filled.Settings)
}

private val topLevelDestinations = listOf(
    TopLevel.Home, TopLevel.Notes, TopLevel.Reminders, TopLevel.Calendar, TopLevel.Settings
)

@Composable
fun AppRoot(container: AppContainer) {
    val navController = rememberNavController()
    val factory = remember(container) { AppViewModelFactory(container) }

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            NavigationBar {
                topLevelDestinations.forEach { dest ->
                    val selected = currentDestination?.hierarchy?.any { it.route == dest.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(dest.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(dest.icon, contentDescription = dest.label) },
                        label = { Text(dest.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = TopLevel.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(TopLevel.Home.route) {
                val vm: HomeViewModel = viewModel(factory = factory)
                HomeScreen(
                    viewModel = vm,
                    onNewNote = { navController.navigate("note/new") },
                    onOpenReminders = { navController.navigate(TopLevel.Reminders.route) },
                    onOpenCalendar = { navController.navigate(TopLevel.Calendar.route) }
                )
            }
            composable(TopLevel.Notes.route) {
                val vm: NotesViewModel = viewModel(factory = factory)
                NotesScreen(
                    viewModel = vm,
                    onOpenNote = { noteId -> navController.navigate("note/$noteId") }
                )
            }
            composable("note/{noteId}") { backStackEntry ->
                val vm: NotesViewModel = viewModel(factory = factory)
                val noteId = backStackEntry.arguments?.getString("noteId") ?: "new"
                NoteEditorScreen(
                    viewModel = vm,
                    noteId = noteId,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(TopLevel.Reminders.route) {
                val vm: RemindersViewModel = viewModel(factory = factory)
                RemindersScreen(viewModel = vm)
            }
            composable(TopLevel.Calendar.route) {
                val vm: CalendarViewModel = viewModel(factory = factory)
                CalendarScreen(
                    viewModel = vm,
                    onOpenNote = { noteId -> navController.navigate("note/$noteId") }
                )
            }
            composable(TopLevel.Settings.route) {
                val vm: SettingsViewModel = viewModel(factory = factory)
                SettingsScreen(viewModel = vm)
            }
        }
    }
}
