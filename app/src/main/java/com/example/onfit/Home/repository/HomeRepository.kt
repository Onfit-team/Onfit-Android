package com.example.onfit.Home.repository

import com.example.onfit.Home.model.DateResponseWrapper
import com.example.onfit.Home.model.RecommendItemsResponse
import com.example.onfit.Home.model.SimilarWeatherResponse
import com.example.onfit.network.RetrofitInstance
import retrofit2.Response

class HomeRepository {
    suspend fun getDate(): Response<DateResponseWrapper> {
        return RetrofitInstance.api.getCurrentDate()
    }

    suspend fun getRecommendItems(token: String, tempAvg: Double): Response<RecommendItemsResponse> {
        return RetrofitInstance.api.getRecommendItems("Bearer $token", tempAvg)
    }

    // similar-weather
    suspend fun getSimilarWeather(token: String, tempAvg: Double): Response<SimilarWeatherResponse> {
        return RetrofitInstance.api.getSimilarWeather("Bearer $token", tempAvg)
    }
}
