package com.sema.manipulationscreener.model

import com.google.gson.annotations.SerializedName

// Bybit V5 API response for tickers
data class BybitTickerResponse(
    @SerializedName("retCode") val retCode: Int,
    @SerializedName("retMsg") val retMsg: String,
    @SerializedName("result") val result: BybitTickerResult
)

data class BybitTickerResult(
    @SerializedName("category") val category: String,
    @SerializedName("list") val list: List<BybitTicker>
)

data class BybitTicker(
    @SerializedName("symbol") val symbol: String,
    @SerializedName("lastPrice") val lastPrice: String,
    @SerializedName("highPrice24h") val highPrice24h: String,
    @SerializedName("lowPrice24h") val lowPrice24h: String,
    @SerializedName("prevPrice24h") val prevPrice24h: String,
    @SerializedName("volume24h") val volume24h: String,
    @SerializedName("turnover24h") val turnover24h: String,
    @SerializedName("price24hPcnt") val price24hPcnt: String,
    @SerializedName("markPrice") val markPrice: String
)

// Bybit V5 API response for klines (candlesticks)
data class BybitKlineResponse(
    @SerializedName("retCode") val retCode: Int,
    @SerializedName("retMsg") val retMsg: String,
    @SerializedName("result") val result: BybitKlineResult
)

data class BybitKlineResult(
    @SerializedName("symbol") val symbol: String,
    @SerializedName("category") val category: String,
    @SerializedName("list") val list: List<List<String>>
)

// Parsed candle data
data class Candle(
    val timestamp: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double
)

// Detected manipulation coin
data class ManipulationCoin(
    val symbol: String,
    val currentPrice: Double,
    val pumpPercent: Double,
    val pumpHighPrice: Double,
    val prePumpPrice: Double,
    val dropFromHighPercent: Double,
    val markPrice: Double,
    val volume24h: Double,
    val turnover24h: Double,
    val candles: List<Candle>,
    val manipulationType: ManipulationType,
    val timeframeMinutes: Int,
    val priceChange5m: Double = 0.0,
    val priceChange15m: Double = 0.0,
    val priceRange5m: Double = 0.0,
    val priceRange15m: Double = 0.0
)

enum class ManipulationType {
    SHARP_PUMP_AND_DUMP,
    PUMP_REVERSAL,
    VERTICAL_PUMP
}

enum class Exchange(val displayName: String) {
    BYBIT("Bybit"),
    GATEIO("Gate.io")
}

// Settings for detection
data class ScreenerSettings(
    val minPumpPercent: Double = 30.0,
    val minDropFromHighPercent: Double = 10.0,
    val scanTimeframeMinutes: Int = 240,
    val klineInterval: String = "15",
    val minTurnover24h: Double = 500_000.0,
    val exchange: Exchange = Exchange.BYBIT
)
