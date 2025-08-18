package com.example.onfit.Wardrobe.Network

import com.google.gson.annotations.SerializedName

/**
 * ===== 기본 API 응답 구조 =====
 */
data class ApiResponse<T>(
    val isSuccess: Boolean,
    val code: String,
    val message: String,
    val result: T?
)

/**
 * ===== 옷장 관련 응답 구조들 =====
 */

/**
 * 옷장 전체 조회 응답
 */
data class WardrobeResponse(
    val isSuccess: Boolean,
    val code: String,
    val message: String,
    val result: WardrobeResult?
)

data class WardrobeResult(
    val totalCount: Int,
    val categories: List<CategoryDto>,
    val subcategories: List<SubcategoryDto>? = null,
    val items: List<WardrobeItemDto>,
    val appliedFilter: AppliedFilterDto? = null
)

/**
 * 아이템 등록 응답
 */
data class RegisterItemResponse(
    val isSuccess: Boolean,
    val code: String,
    val message: String,
    val result: RegisterItemResult?
)

data class RegisterItemResult(
    val itemId: Int,
    val message: String? = null
)

/**
 * 아이템 상세 조회 응답
 */
data class WardrobeItemDetailResponse(
    val isSuccess: Boolean,
    val code: String,
    val message: String,
    val result: WardrobeItemDetail?
)

/**
 * 이미지 업로드 응답
 */
data class ImageUploadResponse(
    val isSuccess: Boolean,
    val code: String,
    val message: String,
    val result: ImageUploadResult
)

data class ImageUploadResult(
    val id: String? = null,
    @SerializedName("image_url") val imageUrl: String
)

/**
 * 브랜드 목록 응답
 */
data class BrandsResponse(
    val isSuccess: Boolean,
    val code: String,
    val message: String,
    val result: List<String>
)

/**
 * 에러 응답
 */
data class ErrorResponse(
    val resultType: String,
    val error: ErrorData?,
    val success: Any?
)

data class ErrorData(
    val errorCode: String,
    val reason: String,
    val data: Any?
)