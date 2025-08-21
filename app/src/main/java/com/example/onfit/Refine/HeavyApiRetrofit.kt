package com.example.onfit.Refine

import com.example.onfit.OutfitRegister.RetrofitClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object HeavyApiRetrofit {
    private const val BASE_URL = "http://3.36.113.173/" // 바꾸려면 여기

    private val client = RetrofitClient.client.newBuilder()
        .readTimeout(90, java.util.concurrent.TimeUnit.SECONDS)   // detect/refine 대비
        .callTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}