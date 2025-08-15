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

    // 날짜별 outfitId 캐시 (상세 조회 시 사용)
    private val dateToOutfitIdCache = mutableMapOf<String, Int>()
    private val checkedOutfitIds = mutableSetOf<Int>()

    /**
     * 캘린더에 등록된 코디 날짜를 서버에서 강제 fetch (onResume 시 호출)
     */
    fun refreshAllOutfitDates() {
        viewModelScope.launch {
            val dates = mutableSetOf<String>()
            for (outfitId in 1..1000) {
                when (val result = repository.getOutfitImage(outfitId)) {
                    is CalendarResult.Success -> {
                        val outfitDate = result.data.date
                        dates.add(outfitDate)
                        dateToOutfitIdCache[outfitDate] = outfitId
                        checkedOutfitIds.add(outfitId)
                    }
                    is CalendarResult.Failure -> {
                        checkedOutfitIds.add(outfitId)
                        if (!result.message.contains("NOT_EXISTS")) break
                    }
                    is CalendarResult.Error -> break
                }
            }
            // UI 상태 갱신 (서버 기준)
            _uiState.value = _uiState.value.copy(datesWithOutfits = dates)
        }
    }

    /**
     * 날짜 클릭 시 상세 outfit 불러오기 (기존 방식 유지)
     */
    fun onDateSelected(selectedDate: String) {
        clearPreviousData()
        val cachedOutfitId = dateToOutfitIdCache[selectedDate]
        if (cachedOutfitId != null) {
            loadOutfitData(cachedOutfitId)
            return
        }
        findOutfitIdByDate(selectedDate)
    }

    private fun findOutfitIdByDate(targetDate: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            for (outfitId in 1..1000) {
                if (checkedOutfitIds.contains(outfitId)) continue
                when (val result = repository.getOutfitImage(outfitId)) {
                    is CalendarResult.Success -> {
                        val outfitDate = result.data.date
                        dateToOutfitIdCache[outfitDate] = outfitId
                        checkedOutfitIds.add(outfitId)
                        if (outfitDate == targetDate) {
                            loadOutfitData(outfitId)
                            return@launch
                        }
                    }
                    is CalendarResult.Failure -> {
                        checkedOutfitIds.add(outfitId)
                        if (result.message.contains("NOT_EXISTS")) {
                            continue
                        } else {
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
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                hasOutfitData = false,
                errorMessage = "해당 날짜에 등록된 코디가 없습니다"
            )
        }
    }

    private fun loadOutfitData(outfitId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            launch { loadOutfitImage(outfitId) }
            launch { loadOutfitText(outfitId) }
        }
    }

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

    private fun clearPreviousData() {
        _uiState.value = _uiState.value.copy(
            outfitImage = null,
            outfitText = null,
            hasOutfitData = false,
            errorMessage = null
        )
    }

    private fun handleError(message: String) {
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            hasOutfitData = false,
            errorMessage = message
        )
    }

    fun clearErrorMessage() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

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

    fun clearTagError() {
        _uiState.value = _uiState.value.copy(tagErrorMessage = null)
    }
}