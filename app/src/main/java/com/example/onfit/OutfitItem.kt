package com.example.onfit

data class OutfitItem(
    val imageResId: Int,
    var isInCloset: Boolean = true // 옷장에서 찾아보기 눌렀을 때 회색으로 버튼 비활성화
)