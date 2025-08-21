package com.example.onfit.ItemRegister

import android.util.Log
import com.kakao.sdk.v2.auth.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ItemRegisterRetrofit {
    private const val BASE_URL = "http://3.36.113.173/"

    private val httpLogger = HttpLoggingInterceptor { msg ->
        Log.d("Wardrobe-HTTP", msg)
    }.apply {
        level = if(BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
        else HttpLoggingInterceptor.Level.NONE
        redactHeader("Authorization")
        redactHeader("Cookie")
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(httpLogger)
        .connectTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val api: ItemRegisterApiService by lazy {
        retrofit.create(ItemRegisterApiService::class.java)
    }
}