package com.sema.manipulationscreener

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sema.manipulationscreener.ui.screens.MainScreen
import com.sema.manipulationscreener.ui.screens.SettingsScreen
import com.sema.manipulationscreener.ui.theme.ManipulationScreenerTheme
import com.sema.manipulationscreener.viewmodel.ScreenerViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ManipulationScreenerTheme {
                ManipulationScreenerApp()
            }
        }
    }
}

@Composable
fun ManipulationScreenerApp() {
    val viewModel: ScreenerViewModel = viewModel()
    var showSettings by remember { mutableStateOf(false) }

    if (showSettings) {
        SettingsScreen(
            viewModel = viewModel,
            onBack = { showSettings = false }
        )
    } else {
        MainScreen(
            viewModel = viewModel,
            onOpenSettings = { showSettings = true }
        )
    }
}
