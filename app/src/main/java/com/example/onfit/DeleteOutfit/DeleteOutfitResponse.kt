package com.example.onfit.DeleteOutfit

data class DeleteOutfitResponse(
    val isSuccess: Boolean,
    val code: String,
    val message: String,
    val result: DeleteOutfitResult?
)
data class DeleteOutfitResult(val detail: String?)