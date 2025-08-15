package com.example.onfit.Community.model

import com.example.onfit.KakaoLogin.model.ErrorResponse

data class TagListResponse(
    val result: List<TagItem>?,
    val error: ErrorResponse?,
    val success: Boolean?
)

data class TagItem(
    val id: Int,
    val name: String
)
