package com.example.onfit.ItemRegister

import com.google.gson.annotations.SerializedName

data class ItemRegisterRequest(
    val category: Int,
    val subcategory: Int,
    val season: Int,
    val color: Int,
    val brand: String,
    val size: String,
    val purchaseDate: String, // "YYYY-MM-DD"
    val image: String,        // 절대 URL (예: https://...)
    val price: Int,
    val purchaseSite: String,
    val tagIds: List<Int>     // 최대 3개 (아래에서 제한)
)

// 공용 응답 랩퍼 (result/data, isSuccess/success 양쪽 호환)
data class ApiEnvelope<T>(
    @SerializedName("isSuccess") val isSuccess: Boolean? = null,
    @SerializedName("success")   val successAlt: Boolean? = null,
    @SerializedName("code")      val code: String? = null,
    @SerializedName("message")   val message: String? = null,
    @SerializedName("result")    val result: T? = null,
    @SerializedName("data")      val data: T? = null,
    @SerializedName("payload")   val payloadField: T? = null // ← 실제 JSON 'payload' 받는 용도
) {
    val ok: Boolean get() = (isSuccess == true) || (successAlt == true)
    val payload: T? get() = result ?: data
}

data class ItemCreateResult(
    @SerializedName("itemId") val itemId: Long
)