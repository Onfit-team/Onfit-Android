package com.example.onfit.network

import com.example.onfit.Home.model.DateResponseWrapper
import com.example.onfit.Home.model.WeatherResponse
import com.example.onfit.model.CurrentWeatherResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header

interface ApiService {
    @GET("/common/date")
    suspend fun getCurrentDate(): Response<DateResponseWrapper>

    @GET("weather/current")
    suspend fun getCurrentWeather(
        @Header("Authorization") token: String
    ): Response<CurrentWeatherResponse>

    @GET("weather/tomorrow")
    suspend fun getTomorrowWeather(
        @Header("Authorization") token: String
    ): Response<WeatherResponse>


}
