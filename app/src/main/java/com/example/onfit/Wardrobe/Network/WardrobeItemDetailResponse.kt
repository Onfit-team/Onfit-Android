package com.example.onfit.Wardrobe.Network

data class WardrobeItemDetailResponse(
    val isSuccess: Boolean,
    val code: String,
    val message: String,
    val result: WardrobeItemDetail?
)

data class WardrobeItemDetail(
    val id: Int,
    val category: Int,
    val subcategory: Int,
    val brand: String,
    val color: Int,
    val size: String,
    val season: Int,
    val purchaseDate: String?,
    val image: String,
    val price: Int?,
    val purchaseSite: String?,
    val tags: WardrobeItemTags
)

data class WardrobeItemTags(
    val moodTags: List<WardrobeTag>,
    val purposeTags: List<WardrobeTag>
)

data class WardrobeTag(
    val id: Int,
    val name: String,
    val type: String
)