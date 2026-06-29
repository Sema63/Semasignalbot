package com.sema.manipulationscreener.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sema.manipulationscreener.model.Exchange
import com.sema.manipulationscreener.model.ScreenerSettings
import com.sema.manipulationscreener.ui.theme.AccentOrange
import com.sema.manipulationscreener.ui.theme.DarkBackground
import com.sema.manipulationscreener.ui.theme.DarkCard
import com.sema.manipulationscreener.ui.theme.DarkSurface
import com.sema.manipulationscreener.ui.theme.TextSecondary
import com.sema.manipulationscreener.viewmodel.ScreenerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: ScreenerViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentSettings = uiState.settings

    var minPumpPercent by remember { mutableDoubleStateOf(currentSettings.minPumpPercent) }
    var minDropPercent by remember { mutableDoubleStateOf(currentSettings.minDropFromHighPercent) }
    var selectedInterval by remember { mutableStateOf(currentSettings.klineInterval) }
    var minTurnover by remember { mutableDoubleStateOf(currentSettings.minTurnover24h) }
    var selectedExchange by remember { mutableStateOf(currentSettings.exchange) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurface)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = "НАСТРОЙКИ СКРИНЕРА",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Exchange selection
            SettingSection(title = "Биржа") {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Exchange.entries.forEach { exchange ->
                        FilterChip(
                            selected = selectedExchange == exchange,
                            onClick = { selectedExchange = exchange },
                            label = { Text(exchange.displayName) },
                            modifier = Modifier.padding(end = 8.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AccentOrange.copy(alpha = 0.3f),
                                selectedLabelColor = AccentOrange
                            )
                        )
                    }
                }
                Text(
                    text = "Bybit - основная, Gate.io - альтернатива",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Min pump percent
            SettingSection(title = "Минимальный памп (%)") {
                Text(
                    text = "${minPumpPercent.toInt()}%",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentOrange
                )
                Slider(
                    value = minPumpPercent.toFloat(),
                    onValueChange = { minPumpPercent = it.toDouble() },
                    valueRange = 10f..500f,
                    steps = 48,
                    colors = SliderDefaults.colors(
                        thumbColor = AccentOrange,
                        activeTrackColor = AccentOrange
                    )
                )
                Text(
                    text = "Монеты с пампом менее ${minPumpPercent.toInt()}% пропущены",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Min drop from high
            SettingSection(title = "Минимальный откат от хая (%)") {
                Text(
                    text = "${minDropPercent.toInt()}%",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentOrange
                )
                Slider(
                    value = minDropPercent.toFloat(),
                    onValueChange = { minDropPercent = it.toDouble() },
                    valueRange = 5f..80f,
                    steps = 14,
                    colors = SliderDefaults.colors(
                        thumbColor = AccentOrange,
                        activeTrackColor = AccentOrange
                    )
                )
                Text(
                    text = "Откат от хая минимум ${minDropPercent.toInt()}%",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Kline interval
            SettingSection(title = "Таймфрейм свечей") {
                Row(modifier = Modifier.fillMaxWidth()) {
                    listOf("5" to "5м", "15" to "15м", "30" to "30м", "60" to "1ч", "240" to "4ч").forEach { (value, label) ->
                        FilterChip(
                            selected = selectedInterval == value,
                            onClick = { selectedInterval = value },
                            label = { Text(label) },
                            modifier = Modifier.padding(end = 8.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AccentOrange.copy(alpha = 0.3f),
                                selectedLabelColor = AccentOrange
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Min turnover
            SettingSection(title = "Мин. оборот 24ч (USDT)") {
                Text(
                    text = formatTurnover(minTurnover),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentOrange
                )
                Slider(
                    value = minTurnover.toFloat(),
                    onValueChange = { minTurnover = it.toDouble() },
                    valueRange = 100_000f..10_000_000f,
                    colors = SliderDefaults.colors(
                        thumbColor = AccentOrange,
                        activeTrackColor = AccentOrange
                    )
                )
                Text(
                    text = "Фильтр по ликвидности",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Apply button
            Button(
                onClick = {
                    viewModel.updateSettings(
                        ScreenerSettings(
                            minPumpPercent = minPumpPercent,
                            minDropFromHighPercent = minDropPercent,
                            klineInterval = selectedInterval,
                            minTurnover24h = minTurnover,
                            exchange = selectedExchange
                        )
                    )
                    viewModel.scan()
                    onBack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentOrange)
            ) {
                Text(
                    text = "ПРИМЕНИТЬ И СКАНИРОВАТЬ",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun SettingSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkCard, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        content()
    }
}

private fun formatTurnover(value: Double): String {
    return when {
        value >= 1_000_000 -> String.format("%.1fM", value / 1_000_000)
        value >= 1_000 -> String.format("%.0fK", value / 1_000)
        else -> String.format("%.0f", value)
    }
}
