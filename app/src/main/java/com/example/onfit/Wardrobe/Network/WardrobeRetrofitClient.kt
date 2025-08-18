package com.example.onfit.Wardrobe.Network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Wardrobe 전용 Retrofit 클라이언트
 * 기존 RetrofitClient와 분리하여 관리
 */
object WardrobeRetrofitClient {

    private const val BASE_URL = "http://3.36.113.173/"

    /**
     * HTTP 로깅 인터셉터
     */
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    /**
     * OkHttp 클라이언트
     */
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * Retrofit 인스턴스
     */
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    /**
     * Wardrobe 서비스 인스턴스
     */
    val wardrobeService: WardrobeService by lazy {
        retrofit.create(WardrobeService::class.java)
    }
}

/**
 * 기존 RetrofitClient도 유지 (하위 호환성)
 */
object RetrofitClient {

    private const val BASE_URL = "http://15.164.35.198:3001/"

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
        )
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val wardrobeService: WardrobeService by lazy {
        retrofit.create(WardrobeService::class.java)
    }
}