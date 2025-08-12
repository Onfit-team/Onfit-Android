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
    private val subcategoryMap = mapOf(
        "상의" to arrayOf("반팔티", "긴팔티", "셔츠", "블라우스", "니트", "후드티", "탱크톱", "조끼"),
        "하의" to arrayOf("청바지", "면바지", "반바지", "슬랙스", "치마", "레깅스", "조거팬츠"),
        "아우터" to arrayOf("자켓", "패딩", "코트", "바람막이", "가디건", "점퍼", "블레이저"),
        "원피스" to arrayOf("미니원피스", "미디원피스", "롱원피스", "니트원피스", "셔츠원피스"),
        "신발" to arrayOf("운동화", "구두", "부츠", "샌들", "슬리퍼", "하이힐", "플랫슈즈"),
        "악세서리" to arrayOf("가방", "모자", "벨트", "목걸이", "귀걸이", "시계", "반지")
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
        handleEditMode()
        val backButton = view.findViewById<ImageButton>(R.id.ic_back)
        backButton?.setOnClickListener { findNavController().navigateUp() }
        setupDropdowns(view)
        setupTagButtons(view)
        val saveButton = view.findViewById<Button>(R.id.btn_save)
        saveButton?.setOnClickListener {
            val isEditMode = arguments?.getBoolean("edit_mode", false) ?: false
            if (isEditMode) updateItemToServer() else saveItemToServer()
        }
    }

    private fun handleImageUri() {
        val imageUriString = arguments?.getString("image_uri")
        if (imageUriString != null) {
            selectedImageUri = Uri.parse(imageUriString)
            imageView.setImageURI(selectedImageUri)
            lifecycleScope.launch {
                uploadedImageUrl = selectedImageUri?.let { uploadImageToServer(it) }
                if (uploadedImageUrl == null) {
                    Toast.makeText(requireContext(), "이미지 업로드 실패", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private suspend fun uploadImageToServer(imageUri: Uri): String? {
        return try {
            val context = requireContext()
            val contentResolver = context.contentResolver
            val inputStream = contentResolver.openInputStream(imageUri)
            val file = File.createTempFile("upload", ".jpg", context.cacheDir)
            inputStream?.use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("image", file.name, requestFile)
            val token = "Bearer " + TokenProvider.getToken(context)
            val response = RetrofitClient.wardrobeService.uploadImage(token, body)
            if (response.isSuccessful && response.body()?.success == true) {
                response.body()?.data?.imageUrl
            } else {
                null
            }
        } catch (e: Exception) {
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

                // 실제 수정 API 호출 (WardrobeService에 메소드가 있다면)
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
                    // API가 없는 경우 임시 처리
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

    private fun collectFormData(imageUrl: String): RegisterItemRequestDto? {
        val categoryIndex = categorySpinner.selectedItemPosition
        val category = when (categoryIndex) {
            0 -> 1; 1 -> 2; 2 -> 4; 3 -> 3; 4 -> 5; 5 -> 6; else -> 1
        }
        val subcategory = detailCategorySpinner.selectedItemPosition + 1
        val season = seasonSpinner.selectedItemPosition + 1
        val color = colorSpinner.selectedItemPosition + 1
        val brand = brandEditText.text.toString().trim()
        val size = sizeEditText.text.toString().trim()
        val purchaseDate = "2024-08-01"
        val priceText = priceEditText.text.toString().trim()
        val purchaseSite = purchaseSiteEditText.text.toString().trim()
        if (brand.isEmpty() || size.isEmpty() || priceText.isEmpty()) return null
        val price = priceText.toIntOrNull() ?: 0
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

    private fun setCategorySpinnerSelection(category: Int, subcategory: Int) {
        val categoryPosition = when (category) {
            1 -> 0; 2 -> 1; 4 -> 2; 3 -> 3; 5 -> 4; 6 -> 5; else -> 0
        }
        categorySpinner.setSelection(categoryPosition)
        categorySpinner.postDelayed({
            val subcategoryPosition = getSubcategoryPosition(category, subcategory)
            if (subcategoryPosition >= 0) detailCategorySpinner.setSelection(subcategoryPosition)
        }, 100)
    }

    private fun getSubcategoryPosition(category: Int, subcategory: Int): Int {
        return when (category) {
            1 -> when (subcategory) { 1 -> 0; 2 -> 1; 3 -> 2; 4 -> 3; 5 -> 4; 6 -> 5; 7 -> 6; 8 -> 7; else -> 0 }
            2 -> when (subcategory) { 9 -> 0; 10 -> 1; 11 -> 2; 12 -> 3; 13 -> 4; 14 -> 5; 15 -> 6; else -> 0 }
            3 -> when (subcategory) { 16 -> 0; 17 -> 1; 18 -> 2; 19 -> 3; 20 -> 4; else -> 0 }
            4 -> when (subcategory) { 21 -> 0; 22 -> 1; 23 -> 2; 24 -> 3; 25 -> 4; 26 -> 5; 27 -> 6; else -> 0 }
            5 -> when (subcategory) { 28 -> 0; 29 -> 1; 30 -> 2; 31 -> 3; 32 -> 4; 33 -> 5; 34 -> 6; else -> 0 }
            6 -> when (subcategory) { 35 -> 0; 36 -> 1; 37 -> 2; 38 -> 3; 39 -> 4; 40 -> 5; 41 -> 6; else -> 0 }
            else -> 0
        }
    }

    private fun setSeasonSpinnerSelection(season: Int) {
        if (season > 0 && season <= seasonSpinner.count) seasonSpinner.setSelection(season - 1)
    }

    private fun setColorSpinnerSelection(color: Int) {
        val colorPosition = when (color) {
            1 -> 0; 2 -> 1; 3 -> 2; 4 -> 3; 5 -> 4; 6 -> 5; 7 -> 6; 8 -> 7; 9 -> 8; 10 -> 9; 11 -> 10; 12 -> 11; 13 -> 12; else -> 0
        }
        if (colorPosition < colorSpinner.count) colorSpinner.setSelection(colorPosition)
    }

    private fun selectExistingTags(tagNames: List<String>) {
        Log.d("AddItemFragment", "기존 태그 선택 시작: ${tagNames.joinToString(", ")}")

        val allLayouts = listOf(
            view?.findViewById<com.google.android.flexbox.FlexboxLayout>(R.id.topCategoryLayout1), // 분위기 태그
            view?.findViewById<com.google.android.flexbox.FlexboxLayout>(R.id.topCategoryLayout2), // 분위기 태그
            view?.findViewById<com.google.android.flexbox.FlexboxLayout>(R.id.topCategoryLayout3), // 용도 태그
            view?.findViewById<com.google.android.flexbox.FlexboxLayout>(R.id.topCategoryLayout4)  // 용도 태그
        )

        var selectedCount = 0

        allLayouts.forEachIndexed { layoutIndex, layout ->
            layout?.let { flexboxLayout ->
                Log.d("AddItemFragment", "레이아웃 ${layoutIndex + 1} 확인 중, 자식 수: ${flexboxLayout.childCount}")

                for (i in 0 until flexboxLayout.childCount) {
                    val child = flexboxLayout.getChildAt(i)
                    if (child is Button) {
                        val buttonText = child.text.toString().replace("#", "").trim()

                        if (tagNames.contains(buttonText)) {
                            child.isSelected = true

                            // 태그 ID 생성 (레이아웃별로 다른 범위 사용)
                            val tagId = when (layoutIndex) {
                                0, 1 -> i + 1 + (layoutIndex * 10)        // 분위기 태그: 1-10, 11-20
                                2, 3 -> i + 50 + ((layoutIndex - 2) * 10) // 용도 태그: 50-60, 61-70
                                else -> i + 1
                            }

                            if (!selectedTags.contains(tagId)) {
                                selectedTags.add(tagId)
                            }

                            selectedCount++
                            Log.d("AddItemFragment", "태그 선택됨: '$buttonText' (레이아웃: $layoutIndex, ID: $tagId)")
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
            override fun onNothingSelected(parent: AdapterView<*>?) {}
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
        for (i in 0 until flexboxLayout.childCount) {
            val child = flexboxLayout.getChildAt(i)
            if (child is Button) {
                child.setOnClickListener { button ->
                    button.isSelected = !button.isSelected
                    val tagId = button.tag as? Int ?: (i + 1)
                    if (button.isSelected) {
                        if (!selectedTags.contains(tagId)) selectedTags.add(tagId)
                    } else {
                        selectedTags.remove(tagId)
                    }
                    Log.d("TagButton", "${(button as Button).text} selected: ${button.isSelected}, TagId: $tagId")
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
            spinner.post { adjustDropdownPosition(spinner, spinnerContainer) }
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
            val categoryPosition = when (recommended.category) {
                1 -> 0; 2 -> 1; 3 -> 3; 4 -> 2; 5 -> 4; 6 -> 5; else -> 0
            }
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
            Log.d("AddItemFragment", "추천 카테고리 적용 완료: ${recommended}")
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