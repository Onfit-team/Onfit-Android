package com.example.onfit.Wardrobe.viewmodel

import com.example.onfit.Wardrobe.Network.WardrobeItemDto
import com.example.onfit.Wardrobe.Network.CategoryDto
import com.example.onfit.Wardrobe.Network.SubcategoryDto

/**
 * Wardrobe UI 상태 관리
 */
data class WardrobeUiState(
    // 로딩 상태
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,

    // 데이터
    val wardrobeItems: List<WardrobeItemDto> = emptyList(),
    val categories: List<CategoryDto> = emptyList(),
    val subcategories: List<SubcategoryDto> = emptyList(),

    // 선택된 필터
    val selectedCategory: Int? = null,
    val selectedSubcategory: Int? = null,

    // 에러 상태
    val errorMessage: String? = null,

    // 업로드 상태
    val isUploading: Boolean = false,
    val uploadProgress: Float = 0f,

    // 검색 상태
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    val searchResults: List<WardrobeItemDto> = emptyList(),

    // 등록/수정 상태
    val isRegistering: Boolean = false,
    val registrationSuccess: Boolean = false,

    // UI 상태
    val showEmptyState: Boolean = false
) {
    /**
     * 로딩 중인지 확인
     */
    val isAnyLoading: Boolean
        get() = isLoading || isRefreshing || isUploading || isSearching || isRegistering

    /**
     * 데이터가 있는지 확인
     */
    val hasData: Boolean
        get() = wardrobeItems.isNotEmpty()

    /**
     * 에러가 있는지 확인
     */
    val hasError: Boolean
        get() = !errorMessage.isNullOrEmpty()

    /**
     * 검색 중인지 확인
     */
    val isInSearchMode: Boolean
        get() = searchQuery.isNotEmpty()

    /**
     * 필터가 적용되었는지 확인
     */
    val hasActiveFilter: Boolean
        get() = selectedCategory != null || selectedSubcategory != null
}