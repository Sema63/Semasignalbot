package com.sema.manipulationscreener.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sema.manipulationscreener.ui.components.CoinCard
import com.sema.manipulationscreener.ui.theme.AccentOrange
import com.sema.manipulationscreener.ui.theme.AccentRed
import com.sema.manipulationscreener.ui.theme.DarkBackground
import com.sema.manipulationscreener.ui.theme.DarkSurface
import com.sema.manipulationscreener.ui.theme.TextSecondary
import com.sema.manipulationscreener.viewmodel.ScreenerViewModel
import com.sema.manipulationscreener.viewmodel.SortBy

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: ScreenerViewModel,
    onOpenSettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showSortMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "MANIPULATION SCREENER",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${uiState.settings.exchange.displayName} | Поиск монет для SHORT",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                },
                actions = {
                    // Sort button
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(
                                Icons.Default.Sort,
                                contentDescription = "Сортировка"
                            )
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("По % пампа") },
                                onClick = {
                                    viewModel.updateSortBy(SortBy.PUMP_PERCENT)
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("По % отката") },
                                onClick = {
                                    viewModel.updateSortBy(SortBy.DROP_PERCENT)
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("По обороту") },
                                onClick = {
                                    viewModel.updateSortBy(SortBy.TURNOVER)
                                    showSortMenu = false
                                }
                            )
                        }
                    }

                    // Settings button
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Настройки"
                        )
                    }

                    // Refresh button
                    IconButton(
                        onClick = { viewModel.scan() },
                        enabled = !uiState.isLoading
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Обновить"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSurface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
                .padding(paddingValues)
        ) {
            // Progress bar during scan
            AnimatedVisibility(visible = uiState.isLoading) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LinearProgressIndicator(
                        progress = uiState.progress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = AccentOrange,
                        trackColor = DarkSurface,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Сканирование: ${uiState.scannedCount} / ${uiState.totalCount}",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }

            // Error message
            uiState.error?.let { error ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(AccentRed.copy(alpha = 0.2f))
                        .padding(12.dp)
                ) {
                    Text(
                        text = error,
                        color = AccentRed,
                        fontSize = 13.sp
                    )
                }
            }

            // Results header
            if (!uiState.isLoading && uiState.coins.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Найдено: ${uiState.coins.size} монет",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Мин. памп: ${uiState.settings.minPumpPercent.toInt()}%",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }

            // Coin list
            if (uiState.coins.isEmpty() && !uiState.isLoading && uiState.error == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Манипуляций не обнаружено",
                            fontSize = 16.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Попробуйте снизить порог пампа\nв настройках",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            LazyColumn {
                items(uiState.coins) { coin ->
                    CoinCard(coin = coin)
                }
            }
        }
    }
}
