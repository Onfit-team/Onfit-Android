// RetrofitInstance.kt (전체 맥락 중 핵심 추가)
package com.example.onfit.network

import com.example.onfit.KakaoLogin.api.KakaoAuthService
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor   // ← 추가
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {

    private const val BASE_URL = "http://15.164.35.198:3000/"

    // ✅ 네트워크 로그 보기용 Interceptor
    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(logging) // ← 로그 인터셉터 장착
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client) // ← 로깅 클라이언트 적용
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val api: ApiService by lazy { retrofit.create(ApiService::class.java) }
    val kakaoApi: KakaoAuthService by lazy { retrofit.create(KakaoAuthService::class.java) }
}
