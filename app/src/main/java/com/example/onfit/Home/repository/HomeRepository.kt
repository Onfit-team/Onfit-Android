package com.example.onfit.Home.repository

import com.example.onfit.Home.model.DateResponseWrapper
import com.example.onfit.Home.model.WeatherResponseWrapper
import com.example.onfit.network.RetrofitInstance
import retrofit2.Response

class HomeRepository {
    suspend fun getDate(): Response<DateResponseWrapper> {
        return RetrofitInstance.api.getCurrentDate()
    }

    suspend fun getCurrentWeather(token: String): Response<WeatherResponseWrapper> {
        return RetrofitInstance.api.getCurrentWeather("Bearer $token")
    }

    suspend fun getTomorrowWeather(token: String): Response<WeatherResponseWrapper> {
        return RetrofitInstance.api.getTomorrowWeather("Bearer $token")
    }
}
