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

    @DELETE("calendar/outfit/{outfit_id}")
    suspend fun deleteOutfit(
        @Header("Authorization") token: String,
        @Path("outfit_id") outfitId: Int
    ): Response<CalendarResponse.DeleteOutfitResponse>

}