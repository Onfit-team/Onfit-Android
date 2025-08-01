package com.example.onfit.OutfitRegister

import com.google.gson.annotations.SerializedName

data class ImageUploadResponse(
    @SerializedName("isSuccess") val isSuccess: Boolean,
    val code: String,
    val message: String,
    val result: ImageResult?
)

data class ImageResult(
    @SerializedName("image_url") val imageUrl: String
)
