package com.example.onfit.HomeRegister.model

import android.net.Uri

data class OutfitItem2(
    var imageResId: Int? = null,
    var imageUri: Uri? = null,     // 갤러리에서 선택한 이미지 URI
    var isClosetButtonActive: Boolean = true // 아이템이 옷장에 있는지
)
