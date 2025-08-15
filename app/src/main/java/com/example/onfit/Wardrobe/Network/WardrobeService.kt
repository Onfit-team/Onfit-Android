package com.example.onfit.Wardrobe.Network

<<<<<<< HEAD
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
=======
import retrofit2.Response
import retrofit2.http.*
import okhttp3.MultipartBody
>>>>>>> 3677f88 (refactor: 코드 리팩토링)

interface WardrobeService {

    /**
<<<<<<< HEAD
     * 옷장 아이템 전체 조회
     */
    @GET("/wardrobe/items")
    suspend fun getAllWardrobeItems(
        @Header("Authorization") token: String
    ): Response<ApiResponse<WardrobeResponseDto>>
=======
     * ===== 옷장 아이템 조회 =====
     */

    /**
     * 전체 옷장 아이템 조회
     */
    @GET("wardrobe/items")
    suspend fun getAllWardrobeItems(
        @Header("Authorization") token: String
    ): Response<WardrobeResponse>
>>>>>>> 3677f88 (refactor: 코드 리팩토링)

    /**
     * 카테고리별 옷장 아이템 조회
     */
<<<<<<< HEAD
    @GET("/wardrobe/items/categories")
=======
    @GET("wardrobe/items")
>>>>>>> 3677f88 (refactor: 코드 리팩토링)
    suspend fun getWardrobeItemsByCategory(
        @Header("Authorization") token: String,
        @Query("category") category: Int? = null,
        @Query("subcategory") subcategory: Int? = null
<<<<<<< HEAD
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
=======
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
>>>>>>> 3677f88 (refactor: 코드 리팩토링)

    /**
     * 아이템 등록
     */
<<<<<<< HEAD
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
=======
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
>>>>>>> 3677f88 (refactor: 코드 리팩토링)

    /**
     * 아이템 삭제
     */
<<<<<<< HEAD
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
=======
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
>>>>>>> 3677f88 (refactor: 코드 리팩토링)
     */
    @Multipart
    @POST("/items/upload")
    suspend fun uploadImage(
<<<<<<< HEAD
        @Header("Authorization") authorization: String,
        @Part image: MultipartBody.Part
    ): Response<ImageUploadResponse>
}
=======
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
>>>>>>> 3677f88 (refactor: 코드 리팩토링)
