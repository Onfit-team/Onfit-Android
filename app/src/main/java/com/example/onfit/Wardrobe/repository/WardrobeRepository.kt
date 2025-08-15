package com.example.onfit.Wardrobe.repository

import android.content.Context
import android.net.Uri
import com.example.onfit.KakaoLogin.util.TokenProvider
import com.example.onfit.Wardrobe.Network.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class WardrobeRepository(private val context: Context) {

    private val wardrobeService = WardrobeRetrofitClient.wardrobeService

    /**
     * 인증 토큰 가져오기 (TokenProvider 현재 상태 그대로 사용)
     */
    private fun getAuthToken(): String {
        return try {
            val token = TokenProvider.getToken(context)
            if (!token.isNullOrEmpty()) {
                "Bearer $token"
            } else {
                // 🔥 개발용 임시 토큰 (실제 토큰으로 교체 필요)
                "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VySWQiOjUsImlhdCI6MTc1NTE2MTk3NiwiZXhwIjoxNzU1NzY2Nzc2fQ.UyvHSiZnYVeMYsgM-frT9A-hdakdYfUNVPBluuvuBRU"
            }
        } catch (e: Exception) {
            // TokenProvider에 문제가 있으면 임시 토큰 사용
            "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VySWQiOjUsImlhdCI6MTc1NTE2MTk3NiwiZXhwIjoxNzU1NzY2Nzc2fQ.UyvHSiZnYVeMYsgM-frT9A-hdakdYfUNVPBluuvuBRU"
        }
    }

    /**
     * 토큰 존재 여부 확인
     */
    private fun hasValidToken(): Boolean {
        return try {
            val token = TokenProvider.getToken(context)
            !token.isNullOrEmpty()
        } catch (e: Exception) {
            // TokenProvider에 문제가 있어도 임시 토큰이 있으므로 true 반환
            true
        }
    }

    /**
     * 전체 옷장 아이템 조회
     */
    suspend fun getAllWardrobeItems(): Result<WardrobeResult> {
        return try {
            if (!hasValidToken()) {
                return Result.failure(Exception("로그인이 필요합니다"))
            }

            val token = getAuthToken()
            val response = wardrobeService.getAllWardrobeItems(token)

            if (response.isSuccessful && response.body()?.isSuccess == true) {
                Result.success(response.body()!!.result!!)
            } else {
                val errorMessage = response.body()?.message ?: "HTTP ${response.code()}"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 카테고리별 옷장 아이템 조회
     */
    suspend fun getWardrobeItemsByCategory(
        category: Int? = null,
        subcategory: Int? = null
    ): Result<WardrobeResult> {
        return try {
            if (!hasValidToken()) {
                return Result.failure(Exception("로그인이 필요합니다"))
            }

            val token = getAuthToken()
            val response = wardrobeService.getWardrobeItemsByCategory(token, category, subcategory)

            if (response.isSuccessful && response.body()?.isSuccess == true) {
                Result.success(response.body()!!.result!!)
            } else {
                val errorMessage = response.body()?.message ?: "HTTP ${response.code()}"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 아이템 상세 조회
     */
    suspend fun getWardrobeItemDetail(itemId: Int): Result<WardrobeItemDetail> {
        return try {
            if (!hasValidToken()) {
                return Result.failure(Exception("로그인이 필요합니다"))
            }

            val token = getAuthToken()
            val response = wardrobeService.getWardrobeItemDetail(itemId, token)

            if (response.isSuccessful && response.body()?.isSuccess == true) {
                Result.success(response.body()!!.result!!)
            } else {
                val errorMessage = response.body()?.message ?: "HTTP ${response.code()}"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 아이템 등록
     * 실제 이미지를 넣은 경우: uploadImage() -> 받은 url을 RegisterItemRequestDto.image에 넣어서 이 함수 호출
     * 이미지 없는 경우: image=null/""로 RegisterItemRequestDto 생성
     */
    suspend fun registerItem(request: RegisterItemRequestDto): Result<RegisterItemResult> {
        return try {
            if (!hasValidToken()) {
                return Result.failure(Exception("로그인이 필요합니다"))
            }

            val token = getAuthToken()
            val response = wardrobeService.registerItem(token, request)

            if (response.isSuccessful && response.body()?.isSuccess == true) {
                Result.success(response.body()!!.result!!)
            } else {
                val errorMessage = response.body()?.message ?: "Registration failed"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 이미지 업로드 (S3 URL 리턴)
     * 업로드 성공 시 S3 url 반환
     * → RegisterItemRequestDto.image에 반드시 이 url을 넣어야 함!
     */
    suspend fun uploadImage(imageUri: Uri): Result<String> {
        return try {
            if (!hasValidToken()) {
                return Result.failure(Exception("로그인이 필요합니다"))
            }

            val token = getAuthToken()
            val imagePart = uriToMultipartPart(imageUri, "image")
            val response = wardrobeService.uploadImage(token, imagePart)

            if (response.isSuccessful && response.body()?.isSuccess == true) {
                val imageUrl = response.body()!!.result.imageUrl
                Result.success(imageUrl)
            } else {
                val errorMessage = response.body()?.message ?: "Image upload failed"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 아이템 등록 with 이미지 업로드
     * 1. 이미지가 있으면 업로드
     * 2. RegisterItemRequestDto.image에 S3 url을 세팅
     * 3. 아이템 등록 요청
     */
    suspend fun registerItemWithImage(
        request: RegisterItemRequestDto,
        imageUri: Uri?
    ): Result<RegisterItemResult> {
        return try {
            var updatedRequest = request

            // 1. 이미지가 있으면 업로드 후 url을 받아서 dto에 세팅
            if (imageUri != null) {
                val imageUrlResult = uploadImage(imageUri)
                if (imageUrlResult.isSuccess) {
                    val imageUrl = imageUrlResult.getOrNull() ?: ""
                    updatedRequest = request.copy(image = imageUrl)
                } else {
                    return Result.failure(Exception("이미지 업로드 실패: ${imageUrlResult.exceptionOrNull()?.message}"))
                }
            }

            // 2. 아이템 등록
            registerItem(updatedRequest)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 아이템 수정
     * (수정할 때도 image 필드가 S3 url이어야 함)
     */
    suspend fun updateWardrobeItem(
        itemId: Int,
        request: RegisterItemRequestDto,
        imageUri: Uri? = null
    ): Result<Unit> {
        return try {
            var updatedRequest = request

            // 1. 이미지가 새로 있으면 업로드 후 url을 세팅
            if (imageUri != null) {
                val imageUrlResult = uploadImage(imageUri)
                if (imageUrlResult.isSuccess) {
                    val imageUrl = imageUrlResult.getOrNull() ?: ""
                    updatedRequest = request.copy(image = imageUrl)
                } else {
                    return Result.failure(Exception("이미지 업로드 실패: ${imageUrlResult.exceptionOrNull()?.message}"))
                }
            }

            // 2. 아이템 수정
            if (!hasValidToken()) {
                return Result.failure(Exception("로그인이 필요합니다"))
            }

            val token = getAuthToken()
            val response = wardrobeService.updateWardrobeItem(itemId, token, updatedRequest)

            if (response.isSuccessful && response.body()?.isSuccess == true) {
                Result.success(Unit)
            } else {
                val errorMessage = response.body()?.message ?: "Update failed"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 아이템 삭제
     */
    suspend fun deleteWardrobeItem(itemId: Int): Result<Unit> {
        return try {
            if (!hasValidToken()) {
                return Result.failure(Exception("로그인이 필요합니다"))
            }

            val token = getAuthToken()
            val response = wardrobeService.deleteWardrobeItem(itemId, token)

            if (response.isSuccessful && response.body()?.isSuccess == true) {
                Result.success(Unit)
            } else {
                val errorMessage = response.body()?.message ?: "Delete failed"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 브랜드 목록 조회
     */
    suspend fun getBrandsList(): Result<List<String>> {
        return try {
            if (!hasValidToken()) {
                return Result.failure(Exception("로그인이 필요합니다"))
            }

            val token = getAuthToken()
            val response = wardrobeService.getBrandsList(token)

            if (response.isSuccessful && response.body()?.isSuccess == true) {
                Result.success(response.body()!!.result ?: emptyList())
            } else {
                // API가 없는 경우 빈 리스트 반환
                Result.success(emptyList())
            }
        } catch (e: Exception) {
            // API가 구현되지 않은 경우 임시로 빈 리스트 반환
            Result.success(emptyList())
        }
    }

    /**
     * URI를 MultipartBody.Part로 변환
     */
    private fun uriToMultipartPart(uri: Uri, partName: String): MultipartBody.Part {
        val resolver = context.contentResolver
        val mimeType = resolver.getType(uri) ?: "image/*"

        val inputStream = resolver.openInputStream(uri)
        val tempFile = File.createTempFile("upload_", ".jpg", context.cacheDir)

        inputStream?.use { input ->
            tempFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        val requestFile = tempFile.asRequestBody(mimeType.toMediaTypeOrNull())
        return MultipartBody.Part.createFormData(partName, tempFile.name, requestFile)
    }

    /**
     * 검색 기능 (로컬에서 처리)
     */
    suspend fun searchItems(query: String, allItems: List<WardrobeItemDto>): Result<List<WardrobeItemDto>> {
        return try {
            val filteredItems = allItems.filter { item ->
                item.brand.contains(query, ignoreCase = true) ||
                        item.id.toString().contains(query)
            }
            Result.success(filteredItems)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 디버그용 토큰 정보
     */
    fun getTokenInfo(): String {
        return try {
            val token = TokenProvider.getToken(context)
            """
            Token exists: ${!token.isNullOrEmpty()}
            Token preview: ${token?.take(20)}...
        """.trimIndent()
        } catch (e: Exception) {
            "Token info error: ${e.message}"
        }
    }
}