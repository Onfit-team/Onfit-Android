package com.example.onfit.HomeRegister.model

import com.example.onfit.HomeRegister.service.AiCropService
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val BASE_URL = "http://15.164.35.198:3000/"

    val instance: AiCropService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AiCropService::class.java)
    }

}