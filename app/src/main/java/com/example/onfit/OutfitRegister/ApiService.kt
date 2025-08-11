package com.example.onfit.OutfitRegister

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ApiService {
    // 이미지 업로드 API
    @Multipart
    @POST("/items/upload")
    suspend fun uploadImage(
        @Header("Authorization") token: String,
        @Part image: MultipartBody.Part
    ): Response<ImageUploadResponse>

    // Outfit 등록 API
    @POST("/outfits")
    suspend fun registerOutfit(
        @Header("Authorization") token: String,
        @Body body: RequestBody
    ): Response<OutfitResponse>
}