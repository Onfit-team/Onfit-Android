package com.example.onfit.Wardrobe.Network

/**
 * API 호출 결과를 나타내는 sealed class
 * ViewModel에서 UI 상태 관리용
 */
sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val exception: Throwable) : ApiResult<Nothing>()
    data class Loading(val isLoading: Boolean = true) : ApiResult<Nothing>()
}