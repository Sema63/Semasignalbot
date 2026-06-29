package com.sema.manipulationscreener.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sema.manipulationscreener.api.RetrofitClient
import com.sema.manipulationscreener.model.Candle
import com.sema.manipulationscreener.model.Exchange
import com.sema.manipulationscreener.model.ManipulationCoin
import com.sema.manipulationscreener.model.ScreenerSettings
import com.sema.manipulationscreener.util.ManipulationDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

data class ScreenerUiState(
    val coins: List<ManipulationCoin> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val progress: Float = 0f,
    val scannedCount: Int = 0,
    val totalCount: Int = 0,
    val settings: ScreenerSettings = ScreenerSettings(),
    val sortBy: SortBy = SortBy.PUMP_PERCENT
)

enum class SortBy {
    PUMP_PERCENT,
    DROP_PERCENT,
    TURNOVER
}

data class TickerInfo(
    val symbol: String,
    val lastPrice: Double,
    val markPrice: Double,
    val volume24h: Double,
    val turnover24h: Double
)

class ScreenerViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ScreenerUiState())
    val uiState: StateFlow<ScreenerUiState> = _uiState.asStateFlow()

    private val semaphore = Semaphore(5)

    init {
        scan()
    }

    fun scan() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null,
                progress = 0f,
                scannedCount = 0,
                coins = emptyList()
            )

            try {
                val settings = _uiState.value.settings

                val candidates = when (settings.exchange) {
                    Exchange.BYBIT -> fetchBybitTickers(settings)
                    Exchange.GATEIO -> fetchGateIoTickers(settings)
                }

                val totalCount = candidates.size
                _uiState.value = _uiState.value.copy(totalCount = totalCount)

                if (candidates.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        progress = 1f
                    )
                    return@launch
                }

                val detectedCoins = mutableListOf<ManipulationCoin>()
                var scanned = 0

                val jobs = candidates.map { ticker ->
                    async {
                        semaphore.withPermit {
                            try {
                                val candles = when (settings.exchange) {
                                    Exchange.BYBIT -> fetchBybitKlines(ticker.symbol, settings)
                                    Exchange.GATEIO -> fetchGateIoKlines(ticker.symbol, settings)
                                }

                                ManipulationDetector.detect(
                                    symbol = ticker.symbol,
                                    candles = candles,
                                    currentPrice = ticker.lastPrice,
                                    markPrice = ticker.markPrice,
                                    volume24h = ticker.volume24h,
                                    turnover24h = ticker.turnover24h,
                                    settings = settings
                                )
                            } catch (_: Exception) {
                                null
                            } finally {
                                synchronized(this) {
                                    scanned++
                                    _uiState.value = _uiState.value.copy(
                                        scannedCount = scanned,
                                        progress = scanned.toFloat() / totalCount.toFloat()
                                    )
                                }
                            }
                        }
                    }
                }

                val results = jobs.awaitAll()
                results.filterNotNull().forEach { detectedCoins.add(it) }

                val sorted = sortCoins(detectedCoins, _uiState.value.sortBy)

                _uiState.value = _uiState.value.copy(
                    coins = sorted,
                    isLoading = false,
                    progress = 1f,
                    scannedCount = totalCount
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error: ${e.message}"
                )
            }
        }
    }

    fun updateSettings(settings: ScreenerSettings) {
        _uiState.value = _uiState.value.copy(settings = settings)
    }

    fun updateSortBy(sortBy: SortBy) {
        _uiState.value = _uiState.value.copy(
            sortBy = sortBy,
            coins = sortCoins(_uiState.value.coins, sortBy)
        )
    }

    private fun sortCoins(
        coins: List<ManipulationCoin>,
        sortBy: SortBy
    ): List<ManipulationCoin> {
        return when (sortBy) {
            SortBy.PUMP_PERCENT -> coins.sortedByDescending { it.pumpPercent }
            SortBy.DROP_PERCENT -> coins.sortedByDescending { it.dropFromHighPercent }
            SortBy.TURNOVER -> coins.sortedByDescending { it.turnover24h }
        }
    }

    // --- Bybit ---

    private suspend fun fetchBybitTickers(settings: ScreenerSettings): List<TickerInfo> {
        val response = RetrofitClient.bybitApi.getTickers(category = "linear")
        if (response.retCode != 0) {
            throw RuntimeException("Bybit API: ${response.retMsg}")
        }
        return response.result.list
            .filter { it.symbol.endsWith("USDT") }
            .filter { (it.turnover24h.toDoubleOrNull() ?: 0.0) >= settings.minTurnover24h }
            .map { ticker ->
                TickerInfo(
                    symbol = ticker.symbol,
                    lastPrice = ticker.lastPrice.toDoubleOrNull() ?: 0.0,
                    markPrice = ticker.markPrice.toDoubleOrNull() ?: 0.0,
                    volume24h = ticker.volume24h.toDoubleOrNull() ?: 0.0,
                    turnover24h = ticker.turnover24h.toDoubleOrNull() ?: 0.0
                )
            }
    }

    private suspend fun fetchBybitKlines(symbol: String, settings: ScreenerSettings): List<Candle> {
        val response = RetrofitClient.bybitApi.getKlines(
            symbol = symbol,
            interval = settings.klineInterval,
            limit = 96
        )
        if (response.retCode != 0) return emptyList()
        return response.result.list.mapNotNull { item ->
            if (item.size >= 6) {
                Candle(
                    timestamp = item[0].toLongOrNull() ?: return@mapNotNull null,
                    open = item[1].toDoubleOrNull() ?: return@mapNotNull null,
                    high = item[2].toDoubleOrNull() ?: return@mapNotNull null,
                    low = item[3].toDoubleOrNull() ?: return@mapNotNull null,
                    close = item[4].toDoubleOrNull() ?: return@mapNotNull null,
                    volume = item[5].toDoubleOrNull() ?: return@mapNotNull null
                )
            } else null
        }
    }

    // --- Gate.io ---

    private suspend fun fetchGateIoTickers(settings: ScreenerSettings): List<TickerInfo> {
        val tickers = RetrofitClient.gateIoApi.getTickers()
        return tickers
            .filter { it.contract.endsWith("_USDT") }
            .filter { (it.volume24hSettle.toDoubleOrNull() ?: 0.0) >= settings.minTurnover24h }
            .map { ticker ->
                TickerInfo(
                    symbol = ticker.contract,
                    lastPrice = ticker.last.toDoubleOrNull() ?: 0.0,
                    markPrice = ticker.markPrice.toDoubleOrNull() ?: 0.0,
                    volume24h = ticker.volume24h.toDoubleOrNull() ?: 0.0,
                    turnover24h = ticker.volume24hSettle.toDoubleOrNull() ?: 0.0
                )
            }
    }

    private suspend fun fetchGateIoKlines(symbol: String, settings: ScreenerSettings): List<Candle> {
        val intervalMap = mapOf(
            "5" to "5m", "15" to "15m", "30" to "30m", "60" to "1h", "240" to "4h"
        )
        val interval = intervalMap[settings.klineInterval] ?: "15m"
        val candles = RetrofitClient.gateIoApi.getKlines(
            contract = symbol,
            interval = interval,
            limit = 96
        )
        return candles.map { c ->
            Candle(
                timestamp = c.timestamp * 1000,
                open = c.open.toDoubleOrNull() ?: 0.0,
                high = c.high.toDoubleOrNull() ?: 0.0,
                low = c.low.toDoubleOrNull() ?: 0.0,
                close = c.close.toDoubleOrNull() ?: 0.0,
                volume = c.volume.toDouble()
            )
        }
    }
}
