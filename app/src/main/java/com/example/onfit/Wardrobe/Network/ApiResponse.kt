package com.example.onfit.Wardrobe.Network

data class ApiResponse<T>(
    val isSuccess: Boolean,
    val code: String,
    val message: String,
    val result: T?
)

data class RecommendedCategoriesResult(
    val category: Int,
    val subcategory: Int,
    val season: Int,
    val color: Int
)

data class BrandsResponse(
    val isSuccess: Boolean,
    val code: String,
    val message: String,
    val result: List<String>
)

data class FilterSearchResponse(
    val resultType: String,
    val items: List<FilterSearchItem>
)

data class FilterSearchItem(
    val id: Int,
    val image: String,
    val mainCategory: String,
    val subCategory: String,
    val color: String,
    val season: String,
    val styleTags: List<String>
)

data class ImageUploadResponse(
    val success: Boolean,
    val message: String,
    val data: ImageUploadData?
)

data class ImageUploadData(
    val image_url: String
)

data class ItemOutfitHistoryResponse(
    val success: Boolean,
    val message: String,
    val data: List<OutfitHistory>?
)

data class OutfitHistory(
    val outfitId: Int,
    val date: String,
    val imageUrl: String,
    val description: String?
)

data class RecommendationResponse(
    val isSuccess: Boolean,
    val code: String,
    val message: String,
    val result: List<RecommendedItem>
)

data class RecommendedItem(
    val id: Int,
    val category: Int,
    val subcategory: Int,
    val brand: String,
    val color: Int,
    val size: String,
    val season: Int,
    val image: String,
    val tags: List<RecommendationTag>,
    val matchingScore: Int,
    val scoreBreakdown: ScoreBreakdown
)

data class RecommendationTag(
    val id: Int,
    val name: String,
    val type: String
)

data class ScoreBreakdown(
    val season: Int,
    val color: Int,
    val tag: Int
)

data class RecommendationErrorResponse(
    val resultType: String,
    val error: RecommendationError?,
    val success: Boolean? // 또는 null
)

data class RecommendationError(
    val errorCode: String,
    val reason: String,
    val data: Any?
)

data class SimpleErrorResponse(
    val message: String
)