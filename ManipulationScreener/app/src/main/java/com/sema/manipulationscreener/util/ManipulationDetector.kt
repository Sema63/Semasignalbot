package com.sema.manipulationscreener.util

import com.sema.manipulationscreener.model.Candle
import com.sema.manipulationscreener.model.ManipulationCoin
import com.sema.manipulationscreener.model.ManipulationType
import com.sema.manipulationscreener.model.ScreenerSettings

object ManipulationDetector {

    /**
     * Detects manipulation pattern in kline data.
     *
     * Pattern: sharp vertical pump followed by significant drop.
     * Like on the screenshots: coin pumps 30-300%+ in short time,
     * then starts dumping — ideal for short entry.
     *
     * Algorithm:
     * 1. Find the highest price in recent candles
     * 2. Find the lowest price before the pump (pre-pump base)
     * 3. Calculate pump percentage = (high - base) / base * 100
     * 4. Calculate drop from high = (high - current) / high * 100
     * 5. Check if pump is sharp (happened over few candles)
     * 6. If pump% > threshold AND drop% > threshold => manipulation detected
     */
    fun detect(
        symbol: String,
        candles: List<Candle>,
        currentPrice: Double,
        markPrice: Double,
        volume24h: Double,
        turnover24h: Double,
        settings: ScreenerSettings
    ): ManipulationCoin? {
        if (candles.size < 10) return null

        // Sort candles by timestamp ascending
        val sorted = candles.sortedBy { it.timestamp }

        // Find the highest high in the dataset
        val maxCandle = sorted.maxByOrNull { it.high } ?: return null
        val maxHigh = maxCandle.high
        val maxIndex = sorted.indexOf(maxCandle)

        // Need at least some candles before the peak to find the base
        if (maxIndex < 3) return null

        // Find pre-pump base: look at candles before the peak,
        // find the minimum low in the range before the pump started
        val prePumpCandles = sorted.subList(0, maxIndex)
        val baseCandle = prePumpCandles.minByOrNull { it.low } ?: return null
        val baseLow = baseCandle.low
        val baseIndex = sorted.indexOf(baseCandle)

        if (baseLow <= 0) return null

        // Calculate pump percentage
        val pumpPercent = ((maxHigh - baseLow) / baseLow) * 100.0

        // Calculate how sharp the pump was (number of candles from base to peak)
        val pumpDurationCandles = maxIndex - baseIndex

        // Calculate drop from high
        val dropFromHighPercent = ((maxHigh - currentPrice) / maxHigh) * 100.0

        // Check if meets criteria
        if (pumpPercent < settings.minPumpPercent) return null
        if (dropFromHighPercent < settings.minDropFromHighPercent) return null

        // Determine manipulation type based on characteristics
        val manipulationType = when {
            pumpDurationCandles <= 3 && pumpPercent > 50 -> ManipulationType.VERTICAL_PUMP
            dropFromHighPercent > pumpPercent * 0.5 -> ManipulationType.SHARP_PUMP_AND_DUMP
            else -> ManipulationType.PUMP_REVERSAL
        }

        return ManipulationCoin(
            symbol = symbol,
            currentPrice = currentPrice,
            pumpPercent = pumpPercent,
            pumpHighPrice = maxHigh,
            prePumpPrice = baseLow,
            dropFromHighPercent = dropFromHighPercent,
            markPrice = markPrice,
            volume24h = volume24h,
            turnover24h = turnover24h,
            candles = sorted,
            manipulationType = manipulationType,
            timeframeMinutes = settings.scanTimeframeMinutes
        )
    }
}
