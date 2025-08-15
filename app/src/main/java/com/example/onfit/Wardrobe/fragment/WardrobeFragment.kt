package com.example.onfit.Wardrobe.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.onfit.R
import com.example.onfit.RegisterItemBottomSheet
import com.example.onfit.Wardrobe.adapter.WardrobeAdapter
import com.example.onfit.Wardrobe.repository.WardrobeRepository
import com.example.onfit.Wardrobe.viewmodel.WardrobeViewModel
import com.example.onfit.Wardrobe.viewmodel.WardrobeUiState
import com.example.onfit.Wardrobe.Network.WardrobeItemDto
import com.example.onfit.Wardrobe.Network.CategoryDto
import com.example.onfit.Wardrobe.Network.SubcategoryDto
import kotlinx.coroutines.launch
import java.util.*

open class WardrobeFragment : Fragment() {

    // MVVM 구조
    private lateinit var repository: WardrobeRepository
    private lateinit var viewModel: WardrobeViewModel

    // UI 컴포넌트
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: WardrobeAdapter
    private lateinit var subFilterLayout: LinearLayout
    private lateinit var subFilterScrollView: HorizontalScrollView

    // 상태 관리
    private var selectedIndex = 0
    private var selectedTopCategoryButton: Button? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Repository와 ViewModel 초기화
        repository = WardrobeRepository(requireContext())
        viewModel = WardrobeViewModel(requireContext())

        Log.d("WardrobeFragment", "Repository 토큰 정보: ${repository.getTokenInfo()}")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_wardrobe, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViews(view)
        setupRecyclerView()
        setupTopCategoryButtons(view)
        setupFragmentResultListeners()
        observeViewModel()

        // 초기 데이터 로드
        viewModel.loadAllWardrobeItems()
    }

    /**
     * ViewModel 상태 관찰
     */
    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                handleUiState(state)
            }
        }
    }

    /**
     * UI 상태 처리
     */
    private fun handleUiState(state: WardrobeUiState) {
        // 로딩 상태 처리
        if (state.isLoading) {
            showLoading(true)
        } else {
            showLoading(false)
        }

        // 데이터 업데이트
        if (state.hasData) {
            adapter.updateWithApiData(state.wardrobeItems)
            updateCategoryButtonsWithCount(state.categories)
            updateSubCategories(state.subcategories)
        }

        // 에러 처리
        if (state.hasError) {
            showError(state.errorMessage ?: "알 수 없는 오류가 발생했습니다")
            viewModel.clearErrorMessage()
        }

        // 등록 성공 처리
        if (state.registrationSuccess) {
            Toast.makeText(context, "아이템이 등록되었습니다", Toast.LENGTH_SHORT).show()
            viewModel.clearRegistrationSuccess()

            // CalendarFragment에 알림
            notifyCalendarFragmentOfNewItem(getCurrentDate())
        }

        // 빈 상태 처리
        if (state.showEmptyState) {
            showEmptyState(true)
        } else {
            showEmptyState(false)
        }
    }

    /**
     * Fragment Result Listener 설정
     */
    private fun setupFragmentResultListeners() {
        // 아이템 등록 완료 결과 받기
        parentFragmentManager.setFragmentResultListener("item_registered", this) { _, bundle ->
            val isSuccess = bundle.getBoolean("success", false)
            val registeredDate = bundle.getString("registered_date")

            if (isSuccess) {
                // ViewModel을 통해 데이터 새로고침
                viewModel.refreshWardrobeItems()

                // CalendarFragment에 알림 전달
                notifyCalendarFragmentOfNewItem(registeredDate)

                Toast.makeText(context, "아이템이 등록되었습니다", Toast.LENGTH_SHORT).show()
            }
        }

        // 검색 결과 받기
        parentFragmentManager.setFragmentResultListener("search_results", this) { _, bundle ->
            val filteredIds = bundle.getIntArray("filtered_item_ids")
            if (filteredIds != null) {
                val currentItems = viewModel.uiState.value.wardrobeItems
                val filteredItems = currentItems.filter { it.id in filteredIds }

                // 추가 로컬 필터링
                val finalItems = applyLocalFiltering(bundle, filteredItems)
                adapter.updateWithApiData(finalItems)

                Toast.makeText(context, "${finalItems.size}개 아이템 검색됨", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * CalendarFragment에 새 아이템 등록 알림
     */
    private fun notifyCalendarFragmentOfNewItem(registeredDate: String?) {
        val bundle = Bundle().apply {
            putString("registered_date", registeredDate ?: getCurrentDate())
            putBoolean("wardrobe_updated", true)
            putLong("timestamp", System.currentTimeMillis())
        }

        parentFragmentManager.setFragmentResult("outfit_registered", bundle)
        Log.d("WardrobeFragment", "CalendarFragment에 알림 전달: $registeredDate")
    }

    /**
     * 로컬 필터링 적용
     */
    private fun applyLocalFiltering(bundle: Bundle, items: List<WardrobeItemDto>): List<WardrobeItemDto> {
        var filteredItems = items

        bundle.getString("filter_season")?.let { season ->
            if (season.isNotEmpty()) {
                val seasonId = when (season) {
                    "봄ㆍ가을" -> 1
                    "여름" -> 2
                    "겨울" -> 4
                    else -> null
                }
                seasonId?.let {
                    filteredItems = filteredItems.filter { it.season == seasonId }
                }
            }
        }

        bundle.getString("filter_brand")?.let { brand ->
            if (brand.isNotEmpty()) {
                filteredItems = filteredItems.filter {
                    it.brand.contains(brand, ignoreCase = true)
                }
            }
        }

        return filteredItems
    }

    private fun initializeViews(view: View) {
        recyclerView = view.findViewById(R.id.wardrobeRecyclerView)
        subFilterLayout = view.findViewById(R.id.subFilterLayout)
        subFilterScrollView = view.findViewById(R.id.subFilterScrollView)

        setupAddButton(view)
        setupSearchButton(view)
    }

    private fun setupAddButton(view: View) {
        val addButton = view.findViewById<ImageButton>(R.id.wardrobe_register_btn)
        addButton?.setOnClickListener {
            val bottomSheet = RegisterItemBottomSheet()
            bottomSheet.show(parentFragmentManager, "RegisterItemBottomSheet")
        }
    }

    private fun setupSearchButton(view: View) {
        val searchButton = view.findViewById<ImageButton>(R.id.ic_search)
        searchButton?.setOnClickListener {
            try {
                findNavController().navigate(R.id.wardrobeSearchFragment)
            } catch (e: Exception) {
                Log.e("WardrobeFragment", "Navigation 실패: ${e.message}")
                showSimpleSearch()
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = WardrobeAdapter(
            itemList = emptyList<Any>(),
            onItemClick = { item: Any ->
                when (item) {
                    is WardrobeItemDto -> navigateToClothesDetail(item.id)
                    is Int -> navigateToClothesDetail(item)
                }
            }
        )
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        recyclerView.adapter = adapter
    }

    /**
     * 상위 카테고리 버튼 설정
     */
    private fun setupTopCategoryButtons(view: View) {
        val topCategories = mapOf(
            R.id.btnTopCategory1 to Pair("전체", null),
            R.id.btnTopCategory2 to Pair("상의", 1),
            R.id.btnTopCategory3 to Pair("하의", 2),
            R.id.btnTopCategory4 to Pair("아우터", 4),
            R.id.btnTopCategory5 to Pair("원피스", 3),
            R.id.btnTopCategory6 to Pair("신발", 5)
        )

        topCategories.forEach { (id, categoryData) ->
            val button = view.findViewById<Button>(id)
            val (categoryName, categoryId) = categoryData

            button?.setOnClickListener {
                // 이전 선택된 버튼 상태 해제
                selectedTopCategoryButton?.isSelected = false

                // 새로 선택된 버튼 상태 설정
                button.isSelected = true
                selectedTopCategoryButton = button

                if (categoryName == "전체") {
                    viewModel.loadAllWardrobeItems()
                } else {
                    viewModel.loadWardrobeItemsByCategory(category = categoryId)
                }
            }

            // 첫 번째 버튼(전체)을 기본 선택 상태로 설정
            if (categoryName == "전체") {
                button.isSelected = true
                selectedTopCategoryButton = button
            }
        }
    }

    /**
     * 카테고리 버튼 텍스트 업데이트
     */
    private fun updateCategoryButtonsWithCount(categories: List<CategoryDto>) {
        if (!isAdded || context == null) return

        try {
            // 전체 개수 업데이트
            val totalCount = viewModel.uiState.value.wardrobeItems.size
            view?.findViewById<Button>(R.id.btnTopCategory1)?.text = "전체 $totalCount"

            // 각 카테고리 개수 업데이트
            categories.forEach { category ->
                val buttonId = when (category.category) {
                    1 -> R.id.btnTopCategory2 // 상의
                    2 -> R.id.btnTopCategory3 // 하의
                    3 -> R.id.btnTopCategory5 // 원피스
                    4 -> R.id.btnTopCategory4 // 아우터
                    5 -> R.id.btnTopCategory6 // 신발
                    else -> null
                }

                buttonId?.let { id ->
                    val button = view?.findViewById<Button>(id)
                    button?.text = "${category.name} ${category.count}"
                }
            }
        } catch (e: Exception) {
            Log.e("WardrobeFragment", "Error updating category buttons", e)
        }
    }

    /**
     * 하위 카테고리 업데이트
     */
    private fun updateSubCategories(subcategories: List<SubcategoryDto>) {
        if (subcategories.isNotEmpty()) {
            updateSubFiltersWithApiData(subcategories)
        } else {
            // 서브카테고리가 없으면 전체만 표시
            subFilterLayout.removeAllViews()
            val allButton = createFilterButton("전체", 0, 1)
            subFilterLayout.addView(allButton)
            updateButtonSelection(0)
        }
    }

    /**
     * API 데이터로 하위 필터 업데이트
     */
    private fun updateSubFiltersWithApiData(subcategories: List<SubcategoryDto>) {
        if (!isAdded || context == null) return

        subFilterLayout.removeAllViews()
        selectedIndex = 0

        // 전체 버튼 추가
        val allButton = createFilterButton("전체", 0, subcategories.size + 1)
        allButton.setOnClickListener {
            if (isAdded && context != null) {
                updateButtonSelection(0)
                moveUnderline(0)
                val currentCategory = getCurrentSelectedCategory()
                viewModel.loadWardrobeItemsByCategory(category = currentCategory, subcategory = null)
            }
        }
        subFilterLayout.addView(allButton)

        // 서브카테고리 버튼들 추가
        subcategories.forEachIndexed { index, subcategoryDto ->
            val displayName = getSubcategoryName(subcategoryDto.subcategory)
            val button = createFilterButton(displayName, index + 1, subcategories.size + 1)

            button.setOnClickListener {
                if (isAdded && context != null) {
                    updateButtonSelection(index + 1)
                    moveUnderline(index + 1)
                    val currentCategory = getCurrentSelectedCategory()
                    viewModel.loadWardrobeItemsByCategory(
                        category = currentCategory,
                        subcategory = subcategoryDto.subcategory
                    )
                }
            }
            subFilterLayout.addView(button)
        }

        updateButtonSelection(0)
        subFilterLayout.post {
            if (isAdded && view != null) {
                moveUnderline(0)
            }
        }
    }

    /**
     * 현재 선택된 상위 카테고리 ID 가져오기
     */
    private fun getCurrentSelectedCategory(): Int? {
        selectedTopCategoryButton?.let { button ->
            return when (button.id) {
                R.id.btnTopCategory1 -> null // 전체
                R.id.btnTopCategory2 -> 1 // 상의
                R.id.btnTopCategory3 -> 2 // 하의
                R.id.btnTopCategory4 -> 4 // 아우터
                R.id.btnTopCategory5 -> 3 // 원피스
                R.id.btnTopCategory6 -> 5 // 신발
                else -> null
            }
        }
        return null
    }

    /**
     * 간단한 검색 다이얼로그
     */
    private fun showSimpleSearch() {
        val builder = android.app.AlertDialog.Builder(requireContext())
        val input = EditText(requireContext())
        input.hint = "브랜드, ID로 검색"

        builder.setTitle("아이템 검색")
            .setView(input)
            .setPositiveButton("검색") { _, _ ->
                val query = input.text.toString().trim()
                if (query.isNotEmpty()) {
                    performLocalSearch(query)
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    /**
     * 로컬 검색 수행
     */
    private fun performLocalSearch(query: String) {
        val currentItems = viewModel.uiState.value.wardrobeItems
        val filteredItems = currentItems.filter { item ->
            item.brand.contains(query, ignoreCase = true) ||
                    item.id.toString().contains(query)
        }

        if (filteredItems.isNotEmpty()) {
            adapter.updateWithApiData(filteredItems)
            Toast.makeText(context, "${filteredItems.size}개의 아이템을 찾았습니다", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "검색 결과가 없습니다", Toast.LENGTH_SHORT).show()
            adapter.updateWithApiData(currentItems)
        }
    }

    /**
     * 아이템 상세 화면으로 이동
     */
    private fun navigateToClothesDetail(itemId: Int) {
        try {
            val bundle = Bundle().apply {
                putInt("image_res_id", itemId)
            }
            findNavController().navigate(R.id.clothesDetailFragment, bundle)
        } catch (e: Exception) {
            Log.e("WardrobeFragment", "Navigation 실패: ${e.message}")
        }
    }

    /**
     * 로딩 상태 표시
     */
    private fun showLoading(isLoading: Boolean) {
        // 로딩 인디케이터 표시/숨기기
        // 필요시 ProgressBar 추가
    }

    /**
     * 빈 상태 표시
     */
    private fun showEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            // 빈 상태 UI 표시
            Toast.makeText(context, "등록된 아이템이 없습니다", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 에러 메시지 표시
     */
    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        Log.e("WardrobeFragment", "Error: $message")
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

    // 기존 UI 헬퍼 메서드들
    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private fun createFilterButton(text: String, index: Int, totalCount: Int): Button {
        val button = Button(requireContext()).apply {
            this.text = text
            setTextColor(ContextCompat.getColor(context, R.color.gray))
            background = null
            setPadding(0, 0, 0, 0)
            textSize = 14f
            isAllCaps = false
            minWidth = 0
            minHeight = 0
            setSingleLine(true)
            gravity = android.view.Gravity.CENTER
        }

        val buttonParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            dpToPx(31)
        ).apply {
            when (index) {
                0 -> leftMargin = dpToPx(0)
                totalCount - 1 -> {
                    leftMargin = dpToPx(0)
                    rightMargin = dpToPx(0)
                }
                else -> leftMargin = dpToPx(-5)
            }
        }

        button.setPadding(dpToPx(0), 0, dpToPx(0), 0)
        button.layoutParams = buttonParams
        return button
    }

    private fun updateButtonSelection(newSelectedIndex: Int) {
        if (!isAdded || context == null) return

        try {
            if (selectedIndex < subFilterLayout.childCount) {
                val previousButton = subFilterLayout.getChildAt(selectedIndex) as? Button
                previousButton?.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray))
            }
            if (newSelectedIndex < subFilterLayout.childCount) {
                val newButton = subFilterLayout.getChildAt(newSelectedIndex) as? Button
                newButton?.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
            }
            selectedIndex = newSelectedIndex
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun moveUnderline(selectedIndex: Int) {
        if (!isAdded || view == null || context == null) return

        val activeUnderline = view?.findViewById<View>(R.id.activeUnderline) ?: return
        val selectedButton = subFilterLayout.getChildAt(selectedIndex) as? Button ?: return

        selectedButton.post {
            if (!isAdded || view == null || context == null) return@post

            try {
                val paint = selectedButton.paint
                val textWidth = paint.measureText(selectedButton.text.toString())
                val underlineWidth = textWidth.toInt() + dpToPx(8)
                val buttonWidth = selectedButton.width
                val buttonLeft = selectedButton.left
                val targetLeft = buttonLeft + (buttonWidth - underlineWidth) / 2 - dpToPx(8)

                val layoutParams = activeUnderline.layoutParams as? RelativeLayout.LayoutParams
                layoutParams?.let {
                    it.width = underlineWidth
                    it.leftMargin = targetLeft
                    activeUnderline.layoutParams = it
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun getSubcategoryName(subcategoryId: Int): String {
        return when (subcategoryId) {
            // 상의 (category 1)
            1 -> "반팔티셔츠"
            2 -> "긴팔티셔츠"
            3 -> "민소매"
            4 -> "셔츠/블라우스"
            5 -> "맨투맨"
            6 -> "후드티"
            7 -> "니트/스웨터"
            8 -> "기타"
            // 하의 (category 2)
            9 -> "반바지"
            10 -> "긴바지"
            11 -> "청바지"
            12 -> "트레이닝 팬츠"
            13 -> "레깅스"
            14 -> "스커트"
            15 -> "기타"
            // 원피스 (category 3)
            16 -> "미니원피스"
            17 -> "롱 원피스"
            18 -> "끈 원피스"
            19 -> "니트 원피스"
            20 -> "기타"
            // 아우터 (category 4)
            21 -> "바람막이"
            22 -> "가디건"
            23 -> "자켓"
            24 -> "코트"
            25 -> "패딩"
            26 -> "후드집업"
            27 -> "무스탕/퍼"
            28 -> "기타"
            // 신발 (category 5)
            29 -> "운동화"
            30 -> "부츠"
            31 -> "샌들"
            32 -> "슬리퍼"
            33 -> "구두"
            34 -> "로퍼"
            35 -> "기타"
            // 액세서리 (category 6)
            36 -> "모자"
            37 -> "머플러"
            38 -> "장갑"
            39 -> "양말"
            40 -> "안경/선글라스"
            41 -> "가방"
            42 -> "시계/팔찌/목걸이"
            43 -> "기타"
            else -> "기타"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        try {
            if (::recyclerView.isInitialized) {
                recyclerView.adapter = null
            }
            if (::subFilterLayout.isInitialized) {
                subFilterLayout.removeAllViews()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}