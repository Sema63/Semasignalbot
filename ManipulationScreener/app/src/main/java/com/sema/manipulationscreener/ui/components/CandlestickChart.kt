package com.sema.manipulationscreener.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.sema.manipulationscreener.model.Candle
import com.sema.manipulationscreener.ui.theme.AccentGreen
import com.sema.manipulationscreener.ui.theme.AccentRed

@Composable
fun CandlestickChart(
    candles: List<Candle>,
    modifier: Modifier = Modifier,
    pumpHighPrice: Double = 0.0,
    prePumpPrice: Double = 0.0
) {
    if (candles.isEmpty()) return

    val sorted = candles.sortedBy { it.timestamp }
    val allHighs = sorted.map { it.high }
    val allLows = sorted.map { it.low }
    val maxPrice = allHighs.max()
    val minPrice = allLows.min()
    val priceRange = maxPrice - minPrice

    Canvas(modifier = modifier.fillMaxSize()) {
        if (priceRange <= 0) return@Canvas

        val candleCount = sorted.size
        val candleWidth = size.width / candleCount * 0.6f
        val candleSpacing = size.width / candleCount

        // Draw pump high line (dashed)
        if (pumpHighPrice > 0) {
            val highY = priceToY(pumpHighPrice, minPrice, priceRange, size.height)
            drawLine(
                color = AccentRed.copy(alpha = 0.5f),
                start = Offset(0f, highY),
                end = Offset(size.width, highY),
                strokeWidth = 1f
            )
        }

        // Draw pre-pump price line
        if (prePumpPrice > 0) {
            val baseY = priceToY(prePumpPrice, minPrice, priceRange, size.height)
            drawLine(
                color = AccentGreen.copy(alpha = 0.5f),
                start = Offset(0f, baseY),
                end = Offset(size.width, baseY),
                strokeWidth = 1f
            )
        }

        // Draw candles
        sorted.forEachIndexed { index, candle ->
            val centerX = index * candleSpacing + candleSpacing / 2

            val isGreen = candle.close >= candle.open
            val color = if (isGreen) AccentGreen else AccentRed

            val highY = priceToY(candle.high, minPrice, priceRange, size.height)
            val lowY = priceToY(candle.low, minPrice, priceRange, size.height)
            val openY = priceToY(candle.open, minPrice, priceRange, size.height)
            val closeY = priceToY(candle.close, minPrice, priceRange, size.height)

            // Wick
            drawLine(
                color = color,
                start = Offset(centerX, highY),
                end = Offset(centerX, lowY),
                strokeWidth = 1f
            )

            // Body
            val bodyTop = minOf(openY, closeY)
            val bodyHeight = maxOf(kotlin.math.abs(openY - closeY), 1f)
            drawRect(
                color = color,
                topLeft = Offset(centerX - candleWidth / 2, bodyTop),
                size = Size(candleWidth, bodyHeight)
            )
        }
    }
}

private fun priceToY(
    price: Double,
    minPrice: Double,
    priceRange: Double,
    canvasHeight: Float
): Float {
    val padding = canvasHeight * 0.05f
    val drawHeight = canvasHeight - 2 * padding
    return padding + ((1.0 - (price - minPrice) / priceRange) * drawHeight).toFloat()
}
