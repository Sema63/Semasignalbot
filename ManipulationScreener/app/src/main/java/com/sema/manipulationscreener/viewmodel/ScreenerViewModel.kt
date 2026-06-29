package com.sema.manipulationscreener.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sema.manipulationscreener.api.RetrofitClient
import com.sema.manipulationscreener.model.Candle
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

class ScreenerViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ScreenerUiState())
    val uiState: StateFlow<ScreenerUiState> = _uiState.asStateFlow()

    private val api = RetrofitClient.api
    private val semaphore = Semaphore(5) // limit concurrent API calls

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
                // Step 1: Get all USDT perpetual tickers
                val tickerResponse = api.getTickers(category = "linear")
                if (tickerResponse.retCode != 0) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "API error: ${tickerResponse.retMsg}"
                    )
                    return@launch
                }

                val settings = _uiState.value.settings

                // Filter for USDT pairs with sufficient volume
                val candidates = tickerResponse.result.list.filter { ticker ->
                    ticker.symbol.endsWith("USDT") &&
                        (ticker.turnover24h.toDoubleOrNull() ?: 0.0) >= settings.minTurnover24h
                }

                val totalCount = candidates.size
                _uiState.value = _uiState.value.copy(totalCount = totalCount)

                // Step 2: Scan each candidate for manipulation pattern
                val detectedCoins = mutableListOf<ManipulationCoin>()
                var scanned = 0

                val jobs = candidates.map { ticker ->
                    async {
                        semaphore.withPermit {
                            try {
                                val klineResponse = api.getKlines(
                                    symbol = ticker.symbol,
                                    interval = settings.klineInterval,
                                    limit = 96
                                )

                                if (klineResponse.retCode == 0) {
                                    val candles = parseCandles(klineResponse.result.list)
                                    val currentPrice = ticker.lastPrice.toDoubleOrNull() ?: 0.0
                                    val markPriceVal = ticker.markPrice.toDoubleOrNull() ?: currentPrice
                                    val vol24h = ticker.volume24h.toDoubleOrNull() ?: 0.0
                                    val turnover24h = ticker.turnover24h.toDoubleOrNull() ?: 0.0

                                    val detected = ManipulationDetector.detect(
                                        symbol = ticker.symbol,
                                        candles = candles,
                                        currentPrice = currentPrice,
                                        markPrice = markPriceVal,
                                        volume24h = vol24h,
                                        turnover24h = turnover24h,
                                        settings = settings
                                    )

                                    detected
                                } else {
                                    null
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

    private fun parseCandles(raw: List<List<String>>): List<Candle> {
        return raw.mapNotNull { item ->
            if (item.size >= 6) {
                Candle(
                    timestamp = item[0].toLongOrNull() ?: return@mapNotNull null,
                    open = item[1].toDoubleOrNull() ?: return@mapNotNull null,
                    high = item[2].toDoubleOrNull() ?: return@mapNotNull null,
                    low = item[3].toDoubleOrNull() ?: return@mapNotNull null,
                    close = item[4].toDoubleOrNull() ?: return@mapNotNull null,
                    volume = item[5].toDoubleOrNull() ?: return@mapNotNull null
                )
            } else {
                null
            }
        }
    }
}
