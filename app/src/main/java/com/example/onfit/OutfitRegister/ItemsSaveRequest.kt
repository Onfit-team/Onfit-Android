package com.example.onfit.OutfitRegister

import com.google.gson.annotations.SerializedName

data class ItemsSaveRequest(
    val items: List<ImageOnly>,
    val outfitId: Int
)

data class ImageOnly(
    @SerializedName("image_url") val imageUrl: String
)

data class ItemsSaveResponse(
    val isSuccess: Boolean,
    val code: String?,
    val message: String?,
    val result: ItemsSaveResult?
)

data class ItemsSaveResult(
    val savedCount: Int,
    val items: List<SavedItem>,
    val outfitId: Int
)

data class SavedItem(
    val id: Int,
    @SerializedName("image_url") val imageUrl: String
)