package com.example.onfit.Community.model

data class ToggleLikeResponse(
    val isSuccess: Boolean,
    val code: String?,
    val message: String?,
    val result: Result?
) {
    data class Result(
        val outfit_id: Int,
        val hearted: Boolean,
        val heart_count: Int
    )
}
