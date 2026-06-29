package com.sema.manipulationscreener.api

import com.sema.manipulationscreener.model.BybitKlineResponse
import com.sema.manipulationscreener.model.BybitTickerResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface BybitApi {

    @GET("v5/market/tickers")
    suspend fun getTickers(
        @Query("category") category: String = "linear"
    ): BybitTickerResponse

    @GET("v5/market/kline")
    suspend fun getKlines(
        @Query("category") category: String = "linear",
        @Query("symbol") symbol: String,
        @Query("interval") interval: String,
        @Query("limit") limit: Int = 96
    ): BybitKlineResponse
}
