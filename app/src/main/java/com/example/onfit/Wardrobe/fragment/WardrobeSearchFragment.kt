package com.example.onfit.Wardrobe.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.example.onfit.R
import com.example.onfit.Wardrobe.Network.WardrobeItemDto
import com.example.onfit.Wardrobe.repository.WardrobeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val USE_SEARCH_DUMMY = true

class WardrobeSearchFragment : Fragment() {

    // Repository 추가
    private lateinit var repository: WardrobeRepository

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
    private val colorOptions = arrayOf(
        "색상 선택",
        "블랙", "화이트", "그레이", "네이비", "베이지", "브라운",
        "레드", "핑크", "오렌지", "옐로우", "그린", "블루", "퍼플",
        "스카이블루", "오트밀", "아이보리"
    )
    private var brandOptions = arrayOf("브랜드 로딩 중...")

    private var selectedSeason = ""
    private var selectedColor = ""
    private var selectedBrand = ""
    private val selectedStyleTags = mutableSetOf<String>()
    private val selectedPurposeTags = mutableSetOf<String>()

    // 누락된 변수들 추가
    private var currentSearchQuery = ""
    private var wardrobeItems = listOf<WardrobeItemDto>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = WardrobeRepository(requireContext())
    }

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

        btnSpringFall = view.findViewById(R.id.btnTopCategory1)
        btnSummer = view.findViewById(R.id.btnTopCategory2)
        btnWinter = view.findViewById(R.id.btnTopCategory3)

        styleButtons = mutableListOf()
        purposeButtons = mutableListOf()

        val styleLayout1 = view.findViewById<LinearLayout>(R.id.topCategoryLayout1)
        val styleLayout2 = view.findViewById<LinearLayout>(R.id.topCategoryLayout2)
        addButtonsFromLinearLayout(styleLayout1, styleButtons)
        addButtonsFromLinearLayout(styleLayout2, styleButtons)

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

    private fun loadBrandsFromAPI() {
        lifecycleScope.launch {
            try {
                // 🔥 NEW: 전체 아이템에서 브랜드 추출 + API 브랜드 목록 합치기
                val apiResult = repository.getAllWardrobeItems()
                val registeredBrands = mutableSetOf<String>()

                // 등록된 아이템에서 브랜드 추출
                apiResult.onSuccess { wardrobeResult ->
                    wardrobeResult.items.forEach { item ->
                        if (!item.brand.isNullOrBlank() && item.brand.trim().isNotEmpty()) {
                            registeredBrands.add(item.brand.trim())
                        }
                    }
                }

                // API에서 브랜드 목록 가져오기
                repository.getBrandsList()
                    .onSuccess { apiBrands ->
                        val allBrands = mutableSetOf<String>()

                        // API 브랜드 추가
                        allBrands.addAll(apiBrands.filter { it.isNotBlank() })

                        // 등록된 브랜드 추가
                        allBrands.addAll(registeredBrands)

                        // 더미 브랜드도 추가 (없는 경우 대비)
                        if (allBrands.isEmpty()) {
                            allBrands.addAll(listOf("아디다스", "나이키", "자라", "유니클로", "H&M", "무신사", "SPAO"))
                        }

                        // 알파벳 순으로 정렬
                        val sortedBrands = allBrands.toList().sorted()
                        brandOptions = sortedBrands.toTypedArray()

                        Log.d("WardrobeSearchFragment", "브랜드 로드 완료: ${sortedBrands.size}개 (등록된 브랜드: ${registeredBrands.size}개)")

                        setupBrandSelectionWithAPI(sortedBrands)
                    }
                    .onFailure {
                        // API 실패 시 등록된 브랜드만 사용
                        if (registeredBrands.isNotEmpty()) {
                            val sortedBrands = registeredBrands.toList().sorted()
                            brandOptions = sortedBrands.toTypedArray()
                            setupBrandSelectionWithAPI(sortedBrands)
                            Log.d("WardrobeSearchFragment", "등록된 브랜드만 사용: ${sortedBrands.size}개")
                        } else {
                            setupDummyBrands()
                        }
                    }

            } catch (e: Exception) {
                Log.e("WardrobeSearchFragment", "브랜드 로딩 실패", e)
                setupDummyBrands()
            }
        }
    }

    private fun setupDummyBrands() {
        val dummyBrands = listOf("아디다스", "나이키", "자라", "유니클로", "H&M", "무신사", "SPAO").sorted()
        brandOptions = dummyBrands.toTypedArray()
        setupBrandSelectionWithAPI(dummyBrands)
        Log.d("WardrobeSearchFragment", "더미 브랜드 사용: ${dummyBrands.size}개")
    }

    private fun setupBrandSelectionWithAPI(brands: List<String>) {
        val brandScrollView = brandPopupOverlay.findViewById<ScrollView>(R.id.brand_scroll_view)
        val brandContainer = brandScrollView?.getChildAt(0) as? LinearLayout
        brandContainer?.removeAllViews()

        brands.forEachIndexed { index, brandName ->
            val brandTextView = TextView(requireContext()).apply {
                text = brandName
                textSize = 16f
                // 🔥 UI 개선: 상하 패딩 증가, 좌우 패딩 유지
                setPadding(24, 28, 24, 28) // 기존 20 → 28로 증가
                setOnClickListener {
                    selectedBrand = brandName
                    brandDropdownText.text = brandName
                    brandDropdownText.setTextColor(
                        resources.getColor(android.R.color.black, requireContext().theme)
                    )
                    hideBrandPopup()
                    Log.d("WardrobeSearchFragment", "브랜드 선택: $brandName")
                }
            }

            brandContainer?.addView(brandTextView)

            // 🔥 UI 개선: 구분선 제거하고 마지막 아이템이 아닌 경우에만 여백 추가
            if (index < brands.size - 1) {
                val spacer = View(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        4 // 작은 여백만 추가
                    )
                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                }
                brandContainer?.addView(spacer)
            }
        }
    }

    private fun setupListeners() {
        icBack.setOnClickListener { findNavController().navigateUp() }
        brandDropdownContainer.setOnClickListener { showBrandPopup() }
        brandPopupOverlay.setOnClickListener { hideBrandPopup() }
        brandPopupOverlay.getChildAt(1)?.setOnClickListener { }

        // 🔥 더미 + API 통합 검색으로 변경
        btnSave.setOnClickListener { applyFiltersUnified() }
    }

    // 🔥 새로운 통합 필터 함수 추가
    private fun applyFiltersUnified() {
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
                Log.d("WardrobeSearchFragment", "🔍 통합 필터 검색 시작")
                Log.d("WardrobeSearchFragment", "선택된 필터 - 계절: '$selectedSeason', 색상: '$selectedColor', 브랜드: '$selectedBrand'")

                // 🔥 STEP 1: API 아이템 로드 (기존 로직 그대로)
                val allItemsResult = repository.getAllWardrobeItems().getOrDefault(
                    com.example.onfit.Wardrobe.Network.WardrobeResult(
                        totalCount = 0,
                        items = emptyList(),
                        categories = emptyList()
                    )
                )

                val basicItems = allItemsResult.items
                Log.d("WardrobeSearchFragment", "📦 API 기본 아이템 로드: ${basicItems.size}개")

                // 🔥 STEP 2: API 아이템 상세 정보 로드 (기존 로직 그대로)
                val detailedAPIItems = mutableListOf<com.example.onfit.Wardrobe.Network.WardrobeItemDto>()

                basicItems.forEachIndexed { index, item ->
                    try {
                        Log.d("WardrobeSearchFragment", "📊 API 아이템 ${item.id} 상세정보 요청 (${index + 1}/${basicItems.size})")

                        val detailResult = repository.getWardrobeItemDetail(item.id)

                        if (detailResult.isSuccess) {
                            val detail = detailResult.getOrNull()
                            if (detail != null) {
                                val detailedItem = com.example.onfit.Wardrobe.Network.WardrobeItemDto(
                                    id = detail.id,
                                    image = detail.image ?: "",
                                    brand = detail.brand ?: "",
                                    season = detail.season,
                                    color = detail.color,
                                    category = detail.category,
                                    subcategory = detail.subcategory
                                )
                                detailedAPIItems.add(detailedItem)
                                Log.d("WardrobeSearchFragment", "✅ API 아이템 ${item.id} 로드 성공")
                            } else {
                                detailedAPIItems.add(item)
                            }
                        } else {
                            detailedAPIItems.add(item)
                        }
                    } catch (e: Exception) {
                        Log.e("WardrobeSearchFragment", "💥 API 아이템 ${item.id} 예외 발생", e)
                        detailedAPIItems.add(item)
                    }
                }

                // 🔥 STEP 3: 더미 데이터 로드
                val dummyItems = if (USE_SEARCH_DUMMY) {
                    loadDummyWardrobeFromAssets()
                } else {
                    emptyList()
                }
                Log.d("WardrobeSearchFragment", "🎭 더미 아이템 로드: ${dummyItems.size}개")

                // 🔥 STEP 4: API + 더미 데이터 결합
                val allItems = detailedAPIItems + dummyItems
                Log.d("WardrobeSearchFragment", "🔗 전체 아이템 (API + 더미): ${allItems.size}개")

                // 🔥 STEP 5: 통합 필터링 수행
                Log.d("WardrobeSearchFragment", "🔍 통합 필터링 시작...")
                val filteredItems = performLocalFilter(allItems)
                Log.d("WardrobeSearchFragment", "🎉 통합 필터링 완료: ${filteredItems.size}개 아이템 발견")

                // 🔥 STEP 6: FragmentResult 전달
                withContext(Dispatchers.Main) {
                    Log.d("WardrobeSearchFragment", "📡 Main 스레드에서 FragmentResult 전달 시작...")

                    val filteredIds = filteredItems.map { it.id }.toIntArray()
                    Log.d("WardrobeSearchFragment", "📦 결과 ID들: ${filteredIds.contentToString()}")

                    val bundle = Bundle().apply {
                        putIntArray("filtered_item_ids", filteredIds)
                        putString("search_query", "통합 필터 검색")
                        putString("filter_season", selectedSeason)
                        putString("filter_color", selectedColor)
                        putString("filter_brand", selectedBrand)
                        putBoolean("filter_applied", true)
                    }

                    if (isAdded && context != null && !parentFragmentManager.isDestroyed) {
                        try {
                            parentFragmentManager.setFragmentResult("search_results", bundle)
                            Log.d("WardrobeSearchFragment", "✅ FragmentResult 전달 성공!")

                            val message = if (filteredItems.isNotEmpty()) {
                                "${filteredItems.size}개의 아이템을 찾았습니다"
                            } else {
                                "검색 조건에 맞는 아이템이 없습니다"
                            }

                            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                            Log.d("WardrobeSearchFragment", "📢 토스트 메시지 표시: $message")

                            view?.postDelayed({
                                if (isAdded && context != null) {
                                    try {
                                        Log.d("WardrobeSearchFragment", "🚪 Navigation 시도...")
                                        findNavController().navigateUp()
                                        Log.d("WardrobeSearchFragment", "✅ Navigation 성공!")
                                    } catch (e: Exception) {
                                        Log.e("WardrobeSearchFragment", "❌ Navigation 실패", e)
                                    }
                                }
                            }, 100)

                        } catch (e: Exception) {
                            Log.e("WardrobeSearchFragment", "❌ FragmentResult 전달 실패", e)
                            Toast.makeText(requireContext(), "검색 결과 전달 실패: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }

            } catch (e: Exception) {
                Log.e("WardrobeSearchFragment", "💥 통합 필터 검색 중 치명적 오류 발생", e)
                withContext(Dispatchers.Main) {
                    showError("필터 검색 중 오류가 발생했습니다: ${e.message}")
                }
            }
        }
    }

    private fun setupSpinners() {
        val colorAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, colorOptions)
        colorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerColor.adapter = colorAdapter

        spinnerColor.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedColor = if (position == 0) "" else colorOptions[position]
            }
            override fun onNothingSelected(parent: AdapterView<*>?) { selectedColor = "" }
        }
        setupColorSpinnerClick()
    }

    private fun setupColorSpinnerClick() {
        val colorSpinnerContainer = spinnerColor.parent as? LinearLayout
        spinnerColor.setOnTouchListener { _, _ ->
            spinnerColor.post { adjustColorDropdownPosition(spinnerColor, colorSpinnerContainer) }
            false
        }
        colorSpinnerContainer?.setOnClickListener { spinnerColor.performClick() }
        colorSpinnerContainer?.let { container ->
            for (i in 0 until container.childCount) {
                val child = container.getChildAt(i)
                if (child is ImageView) {
                    child.setOnClickListener { spinnerColor.performClick() }
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
            } catch (_: Exception) {}
        } catch (_: Exception) {}
    }

    private fun setupButtons() {
        btnSpringFall.setOnClickListener { selectSeason("봄ㆍ가을", btnSpringFall) }
        btnSummer.setOnClickListener { selectSeason("여름", btnSummer) }
        btnWinter.setOnClickListener { selectSeason("겨울", btnWinter) }
        styleButtons.forEach { button -> button.setOnClickListener { toggleStyleTag(button) } }
        purposeButtons.forEach { button -> button.setOnClickListener { togglePurposeTag(button) } }
    }

    private fun selectSeason(season: String, selectedButton: Button) {
        if (selectedSeason == season) {
            selectedSeason = ""
            selectedButton.isSelected = false
            return
        }
        selectedSeason = season
        listOf(btnSpringFall, btnSummer, btnWinter).forEach { btn -> btn.isSelected = false }
        selectedButton.isSelected = true
    }

    private fun toggleStyleTag(button: Button) {
        val tag = button.text.toString().replace("#", "")
        if (selectedStyleTags.contains(tag)) {
            selectedStyleTags.remove(tag)
            button.isSelected = false
        } else {
            selectedStyleTags.add(tag)
            button.isSelected = true
        }
    }

    private fun togglePurposeTag(button: Button) {
        val tag = button.text.toString().replace("#", "")
        if (selectedPurposeTags.contains(tag)) {
            selectedPurposeTags.remove(tag)
            button.isSelected = false
        } else {
            selectedPurposeTags.add(tag)
            button.isSelected = true
        }
    }

    private fun showBrandPopup() {
        adjustBrandPopupHeight()
        brandPopupOverlay.visibility = View.VISIBLE
    }

    private fun adjustBrandPopupHeight() {
        val brandScrollView = brandPopupOverlay.findViewById<ScrollView>(R.id.brand_scroll_view)
        if (brandScrollView != null) {
            val brandListContainer = brandScrollView.getChildAt(0) as LinearLayout
            val itemCount = brandListContainer.childCount / 2 + 1 // spacer 때문에 절반으로 나누고 1 추가

            // 🔥 UI 개선: 아이템 높이 증가 (패딩 증가 반영)
            val itemHeight = 64 * resources.displayMetrics.density // 기존 60 → 64
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

    /**
     * Repository를 사용한 필터 검색
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
                Log.d("WardrobeSearchFragment", "🔍 필터 검색 시작")
                Log.d("WardrobeSearchFragment", "선택된 필터 - 계절: '$selectedSeason', 색상: '$selectedColor', 브랜드: '$selectedBrand'")

                // 🔥 STEP 1: 전체 아이템 목록 가져오기
                val allItemsResult = repository.getAllWardrobeItems().getOrDefault(
                    com.example.onfit.Wardrobe.Network.WardrobeResult(
                        totalCount = 0,
                        items = emptyList(),
                        categories = emptyList()
                    )
                )

                val basicItems = allItemsResult.items
                Log.d("WardrobeSearchFragment", "📦 기본 아이템 로드: ${basicItems.size}개")

                if (basicItems.isEmpty()) {
                    Log.w("WardrobeSearchFragment", "⚠️ 등록된 아이템이 없습니다")
                    Toast.makeText(requireContext(), "등록된 아이템이 없습니다", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                // 🔥 STEP 2: 각 아이템의 상세 정보 로드
                Log.d("WardrobeSearchFragment", "🔄 모든 아이템의 상세 정보 로드 시작...")
                val detailedItems = mutableListOf<com.example.onfit.Wardrobe.Network.WardrobeItemDto>()

                basicItems.forEachIndexed { index, item ->
                    try {
                        Log.d("WardrobeSearchFragment", "📊 아이템 ${item.id} 상세정보 요청 (${index + 1}/${basicItems.size})")

                        val detailResult = repository.getWardrobeItemDetail(item.id)

                        if (detailResult.isSuccess) {
                            val detail = detailResult.getOrNull()
                            if (detail != null) {
                                val detailedItem = com.example.onfit.Wardrobe.Network.WardrobeItemDto(
                                    id = detail.id,
                                    image = detail.image ?: "",
                                    brand = detail.brand ?: "",
                                    season = detail.season,
                                    color = detail.color,
                                    category = detail.category,
                                    subcategory = detail.subcategory
                                )
                                detailedItems.add(detailedItem)
                                Log.d("WardrobeSearchFragment", "✅ 아이템 ${item.id} 로드 성공 - season=${detail.season}, color=${detail.color}, brand='${detail.brand}'")
                            } else {
                                Log.w("WardrobeSearchFragment", "⚠️ 아이템 ${item.id} 상세정보가 null")
                                detailedItems.add(item)
                            }
                        } else {
                            Log.e("WardrobeSearchFragment", "❌ 아이템 ${item.id} 상세정보 실패: ${detailResult.exceptionOrNull()?.message}")
                            detailedItems.add(item)
                        }
                    } catch (e: Exception) {
                        Log.e("WardrobeSearchFragment", "💥 아이템 ${item.id} 예외 발생", e)
                        detailedItems.add(item)
                    }
                }

                Log.d("WardrobeSearchFragment", "🎯 상세정보 로드 완료: ${detailedItems.size}개")
                wardrobeItems = detailedItems

                // 🔥 STEP 3: 로드된 상세 정보로 필터링 수행
                Log.d("WardrobeSearchFragment", "🔍 필터링 시작...")
                val filteredItems = performLocalFilter(detailedItems)
                Log.d("WardrobeSearchFragment", "🎉 필터링 완료: ${filteredItems.size}개 아이템 발견")

                // 🔥 STEP 4: FragmentResult 전달 - UI 스레드에서 실행
                withContext(Dispatchers.Main) {
                    Log.d("WardrobeSearchFragment", "📡 Main 스레드에서 FragmentResult 전달 시작...")

                    val filteredIds = filteredItems.map { it.id }.toIntArray()
                    Log.d("WardrobeSearchFragment", "📦 Bundle 데이터:")
                    Log.d("WardrobeSearchFragment", "  - filtered_item_ids: ${filteredIds.contentToString()}")
                    Log.d("WardrobeSearchFragment", "  - filter_applied: true")

                    val bundle = Bundle().apply {
                        putIntArray("filtered_item_ids", filteredIds)
                        putString("search_query", "필터 검색")
                        putString("filter_season", selectedSeason)
                        putString("filter_color", selectedColor)
                        putString("filter_brand", selectedBrand)
                        putBoolean("filter_applied", true)
                    }

                    // 🔥 FragmentManager 상태 확인
                    Log.d("WardrobeSearchFragment", "📋 FragmentManager 상태:")
                    Log.d("WardrobeSearchFragment", "  - parentFragmentManager: $parentFragmentManager")
                    Log.d("WardrobeSearchFragment", "  - isDestroyed: ${parentFragmentManager.isDestroyed}")
                    Log.d("WardrobeSearchFragment", "  - fragment isAdded: $isAdded")
                    Log.d("WardrobeSearchFragment", "  - fragment context: ${context != null}")

                    if (isAdded && context != null && !parentFragmentManager.isDestroyed) {
                        try {
                            // 🔥 FragmentResult 전달
                            parentFragmentManager.setFragmentResult("search_results", bundle)
                            Log.d("WardrobeSearchFragment", "✅ FragmentResult 전달 성공!")

                            // 🔥 결과 메시지 표시
                            val message = if (filteredItems.isNotEmpty()) {
                                "${filteredItems.size}개의 아이템을 찾았습니다"
                            } else {
                                "검색 조건에 맞는 아이템이 없습니다"
                            }

                            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                            Log.d("WardrobeSearchFragment", "📢 토스트 메시지 표시: $message")

                            // 🔥 약간의 지연 후 Navigation (FragmentResult 전달 보장)
                            view?.postDelayed({
                                if (isAdded && context != null) {
                                    try {
                                        Log.d("WardrobeSearchFragment", "🚪 Navigation 시도...")
                                        findNavController().navigateUp()
                                        Log.d("WardrobeSearchFragment", "✅ Navigation 성공!")
                                    } catch (e: Exception) {
                                        Log.e("WardrobeSearchFragment", "❌ Navigation 실패", e)
                                    }
                                }
                            }, 100) // 100ms 지연

                        } catch (e: Exception) {
                            Log.e("WardrobeSearchFragment", "❌ FragmentResult 전달 실패", e)
                            Toast.makeText(requireContext(), "검색 결과 전달 실패: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Log.e("WardrobeSearchFragment", "❌ Fragment 상태 불량 - FragmentResult 전달 불가")
                        Log.e("WardrobeSearchFragment", "  - isAdded: $isAdded, context: ${context != null}, managerDestroyed: ${parentFragmentManager.isDestroyed}")
                    }
                }

            } catch (e: Exception) {
                Log.e("WardrobeSearchFragment", "💥 필터 검색 중 치명적 오류 발생", e)
                withContext(Dispatchers.Main) {
                    showError("필터 검색 중 오류가 발생했습니다: ${e.message}")
                }
            }
        }
    }

    /**
     * 로컬 필터링 - 디버깅 강화 버전
     */
    private fun performLocalFilter(items: List<WardrobeItemDto>): List<WardrobeItemDto> {
        Log.d("WardrobeSearchFragment", "=== 통합 필터링 시작 ===")
        Log.d("WardrobeSearchFragment", "전체 아이템 수: ${items.size}")
        Log.d("WardrobeSearchFragment", "선택된 필터:")
        Log.d("WardrobeSearchFragment", "  - 계절: '$selectedSeason'")
        Log.d("WardrobeSearchFragment", "  - 색상: '$selectedColor'")
        Log.d("WardrobeSearchFragment", "  - 브랜드: '$selectedBrand'")

        // 🔥 모든 아이템의 현재 상태 로깅
        Log.d("WardrobeSearchFragment", "--- 모든 아이템 상태 확인 ---")
        items.forEachIndexed { index, item ->
            val itemType = if (item.id < 0) "더미" else "API"
            Log.d("WardrobeSearchFragment", "아이템 ${index + 1} ($itemType): ID=${item.id}, season=${item.season}, color=${item.color}, brand='${item.brand}'")
        }

        // 🔥 필터링 수행
        val filteredItems = items.filter { item ->
            var matches = true
            val reasons = mutableListOf<String>()
            val itemType = if (item.id < 0) "더미" else "API"

            // 계절 필터 확인
            if (selectedSeason.isNotEmpty()) {
                val seasonId = convertSeasonToAPI(selectedSeason)
                if (seasonId != null) {
                    val seasonMatch = item.season == seasonId
                    Log.d("WardrobeSearchFragment", "$itemType 아이템 ${item.id} 계절 체크: ${item.season} == $seasonId → $seasonMatch")
                    if (!seasonMatch) {
                        matches = false
                        reasons.add("계절 불일치 (${item.season} != $seasonId)")
                    }
                } else {
                    Log.w("WardrobeSearchFragment", "⚠️ 알 수 없는 계절: '$selectedSeason'")
                }
            }

            // 색상 필터 확인
            if (selectedColor.isNotEmpty()) {
                val colorId = convertColorToAPI(selectedColor)
                if (colorId != null) {
                    val colorMatch = item.color == colorId
                    Log.d("WardrobeSearchFragment", "$itemType 아이템 ${item.id} 색상 체크: ${item.color} == $colorId → $colorMatch")
                    if (!colorMatch) {
                        matches = false
                        reasons.add("색상 불일치 (${item.color} != $colorId)")
                    }
                } else {
                    Log.w("WardrobeSearchFragment", "⚠️ 알 수 없는 색상: '$selectedColor'")
                }
            }

            // 🔥 브랜드 필터 확인 (null 안전성 개선)
            if (selectedBrand.isNotEmpty()) {
                val brandMatch = item.brand?.contains(selectedBrand, ignoreCase = true) == true
                Log.d("WardrobeSearchFragment", "$itemType 아이템 ${item.id} 브랜드 체크: '${item.brand}' contains '$selectedBrand' → $brandMatch")
                if (!brandMatch) {
                    matches = false
                    reasons.add("브랜드 불일치")
                }
            }

            // 결과 로깅
            if (matches) {
                Log.d("WardrobeSearchFragment", "✅ $itemType 아이템 ${item.id} 조건 충족!")
            } else {
                Log.d("WardrobeSearchFragment", "❌ $itemType 아이템 ${item.id} 조건 불충족: ${reasons.joinToString(", ")}")
            }

            matches
        }

        Log.d("WardrobeSearchFragment", "=== 통합 필터링 결과 ===")
        Log.d("WardrobeSearchFragment", "조건에 맞는 아이템: ${filteredItems.size}개")
        filteredItems.forEach { item ->
            val itemType = if (item.id < 0) "더미" else "API"
            Log.d("WardrobeSearchFragment", "✅ 결과 아이템 ($itemType): ID=${item.id}, season=${item.season}, color=${item.color}, brand='${item.brand}'")
        }
        Log.d("WardrobeSearchFragment", "=== 통합 필터링 끝 ===")

        return filteredItems
    }

    private fun convertSeasonToAPI(season: String): Int? {
        val result = when (season) {
            "봄ㆍ가을" -> 1
            "여름" -> 2
            "겨울" -> 4
            else -> null
        }
        Log.d("WardrobeSearchFragment", "계절 변환: '$season' → $result")
        return result
    }

    private fun convertColorToAPI(color: String): Int? {
        val result = when (color) {
            "블랙" -> 1
            "화이트" -> 2
            "그레이" -> 3
            "네이비" -> 4
            "베이지" -> 5
            "브라운" -> 6
            "레드" -> 7
            "핑크" -> 8
            "오렌지" -> 9
            "옐로우" -> 10
            "그린" -> 11
            "블루" -> 12
            "퍼플" -> 13
            "스카이블루" -> 14
            "오트밀" -> 15
            "아이보리" -> 16
            else -> null
        }
        Log.d("WardrobeSearchFragment", "색상 변환: '$color' → $result")
        return result
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        Log.e("WardrobeSearchFragment", message)
    }

    // 🔥 더미 데이터 로드 함수 추가 (WardrobeFragment에서 복사)
    private fun loadDummyWardrobeFromAssets(): List<WardrobeItemDto> {
        if (!USE_SEARCH_DUMMY) return emptyList()

        try {
            Log.d("WardrobeSearchFragment", "🎭 하드코딩된 더미 옷장 아이템 생성 시작")

            val hardcodedItems = listOf(
                // 코디 1 관련 아이템들
                HardcodedWardrobeItem(
                    imageName = "shirts1",
                    category = 1, subcategory = 4,
                    brand = "자라", season = 2, color = 2,
                    tag1 = 1, tag2 = null, tag3 = null,
                    purchasePlace = "자라 강남점",
                    purchasePrice = "59,000원",
                    purchaseDate = "2024-03-15"
                ),
                HardcodedWardrobeItem(
                    imageName = "pants1",
                    category = 2, subcategory = 10,
                    brand = "유니클로", season = 1, color = 6,
                    tag1 = 1, tag2 = 4, tag3 = null,
                    purchasePlace = "유니클로 온라인",
                    purchasePrice = "29,900원",
                    purchaseDate = "2024-02-20"
                ),
                HardcodedWardrobeItem(
                    imageName = "shoes1",
                    category = 5, subcategory = 29,
                    brand = "나이키", season = 2, color = 6,
                    tag1 = 2, tag2 = 4, tag3 = null,
                    purchasePlace = "나이키 공식몰",
                    purchasePrice = "139,000원",
                    purchaseDate = "2024-01-10"
                ),
                HardcodedWardrobeItem(
                    imageName = "shirts2",
                    category = 1, subcategory = 1,
                    brand = "자라", season = 2, color = 1,
                    tag1 = 10, tag2 = null, tag3 = null,
                    purchasePlace = "자라 홍대점",
                    purchasePrice = "19,900원",
                    purchaseDate = "2024-06-05"
                ),
                HardcodedWardrobeItem(
                    imageName = "pants2",
                    category = 2, subcategory = 9,
                    brand = "리바이스", season = 2, color = 6,
                    tag1 = null, tag2 = null, tag3 = null,
                    purchasePlace = "리바이스 매장",
                    purchasePrice = "89,000원",
                    purchaseDate = "2024-05-12"
                ),
                HardcodedWardrobeItem(
                    imageName = "shoes2",
                    category = 5, subcategory = 29,
                    brand = "아디다스", season = 1, color = 1,
                    tag1 = 2, tag2 = 13, tag3 = null,
                    purchasePlace = "아디다스 온라인",
                    purchasePrice = "119,000원",
                    purchaseDate = "2024-04-08"
                ),
                HardcodedWardrobeItem(
                    imageName = "shirts3",
                    category = 1, subcategory = 4,
                    brand = "H&M", season = 2, color = 1,
                    tag1 = 3, tag2 = 11, tag3 = null,
                    purchasePlace = "H&M 명동점",
                    purchasePrice = "24,900원",
                    purchaseDate = "2024-07-01"
                ),
                HardcodedWardrobeItem(
                    imageName = "shoes3",
                    category = 5, subcategory = 29,
                    brand = "닥터마틴", season = 1, color = 2,
                    tag1 = 3, tag2 = 17, tag3 = null,
                    purchasePlace = "닥터마틴 강남점",
                    purchasePrice = "259,000원",
                    purchaseDate = "2024-03-20"
                ),
                HardcodedWardrobeItem(
                    imageName = "pants3",
                    category = 2, subcategory = 10,
                    brand = "MCM", season = 1, color = 1,
                    tag1 = 9, tag2 = 11, tag3 = null,
                    purchasePlace = "MCM 백화점",
                    purchasePrice = "189,000원",
                    purchaseDate = "2024-02-14"
                ),
                HardcodedWardrobeItem(
                    imageName = "acc3",
                    category = 6, subcategory = 40,
                    brand = "무지", season = 2, color = 1,
                    tag1 = 9, tag2 = null, tag3 = null,
                    purchasePlace = "무지 매장",
                    purchasePrice = "39,000원",
                    purchaseDate = "2024-06-20"
                ),
                HardcodedWardrobeItem(
                    imageName = "shirts4",
                    category = 1, subcategory = 4,
                    brand = "유니클로", season = 2, color = 3,
                    tag1 = 2, tag2 = 11, tag3 = null,
                    purchasePlace = "유니클로 홍대점",
                    purchasePrice = "29,900원",
                    purchaseDate = "2024-06-15"
                ),
                HardcodedWardrobeItem(
                    imageName = "pants4",
                    category = 2, subcategory = 10,
                    brand = "자라", season = 1, color = 1,
                    tag1 = 4, tag2 = 15, tag3 = null,
                    purchasePlace = "자라 온라인",
                    purchasePrice = "39,900원",
                    purchaseDate = "2024-04-25"
                ),
                HardcodedWardrobeItem(
                    imageName = "bag4",
                    category = 6, subcategory = 41,
                    brand = "무지", season = 2, color = 1,
                    tag1 = 4, tag2 = 11, tag3 = null,
                    purchasePlace = "무지 매장",
                    purchasePrice = "49,000원",
                    purchaseDate = "2024-05-30"
                ),
                HardcodedWardrobeItem(
                    imageName = "shoes4",
                    category = 5, subcategory = 31,
                    brand = "무지", season = 2, color = 1,
                    tag1 = 13, tag2 = null, tag3 = null,
                    purchasePlace = "무지 온라인",
                    purchasePrice = "29,900원",
                    purchaseDate = "2024-07-10"
                )
            )

            // WardrobeItemDto로 변환
            val dummyItems = hardcodedItems.mapIndexed { index, item ->
                WardrobeItemDto(
                    id = -(1000 + index),
                    image = "drawable://${item.imageName}",
                    brand = item.brand,
                    season = item.season,
                    color = item.color,
                    category = item.category,
                    subcategory = item.subcategory
                )
            }

            Log.d("WardrobeSearchFragment", "✅ 하드코딩된 더미 옷장 아이템 ${dummyItems.size}개 생성")

            return dummyItems

        } catch (e: Exception) {
            Log.e("WardrobeSearchFragment", "하드코딩된 더미 데이터 생성 실패", e)
            return emptyList()
        }
    }

    // 🔥 더미 데이터 클래스 추가
    data class HardcodedWardrobeItem(
        val imageName: String,
        val category: Int,
        val subcategory: Int,
        val brand: String,
        val season: Int,
        val color: Int,
        val tag1: Int?,
        val tag2: Int?,
        val tag3: Int?,
        val purchasePlace: String,
        val purchasePrice: String,
        val purchaseDate: String
    )


}