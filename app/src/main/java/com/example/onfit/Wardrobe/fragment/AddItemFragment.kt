package com.example.onfit.Wardrobe.fragment

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.graphics.drawable.Drawable
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.onfit.R
import com.example.onfit.Wardrobe.repository.WardrobeRepository
import com.example.onfit.Wardrobe.Network.RegisterItemRequestDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import android.app.Activity
import android.content.Intent
import android.provider.MediaStore
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts

class AddItemFragment : Fragment() {

    // Repository
    private lateinit var repository: WardrobeRepository

    // 이미지 관련 변수들
    private lateinit var ivClothes: ImageView
    private lateinit var tvTitle: TextView
    private lateinit var btnChangeToDefault: Button
    private var selectedImageUri: Uri? = null
    private var aiImageFailed: Boolean = false
    private val defaultImageResId = R.drawable.clothes8 // 기본 이미지(수정 가능)

    // 편집 모드 관련 변수들
    private var isEditMode = false
    private var itemId = -1

    // 선택된 태그들을 저장할 리스트
    private val selectedTags = mutableListOf<String>()

    // 태그 텍스트를 ID로 매핑하는 맵 추가
    private val tagTextToIdMap = mapOf(
        // 분위기 태그
        "#캐주얼" to 1,
        "#스트릿" to 2,
        "#미니멀" to 3,
        "#클래식" to 4,
        "#빈티지" to 5,
        "#러블리" to 6,
        "#페미닌" to 7,
        "#보이시" to 8,
        "#모던" to 9,

        // 용도 태그
        "#데일리" to 10,
        "#출근룩" to 11,
        "#데이트룩" to 12,
        "#나들이룩" to 13,
        "#여행룩" to 14,
        "#운동복" to 15,
        "#하객룩" to 16,
        "#파티룩" to 17
    )

    // 편집 모드용 기존 데이터 저장
    private var existingItemData: ExistingItemData? = null

    // 원본 이미지 URI를 저장할 변수
    private var originalImageUri: Uri? = null
    private var isAiImageApplied: Boolean = false


    // AI 처리된 이미지 URL 저장 변수 추가
    private var aiProcessedImageUrl: String? = null

    // 기존 아이템 데이터 클래스
    data class ExistingItemData(
        val category: Int,
        val subcategory: Int,
        val season: Int,
        val color: Int,
        val brand: String?,
        val size: String?,
        val price: Int?,
        val purchaseSite: String?,
        val tagIds: List<Int>?
    )

    private lateinit var changeImageLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Repository 초기화
        repository = WardrobeRepository(requireContext())

        // 🔥 이미지 변경을 위한 갤러리 런처 초기화
        changeImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            // 🔥 갤러리에서 돌아온 후 bottom navigation 다시 숨기기
            activity?.findViewById<View>(R.id.bottomNavigationView)?.visibility = View.GONE

            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { newImageUri ->
                    Log.d("AddItemFragment", "새 이미지 선택됨: $newImageUri")

                    // 새 이미지로 교체
                    selectedImageUri = newImageUri
                    ivClothes.setImageURI(newImageUri)

                    // 버튼 텍스트와 상태 변경
                    btnChangeToDefault.text = "다른 이미지로 변경하기"
                    aiImageFailed = false

                    // 제목 변경
                    tvTitle.text = "선택한 이미지로\n아이템을 등록해주세요!"

                    Toast.makeText(requireContext(), "이미지가 변경되었습니다", Toast.LENGTH_SHORT).show()
                }
            }
        }

        Log.d("AddItemFragment", "Repository 토큰 정보: ${repository.getTokenInfo()}")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_add_item, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupArguments()
        setupImageDisplay()

        // 뒤로가기 버튼
        val backButton = view.findViewById<ImageButton>(R.id.ic_back)
        backButton?.setOnClickListener {
            findNavController().navigateUp()
        }

        // 드롭다운 설정
        setupDropdowns(view)

        // 태그 버튼 설정
        setupTagButtons(view)

        // 🔥 편집 모드일 때 기존 데이터 로드
        if (isEditMode) {
            loadExistingItemData()
        }

        // 🔥 Repository를 사용한 저장 로직
        val saveButton = view.findViewById<Button>(R.id.btn_save)
        saveButton?.setOnClickListener {
            saveItemToWardrobeWithRepository()
        }

        // 🔥 기존 이미지로 변경 버튼 클릭 리스너
        btnChangeToDefault.setOnClickListener {
            Log.d("AddItemFragment", "버튼 클릭: ${btnChangeToDefault.text}")

            when {
                // AI 실패 상태인 경우 -> 기본 이미지로 등록
                aiImageFailed -> {
                    Log.d("AddItemFragment", "AI 실패 상태 - 기본 이미지로 설정")

                    ivClothes.setImageResource(defaultImageResId)
                    selectedImageUri = null
                    aiImageFailed = false

                    btnChangeToDefault.visibility = View.GONE
                    tvTitle.text = "기본 이미지로\n아이템을 등록해주세요!"
                }

                // AI 성공 상태인 경우 -> 원본 이미지로 복원
                isAiImageApplied && originalImageUri != null -> {
                    Log.d("AddItemFragment", "원본 이미지로 복원: $originalImageUri")

                    // 원본 이미지로 바로 복원
                    selectedImageUri = originalImageUri
                    ivClothes.setImageURI(originalImageUri)

                    // 상태 변경
                    isAiImageApplied = false
                    btnChangeToDefault.text = "AI 이미지로 변경"
                    tvTitle.text = "선택한 이미지로\n아이템을 등록해주세요!"

                    Toast.makeText(requireContext(), "원본 이미지로 변경되었습니다", Toast.LENGTH_SHORT).show()
                }

                // 원본 상태에서 다시 AI 이미지로 변경
                !isAiImageApplied && originalImageUri != null -> {
                    Log.d("AddItemFragment", "AI 이미지 재생성 요청")

                    // AI 이미지 재생성 (저장된 AI 이미지가 있다면 바로 사용)
                    btnChangeToDefault.text = "기존 이미지로 변경"
                    tvTitle.text = "AI가 이미지를\n깔끔하게 만들었어요!"
                    isAiImageApplied = true

                    Toast.makeText(requireContext(), "AI 이미지로 변경되었습니다", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun initViews(view: View) {
        ivClothes = view.findViewById(R.id.iv_clothes)
        tvTitle = view.findViewById(R.id.tv_title)
        btnChangeToDefault = view.findViewById(R.id.btn_change_to_default)
    }

    private fun setupArguments() {
        arguments?.let { bundle ->
            // 편집 모드 확인
            isEditMode = bundle.getBoolean("edit_mode", false)
            itemId = bundle.getInt("item_id", -1)

            // 🔥 NEW: 편집 모드일 때 기존 데이터 수집
            if (isEditMode) {
                existingItemData = ExistingItemData(
                    category = bundle.getInt("item_category", 1),
                    subcategory = bundle.getInt("item_subcategory", 1),
                    season = bundle.getInt("item_season", 1),
                    color = bundle.getInt("item_color", 1),
                    brand = bundle.getString("item_brand"),
                    size = bundle.getString("item_size"),
                    price = bundle.getInt("item_price", 0).takeIf { it > 0 },
                    purchaseSite = bundle.getString("item_purchase_site"),
                    tagIds = bundle.getIntegerArrayList("item_tag_ids")
                )

                Log.d("AddItemFragment", "편집 모드 기존 데이터: $existingItemData")
            }

            Log.d("AddItemFragment", "편집 모드: $isEditMode, 아이템 ID: $itemId")
        }
    }

    // 🔥 수정된 setupImageDisplay 함수 - 진입 시 즉시 AI 처리
    private fun setupImageDisplay() {
        arguments?.let { bundle ->
            when {
                // 1. 편집 모드인 경우
                isEditMode -> {
                    val itemImage = bundle.getString("item_image")
                    tvTitle.text = "아이템 정보를 수정해주세요"

                    // 🔥 NEW: Assets 이미지 처리 추가
                    when {
                        // Assets 이미지 처리
                        itemImage?.startsWith("file:///android_asset/") == true -> {
                            Log.d("AddItemFragment", "📱 Assets 이미지 로딩: $itemImage")
                            loadAssetsImage(itemImage)
                        }
                        // 기존 네트워크/로컬 이미지 처리
                        !itemImage.isNullOrEmpty() -> {
                            Log.d("AddItemFragment", "🌐 네트워크/로컬 이미지 로딩: $itemImage")
                            loadImageIntoView(itemImage)
                        }
                        // 이미지가 없는 경우
                        else -> {
                            Log.d("AddItemFragment", "❌ 이미지 URI 없음")
                            ivClothes.setImageResource(defaultImageResId)
                        }
                    }

                    btnChangeToDefault.visibility = View.VISIBLE
                    btnChangeToDefault.text = "이미지 변경하기"
                }

                // 2. 새 아이템 추가 - 이미지 URI가 있는 경우 -> 🔥 즉시 AI 처리
                bundle.containsKey("image_uri") -> {
                    val imageUriString = bundle.getString("image_uri")
                    if (!imageUriString.isNullOrEmpty()) {
                        // 🔥 원본 이미지 URI 저장
                        originalImageUri = Uri.parse(imageUriString)
                        selectedImageUri = originalImageUri

                        // 🔥 원본 이미지 먼저 표시
                        ivClothes.setImageURI(selectedImageUri)
                        tvTitle.text = "AI가 이미지를 처리 중입니다..."

                        // 🔥 즉시 AI 처리 시작
                        processImageWithAI()
                    }
                }

                else -> {
                    // 기본 이미지 표시
                    ivClothes.setImageResource(defaultImageResId)
                    tvTitle.text = "새 아이템을 등록해주세요"
                    btnChangeToDefault.visibility = View.GONE
                }
            }
        } ?: run {
            // arguments가 null인 경우
            ivClothes.setImageResource(defaultImageResId)
            tvTitle.text = "새 아이템을 등록해주세요"
            btnChangeToDefault.visibility = View.GONE
        }

        Log.d("AddItemFragment", "🔍 setupImageDisplay 완료")
        Log.d("AddItemFragment", "🔍 원본 이미지 URI: $originalImageUri")
    }

    private fun loadAssetsImage(assetUri: String) {
        try {
            val assetPath = assetUri.removePrefix("file:///android_asset/")
            val inputStream = requireContext().assets.open(assetPath)
            val drawable = Drawable.createFromStream(inputStream, null)

            if (drawable != null) {
                ivClothes.setImageDrawable(drawable)
                Log.d("AddItemFragment", "✅ Assets 이미지 로딩 성공: $assetPath")
            } else {
                Log.e("AddItemFragment", "❌ Drawable 생성 실패: $assetPath")
                ivClothes.setImageResource(defaultImageResId)
            }

            inputStream.close()
        } catch (e: Exception) {
            Log.e("AddItemFragment", "❌ Assets 이미지 로딩 실패: ${e.message}", e)
            ivClothes.setImageResource(defaultImageResId)
        }
    }

    // 🔥 NEW: 이미지 진입 시 즉시 AI 처리하는 함수
    private fun processImageWithAI() {
        if (selectedImageUri == null) {
            Log.e("AddItemFragment", "❌ selectedImageUri가 null입니다")
            return
        }

        Log.d("AddItemFragment", "🤖 AI 이미지 처리 시작: $selectedImageUri")

        lifecycleScope.launch {
            try {
                // 로딩 상태 표시
                withContext(Dispatchers.Main) {
                    tvTitle.text = "AI가 이미지를\n깔끔하게 만들고 있어요..."
                    // 로딩 인디케이터 표시 (선택사항)
                }

                // 🔥 AI 이미지 처리 (Repository의 uploadImage 사용)
                repository.uploadImage(selectedImageUri!!)
                    .onSuccess { aiImageUrl ->
                        Log.d("AddItemFragment", "✅ AI 이미지 생성 성공: $aiImageUrl")

                        withContext(Dispatchers.Main) {
                            // 🔥 AI 처리된 이미지로 교체
                            Glide.with(requireContext())
                                .load(aiImageUrl)
                                .placeholder(R.drawable.clothes8)
                                .error(R.drawable.clothes8)
                                .into(ivClothes)

                            // 🔥 상태 업데이트
                            isAiImageApplied = true
                            aiImageFailed = false

                            // 🔥 UI 업데이트
                            btnChangeToDefault.visibility = View.VISIBLE
                            btnChangeToDefault.text = "기존 이미지로 변경"
                            tvTitle.text = "AI가 이미지를\n깔끔하게 만들었어요!"

                            // 🔥 AI 처리된 이미지 URL 저장 (저장 시 사용)
                            aiProcessedImageUrl = aiImageUrl

                            Toast.makeText(requireContext(), "AI가 이미지를 깔끔하게 만들었습니다!", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .onFailure { throwable ->
                        Log.e("AddItemFragment", "❌ AI 이미지 생성 실패: ${throwable.message}")

                        withContext(Dispatchers.Main) {
                            // 🔥 실패 시 원본 이미지 유지
                            aiImageFailed = true
                            isAiImageApplied = false

                            btnChangeToDefault.visibility = View.VISIBLE
                            btnChangeToDefault.text = "기본 이미지로 등록하기"
                            tvTitle.text = "AI 이미지 생성에 실패했습니다\n원본 이미지로 등록하시겠습니까?"

                            Toast.makeText(requireContext(), "AI 이미지 생성에 실패했습니다. 원본 이미지로 등록됩니다.", Toast.LENGTH_LONG).show()
                        }
                    }

            } catch (e: Exception) {
                Log.e("AddItemFragment", "💥 AI 처리 중 예외 발생", e)

                withContext(Dispatchers.Main) {
                    aiImageFailed = true
                    isAiImageApplied = false

                    btnChangeToDefault.visibility = View.VISIBLE
                    btnChangeToDefault.text = "기본 이미지로 등록하기"
                    tvTitle.text = "AI 이미지 생성에 실패했습니다\n원본 이미지로 등록하시겠습니까?"

                    Toast.makeText(requireContext(), "AI 처리 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // 🔥 완전한 loadImageFromUrl 함수 (기존 loadImageFromUrlWithOriginalPlaceholder 대체)
    private fun loadImageFromUrl(imageUrl: String) {
        if (!isAdded || context == null) {
            Log.w("AddItemFragment", "Fragment가 attach되지 않음 - Glide 호출 건너뜀")
            return
        }

        try {
            if (originalImageUri != null) {
                // 🔥 원본 이미지가 있는 경우 - 원본 이미지를 활용
                Glide.with(requireContext())
                    .load(imageUrl) // AI 처리된 이미지 로드
                    .error(originalImageUri) // 에러 시 원본 이미지로 복원
                    .into(ivClothes)

                Log.d("AddItemFragment", "✅ AI 이미지 로드 (원본 이미지 백업): $imageUrl")
            } else {
                // 🔥 원본 이미지가 없는 경우 - 기본 이미지 사용
                Glide.with(requireContext())
                    .load(imageUrl)
                    .placeholder(R.drawable.clothes8)
                    .error(R.drawable.clothes8)
                    .into(ivClothes)

                Log.d("AddItemFragment", "✅ AI 이미지 로드 (기본 이미지 백업): $imageUrl")
            }

        } catch (e: Exception) {
            Log.e("AddItemFragment", "❌ AI 이미지 로드 실패", e)

            // 🔥 실패 시 처리: 원본 이미지 우선, 없으면 기본 이미지
            try {
                if (originalImageUri != null) {
                    ivClothes.setImageURI(originalImageUri)
                    Log.d("AddItemFragment", "원본 이미지로 복원됨")
                } else {
                    ivClothes.setImageResource(R.drawable.clothes8)
                    Log.d("AddItemFragment", "기본 이미지로 복원됨")
                }
            } catch (fallbackException: Exception) {
                Log.e("AddItemFragment", "이미지 복원도 실패", fallbackException)
                // 최후의 수단
                ivClothes.setImageResource(R.drawable.clothes8)
            }
        }
    }

    // AI 처리를 건너뛰고 바로 등록
    private suspend fun registerNewItemWithRepository(formData: RegisterItemRequestDto) {
        try {
            // 🔥 이미 AI 처리가 완료된 경우 해당 URL 사용
            val imageUrl = when {
                isAiImageApplied && !aiProcessedImageUrl.isNullOrEmpty() -> {
                    Log.d("AddItemFragment", "✅ 이미 처리된 AI 이미지 사용: $aiProcessedImageUrl")
                    aiProcessedImageUrl!!
                }
                aiImageFailed && selectedImageUri == null -> {
                    Log.d("AddItemFragment", "✅ 기본 이미지로 등록")
                    "default_image" // 기본 이미지 URL
                }
                originalImageUri != null -> {
                    Log.d("AddItemFragment", "✅ 원본 이미지로 등록")
                    // 원본 이미지 다시 업로드 (AI 처리 없이)
                    repository.uploadImage(originalImageUri!!).getOrThrow()
                }
                else -> {
                    throw Exception("등록할 이미지가 없습니다")
                }
            }

            // 아이템 등록
            val finalRequest = formData.copy(image = imageUrl)
            repository.registerItem(finalRequest)
                .onSuccess { result ->
                    Log.d("AddItemFragment", "✅ 아이템 등록 성공: ${result.itemId}")

                    withContext(Dispatchers.Main) {
                        notifyRegistrationComplete(true, formData.purchaseDate)
                        Toast.makeText(requireContext(), "새 아이템이 추가되었습니다", Toast.LENGTH_SHORT).show()
                        findNavController().navigateUp()
                    }
                }
                .onFailure { throwable ->
                    Log.e("AddItemFragment", "❌ 아이템 등록 실패: ${throwable.message}")
                    throw Exception(throwable.message ?: "아이템 등록 실패", throwable)
                }

        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                notifyRegistrationComplete(false, null)
                handleError(e, "아이템 등록에 실패했습니다")
            }
        }
    }

    private fun loadImageIntoView(imageUrl: String) {
        when {
            // 🔥 NEW: Assets 이미지 처리
            imageUrl.startsWith("file:///android_asset/") -> {
                loadAssetsImage(imageUrl)
                btnChangeToDefault.visibility = View.VISIBLE
            }
            // 네트워크 이미지
            imageUrl.startsWith("http") -> {
                Glide.with(this)
                    .load(imageUrl)
                    .placeholder(defaultImageResId)
                    .error(defaultImageResId)
                    .into(ivClothes)
                btnChangeToDefault.visibility = View.GONE
            }
            // 로컬 이미지나 URI
            else -> {
                try {
                    val uri = Uri.parse(imageUrl)
                    ivClothes.setImageURI(uri)
                    selectedImageUri = uri
                    btnChangeToDefault.visibility = View.GONE
                } catch (e: Exception) {
                    ivClothes.setImageResource(defaultImageResId)
                    btnChangeToDefault.visibility = View.GONE
                }
            }
        }
    }

    /**
     * 🔥 NEW: 편집 모드일 때 기존 아이템 데이터 로드
     */
    private fun loadExistingItemData() {
        existingItemData?.let { data ->
            // 🔥 기존 태그 복원 (태그 ID 사용)
            data.tagIds?.let { tagIds ->
                restoreSelectedTags(tagIds)
            }

            // EditText 필드들 복원
            view?.let { v ->
                data.brand?.let { v.findViewById<EditText>(R.id.et_brand)?.setText(it) }
                data.size?.let { v.findViewById<EditText>(R.id.et_size)?.setText(it) }
                data.price?.let { v.findViewById<EditText>(R.id.et_price)?.setText(it.toString()) }
                data.purchaseSite?.let { v.findViewById<EditText>(R.id.et_site)?.setText(it) }
            }

            Log.d("AddItemFragment", "기존 데이터 복원 완료: 태그 개수=${data.tagIds?.size ?: 0}")
        }
    }

    /**
     * 🔥 Repository를 사용한 저장 로직
     */
    private fun saveItemToWardrobeWithRepository() {
        // 폼 데이터 수집
        val formData = collectFormDataSync()
        if (formData == null) {
            Toast.makeText(requireContext(), "필수 정보를 모두 입력해주세요", Toast.LENGTH_SHORT).show()
            return
        }

        // 🔥 수정: AI 실패 상태이거나 이미지가 없는 경우 처리
        if (selectedImageUri == null && !isEditMode && !aiImageFailed) {
            Toast.makeText(requireContext(), "이미지를 선택해주세요", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                showLoading(true)

                if (isEditMode && itemId > 0) {
                    // 🔥 수정 모드
                    updateItemWithRepository(formData)
                } else {
                    // 🔥 새 등록 모드
                    if (aiImageFailed) {
                        // AI 실패 상태에서는 기본 이미지로 등록
                        registerWithDefaultImage(formData)
                    } else {
                        // 정상적인 이미지 업로드 시도
                        registerNewItemWithRepository(formData)
                    }
                }

            } catch (e: Exception) {
                handleError(e, "저장 중 오류가 발생했습니다")
            } finally {
                showLoading(false)
            }
        }
    }

    private suspend fun registerWithDefaultImage(formData: RegisterItemRequestDto) {
        try {
            Log.d("AddItemFragment", "기본 이미지로 아이템 등록 시작")

            // 기본 이미지 URL 설정 (서버에서 처리할 기본 이미지 경로)
            val defaultImageUrl = "default_image" // 또는 서버에서 정의한 기본 이미지 URL

            val finalRequest = formData.copy(image = defaultImageUrl)

            repository.registerItem(finalRequest)
                .onSuccess { result ->
                    Log.d("AddItemFragment", "기본 이미지로 아이템 등록 성공: ${result.itemId}")

                    withContext(Dispatchers.Main) {
                        // 성공 결과 전달
                        notifyRegistrationComplete(true, formData.purchaseDate)

                        Toast.makeText(requireContext(), "기본 이미지로 아이템이 추가되었습니다", Toast.LENGTH_SHORT).show()
                        findNavController().navigateUp()
                    }
                }
                .onFailure { throwable ->
                    Log.e("AddItemFragment", "기본 이미지 아이템 등록 실패: ${throwable.message}")
                    withContext(Dispatchers.Main) {
                        notifyRegistrationComplete(false, null)
                        // Exception으로 변환해서 handleError 호출
                        handleError(Exception(throwable.message ?: "등록 실패", throwable), "아이템 등록에 실패했습니다")
                    }
                }

        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                notifyRegistrationComplete(false, null)
                handleError(e, "기본 이미지 등록에 실패했습니다")
            }
        }
    }

    /**
     * 🔥 아이템 수정 (Repository 사용)
     */
    private suspend fun updateItemWithRepository(formData: RegisterItemRequestDto) {
        try {
            // 이미지 처리
            val finalImageUrl = if (selectedImageUri != null) {
                // 새 이미지가 선택된 경우 업로드
                repository.uploadImage(selectedImageUri!!).getOrThrow()
            } else {
                // 기존 이미지 사용
                arguments?.getString("item_image") ?: formData.image
            }

            val finalRequest = formData.copy(image = finalImageUrl)

            repository.updateWardrobeItem(itemId, finalRequest)
                .onSuccess {
                    withContext(Dispatchers.Main) {
                        notifyRegistrationComplete(true, formData.purchaseDate, isUpdate = true)
                        Toast.makeText(requireContext(), "아이템이 수정되었습니다", Toast.LENGTH_SHORT).show()
                        findNavController().navigateUp()
                    }
                }
                .onFailure { throwable ->
                    // Exception으로 변환해서 throw
                    throw Exception(throwable.message ?: "아이템 수정 실패", throwable)
                }

        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                notifyRegistrationComplete(false, null, isUpdate = true)
                handleError(e, "아이템 수정에 실패했습니다")
            }
        }
    }

    /**
     * 🔥 MODIFIED: 등록 결과를 부모 Fragment들에게 전달 (업데이트 여부 추가)
     */
    private fun notifyRegistrationComplete(isSuccess: Boolean, purchaseDate: String?, isUpdate: Boolean = false) {
        val bundle = Bundle().apply {
            putBoolean("success", isSuccess)
            putString("registered_date", purchaseDate ?: getCurrentDate())
            putBoolean("edit_mode", isEditMode)
            putLong("timestamp", System.currentTimeMillis())
        }

        // RegisterItemBottomSheet에 알림
        parentFragmentManager.setFragmentResult("add_item_complete", bundle)

        // WardrobeFragment에 직접 알림
        val wardrobeBundle = Bundle().apply {
            putBoolean("success", isSuccess)
            putString("action", if (isEditMode) "updated" else "added")
            putString("registered_date", purchaseDate ?: getCurrentDate())
            putBoolean("force_refresh", isUpdate) // 🔥 NEW: 수정 시 강제 새로고침
        }

        val resultKey = if (isEditMode) "wardrobe_item_updated" else "item_registered"
        parentFragmentManager.setFragmentResult(resultKey, wardrobeBundle)

        // CalendarFragment에 직접 알림 (새 등록인 경우에만)
        if (!isEditMode && isSuccess) {
            parentFragmentManager.setFragmentResult("outfit_registered", bundle)
        }

        Log.d("AddItemFragment", "등록 결과 전달: success=$isSuccess, date=$purchaseDate, editMode=$isEditMode, isUpdate=$isUpdate")
    }

    /**
     * 로딩 상태 표시
     */
    private fun showLoading(isLoading: Boolean) {
        view?.findViewById<Button>(R.id.btn_save)?.apply {
            isEnabled = !isLoading
            text = if (isLoading) "저장 중..." else if (isEditMode) "수정하기" else "등록하기"
        }
    }

    /**
     * 에러 처리
     */
    private fun handleError(exception: Exception, defaultMessage: String) {
        val errorMessage = when {
            exception.message?.contains("로그인") == true -> "로그인이 필요합니다"
            exception.message?.contains("이미지") == true -> "이미지 업로드에 실패했습니다"
            exception.message?.contains("네트워크") == true -> "네트워크 연결을 확인해주세요"
            else -> exception.message ?: defaultMessage
        }

        Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
        Log.e("AddItemFragment", "Error: ${exception.message}", exception)
    }

    /**
     * 현재 날짜 반환
     */
    private fun getCurrentDate(): String {
        val calendar = Calendar.getInstance()
        return String.format(
            "%04d-%02d-%02d",
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    /**
     * 🔥 MODIFIED: 폼 데이터 수집 - 태그 처리 수정
     */
    private fun collectFormDataSync(): RegisterItemRequestDto? {
        return try {
            val view = requireView()

            // 스피너에서 선택된 값들 가져오기
            val categorySpinner = view.findViewById<Spinner>(R.id.spinner_category)
            val detailCategorySpinner = view.findViewById<Spinner>(R.id.spinner_detail_category)
            val seasonSpinner = view.findViewById<Spinner>(R.id.spinner_season)
            val colorSpinner = view.findViewById<Spinner>(R.id.spinner_color)

            // EditText에서 값들 가져오기
            val brandEdit = view.findViewById<EditText>(R.id.et_brand)
            val sizeEdit = view.findViewById<EditText>(R.id.et_size)
            val priceEdit = view.findViewById<EditText>(R.id.et_price)
            val siteEdit = view.findViewById<EditText>(R.id.et_site)

            // 카테고리 매핑
            val categoryMapping = mapOf(
                0 to 1, // 상의
                1 to 2, // 하의
                2 to 3, // 원피스
                3 to 4, // 아우터
                4 to 5, // 신발
                5 to 6  // 액세서리
            )

            val category = categoryMapping[categorySpinner.selectedItemPosition] ?: 1
            val subcategory = mapSubcategoryIndex(categorySpinner.selectedItemPosition, detailCategorySpinner.selectedItemPosition)

            // 🔥 MODIFIED: 계절 매핑 변경 (봄ㆍ가을/여름/겨울)
            val season = when (seasonSpinner.selectedItemPosition) {
                0 -> 1 // 봄ㆍ가을
                1 -> 2 // 여름
                2 -> 4 // 겨울
                else -> 1
            }

            val color = colorSpinner.selectedItemPosition + 1

            val brand = brandEdit.text.toString()
            val size = sizeEdit.text.toString()
            val priceText = priceEdit.text.toString()
            val price = if (priceText.isNotEmpty()) priceText.toIntOrNull() ?: 0 else 0
            val purchaseSite = siteEdit.text.toString()
            val purchaseDate = getCurrentDate()

            // 🔥 선택된 태그들을 ID로 변환
            val tagIds = selectedTags.mapNotNull { tagText ->
                tagTextToIdMap[tagText]
            }

            Log.d("AddItemFragment", "수집된 데이터: category=$category, subcategory=$subcategory, season=$season, color=$color")
            Log.d("AddItemFragment", "선택된 태그: $selectedTags")
            Log.d("AddItemFragment", "변환된 태그 ID들: $tagIds")

            RegisterItemRequestDto(
                category = category,
                subcategory = subcategory,
                season = season,
                color = color,
                brand = brand,
                size = size,
                purchaseDate = purchaseDate,
                image = "", // Repository에서 설정됨
                price = price,
                purchaseSite = purchaseSite,
                tagIds = tagIds
            )
        } catch (e: Exception) {
            Log.e("AddItemFragment", "폼 데이터 수집 실패", e)
            null
        }
    }

    /**
     * 카테고리와 세부카테고리 인덱스를 실제 subcategory ID로 변환
     */
    private fun mapSubcategoryIndex(categoryIndex: Int, subcategoryIndex: Int): Int {
        return when (categoryIndex) {
            0 -> subcategoryIndex + 1  // 상의: 1-8
            1 -> subcategoryIndex + 9  // 하의: 9-15
            2 -> subcategoryIndex + 16 // 원피스: 16-20
            3 -> subcategoryIndex + 21 // 아우터: 21-28
            4 -> subcategoryIndex + 29 // 신발: 29-35
            5 -> subcategoryIndex + 36 // 액세서리: 36-43
            else -> 1
        }
    }

    override fun onResume() {
        super.onResume()
        activity?.findViewById<View>(R.id.bottomNavigationView)?.visibility = View.GONE
    }

    override fun onPause() {
        super.onPause()
        activity?.findViewById<View>(R.id.bottomNavigationView)?.visibility = View.VISIBLE
    }

    // 기존 UI 설정 메서드들은 그대로 유지
    private fun setupDropdowns(view: View) {
        setupCategorySpinner(view)
        // 🔥 MODIFIED: 계절 스피너 수정 (봄ㆍ가을/여름/겨울)
        setupSpinnerWithContainer(view, R.id.spinner_season, arrayOf("봄ㆍ가을", "여름", "겨울"))
        setupSpinnerWithContainer(view, R.id.spinner_color, arrayOf("블랙", "화이트", "그레이", "네이비", "브라운", "베이지", "레드", "핑크", "옐로우", "그린", "블루", "퍼플"))

        // 🔥 NEW: 편집 모드일 때 스피너 초기값 설정
        if (isEditMode) {
            setSpinnerInitialValues(view)
        }
    }

    // 🔥 NEW: 편집 모드일 때 스피너 초기값 설정
    private fun setSpinnerInitialValues(view: View) {
        existingItemData?.let { data ->
            // 카테고리 스피너 설정
            val categoryIndex = when (data.category) {
                1 -> 0 // 상의
                2 -> 1 // 하의
                3 -> 2 // 원피스
                4 -> 3 // 아우터
                5 -> 4 // 신발
                6 -> 5 // 액세서리
                else -> 0
            }
            view.findViewById<Spinner>(R.id.spinner_category)?.setSelection(categoryIndex)

            // 세부 카테고리 스피너 설정 (카테고리 선택 후 설정해야 함)
            view.post {
                val subcategoryIndex = when (data.category) {
                    1 -> (data.subcategory - 1).coerceAtLeast(0) // 상의: 1-8
                    2 -> (data.subcategory - 9).coerceAtLeast(0) // 하의: 9-15
                    3 -> (data.subcategory - 16).coerceAtLeast(0) // 원피스: 16-20
                    4 -> (data.subcategory - 21).coerceAtLeast(0) // 아우터: 21-28
                    5 -> (data.subcategory - 29).coerceAtLeast(0) // 신발: 29-35
                    6 -> (data.subcategory - 36).coerceAtLeast(0) // 액세서리: 36-43
                    else -> 0
                }
                view.findViewById<Spinner>(R.id.spinner_detail_category)?.setSelection(subcategoryIndex)
            }

            // 계절 스피너 설정 (봄ㆍ가을=1, 여름=2, 겨울=4)
            val seasonIndex = when (data.season) {
                1 -> 0 // 봄ㆍ가을
                2 -> 1 // 여름
                4 -> 2 // 겨울
                else -> 0
            }
            view.findViewById<Spinner>(R.id.spinner_season)?.setSelection(seasonIndex)

            // 색상 스피너 설정
            view.findViewById<Spinner>(R.id.spinner_color)?.setSelection((data.color - 1).coerceAtLeast(0))

            Log.d("AddItemFragment", "스피너 초기값 설정: category=$categoryIndex, season=$seasonIndex, color=${data.color - 1}")
        }
    }

    private fun setupCategorySpinner(view: View) {
        val categorySpinner = view.findViewById<Spinner>(R.id.spinner_category)
        val detailCategorySpinner = view.findViewById<Spinner>(R.id.spinner_detail_category)

        val categories = arrayOf("상의", "하의", "원피스", "아우터", "신발", "액세서리")
        setupSpinnerWithContainer(view, R.id.spinner_category, categories)

        val subcategoryMap = mapOf(
            0 to arrayOf("반팔티셔츠", "긴팔티셔츠", "민소매", "셔츠/블라우스", "맨투맨", "후드티", "니트/스웨터", "기타"),
            1 to arrayOf("반바지", "긴바지", "청바지", "트레이닝 팬츠", "레깅스", "스커트", "기타"),
            2 to arrayOf("미니원피스", "롱 원피스", "끈 원피스", "니트 원피스", "기타"),
            3 to arrayOf("바람막이", "가디건", "자켓", "코트", "패딩", "후드집업", "무스탕/퍼", "기타"),
            4 to arrayOf("운동화", "부츠", "샌들", "슬리퍼", "구두", "로퍼", "기타"),
            5 to arrayOf("모자", "머플러", "장갑", "양말", "안경/선글라스", "가방", "시계/팔찌/목걸이", "기타")
        )

        categorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val subcategories = subcategoryMap[position] ?: arrayOf("기타")
                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, subcategories)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                detailCategorySpinner.adapter = adapter
                setupSpinnerWithContainer(requireView(), R.id.spinner_detail_category, subcategories)

                // 🔥 편집 모드일 때 세부 카테고리 선택 유지
                if (isEditMode) {
                    existingItemData?.let { data ->
                        if (data.category == position + 1) { // 현재 선택된 카테고리와 일치하는 경우
                            val subcategoryIndex = when (data.category) {
                                1 -> (data.subcategory - 1).coerceAtLeast(0) // 상의: 1-8
                                2 -> (data.subcategory - 9).coerceAtLeast(0) // 하의: 9-15
                                3 -> (data.subcategory - 16).coerceAtLeast(0) // 원피스: 16-20
                                4 -> (data.subcategory - 21).coerceAtLeast(0) // 아우터: 21-28
                                5 -> (data.subcategory - 29).coerceAtLeast(0) // 신발: 29-35
                                6 -> (data.subcategory - 36).coerceAtLeast(0) // 액세서리: 36-43
                                else -> 0
                            }
                            detailCategorySpinner.post {
                                detailCategorySpinner.setSelection(subcategoryIndex)
                            }
                        }
                    }
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        val initialSubcategories = subcategoryMap[0] ?: arrayOf("기타")
        val initialAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, initialSubcategories)
        initialAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        detailCategorySpinner.adapter = initialAdapter
        setupSpinnerWithContainer(view, R.id.spinner_detail_category, initialSubcategories)
    }

    private fun setupSpinnerWithContainer(view: View, spinnerId: Int, data: Array<String>) {
        val spinner = view.findViewById<Spinner>(spinnerId)
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, data)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner?.adapter = adapter

        val spinnerContainer = spinner?.parent as? LinearLayout
        spinner?.setOnTouchListener { _, _ ->
            spinner.post { adjustDropdownPosition(spinner, spinnerContainer) }
            false
        }
        spinnerContainer?.setOnClickListener { spinner.performClick() }
        spinnerContainer?.let { container ->
            for (i in 0 until container.childCount) {
                val child = container.getChildAt(i)
                if (child is ImageView) {
                    child.setOnClickListener { spinner.performClick() }
                }
            }
        }
    }

    private fun adjustDropdownPosition(spinner: Spinner, spinnerContainer: LinearLayout?) {
        if (spinnerContainer == null) return
        try {
            val popupField = Spinner::class.java.getDeclaredField("mPopup")
            popupField.isAccessible = true
            val popupWindow = popupField.get(spinner) ?: return

            val spinnerLocation = IntArray(2)
            val containerLocation = IntArray(2)
            spinner.getLocationOnScreen(spinnerLocation)
            spinnerContainer.getLocationOnScreen(containerLocation)

            val offsetToContainerLeft = spinnerLocation[0] - containerLocation[0]
            val containerWidth = spinnerContainer.width
            val maxHeight = (250 * resources.displayMetrics.density).toInt()

            val setWidthMethod = popupWindow.javaClass.getMethod("setWidth", Int::class.java)
            setWidthMethod.invoke(popupWindow, containerWidth)

            val setHeightMethod = popupWindow.javaClass.getMethod("setHeight", Int::class.java)
            setHeightMethod.invoke(popupWindow, maxHeight)

            val setHorizontalOffsetMethod = popupWindow.javaClass.getMethod("setHorizontalOffset", Int::class.java)
            setHorizontalOffsetMethod.invoke(popupWindow, -offsetToContainerLeft)
        } catch (e: Exception) {
            Log.e("Spinner", "Failed to adjust dropdown: ${e.message}")
        }
    }

    private fun setupTagButtons(view: View) {
        setupFlexboxLayout(view, R.id.topCategoryLayout1)
        setupFlexboxLayout(view, R.id.topCategoryLayout2)
        setupFlexboxLayout(view, R.id.topCategoryLayout3)
        setupFlexboxLayout(view, R.id.topCategoryLayout4)

        Log.d("AddItemFragment", "태그 버튼 설정 완료")
    }

    /**
     * 🔥 MODIFIED: 태그 선택 로직 강화
     */
    private fun setupFlexboxLayout(view: View, layoutId: Int) {
        val flexboxLayout = view.findViewById<com.google.android.flexbox.FlexboxLayout>(layoutId)
        for (i in 0 until flexboxLayout.childCount) {
            val child = flexboxLayout.getChildAt(i)
            if (child is Button) {
                child.setOnClickListener { button ->
                    button.isSelected = !button.isSelected
                    val tagText = (button as Button).text.toString()

                    if (button.isSelected) {
                        selectedTags.add(tagText)
                        Log.d("TagButton", "태그 추가: $tagText")
                    } else {
                        selectedTags.remove(tagText)
                        Log.d("TagButton", "태그 제거: $tagText")
                    }

                    Log.d("TagButton", "현재 선택된 태그들: $selectedTags")

                    // 🔥 선택된 태그의 ID도 로그로 확인
                    val tagId = tagTextToIdMap[tagText]
                    Log.d("TagButton", "$tagText 의 ID: $tagId")
                }
            }
        }
    }

    /**
     * 🔥 편집 모드에서 기존 태그 선택 상태 복원
     */
    private fun restoreSelectedTags(existingTagIds: List<Int>) {
        // 기존 태그 ID들을 텍스트로 변환
        val idToTagTextMap = tagTextToIdMap.entries.associate { (text, id) -> id to text }

        val existingTagTexts = existingTagIds.mapNotNull { id ->
            idToTagTextMap[id]
        }

        // 선택 상태 복원
        selectedTags.clear()
        selectedTags.addAll(existingTagTexts)

        // UI 상태 복원
        listOf(R.id.topCategoryLayout1, R.id.topCategoryLayout2, R.id.topCategoryLayout3, R.id.topCategoryLayout4).forEach { layoutId ->
            val flexboxLayout = view?.findViewById<com.google.android.flexbox.FlexboxLayout>(layoutId)
            flexboxLayout?.let { layout ->
                for (i in 0 until layout.childCount) {
                    val child = layout.getChildAt(i)
                    if (child is Button) {
                        val tagText = child.text.toString()
                        child.isSelected = existingTagTexts.contains(tagText)
                    }
                }
            }
        }

        Log.d("AddItemFragment", "편집 모드 - 기존 태그 복원: $existingTagTexts")
    }
}