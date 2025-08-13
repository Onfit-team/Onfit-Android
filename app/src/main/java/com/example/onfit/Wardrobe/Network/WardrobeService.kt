package com.example.onfit.Wardrobe.Network

import com.example.onfit.OutfitRegister.ImageUploadResponse
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface WardrobeService {

    /**
     * 옷장 아이템 전체 조회
     */
    @GET("/wardrobe/items")
    suspend fun getAllWardrobeItems(
        @Header("Authorization") token: String
    ): Response<ApiResponse<WardrobeResponseDto>>

    /**
     * 카테고리별 옷장 아이템 조회
     */
    @GET("/wardrobe/items/categories")
    suspend fun getWardrobeItemsByCategory(
        @Header("Authorization") token: String,
        @Query("category") category: Int? = null,
        @Query("subcategory") subcategory: Int? = null
    ): Response<ApiResponse<WardrobeResponseDto>>

    /**
     * 아이템 세부 정보 조회
     */
    @GET("wardrobe/items/{item_id}")
    suspend fun getWardrobeItemDetail(
        @Path("item_id") itemId: Int,
        @Header("Authorization") authorization: String
    ): Response<WardrobeItemDetailResponse>

    /**
     * 아이템 수정 (PUT)
     */
    @PUT("wardrobe/items/{item_id}")
    suspend fun updateWardrobeItem(
        @Path("item_id") itemId: Int,
        @Header("Authorization") authorization: String,
        @Body request: RegisterItemRequestDto
    ): Response<ApiResponse<RegisterItemResponseDto>>

    /**
     * 아이템 등록
     */
    @POST("/wardrobe/items")
    suspend fun registerItem(
        @Header("Authorization") token: String,
        @Body request: RegisterItemRequestDto
    ): Response<ApiResponse<RegisterItemResponseDto>>

    /**
     * 옷 등록 시 추출한 카테고리 조회
     */
    @GET("wardrobe/categories/{item_id}")
    suspend fun getRecommendedCategories(
        @Path("item_id") itemId: Int,
        @Header("Authorization") authorization: String
    ): Response<ApiResponse<RecommendedCategoriesResult>>

    /**
     * 아이템 삭제
     */
    @DELETE("wardrobe/items/{item_id}")
    suspend fun deleteWardrobeItem(
        @Path("item_id") itemId: Int,
        @Header("Authorization") authorization: String
    ): Response<ApiResponse<String>>

    /**
     * 아이템 필터 검색
     */
    @GET("wardrobe/items/filter")
    suspend fun filterWardrobeItems(
        @Header("Authorization") authorization: String,
        @Query("season") season: Int? = null,
        @Query("color") color: Int? = null,
        @Query("brand") brand: String? = null,
        @Query("tag_ids") tagIds: String? = null // "1,10" 형식
    ): Response<ApiResponse<WardrobeResponseDto>>

    /**
     * 브랜드 목록 조회
     */
    @GET("wardrobe/items/brands")
    suspend fun getBrandsList(
        @Header("Authorization") authorization: String
    ): Response<BrandsResponse>

    /**
     * 특정 아이템이 포함된 코디 기록 조회
     */
    @GET("wardrobe/items/{itemId}/outfits")
    suspend fun getItemOutfitHistory(
        @Path("itemId") itemId: Int,
        @Header("Authorization") authorization: String
    ): Response<ItemOutfitHistoryResponse>

    /**
     * 함께 코디하면 좋은 옷장 아이템 추천
     */
    @GET("wardrobe/items/{itemId}/recommendations")
    suspend fun getRecommendedItems(
        @Path("itemId") itemId: Int,
        @Header("Authorization") authorization: String
    ): Response<RecommendationResponse>

    @Multipart
    @POST("/items/refine")
    suspend fun refineImage(
        @Header("Authorization") authorization: String,
        @Part image: MultipartBody.Part
    ): Response<ImageRefineResponse>

    /**
     * 이미지 저장 (/items/save) - result 구조
     */
    @Multipart
    @POST("/items/save")
    suspend fun saveImage(
        @Header("Authorization") authorization: String,
        @Part image: MultipartBody.Part
    ): Response<ImageSaveResponse>

    /**
     * 이미지 업로드 (/items/upload) - data 구조
     */
    @Multipart
    @POST("/items/upload")
    suspend fun uploadImage(
        @Header("Authorization") authorization: String,
        @Part image: MultipartBody.Part
    ): Response<ImageUploadResponse>
}