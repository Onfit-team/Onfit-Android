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
import com.example.onfit.Wardrobe.repository.WardrobeRepository
import com.example.onfit.Wardrobe.Network.RegisterItemRequestDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class AddItemFragment : Fragment() {

    // Repository
    private lateinit var repository: WardrobeRepository

    // 이미지 관련 변수들
    private lateinit var ivClothes: ImageView
    private lateinit var tvTitle: TextView
    private var selectedImageUri: Uri? = null

    // 편집 모드 관련 변수들
    private var isEditMode = false
    private var itemId = -1

    // 선택된 태그들을 저장할 리스트
    private val selectedTags = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Repository 초기화 (토큰 설정은 Repository에서 처리)
        repository = WardrobeRepository(requireContext())

        // 🔥 디버그용 토큰 정보 확인
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

        // 🔥 Repository를 사용한 저장 로직
        val saveButton = view.findViewById<Button>(R.id.btn_save)
        saveButton?.setOnClickListener {
            saveItemToWardrobeWithRepository()
        }
    }

    private fun initViews(view: View) {
        ivClothes = view.findViewById(R.id.iv_clothes)
        tvTitle = view.findViewById(R.id.tv_title)
    }

    private fun setupArguments() {
        arguments?.let { bundle ->
            // 편집 모드 확인
            isEditMode = bundle.getBoolean("edit_mode", false)
            itemId = bundle.getInt("item_id", -1)

            Log.d("AddItemFragment", "편집 모드: $isEditMode, 아이템 ID: $itemId")
        }
    }

    /**
     * 🔥 사용자가 선택한 원본 이미지 표시
     */
    private fun setupImageDisplay() {
        arguments?.let { bundle ->
            when {
                // 1. 편집 모드인 경우
                isEditMode -> {
                    val itemImage = bundle.getString("item_image")
                    tvTitle.text = "아이템 정보를 수정해주세요"
                    if (!itemImage.isNullOrEmpty()) {
                        loadImageIntoView(itemImage)
                    }
                }

                // 2. 새 아이템 추가 - 이미지 URI가 있는 경우
                bundle.containsKey("image_uri") -> {
                    val imageUriString = bundle.getString("image_uri")
                    if (!imageUriString.isNullOrEmpty()) {
                        selectedImageUri = Uri.parse(imageUriString)
                        ivClothes.setImageURI(selectedImageUri)
                        tvTitle.text = "선택한 이미지로\n아이템을 등록해주세요!"
                    }
                }

                else -> {
                    // 기본 이미지 표시
                    ivClothes.setImageResource(R.drawable.clothes8)
                    tvTitle.text = "새 아이템을 등록해주세요"
                }
            }
        }
    }

    private fun loadImageIntoView(imageUrl: String) {
        if (imageUrl.startsWith("http")) {
            // 네트워크 이미지
            Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.clothes8)
                .error(R.drawable.clothes1)
                .into(ivClothes)
        } else {
            // 로컬 이미지나 URI
            try {
                val uri = Uri.parse(imageUrl)
                ivClothes.setImageURI(uri)
                selectedImageUri = uri
            } catch (e: Exception) {
                ivClothes.setImageResource(R.drawable.clothes8)
            }
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

        // 이미지 검증
        if (selectedImageUri == null && !isEditMode) {
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
                    registerNewItemWithRepository(formData)
                }

            } catch (e: Exception) {
                handleError(e, "저장 중 오류가 발생했습니다")
            } finally {
                showLoading(false)
            }
        }
    }

    /**
     * 🔥 새 아이템 등록 (Repository 사용)
     */
    private suspend fun registerNewItemWithRepository(formData: RegisterItemRequestDto) {
        try {
            // 1. 이미지 업로드
            val imageUrl = if (selectedImageUri != null) {
                Log.d("AddItemFragment", "이미지 업로드 시작: $selectedImageUri")

                repository.uploadImage(selectedImageUri!!)
                    .onSuccess { url ->
                        Log.d("AddItemFragment", "이미지 업로드 성공: $url")
                    }
                    .onFailure { exception ->
                        Log.e("AddItemFragment", "이미지 업로드 실패: ${exception.message}")
                        throw exception
                    }
                    .getOrThrow()
            } else {
                throw Exception("이미지가 선택되지 않았습니다")
            }

            // 2. 아이템 등록
            val finalRequest = formData.copy(image = imageUrl)
            repository.registerItem(finalRequest)
                .onSuccess { result ->
                    Log.d("AddItemFragment", "아이템 등록 성공: ${result.itemId}")

                    withContext(Dispatchers.Main) {
                        // 성공 결과 전달
                        notifyRegistrationComplete(true, formData.purchaseDate)

                        Toast.makeText(requireContext(), "새 아이템이 추가되었습니다", Toast.LENGTH_SHORT).show()
                        findNavController().navigateUp()
                    }
                }
                .onFailure { exception ->
                    Log.e("AddItemFragment", "아이템 등록 실패: ${exception.message}")
                    throw exception
                }

        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                notifyRegistrationComplete(false, null)
                handleError(e, "아이템 등록에 실패했습니다")
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
                        notifyRegistrationComplete(true, formData.purchaseDate)
                        Toast.makeText(requireContext(), "아이템이 수정되었습니다", Toast.LENGTH_SHORT).show()
                        findNavController().navigateUp()
                    }
                }
                .onFailure { exception ->
                    throw exception
                }

        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                notifyRegistrationComplete(false, null)
                handleError(e, "아이템 수정에 실패했습니다")
            }
        }
    }

    /**
     * 🔥 등록 결과를 부모 Fragment들에게 전달
     */
    private fun notifyRegistrationComplete(isSuccess: Boolean, purchaseDate: String?) {
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
        }

        val resultKey = if (isEditMode) "wardrobe_item_updated" else "item_registered"
        parentFragmentManager.setFragmentResult(resultKey, wardrobeBundle)

        // CalendarFragment에 직접 알림 (새 등록인 경우에만)
        if (!isEditMode && isSuccess) {
            parentFragmentManager.setFragmentResult("outfit_registered", bundle)
        }

        Log.d("AddItemFragment", "등록 결과 전달: success=$isSuccess, date=$purchaseDate, editMode=$isEditMode")
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
     * 폼 데이터 수집
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
            val season = seasonSpinner.selectedItemPosition + 1
            val color = colorSpinner.selectedItemPosition + 1

            val brand = brandEdit.text.toString()
            val size = sizeEdit.text.toString()
            val priceText = priceEdit.text.toString()
            val price = if (priceText.isNotEmpty()) priceText.toIntOrNull() ?: 0 else 0
            val purchaseSite = siteEdit.text.toString()
            val purchaseDate = getCurrentDate()

            // 선택된 태그 ID들 (현재는 더미)
            val tagIds = listOf<Int>()

            Log.d("AddItemFragment", "수집된 데이터: category=$category, subcategory=$subcategory, season=$season, color=$color")

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
        setupSpinnerWithContainer(view, R.id.spinner_season, arrayOf("봄", "여름", "가을", "겨울", "사계절"))
        setupSpinnerWithContainer(view, R.id.spinner_color, arrayOf("블랙", "화이트", "그레이", "네이비", "브라운", "베이지", "레드", "핑크", "옐로우", "그린", "블루", "퍼플"))
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
    }

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
                    } else {
                        selectedTags.remove(tagText)
                    }
                    Log.d("TagButton", "$tagText selected: ${button.isSelected}")
                }
            }
        }
    }
}