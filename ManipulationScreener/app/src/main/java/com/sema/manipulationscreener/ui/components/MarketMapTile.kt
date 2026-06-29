package com.sema.manipulationscreener.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sema.manipulationscreener.model.ManipulationCoin
import com.sema.manipulationscreener.ui.theme.AccentGreen
import com.sema.manipulationscreener.ui.theme.AccentRed
import com.sema.manipulationscreener.ui.theme.DarkCard
import java.text.DecimalFormat

@Composable
fun MarketMapTile(
    coin: ManipulationCoin,
    modifier: Modifier = Modifier
) {
    val df = DecimalFormat("#,##0.0")
    val priceChange = coin.priceChange5m
    val changeColor = if (priceChange >= 0) AccentGreen else AccentRed
    val bgColor = if (priceChange >= 0) {
        AccentGreen.copy(alpha = (0.05f + (priceChange.toFloat().coerceIn(0f, 20f) / 20f * 0.15f)))
    } else {
        AccentRed.copy(alpha = (0.05f + ((-priceChange).toFloat().coerceIn(0f, 20f) / 20f * 0.15f)))
    }

    val borderColor = changeColor.copy(alpha = 0.3f)

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(6.dp))
            .background(DarkCard)
            .border(1.dp, borderColor, RoundedCornerShape(6.dp))
    ) {
        // Background tint
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgColor)
        )

        // Chart fills the tile
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 20.dp, bottom = 16.dp, start = 2.dp, end = 2.dp)
        ) {
            CandlestickChart(
                candles = coin.candles,
                pumpHighPrice = coin.pumpHighPrice,
                prePumpPrice = coin.prePumpPrice
            )
        }

        // Top overlay: symbol name
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = coin.symbol
                    .removeSuffix("USDT")
                    .removeSuffix("_USDT")
                    .trimEnd('_'),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${if (priceChange >= 0) "+" else ""}${df.format(priceChange)}%",
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = changeColor
            )
        }

        // Bottom overlay: price range and volume
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 4.dp, vertical = 1.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "R:${df.format(coin.priceRange5m)}%",
                fontSize = 7.sp,
                color = Color.White.copy(alpha = 0.7f),
                maxLines = 1
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = formatVol(coin.turnover24h),
                fontSize = 7.sp,
                color = Color.White.copy(alpha = 0.7f),
                maxLines = 1
            )
        }
    }
}

private fun formatVol(volume: Double): String {
    return when {
        volume >= 1_000_000_000 -> String.format("%.0fB", volume / 1_000_000_000)
        volume >= 1_000_000 -> String.format("%.0fM", volume / 1_000_000)
        volume >= 1_000 -> String.format("%.0fK", volume / 1_000)
        else -> String.format("%.0f", volume)
    }
}
