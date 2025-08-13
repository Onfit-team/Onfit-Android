package com.example.onfit.Wardrobe.Network

/**
 * 옷장 전체 조회 응답 데이터
 */
data class WardrobeResponseDto(
    val totalCount: Int,
    val categories: List<CategoryDto>,
    val subcategories: List<SubcategoryDto>,
    val items: List<WardrobeItemDto>,
    val appliedFilter: AppliedFilterDto?
)

/**
 * 옷장 카테고리 정보
 */
data class CategoryDto(
    val category: Int,
    val name: String,
    val count: Int
)

/**
 * 하위 카테고리 정보
 */
data class SubcategoryDto(
    val subcategory: Int,
    val name: String
)

/**
 * 적용된 필터 정보
 */
data class AppliedFilterDto(
    val category: Int,
    val categoryName: String,
    val subcategory: Int?,
    val subcategoryName: String?
)

/**
 * 옷장 아이템 정보
 */
data class WardrobeItemDto(
    val id: Int,
    val image: String,
    val brand: String,
    val season: Int,
    val color: Int,
    val category: Int,
    val subcategory: Int
)

/**
 * 아이템 등록 요청 데이터
 */
data class RegisterItemRequestDto(
    val category: Int,
    val subcategory: Int,
    val season: Int,
    val color: Int,
    val brand: String,
    val size: String,
    val purchaseDate: String,
    val image: String,
    val price: Int,
    val purchaseSite: String,
    val tagIds: List<Int>
)


data class ItemRegistrationResult(
    val id: Int,
    val message: String? = null
)

/**
 * 아이템 등록 응답 데이터
 */
data class RegisterItemResponseDto(
    val itemId: Int
)