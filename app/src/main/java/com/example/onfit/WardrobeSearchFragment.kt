package com.example.onfit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
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
    private val brandOptions = arrayOf("아디다스", "나이키", "자라", "유니클로", "H&M", "무인사", "SPAO")

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
        // Back button - Activity 종료로 변경
        icBack.setOnClickListener {
            requireActivity().finish()
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

        // Navigate back to previous screen
        requireActivity().finish()
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