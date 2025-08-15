package com.example.onfit.Home.model

data class RecommendItemsResponse(
    val isSuccess: Boolean? = null,
    val code: String? = null,
    val message: String? = null,
    val result: RecommendResult? = null,

    // 실패 응답 대비
    val resultType: String? = null,
    val error: RecommendError? = null,
    val success: Any? = null
)

data class RecommendResult(
    val averageTemp: Double? = null,
    val items: List<RecommendItem>? = emptyList(),
    val diurnalMsg: String? = null
)

data class RecommendItem(
    val id: Int? = null,
    val image: String? = null,
    val category: Int? = null,
    val subcategory: Int? = null
)

data class RecommendError(
    val errorCode: String? = null,
    val reason: String? = null,
    val data: Any? = null
)
