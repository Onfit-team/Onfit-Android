package com.example.onfit.calendar.Network

import retrofit2.Response
import retrofit2.http.*

interface CalendarService {

    @GET("calendar/outfit/{outfit_id}/image")
    suspend fun getOutfitImage(
        @Path("outfit_id") outfitId: Int,
        @Header("Authorization") authorization: String
    ): Response<OutfitImageResponse>

    @GET("calendar/outfit/{outfit_id}/text")
    suspend fun getOutfitText(
        @Path("outfit_id") outfitId: Int,
        @Header("Authorization") authorization: String
    ): Response<OutfitTextResponse>

    @GET("calendar/outfit/tag/most")
    suspend fun getMostUsedTag(
        @Header("Authorization") authorization: String
    ): Response<MostUsedTagResponse>

<<<<<<< HEAD
=======
    // 🔥 새로 추가: 현재 날짜 조회
    @GET("common/date")
    suspend fun getCurrentDate(
        @Header("Authorization") authorization: String
    ): Response<CurrentDateResponse>
>>>>>>> 3677f88 (refactor: 코드 리팩토링)
}