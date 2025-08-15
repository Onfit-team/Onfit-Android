package com.example.onfit.Wardrobe.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.example.onfit.R
import com.example.onfit.KakaoLogin.util.TokenProvider
import com.example.onfit.Wardrobe.Network.WardrobeRetrofitClient
import com.example.onfit.Wardrobe.Network.WardrobeItemDto
import com.example.onfit.Wardrobe.repository.WardrobeRepository
import android.util.Log

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
    private val colorOptions = arrayOf("색상 선택", "블랙", "화이트", "그레이", "네이비", "브라운", "베이지", "레드", "핑크", "옐로우", "그린", "블루", "퍼플", "스카이블루", "오트밀", "아이보리")
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
        // Repository 초기화
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
        setupSearchResultListener()

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

        // Purpose buttons - LinearLayout
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
     * Repository를 사용한 브랜드 목록 로드
     */
    private fun loadBrandsFromAPI() {
        lifecycleScope.launch {
            try {
                repository.getBrandsList()
                    .onSuccess { brands ->
                        if (brands.isNotEmpty()) {
                            brandOptions = brands.toTypedArray()
                            setupBrandSelectionWithAPI(brands)
                            Log.d("WardrobeSearchFragment", "브랜드 목록 로드 성공: ${brands.size}개")
                        } else {
                            setupDummyBrands()
                        }
                    }
                    .onFailure { exception ->
                        Log.w("WardrobeSearchFragment", "브랜드 API 호출 실패: ${exception.message}")
                        setupDummyBrands()
                    }
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
        val brandScrollView = brandPopupOverlay.findViewById<ScrollView>(R.id.brand_scroll_view)
        val brandContainer = brandScrollView?.getChildAt(0) as? LinearLayout

        brandContainer?.removeAllViews()

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

            val divider = View(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    1
                )
                setBackgroundColor(resources.getColor(R.color.gray, requireContext().theme))
            }

            brandContainer?.addView(brandTextView)
            if (brandName != brands.last()) {
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

        // Brand popup overlay
        brandPopupOverlay.setOnClickListener {
            hideBrandPopup()
        }

        // Prevent popup from closing when clicking on content
        brandPopupOverlay.getChildAt(1)?.setOnClickListener { }

        // Save button
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
        } else {
            selectedStyleTags.add(tag)
            button.isSelected = true
        }

        Log.d("WardrobeSearchFragment", "현재 분위기 태그들: ${selectedStyleTags.joinToString(", ")}")
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

    private fun setupSearchResultListener() {
        parentFragmentManager.setFragmentResultListener("search_results", this) { _, bundle ->
            val filteredIds = bundle.getIntArray("filtered_item_ids")

            if (filteredIds != null) {
                val filteredItems = wardrobeItems.filter { it.id in filteredIds }
                Log.d("WardrobeSearchFragment", "${filteredItems.size}개 아이템 검색됨")
                Toast.makeText(context, "${filteredItems.size}개 아이템 검색됨", Toast.LENGTH_SHORT).show()
            }
        }
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
                        putString("filter_season", selectedSeason)
                        putString("filter_color", selectedColor)
                        putString("filter_brand", selectedBrand)
                    }

                    parentFragmentManager.setFragmentResult("search_results", bundle)
                    Toast.makeText(context, "${filteredItems.size}개의 아이템을 찾았습니다", Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                } else {
                    Toast.makeText(context, "검색 결과가 없습니다", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                Log.e("WardrobeSearchFragment", "필터 검색 실패", e)
                showError("필터 검색 중 오류가 발생했습니다: ${e.message}")
            }
        }
    }

    /**
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

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        Log.e("WardrobeSearchFragment", message)
    }
}