package com.example.onfit.network

import com.example.onfit.KakaoLogin.api.KakaoAuthService
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {

    private const val BASE_URL = "http://15.164.35.198:3000/"

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val api: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }
    val kakaoApi: KakaoAuthService by lazy {
        retrofit.create(KakaoAuthService::class.java)
    }

    val locationApi: LocationApiService by lazy {
        retrofit.create(LocationApiService::class.java)
    }


}
