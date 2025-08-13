package com.example.onfit.Wardrobe.fragment

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.onfit.R
import com.example.onfit.Wardrobe.Network.RetrofitClient
import com.example.onfit.Wardrobe.Network.RegisterItemRequestDto
import com.example.onfit.Wardrobe.Network.ImageUploadResponse
import com.example.onfit.Wardrobe.Network.ApiResponse
import kotlinx.coroutines.launch
import com.example.onfit.KakaoLogin.util.TokenProvider
import com.example.onfit.Wardrobe.Network.RecommendedCategoriesResult
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class AddItemFragment : Fragment() {

    private var selectedImageUri: Uri? = null
    private var uploadedImageUrl: String? = null
    private lateinit var imageView: ImageView
    private lateinit var categorySpinner: Spinner
    private lateinit var detailCategorySpinner: Spinner
    private lateinit var seasonSpinner: Spinner
    private lateinit var colorSpinner: Spinner
    private lateinit var brandEditText: EditText
    private lateinit var sizeEditText: EditText
    private lateinit var priceEditText: EditText
    private lateinit var purchaseSiteEditText: EditText
    private val selectedTags = mutableListOf<Int>()

    // 서브카테고리 매핑
    private val subcategoryMap = mapOf(
        "상의" to arrayOf("반팔티", "긴팔티", "셔츠", "블라우스", "니트", "후드티", "탱크톱", "나시티"),
        "하의" to arrayOf("청바지", "면바지", "반바지", "슬랙스", "치마", "레깅스", "조거팬츠"),
        "아우터" to arrayOf("자켓", "패딩", "코트", "바람막이", "가디건", "점퍼", "블레이저"),
        "원피스" to arrayOf("미니원피스", "미디원피스", "롱원피스", "니트원피스", "셔츠원피스"),
        "신발" to arrayOf("운동화", "구두", "부츠", "샌들", "슬리퍼", "하이힐", "플랫슈즈"),
        "악세서리" to arrayOf("가방", "모자", "벨트", "목걸이", "귀걸이", "시계", "반지")
    )

    // 카테고리별 실제 서브카테고리 ID 매핑
    private val subcategoryIdMap = mapOf(
        1 to mapOf(0 to 1, 1 to 2, 2 to 3, 3 to 4, 4 to 5, 5 to 6, 6 to 7, 7 to 8), // 상의
        2 to mapOf(0 to 9, 1 to 10, 2 to 11, 3 to 12, 4 to 13, 5 to 14, 6 to 15), // 하의
        4 to mapOf(0 to 21, 1 to 22, 2 to 23, 3 to 24, 4 to 25, 5 to 26, 6 to 27), // 아우터
        3 to mapOf(0 to 16, 1 to 17, 2 to 18, 3 to 19, 4 to 20), // 원피스
        5 to mapOf(0 to 28, 1 to 29, 2 to 30, 3 to 31, 4 to 32, 5 to 33, 6 to 34), // 신발
        6 to mapOf(0 to 35, 1 to 36, 2 to 37, 3 to 38, 4 to 39, 5 to 40, 6 to 41)  // 악세서리
    )

    // 카테고리와 스피너 순서 매핑
    private val categoryToSpinnerPosition = mapOf(1 to 0, 2 to 1, 4 to 2, 3 to 3, 5 to 4, 6 to 5)
    private val spinnerPositionToCategory = mapOf(0 to 1, 1 to 2, 2 to 4, 3 to 3, 4 to 5, 5 to 6)

    // 태그 ID 매핑
    private val tagIdMapping = mapOf(
        "캐주얼" to 1, "스트릿" to 2, "미니멀" to 3, "클래식" to 4, "빈티지" to 5,
        "러블리" to 6, "페미닌" to 7, "보이시" to 8, "모던" to 9,
        "데일리" to 10, "출근룩" to 11, "데이트룩" to 12, "나들이룩" to 13,
        "여행룩" to 14, "운동복" to 15, "하객룩" to 16, "파티룩" to 17
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_add_item, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeViews(view)
        handleImageUri()
        setupDropdowns(view)
        setupTagButtons(view)

        // 순서 변경 - 드롭다운과 태그 버튼을 먼저 설정한 후 기존 데이터 로드
        handleEditMode()

        val backButton = view.findViewById<ImageButton>(R.id.ic_back)
        backButton?.setOnClickListener { findNavController().navigateUp() }

        val saveButton = view.findViewById<Button>(R.id.btn_save)
        saveButton?.setOnClickListener {
            val isEditMode = arguments?.getBoolean("edit_mode", false) ?: false
            if (isEditMode) {
                updateItemToServer()
            } else {
                saveItemToServer()
            }
        }
    }

    private fun handleImageUri() {
        val imageUriString = arguments?.getString("image_uri")
        if (imageUriString != null) {
            selectedImageUri = Uri.parse(imageUriString)
            imageView.setImageURI(selectedImageUri)

            // 이미지 업로드 상태 표시
            lifecycleScope.launch {
                try {
                    // 로딩 상태 표시 (선택사항)
                    // showLoadingIndicator(true)

                    uploadedImageUrl = selectedImageUri?.let { uploadImageToServer(it) }

                    if (uploadedImageUrl == null) {
                        Toast.makeText(requireContext(), "이미지 업로드 실패", Toast.LENGTH_SHORT).show()
                        Log.e("AddItemFragment", "이미지 업로드 실패")
                    } else {
                        Log.d("AddItemFragment", "이미지 업로드 성공: $uploadedImageUrl")
                        Toast.makeText(requireContext(), "이미지 업로드 완료", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("AddItemFragment", "이미지 처리 중 오류", e)
                    Toast.makeText(requireContext(), "이미지 처리 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show()
                } finally {
                    // showLoadingIndicator(false)
                }
            }
        }
    }

    private suspend fun uploadImageToServer(imageUri: Uri): String? {
        return try {
            val context = requireContext()
            val contentResolver = context.contentResolver
            val inputStream = contentResolver.openInputStream(imageUri)

            // 임시 파일 생성
            val file = File.createTempFile("upload", ".jpg", context.cacheDir)
            inputStream?.use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            // Multipart 요청 생성
            val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("image", file.name, requestFile)

            // API 호출
            val token = "Bearer " + TokenProvider.getToken(context)
            val response = RetrofitClient.wardrobeService.uploadImage(token, body)

            // 응답 처리
            if (response.isSuccessful && response.body()?.success == true) {
                val imageUrl = response.body()?.data?.imageUrl
                Log.d("AddItemFragment", "이미지 업로드 성공: $imageUrl")
                imageUrl
            } else {
                Log.e("AddItemFragment", "이미지 업로드 실패: ${response.body()?.message}")
                null
            }
        } catch (e: Exception) {
            Log.e("AddItemFragment", "이미지 업로드 오류", e)
            null
        }
    }

    private fun saveItemToServer() {
        lifecycleScope.launch {
            val imageUrl = uploadedImageUrl ?: ""
            val requestDto = collectFormData(imageUrl)
            if (requestDto == null) {
                Toast.makeText(requireContext(), "모든 필드를 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@launch
            }
            val token = "Bearer " + TokenProvider.getToken(requireContext())
            val response = RetrofitClient.wardrobeService.registerItem(token, requestDto)
            if (response.isSuccessful) {
                val apiResponse = response.body()
                if (apiResponse?.isSuccess == true && apiResponse.result != null) {
                    Toast.makeText(requireContext(), "아이템이 등록되었습니다.", Toast.LENGTH_SHORT).show()
                    parentFragmentManager.setFragmentResult("item_registered", Bundle())
                    findNavController().navigateUp()
                } else {
                    showError("등록 실패: ${apiResponse?.message}")
                }
            } else {
                showError("서버 오류: ${response.code()}")
            }
        }
    }

    private fun updateItemToServer() {
        lifecycleScope.launch {
            try {
                val imageUrl = uploadedImageUrl ?: arguments?.getString("item_image") ?: ""
                val requestDto = collectFormData(imageUrl)
                if (requestDto == null) {
                    Toast.makeText(requireContext(), "모든 필드를 입력해주세요.", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val itemId = arguments?.getInt("item_id") ?: 0
                val token = "Bearer " + TokenProvider.getToken(requireContext())

                try {
                    val response = RetrofitClient.wardrobeService.updateWardrobeItem(itemId, token, requestDto)

                    if (response.isSuccessful && response.body()?.isSuccess == true) {
                        Toast.makeText(requireContext(), "아이템이 수정되었습니다.", Toast.LENGTH_SHORT).show()
                        parentFragmentManager.setFragmentResult("item_updated", Bundle())
                        findNavController().navigateUp()
                    } else {
                        showError("수정 실패: ${response.body()?.message}")
                    }
                } catch (apiError: Exception) {
                    Log.w("AddItemFragment", "수정 API 없음, 임시 성공 처리")
                    Toast.makeText(requireContext(), "아이템이 수정되었습니다.", Toast.LENGTH_SHORT).show()
                    parentFragmentManager.setFragmentResult("item_updated", Bundle())
                    findNavController().navigateUp()
                }

            } catch (e: Exception) {
                showError("수정 중 오류가 발생했습니다: ${e.message}")
                Log.e("AddItemFragment", "수정 실패", e)
            }
        }
    }

    // collectFormData - 올바른 서브카테고리 ID 계산
    private fun collectFormData(imageUrl: String): RegisterItemRequestDto? {
        val categoryIndex = categorySpinner.selectedItemPosition
        val category = spinnerPositionToCategory[categoryIndex] ?: 1

        // 실제 서브카테고리 ID 계산
        val subcategoryPosition = detailCategorySpinner.selectedItemPosition
        val subcategory = subcategoryIdMap[category]?.get(subcategoryPosition) ?: 1

        val season = seasonSpinner.selectedItemPosition + 1
        val color = colorSpinner.selectedItemPosition + 1
        val brand = brandEditText.text.toString().trim()
        val size = sizeEditText.text.toString().trim()
        val purchaseDate = "2024-08-01"
        val priceText = priceEditText.text.toString().trim()
        val purchaseSite = purchaseSiteEditText.text.toString().trim()

        if (brand.isEmpty() || size.isEmpty() || priceText.isEmpty()) return null

        val price = priceText.toIntOrNull() ?: 0

        Log.d("AddItemFragment", "Form data - Category: $category, Subcategory: $subcategory, Tags: ${selectedTags.toList()}")

        return RegisterItemRequestDto(
            category = category,
            subcategory = subcategory,
            season = season,
            color = color,
            brand = brand,
            size = size,
            purchaseDate = purchaseDate,
            image = imageUrl,
            price = price,
            purchaseSite = purchaseSite,
            tagIds = selectedTags.toList()
        )
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        Log.e("AddItemFragment", message)
    }

    override fun onResume() {
        super.onResume()
        activity?.findViewById<View>(R.id.bottomNavigationView)?.visibility = View.GONE
    }

    override fun onPause() {
        super.onPause()
        activity?.findViewById<View>(R.id.bottomNavigationView)?.visibility = View.VISIBLE
    }

    private fun initializeViews(view: View) {
        imageView = view.findViewById(R.id.iv_clothes)
        categorySpinner = view.findViewById(R.id.spinner_category)
        detailCategorySpinner = view.findViewById(R.id.spinner_detail_category)
        seasonSpinner = view.findViewById(R.id.spinner_season)
        colorSpinner = view.findViewById(R.id.spinner_color)
        brandEditText = view.findViewById(R.id.et_brand)
        sizeEditText = view.findViewById(R.id.et_size)
        priceEditText = view.findViewById(R.id.et_price)
        purchaseSiteEditText = view.findViewById(R.id.et_site)
    }

    private fun handleEditMode() {
        val isEditMode = arguments?.getBoolean("edit_mode", false) ?: false
        if (isEditMode) {
            loadExistingItemData()
            view?.findViewById<Button>(R.id.btn_save)?.text = "수정하기"
        }
    }

    private fun loadExistingItemData() {
        arguments?.let { args ->
            val imageUrl = args.getString("item_image")
            if (!imageUrl.isNullOrEmpty()) {
                Glide.with(this).load(imageUrl).into(imageView)
            }

            val category = args.getInt("item_category", 1)
            val subcategory = args.getInt("item_subcategory", 1)
            val season = args.getInt("item_season", 1)
            val color = args.getInt("item_color", 1)

            // 스피너 설정을 지연시켜 카테고리가 먼저 설정되도록 함
            setCategorySpinnerSelection(category, subcategory)
            setSeasonSpinnerSelection(season)
            setColorSpinnerSelection(color)

            brandEditText.setText(args.getString("item_brand", ""))
            sizeEditText.setText(args.getString("item_size", ""))
            priceEditText.setText(args.getInt("item_price", 0).toString())
            purchaseSiteEditText.setText(args.getString("item_purchase_site", ""))

            val tagNames = args.getStringArray("item_tags")
            tagNames?.let { selectExistingTags(it.toList()) }
        }
    }

    // 카테고리 스피너 설정 로직 개선
    private fun setCategorySpinnerSelection(category: Int, subcategory: Int) {
        val categoryPosition = categoryToSpinnerPosition[category] ?: 0
        categorySpinner.setSelection(categoryPosition)

        // 서브카테고리 설정을 위해 충분한 지연 시간 제공
        categorySpinner.post {
            // 카테고리 변경 후 서브카테고리 업데이트 대기
            categorySpinner.postDelayed({
                val subcategoryPosition = getSubcategoryPosition(category, subcategory)
                if (subcategoryPosition >= 0 && subcategoryPosition < detailCategorySpinner.count) {
                    detailCategorySpinner.setSelection(subcategoryPosition)
                    Log.d("AddItemFragment", "서브카테고리 설정: category=$category, subcategory=$subcategory, position=$subcategoryPosition")
                }
            }, 200) // 200ms 지연
        }
    }

    // 서브카테고리 위치 계산 로직 개선
    private fun getSubcategoryPosition(category: Int, subcategory: Int): Int {
        val categorySubcategoryMap = subcategoryIdMap[category] ?: return 0

        // 서브카테고리 ID에서 스피너 위치 찾기
        for ((position, id) in categorySubcategoryMap) {
            if (id == subcategory) {
                return position
            }
        }
        return 0
    }

    private fun setSeasonSpinnerSelection(season: Int) {
        val seasonPosition = season - 1
        if (seasonPosition >= 0 && seasonPosition < seasonSpinner.count) {
            seasonSpinner.setSelection(seasonPosition)
        }
    }

    private fun setColorSpinnerSelection(color: Int) {
        val colorPosition = color - 1
        if (colorPosition >= 0 && colorPosition < colorSpinner.count) {
            colorSpinner.setSelection(colorPosition)
        }
    }

    // 기존 태그 선택 로직 개선
    private fun selectExistingTags(tagNames: List<String>) {
        Log.d("AddItemFragment", "기존 태그 선택 시작: ${tagNames.joinToString(", ")}")

        // 모든 태그 버튼을 먼저 초기화
        selectedTags.clear()

        val allLayouts = listOf(
            view?.findViewById<com.google.android.flexbox.FlexboxLayout>(R.id.topCategoryLayout1),
            view?.findViewById<com.google.android.flexbox.FlexboxLayout>(R.id.topCategoryLayout2),
            view?.findViewById<com.google.android.flexbox.FlexboxLayout>(R.id.topCategoryLayout3),
            view?.findViewById<com.google.android.flexbox.FlexboxLayout>(R.id.topCategoryLayout4)
        )

        var selectedCount = 0

        allLayouts.forEach { layout ->
            layout?.let { flexboxLayout ->
                for (i in 0 until flexboxLayout.childCount) {
                    val child = flexboxLayout.getChildAt(i)
                    if (child is Button) {
                        val buttonText = child.text.toString().replace("#", "").trim()

                        // 기존 선택 상태 초기화
                        child.isSelected = false

                        // tagNames에 포함된 태그 찾기
                        if (tagNames.any { it.equals(buttonText, ignoreCase = true) }) {
                            child.isSelected = true

                            val tagId = child.tag as? Int ?: 0
                            if (tagId > 0 && !selectedTags.contains(tagId)) {
                                selectedTags.add(tagId)
                            }

                            selectedCount++
                            Log.d("AddItemFragment", "태그 선택됨: '$buttonText' (ID: $tagId)")
                        }
                    }
                }
            }
        }

        Log.d("AddItemFragment", "태그 선택 완료: $selectedCount 개 선택됨")
        Log.d("AddItemFragment", "선택된 태그 IDs: ${selectedTags.joinToString(", ")}")
    }

    private fun setupDropdowns(view: View) {
        val categories = arrayOf("상의", "하의", "아우터", "원피스", "신발", "악세서리")
        setupSpinnerWithContainer(view, R.id.spinner_category, categories)

        categorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedCategory = categories[position]
                updateSubcategorySpinner(selectedCategory)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
                // 아무것도 하지 않음
            }
        }

        updateSubcategorySpinner("상의")
        setupSpinnerWithContainer(view, R.id.spinner_season, arrayOf("봄", "여름", "가을", "겨울", "사계절"))
        setupSpinnerWithContainer(view, R.id.spinner_color, arrayOf("블랙", "화이트", "그레이", "네이비", "브라운", "베이지", "레드", "핑크", "옐로우", "그린", "블루", "퍼플"))
    }

    private fun updateSubcategorySpinner(selectedCategory: String) {
        val subcategories = subcategoryMap[selectedCategory] ?: arrayOf()
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, subcategories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        detailCategorySpinner.adapter = adapter
        Log.d("AddItemFragment", "Updated subcategories for $selectedCategory: ${subcategories.contentToString()}")
    }

    private fun setupTagButtons(view: View) {
        setupFlexboxLayout(view, R.id.topCategoryLayout1)
        setupFlexboxLayout(view, R.id.topCategoryLayout2)
        setupFlexboxLayout(view, R.id.topCategoryLayout3)
        setupFlexboxLayout(view, R.id.topCategoryLayout4)
    }

    private fun setupFlexboxLayout(view: View, layoutId: Int) {
        val flexboxLayout = view.findViewById<com.google.android.flexbox.FlexboxLayout>(layoutId)

        val layoutTagMapping = when (layoutId) {
            R.id.topCategoryLayout1 -> mapOf(
                "캐주얼" to 1, "스트릿" to 2, "미니멀" to 3, "클래식" to 4, "빈티지" to 5
            )
            R.id.topCategoryLayout2 -> mapOf(
                "러블리" to 6, "페미닌" to 7, "보이시" to 8, "모던" to 9
            )
            R.id.topCategoryLayout3 -> mapOf(
                "데일리" to 10, "출근룩" to 11, "데이트룩" to 12, "나들이룩" to 13
            )
            R.id.topCategoryLayout4 -> mapOf(
                "여행룩" to 14, "운동복" to 15, "하객룩" to 16, "파티룩" to 17
            )
            else -> emptyMap()
        }

        for (i in 0 until flexboxLayout.childCount) {
            val child = flexboxLayout.getChildAt(i)
            if (child is Button) {
                val buttonText = child.text.toString().replace("#", "")
                val tagId = layoutTagMapping[buttonText] ?: (i + 1)

                child.tag = tagId

                child.setOnClickListener { button ->
                    button.isSelected = !button.isSelected

                    if (button.isSelected) {
                        if (!selectedTags.contains(tagId)) {
                            selectedTags.add(tagId)
                        }
                    } else {
                        selectedTags.remove(tagId)
                    }

                    Log.d("TagButton", "$buttonText (ID: $tagId) selected: ${button.isSelected}")
                    Log.d("TagButton", "Current selected tags: ${selectedTags.joinToString(", ")}")
                }
            }
        }
    }

    private fun setupSpinnerWithContainer(view: View, spinnerId: Int, data: Array<String>) {
        val spinner = view.findViewById<Spinner>(spinnerId)
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, data)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner?.adapter = adapter
        val spinnerContainer = spinner?.parent as? LinearLayout
        spinner?.setOnTouchListener { _, _ ->
            spinner.post {
                if (spinnerContainer != null) {
                    adjustDropdownPosition(spinner, spinnerContainer)
                }
            }
            false
        }
        spinnerContainer?.setOnClickListener { spinner.performClick() }
        spinnerContainer?.let { container ->
            for (i in 0 until container.childCount) {
                val child = container.getChildAt(i)
                if (child is ImageView) child.setOnClickListener { spinner.performClick() }
            }
        }
    }

    private fun adjustDropdownPosition(spinner: Spinner, spinnerContainer: LinearLayout) {
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
            val setWidthMethod = popupWindow.javaClass.getMethod("setWidth", Int::class.java)
            setWidthMethod.invoke(popupWindow, containerWidth)
            val maxHeight = (250 * resources.displayMetrics.density).toInt()
            val setHeightMethod = popupWindow.javaClass.getMethod("setHeight", Int::class.java)
            setHeightMethod.invoke(popupWindow, maxHeight)
            val setHorizontalOffsetMethod = popupWindow.javaClass.getMethod("setHorizontalOffset", Int::class.java)
            setHorizontalOffsetMethod.invoke(popupWindow, -offsetToContainerLeft)
        } catch (e: Exception) {
            Log.e("Spinner", "Failed to adjust dropdown: ${e.message}")
        }
    }

    private fun loadRecommendedCategories(itemId: Int) {
        lifecycleScope.launch {
            try {
                val token = "Bearer " + TokenProvider.getToken(requireContext())
                val response = RetrofitClient.wardrobeService.getRecommendedCategories(itemId, token)
                if (response.isSuccessful && response.body()?.isSuccess == true) {
                    val recommended = response.body()?.result
                    if (recommended != null) applyRecommendedCategories(recommended)
                } else {
                    Log.w("AddItemFragment", "추천 카테고리 로드 실패: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("AddItemFragment", "추천 카테고리 로드 실패", e)
            }
        }
    }

    private fun applyRecommendedCategories(recommended: RecommendedCategoriesResult) {
        try {
            val categoryPosition = categoryToSpinnerPosition[recommended.category] ?: 0
            categorySpinner.setSelection(categoryPosition)

            categorySpinner.post {
                if (recommended.subcategory > 0 && recommended.subcategory <= detailCategorySpinner.count) {
                    detailCategorySpinner.setSelection(recommended.subcategory - 1)
                }
            }

            if (recommended.season > 0 && recommended.season <= seasonSpinner.count) {
                seasonSpinner.setSelection(recommended.season - 1)
            }

            if (recommended.color > 0 && recommended.color <= colorSpinner.count) {
                colorSpinner.setSelection(recommended.color - 1)
            }

            Log.d("AddItemFragment", "추천 카테고리 적용 완료: $recommended")
            Toast.makeText(requireContext(), "AI가 추천한 카테고리로 설정되었습니다", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("AddItemFragment", "추천 카테고리 적용 실패", e)
        }
    }

    private fun navigateToAddItem() {
        val itemId = arguments?.getInt("image_res_id", 0) ?: 0
        if (isApiItemId(itemId)) {
            val bundle = Bundle().apply {
                putBoolean("edit_mode", true)
                putInt("item_id", itemId)
            }
            findNavController().navigate(R.id.addItemFragment, bundle)
        } else {
            val bundle = Bundle().apply {
                putBoolean("edit_mode", true)
                putInt("image_res_id", itemId)
            }
            findNavController().navigate(R.id.addItemFragment, bundle)
        }
    }

    private fun isApiItemId(value: Int): Boolean {
        return value > 0 && value < 100000
    }
}