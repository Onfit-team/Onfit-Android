package com.example.onfit.HomeRegister.model

import com.example.onfit.HomeRegister.service.AiCropService
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val BASE_URL = "http://15.164.35.198:3000/"

    // ✅ 요청/응답 헤더만 로그로 출력 (이미지 본문은 출력 안 함)
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.HEADERS
    }

    // ✅ OkHttpClient 생성
    private val client = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor) // 로그 인터셉터 추가
        .build()

    // ✅ Retrofit 인스턴스
    val instance: AiCropService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client) // OkHttpClient 적용
            .build()
            .create(AiCropService::class.java)
    }
}