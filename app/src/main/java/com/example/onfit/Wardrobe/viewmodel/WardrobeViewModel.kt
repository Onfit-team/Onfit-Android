package com.example.onfit.Wardrobe.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.onfit.Wardrobe.repository.WardrobeRepository
import com.example.onfit.Wardrobe.Network.RegisterItemRequestDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WardrobeViewModel(context: Context) : ViewModel() {

    private val repository = WardrobeRepository(context)

    private val _uiState = MutableStateFlow(WardrobeUiState())
    val uiState: StateFlow<WardrobeUiState> = _uiState.asStateFlow()

    /**
     * 전체 옷장 아이템 로드
     */
    fun loadAllWardrobeItems() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null
            )

            repository.getAllWardrobeItems()
                .onSuccess { result ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        wardrobeItems = result.items,
                        categories = result.categories,
                        subcategories = result.subcategories ?: emptyList(),
                        selectedCategory = null,
                        selectedSubcategory = null,
                        showEmptyState = result.items.isEmpty()
                    )
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = exception.message,
                        showEmptyState = false
                    )
                }
        }
    }

    /**
     * 카테고리별 옷장 아이템 로드
     */
    fun loadWardrobeItemsByCategory(category: Int? = null, subcategory: Int? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null
            )

            repository.getWardrobeItemsByCategory(category, subcategory)
                .onSuccess { result ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        wardrobeItems = result.items,
                        categories = result.categories,
                        subcategories = result.subcategories ?: emptyList(),
                        selectedCategory = category,
                        selectedSubcategory = subcategory,
                        showEmptyState = result.items.isEmpty()
                    )
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = exception.message,
                        showEmptyState = false
                    )
                }
        }
    }

    /**
     * 새로고침
     */
    fun refreshWardrobeItems() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true)

            val currentState = _uiState.value
            if (currentState.hasActiveFilter) {
                loadWardrobeItemsByCategory(
                    currentState.selectedCategory,
                    currentState.selectedSubcategory
                )
            } else {
                loadAllWardrobeItems()
            }

            _uiState.value = _uiState.value.copy(isRefreshing = false)
        }
    }

    /**
     * 아이템 등록
     */
    fun registerItem(imageUri: Uri, request: RegisterItemRequestDto) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isRegistering = true,
                errorMessage = null
            )

            // 1. 이미지 업로드
            repository.uploadImage(imageUri)
                .onSuccess { imageUrl ->
                    // 2. 아이템 등록
                    val finalRequest = request.copy(image = imageUrl)
                    repository.registerItem(finalRequest)
                        .onSuccess {
                            _uiState.value = _uiState.value.copy(
                                isRegistering = false,
                                registrationSuccess = true
                            )
                            // 등록 후 데이터 새로고침
                            refreshWardrobeItems()
                        }
                        .onFailure { exception ->
                            _uiState.value = _uiState.value.copy(
                                isRegistering = false,
                                errorMessage = "아이템 등록 실패: ${exception.message}"
                            )
                        }
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isRegistering = false,
                        errorMessage = "이미지 업로드 실패: ${exception.message}"
                    )
                }
        }
    }

    /**
     * 아이템 수정
     */
    fun updateItem(itemId: Int, imageUri: Uri?, request: RegisterItemRequestDto) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isRegistering = true,
                errorMessage = null
            )

            if (imageUri != null) {
                // 새 이미지가 있는 경우
                repository.uploadImage(imageUri)
                    .onSuccess { imageUrl ->
                        val finalRequest = request.copy(image = imageUrl)
                        updateItemWithRequest(itemId, finalRequest)
                    }
                    .onFailure { exception ->
                        _uiState.value = _uiState.value.copy(
                            isRegistering = false,
                            errorMessage = "이미지 업로드 실패: ${exception.message}"
                        )
                    }
            } else {
                // 기존 이미지 사용
                updateItemWithRequest(itemId, request)
            }
        }
    }

    private suspend fun updateItemWithRequest(itemId: Int, request: RegisterItemRequestDto) {
        repository.updateWardrobeItem(itemId, request)
            .onSuccess {
                _uiState.value = _uiState.value.copy(
                    isRegistering = false,
                    registrationSuccess = true
                )
                refreshWardrobeItems()
            }
            .onFailure { exception ->
                _uiState.value = _uiState.value.copy(
                    isRegistering = false,
                    errorMessage = "아이템 수정 실패: ${exception.message}"
                )
            }
    }

    /**
     * 아이템 삭제
     */
    fun deleteItem(itemId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null
            )

            repository.deleteWardrobeItem(itemId)
                .onSuccess {
                    refreshWardrobeItems()
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "아이템 삭제 실패: ${exception.message}"
                    )
                }
        }
    }

    /**
     * 검색 실행
     */
    fun searchItems(query: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                searchQuery = query,
                isSearching = true
            )

            if (query.isEmpty()) {
                // 빈 검색어인 경우 전체 목록으로 복원
                _uiState.value = _uiState.value.copy(
                    isSearching = false,
                    searchResults = emptyList()
                )
                return@launch
            }

            // 로컬에서 검색 (서버 검색 API가 있다면 repository에 추가)
            val filteredItems = _uiState.value.wardrobeItems.filter { item ->
                item.brand.contains(query, ignoreCase = true) ||
                        item.id.toString().contains(query)
            }

            _uiState.value = _uiState.value.copy(
                isSearching = false,
                searchResults = filteredItems
            )
        }
    }

    /**
     * 검색 초기화
     */
    fun clearSearch() {
        _uiState.value = _uiState.value.copy(
            searchQuery = "",
            searchResults = emptyList()
        )
    }

    /**
     * 에러 메시지 초기화
     */
    fun clearErrorMessage() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    /**
     * 등록 성공 상태 초기화
     */
    fun clearRegistrationSuccess() {
        _uiState.value = _uiState.value.copy(registrationSuccess = false)
    }

    /**
     * 필터 초기화
     */
    fun clearFilters() {
        loadAllWardrobeItems()
    }
}