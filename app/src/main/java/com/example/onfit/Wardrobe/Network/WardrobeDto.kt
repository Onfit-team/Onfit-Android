package com.example.onfit.Wardrobe.Network

/**
 * ===== 옷장 데이터 모델들 =====
 */

data class WardrobeResponseDto(
    val totalCount: Int,
    val categories: List<CategoryDto>,
    val subcategories: List<SubcategoryDto>,
    val items: List<WardrobeItemDto>,
    val appliedFilter: AppliedFilterDto?


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
 * 옷장 아이템 상세 정보
 */
data class WardrobeItemDetail(
    val id: Int,
    val category: Int,
    val subcategory: Int,
    val brand: String,
    val color: Int,
    val size: String,
    val season: Int,
    val purchaseDate: String?,
    val image: String,
    val price: Int?,
    val purchaseSite: String?,
    val tags: WardrobeItemTags
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
    val name: String,
    val count: Int? = null
)

/**
 * 적용된 필터 정보
 */
data class AppliedFilterDto(
    val category: Int? = null,
    val categoryName: String? = null,
    val subcategory: Int? = null,
    val subcategoryName: String? = null
)

/**
 * 아이템 태그 정보
 */
data class WardrobeItemTags(
    val moodTags: List<WardrobeTag>?,
    val purposeTags: List<WardrobeTag>?
)

data class WardrobeTag(
    val id: Int,
    val name: String?,
    val type: String? // 만약 type이 서버에서 안 내려오면 ?로
)

    val category: Int? = null,
    val categoryName: String? = null,
    val subcategory: Int? = null,
    val subcategoryName: String? = null
)

/**
 * 아이템 태그 정보
 */
data class WardrobeItemTags(
    val moodTags: List<WardrobeTag>?,
    val purposeTags: List<WardrobeTag>?
)

data class WardrobeTag(
    val id: Int,
    val name: String?,
    val type: String? // 만약 type이 서버에서 안 내려오면 ?로
)

/**
 * ===== 요청 데이터 모델들 =====
 */

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
    val image: String, // <-- 반드시 non-nullable String
    val price: Int,
    val purchaseSite: String,
    val tagIds: List<Int>
)

/**
 * 아이템 수정 요청 데이터
 */
data class UpdateItemRequestDto(
    val category: Int? = null,
    val subcategory: Int? = null,
    val season: Int? = null,
    val color: Int? = null,
    val brand: String? = null,
    val size: String? = null,
    val purchaseDate: String? = null,
    val image: String? = null,
    val price: Int? = null,
    val purchaseSite: String? = null,
    val tagIds: List<Int>? = null

data class ItemRegistrationResult(
    val id: Int,
    val message: String? = null
)

/**
 * 검색 필터 요청 데이터
 */

/**
 * 아이템 수정 요청 데이터
 */
data class UpdateItemRequestDto(
    val category: Int? = null,
    val subcategory: Int? = null,
    val season: Int? = null,
    val color: Int? = null,
    val brand: String? = null,
    val size: String? = null,
    val purchaseDate: String? = null,
    val image: String? = null,
    val price: Int? = null,
    val purchaseSite: String? = null,
    val tagIds: List<Int>? = null
)

/**
 * 검색 필터 요청 데이터
 */
data class SearchFilterRequest(
    val category: Int? = null,
    val subcategory: Int? = null,
    val season: Int? = null,
    val color: Int? = null,
    val brand: String? = null,
    val tagIds: List<Int>? = null,
    val priceMin: Int? = null,
    val priceMax: Int? = null
)