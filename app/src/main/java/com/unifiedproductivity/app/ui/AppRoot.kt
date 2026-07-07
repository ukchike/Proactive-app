package com.unifiedproductivity.app.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.unifiedproductivity.app.di.AppContainer
import com.unifiedproductivity.app.ui.budget.BudgetScreen
import com.unifiedproductivity.app.ui.budget.BudgetViewModel
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

/**
 * iOS-style navigation: Home is a hub of app-like tiles plus priority/due items;
 * each module opens full screen (no bottom tab bar) until the user goes back Home.
 */
@Composable
fun AppRoot(container: AppContainer) {
    val navController = rememberNavController()
    val factory = remember(container) { AppViewModelFactory(container) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            val vm: HomeViewModel = viewModel(factory = factory)
            HomeScreen(
                viewModel = vm,
                onOpenNotes = { navController.navigate("notes") },
                onOpenReminders = { navController.navigate("reminders") },
                onOpenCalendar = { navController.navigate("calendar") },
                onOpenSettings = { navController.navigate("settings") },
                onOpenBudget = { navController.navigate("budget") },
                onOpenNote = { noteId -> navController.navigate("note/$noteId") }
            )
        }
        composable("notes") {
            val vm: NotesViewModel = viewModel(factory = factory)
            NotesScreen(
                viewModel = vm,
                onOpenNote = { noteId -> navController.navigate("note/$noteId") },
                onBack = { navController.popBackStack("home", inclusive = false) }
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
        composable("reminders") {
            val vm: RemindersViewModel = viewModel(factory = factory)
            RemindersScreen(
                viewModel = vm,
                onBack = { navController.popBackStack("home", inclusive = false) }
            )
        }
        composable("calendar") {
            val vm: CalendarViewModel = viewModel(factory = factory)
            CalendarScreen(
                viewModel = vm,
                onOpenNote = { noteId -> navController.navigate("note/$noteId") },
                onOpenBudget = { navController.navigate("budget") },
                onBack = { navController.popBackStack("home", inclusive = false) }
            )
        }
            composable("settings") {
                val vm: SettingsViewModel = viewModel(factory = factory)
                SettingsScreen(
                    viewModel = vm,
                    onBack = { navController.popBackStack("home", inclusive = false) }
                )
            }
            composable("budget") {
                val vm: BudgetViewModel = viewModel(factory = factory)
                BudgetScreen(
                    viewModel = vm,
                    onBack = { navController.popBackStack("home", inclusive = false) }
                )
            }
        }
    }
}
