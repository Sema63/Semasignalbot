package com.sema.manipulationscreener.api

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private const val BYBIT_BASE_URL = "https://api.bybit.com/"
    private const val GATEIO_BASE_URL = "https://api.gateio.ws/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    val bybitApi: BybitApi by lazy {
        Retrofit.Builder()
            .baseUrl(BYBIT_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(BybitApi::class.java)
    }

    val gateIoApi: GateIoApi by lazy {
        Retrofit.Builder()
            .baseUrl(GATEIO_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GateIoApi::class.java)
    }
}
