package com.example.onfit.calendar.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.onfit.calendar.repository.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CalendarViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = CalendarRepository(application.applicationContext)

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    /**
     * 특정 날짜의 코디 데이터 전체 조회
     */
    fun loadOutfitData(outfitId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            // 이미지와 텍스트를 병렬로 로드
            launch { loadOutfitImage(outfitId) }
            launch { loadOutfitText(outfitId) }
        }
    }

    /**
     * 코디 이미지 조회
     */
    private suspend fun loadOutfitImage(outfitId: Int) {
        when (val result = repository.getOutfitImage(outfitId)) {
            is CalendarResult.Success -> {
                _uiState.value = _uiState.value.copy(
                    outfitImage = result.data,
                    hasOutfitData = true,
                    isLoading = false
                )
            }
            is CalendarResult.Failure -> {
                handleError(result.message)
            }
            is CalendarResult.Error -> {
                handleError("네트워크 오류가 발생했습니다")
            }
        }
    }

    /**
     * 코디 텍스트 조회
     */
    private suspend fun loadOutfitText(outfitId: Int) {
        when (val result = repository.getOutfitText(outfitId)) {
            is CalendarResult.Success -> {
                _uiState.value = _uiState.value.copy(
                    outfitText = result.data,
                    hasOutfitData = true,
                    isLoading = false
                )
            }
            is CalendarResult.Failure -> {
                handleError(result.message)
            }
            is CalendarResult.Error -> {
                handleError("네트워크 오류가 발생했습니다")
            }
        }
    }

    /**
     * 캘린더에서 날짜 선택 시 호출
     */
    fun onDateSelected(outfitId: Int) {
        clearPreviousData()
        loadOutfitData(outfitId)
    }

    /**
     * 이전 데이터 초기화
     */
    private fun clearPreviousData() {
        _uiState.value = CalendarUiState()
    }

    /**
     * 에러 처리
     */
    private fun handleError(message: String) {
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            hasOutfitData = false,
            errorMessage = message
        )
    }

    /**
     * 에러 메시지 초기화
     */
    fun clearErrorMessage() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    /**
     * 가장 많이 사용된 태그 조회
     */
    fun loadMostUsedTag() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isTagLoading = true)

            when (val result = repository.getMostUsedTag()) {
                is CalendarResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isTagLoading = false,
                        mostUsedTag = result.data
                    )
                }
                is CalendarResult.Failure -> {
                    _uiState.value = _uiState.value.copy(
                        isTagLoading = false,
                        tagErrorMessage = result.message
                    )
                }
                is CalendarResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isTagLoading = false,
                        tagErrorMessage = "네트워크 오류가 발생했습니다"
                    )
                }
            }
        }
    }

    /**
     * 태그 에러 메시지 초기화
     */
    fun clearTagError() {
        _uiState.value = _uiState.value.copy(tagErrorMessage = null)
    }
}