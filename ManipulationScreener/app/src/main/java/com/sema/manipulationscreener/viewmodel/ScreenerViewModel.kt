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
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

enum class ViewMode {
    MARKET_MAP,
    LIST
}

data class ScreenerUiState(
    val coins: List<ManipulationCoin> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val progress: Float = 0f,
    val scannedCount: Int = 0,
    val totalCount: Int = 0,
    val settings: ScreenerSettings = ScreenerSettings(),
    val sortBy: SortBy = SortBy.PRICE_RANGE_5M,
    val viewMode: ViewMode = ViewMode.MARKET_MAP,
    val gridLimit: Int = 9,
    val autoRefreshEnabled: Boolean = true,
    val lastUpdateTime: Long = 0L
)

enum class SortBy(val displayName: String) {
    PRICE_RANGE_5M("Диапазон 5м"),
    PRICE_RANGE_15M("Диапазон 15м"),
    PUMP_PERCENT("% пампа"),
    DROP_PERCENT("% отката"),
    TURNOVER("Оборот"),
    VOLUME("Объём")
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
    private var autoRefreshJob: Job? = null

    init {
        scan()
        startAutoRefresh()
    }

    private fun startAutoRefresh() {
        autoRefreshJob?.cancel()
        autoRefreshJob = viewModelScope.launch {
            while (true) {
                delay(30_000L)
                if (_uiState.value.autoRefreshEnabled && !_uiState.value.isLoading) {
                    scan()
                }
            }
        }
    }

    fun toggleAutoRefresh() {
        _uiState.value = _uiState.value.copy(
            autoRefreshEnabled = !_uiState.value.autoRefreshEnabled
        )
    }

    fun setViewMode(mode: ViewMode) {
        _uiState.value = _uiState.value.copy(viewMode = mode)
    }

    fun setGridLimit(limit: Int) {
        _uiState.value = _uiState.value.copy(gridLimit = limit)
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

                                val coin = ManipulationDetector.detect(
                                    symbol = ticker.symbol,
                                    candles = candles,
                                    currentPrice = ticker.lastPrice,
                                    markPrice = ticker.markPrice,
                                    volume24h = ticker.volume24h,
                                    turnover24h = ticker.turnover24h,
                                    settings = settings
                                )

                                coin?.let {
                                    val enriched = enrichWithPriceRanges(it, candles)
                                    enriched
                                }
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
                    scannedCount = totalCount,
                    lastUpdateTime = System.currentTimeMillis()
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error: ${e.message}"
                )
            }
        }
    }

    private fun enrichWithPriceRanges(
        coin: ManipulationCoin,
        candles: List<Candle>
    ): ManipulationCoin {
        val sorted = candles.sortedBy { it.timestamp }
        if (sorted.isEmpty()) return coin

        val intervalMinutes = when (_uiState.value.settings.klineInterval) {
            "5" -> 5
            "15" -> 15
            "30" -> 30
            "60" -> 60
            "240" -> 240
            else -> 15
        }

        val candles5m = (5.0 / intervalMinutes).toInt().coerceAtLeast(1)
        val candles15m = (15.0 / intervalMinutes).toInt().coerceAtLeast(1)

        val recent5m = sorted.takeLast(candles5m)
        val recent15m = sorted.takeLast(candles15m)

        val priceChange5m = if (recent5m.isNotEmpty()) {
            val first = recent5m.first().open
            val last = recent5m.last().close
            if (first > 0) ((last - first) / first) * 100.0 else 0.0
        } else 0.0

        val priceChange15m = if (recent15m.isNotEmpty()) {
            val first = recent15m.first().open
            val last = recent15m.last().close
            if (first > 0) ((last - first) / first) * 100.0 else 0.0
        } else 0.0

        val priceRange5m = if (recent5m.isNotEmpty()) {
            val high = recent5m.maxOf { it.high }
            val low = recent5m.minOf { it.low }
            if (low > 0) ((high - low) / low) * 100.0 else 0.0
        } else 0.0

        val priceRange15m = if (recent15m.isNotEmpty()) {
            val high = recent15m.maxOf { it.high }
            val low = recent15m.minOf { it.low }
            if (low > 0) ((high - low) / low) * 100.0 else 0.0
        } else 0.0

        return coin.copy(
            priceChange5m = priceChange5m,
            priceChange15m = priceChange15m,
            priceRange5m = priceRange5m,
            priceRange15m = priceRange15m
        )
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
            SortBy.PRICE_RANGE_5M -> coins.sortedByDescending { it.priceRange5m }
            SortBy.PRICE_RANGE_15M -> coins.sortedByDescending { it.priceRange15m }
            SortBy.PUMP_PERCENT -> coins.sortedByDescending { it.pumpPercent }
            SortBy.DROP_PERCENT -> coins.sortedByDescending { it.dropFromHighPercent }
            SortBy.TURNOVER -> coins.sortedByDescending { it.turnover24h }
            SortBy.VOLUME -> coins.sortedByDescending { it.volume24h }
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

    override fun onCleared() {
        super.onCleared()
        autoRefreshJob?.cancel()
    }
}
