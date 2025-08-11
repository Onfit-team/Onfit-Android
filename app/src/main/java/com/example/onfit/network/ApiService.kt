package com.example.onfit.network

import com.example.onfit.Home.model.BestOutfitResponse
import com.example.onfit.Home.model.DateResponseWrapper
import com.example.onfit.Home.model.LatestStyleResponse
import com.example.onfit.Home.model.RecommendItemsResponse
import com.example.onfit.Home.model.SimilarWeatherResponse
import com.example.onfit.Home.model.WeatherResponse
import com.example.onfit.model.CurrentWeatherResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

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

    @GET("outfits/recent")
    suspend fun getRecentOutfits(
        @Header("Authorization") token: String
    ): Response<LatestStyleResponse>

    @GET("community/outfits/top3")
    suspend fun getTop3BestOutfits(
        @Header("Authorization") token: String
    ): Response<BestOutfitResponse>

    // 온도 구간별 추천
    @GET("home/recommend-items")
    suspend fun getRecommendItems(
        @Header("Authorization") token: String,
        @Query("temp_avg") tempAvg: Double
    ): Response<RecommendItemsResponse>

    // 비슷한 온도의 날 사용자 outfit
    @GET("home/similar-weather")
    suspend fun getSimilarWeather(
        @Header("Authorization") token: String,
        @Query("temp_avg") tempAvg: Double
    ): Response<SimilarWeatherResponse>
}