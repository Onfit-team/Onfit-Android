package com.example.onfit.OutfitRegister

import com.google.gson.annotations.SerializedName

data class ImageUploadResponse(
    @SerializedName("isSuccess") val isSuccess: Boolean? = null,
    @SerializedName("success")   val successAlt: Boolean? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("data") val data: UploadData? = null,
    @SerializedName("result")    val result: UploadData? = null
) {
    val ok: Boolean get() = (isSuccess == true) || (successAlt == true)
    val payload: UploadData? get() = data ?: result
}

data class UploadData(
    @SerializedName("image_url") val underscore: String? = null,
    @SerializedName("imageUrl")  val camel: String? = null
) {
    val imageUrl: String? get() = underscore ?: camel
}