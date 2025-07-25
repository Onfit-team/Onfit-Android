package com.example.onfit.Wardrobe.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.onfit.R
import com.google.android.flexbox.FlexboxLayout

class WardrobeSearchFragment : Fragment() {

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
    private val colorOptions = arrayOf("색상 선택", "블랙", "화이트", "그레이", "네이비", "브라운", "베이지", "레드", "핑크", "옐로우", "그린", "블루", "퍼플")
    private val brandOptions = arrayOf("아디다스", "나이키", "자라", "유니클로", "H&M", "무신사", "SPAO")

    private var selectedSeason = ""
    private var selectedColor = ""
    private var selectedBrand = ""
    private val selectedStyleTags = mutableSetOf<String>()
    private val selectedPurposeTags = mutableSetOf<String>()

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
    }

    override fun onResume() {
        super.onResume()
        // 바텀네비게이션 숨기기
        activity?.findViewById<View>(R.id.bottomNavigationView)?.visibility = View.GONE
    }

    override fun onPause() {
        super.onPause()
        // 바텀네비게이션 다시 보이기
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

        // Purpose buttons - 둘 다 LinearLayout으로 통일
        val purposeLayout1 = view.findViewById<LinearLayout>(R.id.topCategoryLayout3)
        val purposeLayout2 = view.findViewById<LinearLayout>(R.id.topCategoryLayout4)

        addButtonsFromLinearLayout(purposeLayout1, purposeButtons)
        addButtonsFromLinearLayout(purposeLayout2, purposeButtons)
    }

    private fun addButtonsFromFlexboxLayout(layout: FlexboxLayout?, buttonList: MutableList<Button>) {
        layout?.let {
            for (i in 0 until it.childCount) {
                val child = it.getChildAt(i)
                if (child is Button) {
                    buttonList.add(child)
                }
            }
        }
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

    private fun setupListeners() {
        // Back button - Navigation으로 뒤로가기 (수정됨)
        icBack.setOnClickListener {
            findNavController().navigateUp()
            // 또는 parentFragmentManager.popBackStack() 사용
        }

        // Brand dropdown
        brandDropdownContainer.setOnClickListener {
            showBrandPopup()
        }

        // Brand popup overlay (close popup when clicking outside)
        brandPopupOverlay.setOnClickListener {
            hideBrandPopup()
        }

        // Prevent popup from closing when clicking on the popup content
        brandPopupOverlay.getChildAt(1)?.setOnClickListener {
            // Do nothing - prevents the click from bubbling up to the overlay
        }

        // Brand selection
        setupBrandSelection()

        // Save button
        btnSave.setOnClickListener {
            applyFilters()
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

        // 색상 스피너 컨테이너와 화살표 클릭 이벤트 추가
        setupColorSpinnerClick()
    }

    private fun setupColorSpinnerClick() {
        // 색상 스피너가 포함된 LinearLayout 찾기
        val colorSpinnerContainer = spinnerColor.parent as? LinearLayout

        // 드롭다운 위치 조정
        spinnerColor.setOnTouchListener { _, _ ->
            spinnerColor.post {
                adjustColorDropdownPosition(spinnerColor, colorSpinnerContainer)
            }
            false
        }

        // 전체 컨테이너 클릭 시 스피너 열기
        colorSpinnerContainer?.setOnClickListener {
            spinnerColor.performClick()
        }

        // 컨테이너 내의 ImageView(화살표) 클릭 시도 스피너 열기
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

            // 스피너와 컨테이너의 실제 위치 계산
            val spinnerLocation = IntArray(2)
            val containerLocation = IntArray(2)

            spinner.getLocationOnScreen(spinnerLocation)
            spinnerContainer.getLocationOnScreen(containerLocation)

            // 가로: 스피너가 컨테이너 왼쪽 경계로부터 얼마나 떨어져 있는지 계산
            // 빨간색 박스(컨테이너)의 왼쪽 경계에서 시작하도록 조정
            val horizontalOffsetToContainerLeft = spinnerLocation[0] - containerLocation[0]

            // 세로: -16dp 적용 (박스 위로 살짝 올리기)
            val verticalOffset = -(16 * resources.displayMetrics.density).toInt()

            android.util.Log.d("ColorSpinner", "Spinner pos: ${spinnerLocation[0]}, Container pos: ${containerLocation[0]}")
            android.util.Log.d("ColorSpinner", "Container width: ${spinnerContainer.width}")
            android.util.Log.d("ColorSpinner", "Horizontal offset to container left: $horizontalOffsetToContainerLeft")

            // 컨테이너 너비로 드롭다운 너비 설정 (빨간색 박스와 동일한 너비)
            val containerWidth = spinnerContainer.width
            val setWidthMethod = popupWindow.javaClass.getMethod("setWidth", Int::class.java)
            setWidthMethod.invoke(popupWindow, containerWidth)

            // 높이 제한
            val maxHeight = (250 * resources.displayMetrics.density).toInt()
            val setHeightMethod = popupWindow.javaClass.getMethod("setHeight", Int::class.java)
            setHeightMethod.invoke(popupWindow, maxHeight)

            // 가로 위치: 빨간색 박스(컨테이너)의 왼쪽 경계에서 시작
            val setHorizontalOffsetMethod = popupWindow.javaClass.getMethod("setHorizontalOffset", Int::class.java)
            setHorizontalOffsetMethod.invoke(popupWindow, -horizontalOffsetToContainerLeft)

            // 세로 위치: -16dp 적용
            try {
                val setVerticalOffsetMethod = popupWindow.javaClass.getMethod("setVerticalOffset", Int::class.java)
                setVerticalOffsetMethod.invoke(popupWindow, verticalOffset)
                android.util.Log.d("ColorSpinner", "Vertical offset applied: $verticalOffset")
            } catch (e: Exception) {
                android.util.Log.e("ColorSpinner", "Vertical offset failed: ${e.message}")
            }

            android.util.Log.d("ColorSpinner", "Color dropdown positioned at container left edge")

        } catch (e: Exception) {
            android.util.Log.e("ColorSpinner", "Failed to adjust color dropdown: ${e.message}")
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
        // Allow deselection by clicking the same button
        if (selectedSeason == season) {
            selectedSeason = ""
            selectedButton.isSelected = false
            return
        }

        selectedSeason = season

        // Reset all season buttons
        listOf(btnSpringFall, btnSummer, btnWinter).forEach { btn ->
            btn.isSelected = false
        }

        // Select current button
        selectedButton.isSelected = true
    }

    private fun toggleStyleTag(button: Button) {
        val tag = button.text.toString()

        if (selectedStyleTags.contains(tag)) {
            selectedStyleTags.remove(tag)
            button.isSelected = false
        } else {
            selectedStyleTags.add(tag)
            button.isSelected = true
        }
    }

    private fun togglePurposeTag(button: Button) {
        val tag = button.text.toString()

        if (selectedPurposeTags.contains(tag)) {
            selectedPurposeTags.remove(tag)
            button.isSelected = false
        } else {
            selectedPurposeTags.add(tag)
            button.isSelected = true
        }
    }

    private fun setupBrandSelection() {
        val brandViews = listOf(
            R.id.brand_adidas to "아디다스",
            R.id.brand_nike to "나이키",
            R.id.brand_zara to "자라",
            R.id.brand_uniqlo to "유니클로",
            R.id.brand_hm to "H&M",
            R.id.brand_muji to "무인사",
            R.id.brand_spao to "SPAO"
        )

        brandViews.forEach { (viewId, brandName) ->
            brandPopupOverlay.findViewById<TextView>(viewId)?.setOnClickListener {
                selectedBrand = brandName
                brandDropdownText.text = brandName
                brandDropdownText.setTextColor(
                    resources.getColor(android.R.color.black, requireContext().theme)
                )
                hideBrandPopup()
            }
        }
    }

    private fun showBrandPopup() {
        adjustBrandPopupHeight()
        brandPopupOverlay.visibility = View.VISIBLE
    }

    private fun adjustBrandPopupHeight() {
        // Find the ScrollView in the popup
        val brandScrollView = brandPopupOverlay.findViewById<ScrollView>(R.id.brand_scroll_view)

        if (brandScrollView != null) {
            val brandListContainer = brandScrollView.getChildAt(0) as LinearLayout

            // Calculate the total height needed for all brand items
            val itemCount = brandListContainer.childCount
            val itemHeight = 60 * resources.displayMetrics.density // 60dp converted to pixels
            val totalContentHeight = (itemCount * itemHeight).toInt()

            // Get screen height
            val displayMetrics = resources.displayMetrics
            val screenHeight = displayMetrics.heightPixels

            // Calculate maximum allowed height (60% of screen height)
            val maxScrollViewHeight = (screenHeight * 0.6).toInt()

            // Set appropriate height
            val scrollViewLayoutParams = brandScrollView.layoutParams
            scrollViewLayoutParams.height = if (totalContentHeight > maxScrollViewHeight) {
                maxScrollViewHeight
            } else {
                totalContentHeight
            }
            brandScrollView.layoutParams = scrollViewLayoutParams
        } else {
            // If ScrollView is not found by ID, try to find it by traversing the view hierarchy
            val popupContent = brandPopupOverlay.getChildAt(1) as? LinearLayout
            val scrollView = popupContent?.getChildAt(1) as? ScrollView

            scrollView?.let {
                val brandListContainer = it.getChildAt(0) as LinearLayout
                val itemCount = brandListContainer.childCount
                val itemHeight = 60 * resources.displayMetrics.density
                val totalContentHeight = (itemCount * itemHeight).toInt()

                val displayMetrics = resources.displayMetrics
                val screenHeight = displayMetrics.heightPixels
                val maxScrollViewHeight = (screenHeight * 0.6).toInt()

                val scrollViewLayoutParams = it.layoutParams
                scrollViewLayoutParams.height = if (totalContentHeight > maxScrollViewHeight) {
                    maxScrollViewHeight
                } else {
                    totalContentHeight
                }
                it.layoutParams = scrollViewLayoutParams
            }
        }

        // 브랜드 팝업 상단 모서리 둥글게 만들기
        setBrandPopupCornerRadius()
    }

    private fun setBrandPopupCornerRadius() {
        val popupContent = brandPopupOverlay.getChildAt(1) as? LinearLayout
        popupContent?.let {
            // 상단 모서리만 둥글게 하는 drawable 생성
            val cornerRadius = 20f * resources.displayMetrics.density // 20dp를 px로 변환

            val drawable = android.graphics.drawable.GradientDrawable()
            drawable.setColor(android.graphics.Color.WHITE)
            drawable.cornerRadii = floatArrayOf(
                cornerRadius, cornerRadius, // 좌상단
                cornerRadius, cornerRadius, // 우상단
                0f, 0f,                     // 우하단
                0f, 0f                      // 좌하단
            )

            it.background = drawable
        }
    }

    private fun hideBrandPopup() {
        brandPopupOverlay.visibility = View.GONE
    }

    private fun applyFilters() {
        // Validate if at least one filter is selected
        val hasFilters = selectedSeason.isNotEmpty() ||
                selectedColor.isNotEmpty() ||
                selectedBrand.isNotEmpty() ||
                selectedStyleTags.isNotEmpty() ||
                selectedPurposeTags.isNotEmpty()

        if (!hasFilters) {
            Toast.makeText(requireContext(), "최소 하나의 필터를 선택해주세요", Toast.LENGTH_SHORT).show()
            return
        }

        // Create filter object
        val filterData = FilterData(
            season = selectedSeason,
            color = selectedColor,
            brand = selectedBrand,
            styleTags = selectedStyleTags.toList(),
            purposeTags = selectedPurposeTags.toList()
        )

        // TODO: Apply filters to search results
        // This would typically involve:
        // 1. Calling API with filter parameters
        // 2. Updating UI with filtered results
        // 3. Navigating to results screen

        // For now, show confirmation
        val filterSummary = buildString {
            append("적용된 필터:\n")
            if (selectedSeason.isNotEmpty()) append("계절: $selectedSeason\n")
            if (selectedColor.isNotEmpty()) append("색상: $selectedColor\n")
            if (selectedBrand.isNotEmpty()) append("브랜드: $selectedBrand\n")
            if (selectedStyleTags.isNotEmpty()) append("스타일: ${selectedStyleTags.joinToString(", ")}\n")
            if (selectedPurposeTags.isNotEmpty()) append("용도: ${selectedPurposeTags.joinToString(", ")}")
        }

        Toast.makeText(requireContext(), filterSummary, Toast.LENGTH_LONG).show()

        // Navigate back to previous screen (수정됨)
        findNavController().navigateUp()
        // 또는 parentFragmentManager.popBackStack() 사용
    }

    // Function to reset all filters
    private fun resetFilters() {
        selectedSeason = ""
        selectedColor = ""
        selectedBrand = ""
        selectedStyleTags.clear()
        selectedPurposeTags.clear()

        // Reset UI
        listOf(btnSpringFall, btnSummer, btnWinter).forEach { it.isSelected = false }
        styleButtons.forEach { it.isSelected = false }
        purposeButtons.forEach { it.isSelected = false }

        spinnerColor.setSelection(0)
        brandDropdownText.text = "브랜드를 선택하세요"
        brandDropdownText.setTextColor(
            resources.getColor(android.R.color.darker_gray, requireContext().theme)
        )
    }

    data class FilterData(
        val season: String,
        val color: String,
        val brand: String,
        val styleTags: List<String>,
        val purposeTags: List<String>
    )
}