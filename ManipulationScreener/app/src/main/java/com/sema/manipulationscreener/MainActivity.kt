package com.sema.manipulationscreener

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sema.manipulationscreener.ui.screens.ListScreen
import com.sema.manipulationscreener.ui.screens.MarketMapScreen
import com.sema.manipulationscreener.ui.screens.SettingsScreen
import com.sema.manipulationscreener.ui.theme.AccentOrange
import com.sema.manipulationscreener.ui.theme.DarkSurface
import com.sema.manipulationscreener.ui.theme.ManipulationScreenerTheme
import com.sema.manipulationscreener.ui.theme.TextSecondary
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

data class BottomNavItem(
    val label: String,
    val icon: ImageVector
)

@Composable
fun ManipulationScreenerApp() {
    val viewModel: ScreenerViewModel = viewModel()
    var selectedTab by remember { mutableIntStateOf(0) }

    val navItems = listOf(
        BottomNavItem("Карта", Icons.Default.GridView),
        BottomNavItem("Список", Icons.Default.ViewList),
        BottomNavItem("Настройки", Icons.Default.Settings)
    )

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = DarkSurface,
                contentColor = Color.White
            ) {
                navItems.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = {
                            Icon(
                                item.icon,
                                contentDescription = item.label
                            )
                        },
                        label = { Text(item.label, fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AccentOrange,
                            selectedTextColor = AccentOrange,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary,
                            indicatorColor = AccentOrange.copy(alpha = 0.15f)
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        when (selectedTab) {
            0 -> MarketMapScreen(
                viewModel = viewModel,
                modifier = Modifier.padding(paddingValues)
            )
            1 -> ListScreen(
                viewModel = viewModel,
                modifier = Modifier.padding(paddingValues)
            )
            2 -> SettingsScreen(
                viewModel = viewModel,
                onBack = { selectedTab = 0 },
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
}
