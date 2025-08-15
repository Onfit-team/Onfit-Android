package com.example.onfit.Wardrobe.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.lifecycle.lifecycleScope
<<<<<<< HEAD
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import com.example.onfit.R
import com.example.onfit.KakaoLogin.util.TokenProvider
import com.example.onfit.Wardrobe.Network.RetrofitClient
import com.example.onfit.Wardrobe.Network.WardrobeItemDto
import com.google.android.flexbox.FlexboxLayout
=======
import kotlinx.coroutines.launch
import com.example.onfit.R
import com.example.onfit.KakaoLogin.util.TokenProvider
import com.example.onfit.Wardrobe.Network.WardrobeRetrofitClient
import com.example.onfit.Wardrobe.Network.WardrobeItemDto
import com.example.onfit.Wardrobe.repository.WardrobeRepository
>>>>>>> 3677f88 (refactor: 코드 리팩토링)
import android.util.Log

class WardrobeSearchFragment : Fragment() {

<<<<<<< HEAD
=======
    // Repository 추가
    private lateinit var repository: WardrobeRepository

>>>>>>> 3677f88 (refactor: 코드 리팩토링)
    // Views
    private lateinit var icBack: ImageButton
    private lateinit var spinnerColor: Spinner
    private lateinit var brandDropdownContainer: LinearLayout
    private lateinit var brandDropdownText: TextView
    private lateinit var brandPopupOverlay: LinearLayout
    private lateinit var btnSave: Button

    // Season buttons
    private lateinit var btnSpringFall: Button
    private lateinit var btnSummer: Button
    private lateinit var btnWinter: Button

    // Style tag buttons
    private lateinit var styleButtons: MutableList<Button>
    private lateinit var purposeButtons: MutableList<Button>

    // Data
    private val colorOptions = arrayOf("색상 선택", "블랙", "화이트", "그레이", "네이비", "브라운", "베이지", "레드", "핑크", "옐로우", "그린", "블루", "퍼플", "스카이블루", "오트밀", "아이보리")
<<<<<<< HEAD
    private var brandOptions = arrayOf("브랜드 로딩 중...") // API에서 동적으로 로드
=======
    private var brandOptions = arrayOf("브랜드 로딩 중...")
>>>>>>> 3677f88 (refactor: 코드 리팩토링)

    private var selectedSeason = ""
    private var selectedColor = ""
    private var selectedBrand = ""
    private val selectedStyleTags = mutableSetOf<String>()
    private val selectedPurposeTags = mutableSetOf<String>()

    // 누락된 변수들 추가
    private var currentSearchQuery = ""
    private var wardrobeItems = listOf<WardrobeItemDto>()

<<<<<<< HEAD
=======
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Repository 초기화
        repository = WardrobeRepository(requireContext())
    }

>>>>>>> 3677f88 (refactor: 코드 리팩토링)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_wardrobe_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupListeners()
        setupSpinners()
        setupButtons()
<<<<<<< HEAD
        setupSearchResultListener() // 이 함수를 여기로 이동
=======
        setupSearchResultListener()
>>>>>>> 3677f88 (refactor: 코드 리팩토링)

        // API에서 브랜드 목록 로드
        loadBrandsFromAPI()
    }

    override fun onResume() {
        super.onResume()
        activity?.findViewById<View>(R.id.bottomNavigationView)?.visibility = View.GONE
    }

    override fun onPause() {
        super.onPause()
        activity?.findViewById<View>(R.id.bottomNavigationView)?.visibility = View.VISIBLE
    }

    private fun initViews(view: View) {
        icBack = view.findViewById(R.id.ic_back)
        spinnerColor = view.findViewById(R.id.spinner_color)
        brandDropdownContainer = view.findViewById(R.id.brand_dropdown_container)
        brandDropdownText = view.findViewById(R.id.brand_dropdown_text)
        brandPopupOverlay = view.findViewById(R.id.brand_popup_overlay)
        btnSave = view.findViewById(R.id.btn_save)

        // Season buttons
        btnSpringFall = view.findViewById(R.id.btnTopCategory1)
        btnSummer = view.findViewById(R.id.btnTopCategory2)
        btnWinter = view.findViewById(R.id.btnTopCategory3)

        // Initialize button lists
        styleButtons = mutableListOf()
        purposeButtons = mutableListOf()

        // Style buttons (mood) - LinearLayout
        val styleLayout1 = view.findViewById<LinearLayout>(R.id.topCategoryLayout1)
        val styleLayout2 = view.findViewById<LinearLayout>(R.id.topCategoryLayout2)

        addButtonsFromLinearLayout(styleLayout1, styleButtons)
        addButtonsFromLinearLayout(styleLayout2, styleButtons)

<<<<<<< HEAD
        // Purpose buttons - 둘 다 LinearLayout으로 통일
=======
        // Purpose buttons - LinearLayout
>>>>>>> 3677f88 (refactor: 코드 리팩토링)
        val purposeLayout1 = view.findViewById<LinearLayout>(R.id.topCategoryLayout3)
        val purposeLayout2 = view.findViewById<LinearLayout>(R.id.topCategoryLayout4)

        addButtonsFromLinearLayout(purposeLayout1, purposeButtons)
        addButtonsFromLinearLayout(purposeLayout2, purposeButtons)
    }

    private fun addButtonsFromLinearLayout(layout: LinearLayout?, buttonList: MutableList<Button>) {
        layout?.let {
            for (i in 0 until it.childCount) {
                val child = it.getChildAt(i)
                if (child is Button) {
                    buttonList.add(child)
                }
            }
        }
    }

    /**
<<<<<<< HEAD
     * API에서 브랜드 목록 로드
=======
     * Repository를 사용한 브랜드 목록 로드
>>>>>>> 3677f88 (refactor: 코드 리팩토링)
     */
    private fun loadBrandsFromAPI() {
        lifecycleScope.launch {
            try {
<<<<<<< HEAD
                val token = "Bearer " + TokenProvider.getToken(requireContext())

                // API가 구현되지 않은 경우 임시로 더미 데이터 사용
                try {
                    val response = RetrofitClient.wardrobeService.getBrandsList(token)

                    if (response.isSuccessful && response.body()?.isSuccess == true) {
                        val brands = response.body()?.result ?: emptyList()
=======
                repository.getBrandsList()
                    .onSuccess { brands ->
>>>>>>> 3677f88 (refactor: 코드 리팩토링)
                        if (brands.isNotEmpty()) {
                            brandOptions = brands.toTypedArray()
                            setupBrandSelectionWithAPI(brands)
                            Log.d("WardrobeSearchFragment", "브랜드 목록 로드 성공: ${brands.size}개")
                        } else {
                            setupDummyBrands()
                        }
<<<<<<< HEAD
                    } else {
                        setupDummyBrands()
                    }
                } catch (e: Exception) {
                    Log.w("WardrobeSearchFragment", "브랜드 API 호출 실패, 더미 데이터 사용: ${e.message}")
                    setupDummyBrands()
                }

=======
                    }
                    .onFailure { exception ->
                        Log.w("WardrobeSearchFragment", "브랜드 API 호출 실패: ${exception.message}")
                        setupDummyBrands()
                    }
>>>>>>> 3677f88 (refactor: 코드 리팩토링)
            } catch (e: Exception) {
                Log.e("WardrobeSearchFragment", "브랜드 목록 로드 실패", e)
                setupDummyBrands()
            }
        }
    }

    /**
     * 더미 브랜드 데이터 설정
     */
    private fun setupDummyBrands() {
        val dummyBrands = listOf("아디다스", "나이키", "자라", "유니클로", "H&M", "무신사", "SPAO")
        brandOptions = dummyBrands.toTypedArray()
        setupBrandSelectionWithAPI(dummyBrands)
        Log.d("WardrobeSearchFragment", "더미 브랜드 데이터 설정: ${dummyBrands.size}개")
    }

    /**
     * API에서 받은 브랜드 목록으로 브랜드 선택 UI 업데이트
     */
    private fun setupBrandSelectionWithAPI(brands: List<String>) {
<<<<<<< HEAD
        // 브랜드 팝업의 기존 브랜드 목록을 동적으로 생성
        val brandScrollView = brandPopupOverlay.findViewById<ScrollView>(R.id.brand_scroll_view)
        val brandContainer = brandScrollView?.getChildAt(0) as? LinearLayout

        brandContainer?.removeAllViews() // 기존 뷰 제거
=======
        val brandScrollView = brandPopupOverlay.findViewById<ScrollView>(R.id.brand_scroll_view)
        val brandContainer = brandScrollView?.getChildAt(0) as? LinearLayout

        brandContainer?.removeAllViews()
>>>>>>> 3677f88 (refactor: 코드 리팩토링)

        brands.forEach { brandName ->
            val brandTextView = TextView(requireContext()).apply {
                text = brandName
                textSize = 16f
                setPadding(24, 20, 24, 20)
                setOnClickListener {
                    selectedBrand = brandName
                    brandDropdownText.text = brandName
                    brandDropdownText.setTextColor(
                        resources.getColor(android.R.color.black, requireContext().theme)
                    )
                    hideBrandPopup()
                }
            }

<<<<<<< HEAD
            // 브랜드 아이템 사이 구분선 추가
=======
>>>>>>> 3677f88 (refactor: 코드 리팩토링)
            val divider = View(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    1
                )
                setBackgroundColor(resources.getColor(R.color.gray, requireContext().theme))
            }

            brandContainer?.addView(brandTextView)
<<<<<<< HEAD
            if (brandName != brands.last()) { // 마지막 아이템이 아닌 경우만 구분선 추가
=======
            if (brandName != brands.last()) {
>>>>>>> 3677f88 (refactor: 코드 리팩토링)
                brandContainer?.addView(divider)
            }
        }
    }

    private fun setupListeners() {
        // Back button
        icBack.setOnClickListener {
            findNavController().navigateUp()
        }

        // Brand dropdown
        brandDropdownContainer.setOnClickListener {
            showBrandPopup()
        }

<<<<<<< HEAD
        // Brand popup overlay (close popup when clicking outside)
=======
        // Brand popup overlay
>>>>>>> 3677f88 (refactor: 코드 리팩토링)
        brandPopupOverlay.setOnClickListener {
            hideBrandPopup()
        }

<<<<<<< HEAD
        // Prevent popup from closing when clicking on the popup content
        brandPopupOverlay.getChildAt(1)?.setOnClickListener {
            // Do nothing - prevents the click from bubbling up to the overlay
        }

        // Save button - API 필터 검색 호출
=======
        // Prevent popup from closing when clicking on content
        brandPopupOverlay.getChildAt(1)?.setOnClickListener { }

        // Save button
>>>>>>> 3677f88 (refactor: 코드 리팩토링)
        btnSave.setOnClickListener {
            applyFiltersWithAPI()
        }
    }

    private fun setupSpinners() {
        // Color spinner
        val colorAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, colorOptions)
        colorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerColor.adapter = colorAdapter

        spinnerColor.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedColor = if (position == 0) "" else colorOptions[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedColor = ""
            }
        }

        setupColorSpinnerClick()
    }

    private fun setupColorSpinnerClick() {
        val colorSpinnerContainer = spinnerColor.parent as? LinearLayout

        spinnerColor.setOnTouchListener { _, _ ->
            spinnerColor.post {
                adjustColorDropdownPosition(spinnerColor, colorSpinnerContainer)
            }
            false
        }

        colorSpinnerContainer?.setOnClickListener {
            spinnerColor.performClick()
        }

        colorSpinnerContainer?.let { container ->
            for (i in 0 until container.childCount) {
                val child = container.getChildAt(i)
                if (child is ImageView) {
                    child.setOnClickListener {
                        spinnerColor.performClick()
                    }
                }
            }
        }
    }

    private fun adjustColorDropdownPosition(spinner: Spinner, spinnerContainer: LinearLayout?) {
        if (spinnerContainer == null) return

        try {
            val popupField = Spinner::class.java.getDeclaredField("mPopup")
            popupField.isAccessible = true
            val popupWindow = popupField.get(spinner) ?: return

            val spinnerLocation = IntArray(2)
            val containerLocation = IntArray(2)

            spinner.getLocationOnScreen(spinnerLocation)
            spinnerContainer.getLocationOnScreen(containerLocation)

            val horizontalOffsetToContainerLeft = spinnerLocation[0] - containerLocation[0]
            val verticalOffset = -(16 * resources.displayMetrics.density).toInt()

            val containerWidth = spinnerContainer.width
            val setWidthMethod = popupWindow.javaClass.getMethod("setWidth", Int::class.java)
            setWidthMethod.invoke(popupWindow, containerWidth)

            val maxHeight = (250 * resources.displayMetrics.density).toInt()
            val setHeightMethod = popupWindow.javaClass.getMethod("setHeight", Int::class.java)
            setHeightMethod.invoke(popupWindow, maxHeight)

            val setHorizontalOffsetMethod = popupWindow.javaClass.getMethod("setHorizontalOffset", Int::class.java)
            setHorizontalOffsetMethod.invoke(popupWindow, -horizontalOffsetToContainerLeft)

            try {
                val setVerticalOffsetMethod = popupWindow.javaClass.getMethod("setVerticalOffset", Int::class.java)
                setVerticalOffsetMethod.invoke(popupWindow, verticalOffset)
            } catch (e: Exception) {
                Log.e("ColorSpinner", "Vertical offset failed: ${e.message}")
            }

        } catch (e: Exception) {
            Log.e("ColorSpinner", "Failed to adjust color dropdown: ${e.message}")
        }
    }

    private fun setupButtons() {
        // Season buttons
        btnSpringFall.setOnClickListener { selectSeason("봄ㆍ가을", btnSpringFall) }
        btnSummer.setOnClickListener { selectSeason("여름", btnSummer) }
        btnWinter.setOnClickListener { selectSeason("겨울", btnWinter) }

        // Style buttons
        styleButtons.forEach { button ->
            button.setOnClickListener {
                toggleStyleTag(button)
            }
        }

        // Purpose buttons
        purposeButtons.forEach { button ->
            button.setOnClickListener {
                togglePurposeTag(button)
            }
        }
    }

    private fun selectSeason(season: String, selectedButton: Button) {
        if (selectedSeason == season) {
            selectedSeason = ""
            selectedButton.isSelected = false
            return
        }

        selectedSeason = season

        listOf(btnSpringFall, btnSummer, btnWinter).forEach { btn ->
            btn.isSelected = false
        }

        selectedButton.isSelected = true
    }

    private fun toggleStyleTag(button: Button) {
        val tag = button.text.toString().replace("#", "")

        if (selectedStyleTags.contains(tag)) {
            selectedStyleTags.remove(tag)
            button.isSelected = false
<<<<<<< HEAD
            Log.d("WardrobeSearchFragment", "분위기 태그 해제: $tag")
        } else {
            selectedStyleTags.add(tag)
            button.isSelected = true
            Log.d("WardrobeSearchFragment", "분위기 태그 선택: $tag")
=======
        } else {
            selectedStyleTags.add(tag)
            button.isSelected = true
>>>>>>> 3677f88 (refactor: 코드 리팩토링)
        }

        Log.d("WardrobeSearchFragment", "현재 분위기 태그들: ${selectedStyleTags.joinToString(", ")}")
    }

    private fun togglePurposeTag(button: Button) {
        val tag = button.text.toString().replace("#", "")

        if (selectedPurposeTags.contains(tag)) {
            selectedPurposeTags.remove(tag)
            button.isSelected = false
<<<<<<< HEAD
            Log.d("WardrobeSearchFragment", "용도 태그 해제: $tag")
        } else {
            selectedPurposeTags.add(tag)
            button.isSelected = true
            Log.d("WardrobeSearchFragment", "용도 태그 선택: $tag")
=======
        } else {
            selectedPurposeTags.add(tag)
            button.isSelected = true
>>>>>>> 3677f88 (refactor: 코드 리팩토링)
        }

        Log.d("WardrobeSearchFragment", "현재 용도 태그들: ${selectedPurposeTags.joinToString(", ")}")
    }

    private fun showBrandPopup() {
        adjustBrandPopupHeight()
        brandPopupOverlay.visibility = View.VISIBLE
    }

    private fun adjustBrandPopupHeight() {
        val brandScrollView = brandPopupOverlay.findViewById<ScrollView>(R.id.brand_scroll_view)

        if (brandScrollView != null) {
            val brandListContainer = brandScrollView.getChildAt(0) as LinearLayout

            val itemCount = brandListContainer.childCount
            val itemHeight = 60 * resources.displayMetrics.density
            val totalContentHeight = (itemCount * itemHeight).toInt()

            val displayMetrics = resources.displayMetrics
            val screenHeight = displayMetrics.heightPixels
            val maxScrollViewHeight = (screenHeight * 0.6).toInt()

            val scrollViewLayoutParams = brandScrollView.layoutParams
            scrollViewLayoutParams.height = if (totalContentHeight > maxScrollViewHeight) {
                maxScrollViewHeight
            } else {
                totalContentHeight
            }
            brandScrollView.layoutParams = scrollViewLayoutParams
        }

        setBrandPopupCornerRadius()
    }

    private fun setBrandPopupCornerRadius() {
        val popupContent = brandPopupOverlay.getChildAt(1) as? LinearLayout
        popupContent?.let {
            val cornerRadius = 20f * resources.displayMetrics.density

            val drawable = android.graphics.drawable.GradientDrawable()
            drawable.setColor(android.graphics.Color.WHITE)
            drawable.cornerRadii = floatArrayOf(
                cornerRadius, cornerRadius,
                cornerRadius, cornerRadius,
                0f, 0f,
                0f, 0f
            )

            it.background = drawable
        }
    }

    private fun hideBrandPopup() {
        brandPopupOverlay.visibility = View.GONE
    }

<<<<<<< HEAD
    // 검색 결과 리스너를 별도 함수로 분리
    private fun setupSearchResultListener() {
        // 검색 결과 받기 - 중복 제거
        parentFragmentManager.setFragmentResultListener("search_results", this) { _, bundle ->
            val filteredIds = bundle.getIntArray("filtered_item_ids")
            val searchQuery = bundle.getString("search_query")

            if (filteredIds != null) {
                // 필터링된 아이템만 표시
                val filteredItems = wardrobeItems.filter { it.id in filteredIds }

=======
    private fun setupSearchResultListener() {
        parentFragmentManager.setFragmentResultListener("search_results", this) { _, bundle ->
            val filteredIds = bundle.getIntArray("filtered_item_ids")

            if (filteredIds != null) {
                val filteredItems = wardrobeItems.filter { it.id in filteredIds }
>>>>>>> 3677f88 (refactor: 코드 리팩토링)
                Log.d("WardrobeSearchFragment", "${filteredItems.size}개 아이템 검색됨")
                Toast.makeText(context, "${filteredItems.size}개 아이템 검색됨", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
<<<<<<< HEAD
     * API를 사용한 필터 검색
=======
     * Repository를 사용한 필터 검색
>>>>>>> 3677f88 (refactor: 코드 리팩토링)
     */
    private fun applyFiltersWithAPI() {
        val hasFilters = selectedSeason.isNotEmpty() ||
                selectedColor.isNotEmpty() ||
                selectedBrand.isNotEmpty() ||
                selectedStyleTags.isNotEmpty() ||
                selectedPurposeTags.isNotEmpty()

        if (!hasFilters) {
            Toast.makeText(requireContext(), "최소 하나의 필터를 선택해주세요", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
<<<<<<< HEAD
                val token = "Bearer " + TokenProvider.getToken(requireContext())

                // 필터 파라미터 변환
                val seasonParam = convertSeasonToAPI(selectedSeason)
                val colorParam = convertColorToAPI(selectedColor)
                val brandParam = if (selectedBrand.isNotEmpty()) selectedBrand else null
                val tagIdsParam = convertTagsToAPI(selectedStyleTags + selectedPurposeTags)

                Log.d("WardrobeSearchFragment", "필터 파라미터: season=$seasonParam, color=$colorParam, brand=$brandParam, tagIds=$tagIdsParam")

                // 실제 API 호출 시도
                try {
                    val response = RetrofitClient.wardrobeService.filterWardrobeItems(
                        authorization = token,
                        season = seasonParam,
                        color = colorParam,
                        brand = brandParam,
                        tagIds = tagIdsParam
                    )

                    if (response.isSuccessful && response.body()?.isSuccess == true) {
                        val searchResults = response.body()?.result?.items ?: emptyList()

                        if (searchResults.isNotEmpty()) {
                            // 검색 결과를 WardrobeFragment에 전달
                            val bundle = Bundle().apply {
                                putIntArray("filtered_item_ids", searchResults.map { it.id }.toIntArray())
                                putString("search_query", "필터 검색")
                            }

                            parentFragmentManager.setFragmentResult("search_results", bundle)
                            Toast.makeText(context, "${searchResults.size}개의 아이템을 찾았습니다", Toast.LENGTH_SHORT).show()
                            findNavController().navigateUp()
                        } else {
                            Toast.makeText(context, "검색 결과가 없습니다", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        showError("필터 검색 실패: ${response.body()?.message}")
                    }
                } catch (apiError: Exception) {
                    Log.w("WardrobeSearchFragment", "API 호출 실패, 임시 성공 처리: ${apiError.message}")

                    // API 실패 시 임시 처리
                    val bundle = Bundle().apply {
                        putString("filter_applied", "true")
=======
                // Repository를 통한 검색 (현재는 로컬 검색 구현)
                val allItems = repository.getAllWardrobeItems().getOrDefault(
                    com.example.onfit.Wardrobe.Network.WardrobeResult(
                        totalCount = 0,
                        items = emptyList(),
                        categories = emptyList()
                    )
                ).items

                val filteredItems = performLocalFilter(allItems)

                if (filteredItems.isNotEmpty()) {
                    val bundle = Bundle().apply {
                        putIntArray("filtered_item_ids", filteredItems.map { it.id }.toIntArray())
                        putString("search_query", "필터 검색")
>>>>>>> 3677f88 (refactor: 코드 리팩토링)
                        putString("filter_season", selectedSeason)
                        putString("filter_color", selectedColor)
                        putString("filter_brand", selectedBrand)
                    }

<<<<<<< HEAD
                    parentFragmentManager.setFragmentResult("filter_results", bundle)
                    Toast.makeText(context, "필터가 적용되었습니다", Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
=======
                    parentFragmentManager.setFragmentResult("search_results", bundle)
                    Toast.makeText(context, "${filteredItems.size}개의 아이템을 찾았습니다", Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                } else {
                    Toast.makeText(context, "검색 결과가 없습니다", Toast.LENGTH_SHORT).show()
>>>>>>> 3677f88 (refactor: 코드 리팩토링)
                }

            } catch (e: Exception) {
                Log.e("WardrobeSearchFragment", "필터 검색 실패", e)
                showError("필터 검색 중 오류가 발생했습니다: ${e.message}")
            }
        }
    }

    /**
<<<<<<< HEAD
=======
     * 로컬 필터링
     */
    private fun performLocalFilter(items: List<WardrobeItemDto>): List<WardrobeItemDto> {
        return items.filter { item ->
            var matches = true

            // 계절 필터
            if (selectedSeason.isNotEmpty()) {
                val seasonId = convertSeasonToAPI(selectedSeason)
                if (seasonId != null && item.season != seasonId) {
                    matches = false
                }
            }

            // 색상 필터
            if (selectedColor.isNotEmpty()) {
                val colorId = convertColorToAPI(selectedColor)
                if (colorId != null && item.color != colorId) {
                    matches = false
                }
            }

            // 브랜드 필터
            if (selectedBrand.isNotEmpty()) {
                if (!item.brand.contains(selectedBrand, ignoreCase = true)) {
                    matches = false
                }
            }

            matches
        }
    }

    /**
>>>>>>> 3677f88 (refactor: 코드 리팩토링)
     * 계절을 API 파라미터로 변환
     */
    private fun convertSeasonToAPI(season: String): Int? {
        return when (season) {
            "봄ㆍ가을" -> 1
            "여름" -> 2
            "겨울" -> 4
            else -> null
        }
    }

    /**
     * 색상을 API 파라미터로 변환
     */
    private fun convertColorToAPI(color: String): Int? {
        return when (color) {
            "블랙" -> 1
            "화이트" -> 2
            "그레이" -> 3
            "네이비" -> 4
            "브라운" -> 5
            "베이지" -> 6
            "레드" -> 7
            "핑크" -> 8
            "옐로우" -> 9
            "그린" -> 10
            "블루" -> 11
            "퍼플" -> 12
            else -> null
        }
    }

<<<<<<< HEAD
    /**
     * 태그를 API 파라미터로 변환
     */
    private fun convertTagsToAPI(tags: Set<String>): String? {
        if (tags.isEmpty()) return null

        val tagNameToId = mapOf(
            // 분위기 태그
            "캐주얼" to 1, "스트릿" to 2, "미니멀" to 3, "클래식" to 4, "빈티지" to 5,
            "러블리" to 6, "페미닌" to 7, "보이시" to 8, "모던" to 9,
            // 용도 태그
            "데일리" to 10, "출근룩" to 11, "데이트룩" to 12, "나들이룩" to 13,
            "여행룩" to 14, "운동복" to 15, "하객룩" to 16, "파티룩" to 17
        )

        val tagIds = tags.mapNotNull { tagName ->
            val cleanTagName = tagName.replace("#", "")
            val tagId = tagNameToId[cleanTagName]
            Log.d("WardrobeSearchFragment", "태그 변환: '$cleanTagName' -> $tagId")
            tagId
        }

        Log.d("WardrobeSearchFragment", "최종 태그 IDs: ${tagIds.joinToString(",")}")
        return if (tagIds.isNotEmpty()) tagIds.joinToString(",") else null
    }

=======
>>>>>>> 3677f88 (refactor: 코드 리팩토링)
    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        Log.e("WardrobeSearchFragment", message)
    }
}