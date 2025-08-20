package com.example.onfit.Refine

import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST

interface RefineService {
    @POST("/items/refine") // 실제 경로로 교체 (예: "ai/refine")
    @Headers("Content-Type: application/json")
    suspend fun refine(
        @Header("Authorization") authorization: String,
        @Body body: RefineRequest
    ): retrofit2.Response<RefineResponse>
}

data class RefineRequest(val cropId: String)

data class RefineResponse(
    val isSuccess: Boolean,
    val code: String?,
    val message: String?,
    val result: RefineResult?
)

data class RefineResult(
    @SerializedName("refined_id") val refinedId: String,
    @SerializedName("preview_url") val previewUrl: String
)