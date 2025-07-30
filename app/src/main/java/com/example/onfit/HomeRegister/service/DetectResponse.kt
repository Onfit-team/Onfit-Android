package com.example.onfit.HomeRegister.service

data class DetectResponse(
    val isSuccess: Boolean,
    val code: String,
    val message: String,
    val result: DetectResult?
)

data class DetectResult(
    val crops: List<CropItem>
)


data class CropItem(
    val crop_id: String,
    val category: String,
    val bbox: List<Float> // [x1, y1, x2, y2]
)
