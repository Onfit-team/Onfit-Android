package com.example.onfit.calendar.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.onfit.calendar.repository.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class CalendarViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = CalendarRepository(application.applicationContext)

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    // 캐시: 날짜별 outfit_id 매핑
    private val dateToOutfitIdCache = mutableMapOf<String, Int>()

    // 캐시: 이미 확인한 outfit_id들 (성능 최적화)
    private val checkedOutfitIds = mutableSetOf<Int>()

    /**
     * 캘린더에서 날짜 선택 시 호출
     */
    fun onDateSelected(selectedDate: String) {
        clearPreviousData()

        // 캐시에서 먼저 확인
        val cachedOutfitId = dateToOutfitIdCache[selectedDate]
        if (cachedOutfitId != null) {
            loadOutfitData(cachedOutfitId)
            return
        }

        // 캐시에 없으면 날짜로 outfit_id 찾기
        findOutfitIdByDate(selectedDate)
    }

    /**
     * 날짜로 outfit_id 찾기
     */
    private fun findOutfitIdByDate(targetDate: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            // 1부터 순차적으로 확인 (또는 더 효율적인 범위 설정)
            for (outfitId in 1..1000) {
                // 이미 확인한 ID는 스킵
                if (checkedOutfitIds.contains(outfitId)) continue

                when (val result = repository.getOutfitImage(outfitId)) {
                    is CalendarResult.Success -> {
                        val outfitDate = result.data.date

                        // 캐시에 저장
                        dateToOutfitIdCache[outfitDate] = outfitId
                        checkedOutfitIds.add(outfitId)

                        // 찾는 날짜와 일치하면 데이터 로드
                        if (outfitDate == targetDate) {
                            loadOutfitData(outfitId)
                            return@launch
                        }
                    }
                    is CalendarResult.Failure -> {
                        // NOT_EXISTS 에러 시 해당 ID는 체크 완료로 표시
                        checkedOutfitIds.add(outfitId)

                        // 에러 코드가 NOT_EXISTS인 경우 계속 진행
                        if (result.message.contains("NOT_EXISTS")) {
                            continue
                        } else {
                            // 다른 에러는 중단
                            handleError(result.message)
                            return@launch
                        }
                    }
                    is CalendarResult.Error -> {
                        handleError("네트워크 오류가 발생했습니다")
                        return@launch
                    }
                }
            }

            // 해당 날짜에 코디가 없는 경우
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                hasOutfitData = false,
                errorMessage = "해당 날짜에 등록된 코디가 없습니다"
            )
        }
    }

    /**
     * 특정 월의 모든 코디 데이터 미리 로드 (캘린더 표시용)
     */
    fun preloadMonthlyData(year: Int, month: Int) {
        viewModelScope.launch {
            val targetMonth = String.format("%04d-%02d", year, month)

            for (outfitId in 1..1000) {
                if (checkedOutfitIds.contains(outfitId)) continue

                when (val result = repository.getOutfitImage(outfitId)) {
                    is CalendarResult.Success -> {
                        val outfitDate = result.data.date
                        dateToOutfitIdCache[outfitDate] = outfitId
                        checkedOutfitIds.add(outfitId)

                        // UI 업데이트: 해당 월의 데이터면 캘린더에 표시
                        if (outfitDate.startsWith(targetMonth)) {
                            updateCalendarWithOutfit(outfitDate)
                        }
                    }
                    is CalendarResult.Failure -> {
                        checkedOutfitIds.add(outfitId)
                        if (!result.message.contains("NOT_EXISTS")) break
                    }
                    is CalendarResult.Error -> break
                }
            }
        }
    }

    /**
     * 캘린더에 코디 존재 표시 업데이트
     */
    private fun updateCalendarWithOutfit(date: String) {
        val currentDatesWithOutfits = _uiState.value.datesWithOutfits.toMutableSet()
        currentDatesWithOutfits.add(date)

        _uiState.value = _uiState.value.copy(
            datesWithOutfits = currentDatesWithOutfits
        )
    }

    /**
     * 특정 날짜의 코디 데이터 전체 조회
     */
    private fun loadOutfitData(outfitId: Int) {
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
     * 이전 데이터 초기화
     */
    private fun clearPreviousData() {
        _uiState.value = _uiState.value.copy(
            outfitImage = null,
            outfitText = null,
            hasOutfitData = false,
            errorMessage = null
        )
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