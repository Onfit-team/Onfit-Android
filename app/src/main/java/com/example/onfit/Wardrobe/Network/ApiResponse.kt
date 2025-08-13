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

data class ItemOutfitHistoryResponse(
    val isSuccess: Boolean,
    val code: String,
    val message: String,
    val result: List<OutfitRecord>
)

data class OutfitRecord(
    val id: Int,
    val userId: Int,
    val locationId: Int,
    val date: String,
    val weatherTempAvg: Double,
    val feelsLikeTemp: Double,
    val mainImage: String?,
    val memo: String?,
    val isPublished: Boolean,
    val outfitItems: List<OutfitItemRecord>,
    val outfitTags: List<OutfitTagRecord>,
    val outfitLikes: List<OutfitLikeRecord>,
    val user: OutfitUserRecord
)

data class OutfitItemRecord(
    val id: Int,
    val outfitId: Int,
    val itemId: Int,
    val item: OutfitItemDetail
)

data class OutfitItemDetail(
    val id: Int,
    val category: Int,
    val subcategory: Int,
    val brand: String,
    val color: Int,
    val size: String,
    val season: Int,
    val image: String?
)

data class OutfitTagRecord(
    val id: Int,
    val outfitId: Int,
    val tagId: Int,
    val tag: OutfitTag
)

data class OutfitTag(
    val id: Int,
    val name: String,
    val type: String
)

data class OutfitLikeRecord(
    val id: Int,
    val outfitId: Int,
    val userId: Int
)

data class OutfitUserRecord(
    val id: Int,
    val nickname: String,
    val profileImage: String?
)

// 아이템 추천 Response 데이터 클래스들
data class RecommendationResponse(
    val isSuccess: Boolean,
    val code: String,
    val message: String,
    val result: List<RecommendedItemDto>
)

data class RecommendedItemDto(
    val id: Int,
    val category: Int,
    val subcategory: Int,
    val brand: String,
    val color: Int,
    val size: String,
    val season: Int,
    val image: String?,
    val tags: List<ItemTagDto>,
    val matchingScore: Int,
    val scoreBreakdown: ScoreBreakdownDto
)

data class ItemTagDto(
    val id: Int,
    val name: String,
    val type: String
)

data class ScoreBreakdownDto(
    val season: Int? = null,
    val color: Int? = null,
    val tag: Int? = null
)

// 업로드 API (/items/upload) 응답 구조
data class ImageUploadResponse(
    val success: Boolean,
    val message: String,
    val data: ImageUploadData?
)

data class ImageUploadData(
    val image_url: String
)

// AI 리파인 응답 데이터 클래스
data class ImageRefineResponse(
    val isSuccess: Boolean?,
    val resultType: String?,
    val code: String?,
    val message: String?,
    val result: ImageRefineResult?,
    val error: ErrorData?
)

data class ImageRefineResult(
    val refined_id: String?,
    val refined_url: String?
)

data class ErrorData(
    val errorCode: String,
    val reason: String,
    val data: Any?
)

data class ImageSaveResponse(
    val isSuccess: Boolean,
    val code: String,
    val message: String,
    val result: ImageSaveResult?
)

data class ImageSaveResult(
    val id: Int,
    val image_url: String
)