package com.example.onfit.Wardrobe.Network

import retrofit2.Response
import retrofit2.http.*
import okhttp3.MultipartBody

interface WardrobeService {

    /**
     * ===== 옷장 아이템 조회 =====
     */

    /**
     * 전체 옷장 아이템 조회
     */
    @GET("wardrobe/items")
    suspend fun getAllWardrobeItems(
        @Header("Authorization") token: String
    ): Response<WardrobeResponse>

    /**
     * 카테고리별 옷장 아이템 조회
     */
    @GET("wardrobe/items")
    suspend fun getWardrobeItemsByCategory(
        @Header("Authorization") token: String,
        @Query("category") category: Int? = null,
        @Query("subcategory") subcategory: Int? = null
    ): Response<WardrobeResponse>

    /**
     * 아이템 상세 조회
     */
    @GET("wardrobe/items/{itemId}")
    suspend fun getWardrobeItemDetail(
        @Path("itemId") itemId: Int,
        @Header("Authorization") token: String
    ): Response<WardrobeItemDetailResponse>

    /**
     * ===== 아이템 CRUD =====
     */

    /**
     * 아이템 등록
     */
    @POST("wardrobe/items")
    suspend fun registerItem(
        @Header("Authorization") token: String,
        @Body request: RegisterItemRequestDto
    ): Response<RegisterItemResponse>

    /**
     * 아이템 수정
     */
    @PUT("wardrobe/items/{itemId}")
    suspend fun updateWardrobeItem(
        @Path("itemId") itemId: Int,
        @Header("Authorization") token: String,
        @Body request: RegisterItemRequestDto
    ): Response<ApiResponse<Any>>

    /**
     * 아이템 삭제
     */
    @DELETE("wardrobe/items/{itemId}")
    suspend fun deleteWardrobeItem(
        @Path("itemId") itemId: Int,
        @Header("Authorization") token: String
    ): Response<ApiResponse<Any>>

    /**
     * ===== 이미지 관련 =====
     */

    /**
     * 이미지 업로드
     */
    @Multipart
    @POST("/items/upload")
    suspend fun uploadImage(
        @Header("Authorization") token: String,
        @Part image: MultipartBody.Part
    ): Response<ImageUploadResponse>

    /**
     * ===== 검색 및 필터 =====
     */

    /**
     * 아이템 검색
     */
    @POST("wardrobe/items/search")
    suspend fun searchWardrobeItems(
        @Header("Authorization") token: String,
        @Body searchRequest: SearchFilterRequest
    ): Response<WardrobeResponse>

    /**
     * 브랜드 목록 조회
     */
    @GET("wardrobe/brands")
    suspend fun getBrandsList(
        @Header("Authorization") token: String
    ): Response<BrandsResponse>

    /**
     * ===== AI 추천 관련 =====
     */

    /**
     * AI 카테고리 추천
     */
    @Multipart
    @POST("/items/recommend-categories")
    suspend fun getRecommendedCategories(
        @Header("Authorization") token: String,
        @Part image: MultipartBody.Part
    ): Response<ApiResponse<RecommendedCategoriesResult>>

    /**
     * 아이템 추천
     */
    @GET("wardrobe/items/recommendations")
    suspend fun getRecommendedItems(
        @Header("Authorization") token: String,
        @Query("category") category: Int? = null,
        @Query("season") season: Int? = null,
        @Query("limit") limit: Int = 10
    ): Response<ApiResponse<List<WardrobeItemDto>>>
}

/**
 * AI 추천 결과
 */
data class RecommendedCategoriesResult(
    val category: Int,
    val subcategory: Int,
    val season: Int,
    val color: Int
)