package com.sema.manipulationscreener.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sema.manipulationscreener.model.ManipulationCoin
import com.sema.manipulationscreener.model.ManipulationType
import com.sema.manipulationscreener.ui.theme.AccentGreen
import com.sema.manipulationscreener.ui.theme.AccentOrange
import com.sema.manipulationscreener.ui.theme.AccentRed
import com.sema.manipulationscreener.ui.theme.AccentYellow
import com.sema.manipulationscreener.ui.theme.DarkCard
import com.sema.manipulationscreener.ui.theme.TextSecondary
import java.text.DecimalFormat

@Composable
fun CoinCard(
    coin: ManipulationCoin,
    modifier: Modifier = Modifier
) {
    val df = DecimalFormat("#,##0.00")
    val dfPrice = DecimalFormat("#,##0.#####")

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header: Symbol + Type badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = coin.symbol.removeSuffix("USDT").removeSuffix("_USDT").trimEnd('_'),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "/USDT",
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                }

                ManipulationBadge(coin.manipulationType)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Price and pump info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Цена",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = dfPrice.format(coin.currentPrice),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Памп",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = "+${df.format(coin.pumpPercent)}%",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentGreen
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Откат от хая",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = "-${df.format(coin.dropFromHighPercent)}%",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentRed
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Chart
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.background)
            ) {
                CandlestickChart(
                    candles = coin.candles,
                    pumpHighPrice = coin.pumpHighPrice,
                    prePumpPrice = coin.prePumpPrice
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Price range info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                InfoChip(label = "Диап 5м", value = "${df.format(coin.priceRange5m)}%")
                InfoChip(label = "Диап 15м", value = "${df.format(coin.priceRange15m)}%")
                InfoChip(label = "Изм 5м", value = "${if (coin.priceChange5m >= 0) "+" else ""}${df.format(coin.priceChange5m)}%")
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Bottom info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                InfoChip(label = "Хай", value = dfPrice.format(coin.pumpHighPrice))
                InfoChip(label = "База", value = dfPrice.format(coin.prePumpPrice))
                InfoChip(label = "Оборот 24ч", value = formatVolume(coin.turnover24h))
            }
        }
    }
}

@Composable
private fun ManipulationBadge(type: ManipulationType) {
    val (text, color) = when (type) {
        ManipulationType.VERTICAL_PUMP -> "ВЕРТИКАЛЬ" to AccentRed
        ManipulationType.SHARP_PUMP_AND_DUMP -> "ПАМП & ДАМП" to AccentOrange
        ManipulationType.PUMP_REVERSAL -> "РАЗВОРОТ" to AccentYellow
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.2f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun InfoChip(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            fontSize = 10.sp,
            color = TextSecondary
        )
        Text(
            text = value,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun formatVolume(volume: Double): String {
    return when {
        volume >= 1_000_000_000 -> String.format("%.1fB", volume / 1_000_000_000)
        volume >= 1_000_000 -> String.format("%.1fM", volume / 1_000_000)
        volume >= 1_000 -> String.format("%.1fK", volume / 1_000)
        else -> String.format("%.0f", volume)
    }
}
