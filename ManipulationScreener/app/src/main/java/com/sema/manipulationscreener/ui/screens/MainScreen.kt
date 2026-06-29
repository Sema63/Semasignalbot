package com.sema.manipulationscreener.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sema.manipulationscreener.ui.components.CoinCard
import com.sema.manipulationscreener.ui.theme.AccentGreen
import com.sema.manipulationscreener.ui.theme.AccentOrange
import com.sema.manipulationscreener.ui.theme.AccentRed
import com.sema.manipulationscreener.ui.theme.DarkBackground
import com.sema.manipulationscreener.ui.theme.DarkSurface
import com.sema.manipulationscreener.ui.theme.TextSecondary
import com.sema.manipulationscreener.viewmodel.ScreenerViewModel
import com.sema.manipulationscreener.viewmodel.SortBy

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListScreen(
    viewModel: ScreenerViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var showSortMenu by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurface)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "MANIPULATION SCREENER",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "${uiState.settings.exchange.displayName} | ${uiState.sortBy.displayName}",
                    fontSize = 10.sp,
                    color = TextSecondary
                )
            }

            // Sort
            Box {
                IconButton(onClick = { showSortMenu = true }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Sort, contentDescription = "Sort", tint = Color.White, modifier = Modifier.size(20.dp))
                }
                DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                    SortBy.entries.forEach { sort ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    sort.displayName,
                                    fontWeight = if (uiState.sortBy == sort) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            onClick = {
                                viewModel.updateSortBy(sort)
                                showSortMenu = false
                            }
                        )
                    }
                }
            }

            // Auto-refresh
            IconButton(
                onClick = { viewModel.toggleAutoRefresh() },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    if (uiState.autoRefreshEnabled) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Auto-refresh",
                    tint = if (uiState.autoRefreshEnabled) AccentGreen else TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Refresh
            IconButton(
                onClick = { viewModel.scan() },
                enabled = !uiState.isLoading,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }

        // Interval selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurface.copy(alpha = 0.7f))
                .padding(horizontal = 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf("5" to "5м", "15" to "15м", "30" to "30м", "60" to "1ч", "240" to "4ч").forEach { (value, label) ->
                FilterChip(
                    selected = uiState.settings.klineInterval == value,
                    onClick = {
                        viewModel.updateSettings(uiState.settings.copy(klineInterval = value))
                        viewModel.scan()
                    },
                    label = { Text(label, fontSize = 10.sp) },
                    modifier = Modifier
                        .height(28.dp)
                        .padding(end = 4.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AccentOrange.copy(alpha = 0.3f),
                        selectedLabelColor = AccentOrange
                    )
                )
            }
        }

        // Progress
        if (uiState.isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LinearProgressIndicator(
                    progress = uiState.progress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = AccentOrange,
                    trackColor = DarkSurface,
                )
                Text(
                    text = "Сканирование: ${uiState.scannedCount} / ${uiState.totalCount}",
                    fontSize = 10.sp,
                    color = TextSecondary
                )
            }
        }

        // Error
        uiState.error?.let { error ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(AccentRed.copy(alpha = 0.2f))
                    .padding(8.dp)
            ) {
                Text(text = error, color = AccentRed, fontSize = 11.sp)
            }
        }

        // Results header
        if (!uiState.isLoading && uiState.coins.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Найдено: ${uiState.coins.size}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Мин. памп: ${uiState.settings.minPumpPercent.toInt()}%",
                    fontSize = 10.sp,
                    color = TextSecondary
                )
            }
        }

        // Empty state
        if (uiState.coins.isEmpty() && !uiState.isLoading && uiState.error == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Манипуляций не обнаружено",
                        fontSize = 14.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Снизьте порог пампа в настройках",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Coin list
        LazyColumn {
            items(uiState.coins) { coin ->
                CoinCard(coin = coin)
            }
        }
    }
}
