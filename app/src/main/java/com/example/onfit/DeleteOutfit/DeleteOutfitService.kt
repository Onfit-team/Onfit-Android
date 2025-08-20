package com.example.onfit.DeleteOutfit

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.HTTP
import retrofit2.http.Header
import retrofit2.http.Path

interface DeleteOutfitService {
    @HTTP(method = "DELETE", path = "calendar/outfit/{outfit_id}", hasBody = true)
    suspend fun deleteOutfit(
        @Path("outfit_id") outfitId: Int,
        @Header("Authorization") authorization: String,
        @Body body: MutableMap<String, Any> = mutableMapOf() // ← 여기!
    ): Response<DeleteOutfitResponse>
}