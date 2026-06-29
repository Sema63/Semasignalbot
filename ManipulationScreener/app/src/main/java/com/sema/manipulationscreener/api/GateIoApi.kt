package com.sema.manipulationscreener.api

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query

data class GateIoTicker(
    @SerializedName("contract") val contract: String,
    @SerializedName("last") val last: String,
    @SerializedName("high_24h") val high24h: String,
    @SerializedName("low_24h") val low24h: String,
    @SerializedName("volume_24h") val volume24h: String,
    @SerializedName("volume_24h_quote") val volume24hQuote: String,
    @SerializedName("volume_24h_settle") val volume24hSettle: String,
    @SerializedName("change_percentage") val changePercentage: String,
    @SerializedName("mark_price") val markPrice: String
)

data class GateIoCandle(
    @SerializedName("t") val timestamp: Long,
    @SerializedName("o") val open: String,
    @SerializedName("h") val high: String,
    @SerializedName("l") val low: String,
    @SerializedName("c") val close: String,
    @SerializedName("v") val volume: Long,
    @SerializedName("sum") val turnover: String
)

interface GateIoApi {

    @GET("api/v4/futures/usdt/tickers")
    suspend fun getTickers(): List<GateIoTicker>

    @GET("api/v4/futures/usdt/candlesticks")
    suspend fun getKlines(
        @Query("contract") contract: String,
        @Query("interval") interval: String,
        @Query("limit") limit: Int = 96
    ): List<GateIoCandle>
}
