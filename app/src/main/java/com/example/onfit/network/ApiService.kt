package com.example.onfit.network

import com.example.onfit.Community.model.CommunityOutfitsResponse
import com.example.onfit.Community.model.OutfitDetailResponse
import com.example.onfit.Community.model.PublishTodayOutfitResponse
import com.example.onfit.Community.model.TagListResponse
import com.example.onfit.Community.model.TodayOutfitCheckResponse
import com.example.onfit.Community.model.ToggleLikeResponse
import com.example.onfit.Home.model.BestOutfitResponse
import com.example.onfit.Home.model.DateResponseWrapper
import com.example.onfit.Home.model.LatestStyleResponse
import com.example.onfit.Home.model.RecommendItemsResponse
import com.example.onfit.Home.model.SimilarWeatherResponse
import com.example.onfit.Home.model.WeatherResponse
import com.example.onfit.model.CurrentWeatherResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url

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

    @GET("home/recommend-items")
    suspend fun getRecommendItems(
        @Header("Authorization") token: String,
        @Query("temp_avg") tempAvg: Double
    ): Response<RecommendItemsResponse>

    @GET("home/similar-weather")
    suspend fun getSimilarWeather(
        @Header("Authorization") token: String,
        @Query("temp_avg") tempAvg: Double
    ): Response<SimilarWeatherResponse>

    @GET("community/outfits/today/check")
    suspend fun checkTodayOutfitCanBeShared(
        @Header("Authorization") token: String
    ): Response<TodayOutfitCheckResponse>

    @PATCH("community/publish-today-outfit")
    suspend fun publishTodayOutfit(
        @Header("Authorization") token: String
    ): Response<PublishTodayOutfitResponse>

    @GET("community/outfits")
    suspend fun getCommunityOutfits(
        @Header("Authorization") token: String,
        @Query("order") order: String,
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null,
        @Query("tag_ids") tagIds: String? = null
    ): Response<CommunityOutfitsResponse>

    @POST("community/outfits/{outfit_id}/like")
    suspend fun toggleOutfitLike(
        @Header("Authorization") token: String,
        @Path("outfit_id") outfitId: Int
    ): Response<ToggleLikeResponse>

    @GET("community/outfits/{outfit_id}")
    suspend fun getOutfitDetail(
        @Header("Authorization") token: String?,
        @Path("outfit_id") outfitId: Int
    ): Response<OutfitDetailResponse>

    @DELETE("community/outfits/{outfit_id}")
    suspend fun deleteOutfit(
        @Header("Authorization") token: String,
        @Path("outfit_id") outfitId: Int
    ): Response<ResponseBody>

    @GET("/community/tags")
    suspend fun getTagList(
        @Header("Authorization") token: String
    ): Response<TagListResponse>

}
