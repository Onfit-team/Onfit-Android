package com.example.onfit.OutfitRegister

import com.google.gson.annotations.SerializedName

data class ImageUploadResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: UploadData?
)

data class UploadData(
    // 서버가 반환하는 이미지
    @SerializedName("image_url") val imageUrl: String?
)