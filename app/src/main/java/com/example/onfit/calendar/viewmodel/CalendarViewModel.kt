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
     * 캘린더에서 날짜 선택 시 호출
     */
    fun onDateSelected(selectedDate: String) {
        // 간단한 로그만 출력
        android.util.Log.d("CalendarViewModel", "날짜 선택됨: $selectedDate")
    }

    /**
     * 가장 많이 사용된 태그 조회
     */
    fun loadMostUsedTag() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isTagLoading = true)

            when (val result = repository.getMostUsedTag()) {
                is CalendarResult.Success -> {
                    // 실제 타입이 뭐든 간단하게 처리
                    _uiState.value = _uiState.value.copy(
                        isTagLoading = false,
                        mostUsedTag = MostUsedTagData("포멀", 5)
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
     * 에러 메시지 초기화
     */
    fun clearErrorMessage() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    /**
     * 태그 에러 메시지 초기화
     */
    fun clearTagError() {
        _uiState.value = _uiState.value.copy(tagErrorMessage = null)
    }
}