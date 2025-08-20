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
import androidx.core.widget.ImageViewCompat
import com.example.onfit.Wardrobe.Network.WardrobeItemDto
import com.example.onfit.Wardrobe.Network.CategoryDto
import com.example.onfit.Wardrobe.Network.SubcategoryDto
import kotlinx.coroutines.launch
import java.util.*
import android.view.Gravity

open class WardrobeFragment : Fragment() {

    private lateinit var repository: WardrobeRepository
    private lateinit var viewModel: WardrobeViewModel

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: WardrobeAdapter
    private lateinit var subFilterLayout: LinearLayout
    private lateinit var subFilterScrollView: HorizontalScrollView
    private lateinit var searchButton: ImageButton

    private var selectedIndex = 0
    private var selectedTopCategoryButton: Button? = null

    private var isFilterApplied = false

    // 🔥 NEW: 현재 선택된 카테고리 상태 저장
    private var currentSelectedCategory: Int? = null
    private var currentSelectedSubcategory: Int? = null

    // 🔥 NEW: 중복 호출 방지용
    private var lastSubcategoriesSize: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = WardrobeRepository(requireContext())
        viewModel = WardrobeViewModel(requireContext())
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

        viewModel.loadAllWardrobeItems()
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                handleUiState(state)
            }
        }
    }

    private fun handleUiState(state: WardrobeUiState) {
        Log.d("WardrobeFragment", "🔄 handleUiState 호출됨")

        if (state.isLoading) {
            Log.d("WardrobeFragment", "⏳ 로딩 상태")
            showLoading(true)
        } else {
            showLoading(false)
        }

        if (state.hasData) {
            Log.d("WardrobeFragment", "📊 데이터 있음 - 아이템 개수: ${state.wardrobeItems.size}")
            adapter.updateWithApiData(state.wardrobeItems)
            updateCategoryButtonsWithCount(state.categories)

            // 🔥 중복 호출 방지 - 크기로 비교
            if (state.subcategories.size != lastSubcategoriesSize) {
                Log.d("WardrobeFragment", "🔄 세부카테고리 업데이트 필요: ${state.subcategories.size} != $lastSubcategoriesSize")

                state.subcategories.forEach { subcategory ->
                    Log.d("WardrobeFragment", "📋 세부카테고리: ${subcategory.name} (ID: ${subcategory.subcategory})")
                }

                updateSubCategories(state.subcategories)
                lastSubcategoriesSize = state.subcategories.size
            } else {
                Log.d("WardrobeFragment", "⏭️ 세부카테고리 업데이트 스킵 (동일한 크기)")
            }
        } else {
            Log.d("WardrobeFragment", "❌ 데이터 없음")
        }

        if (state.hasError) {
            Log.e("WardrobeFragment", "💥 에러 발생: ${state.errorMessage}")
            showError(state.errorMessage ?: "알 수 없는 오류가 발생했습니다")
            viewModel.clearErrorMessage()
        }

        if (state.registrationSuccess) {
            Toast.makeText(context, "아이템이 등록되었습니다", Toast.LENGTH_SHORT).show()
            viewModel.clearRegistrationSuccess()
            notifyCalendarFragmentOfNewItem(getCurrentDate())
        }

        if (state.showEmptyState) showEmptyState(true) else showEmptyState(false)
    }

    private fun setupFragmentResultListeners() {
        // 🔥 MODIFIED: 아이템 등록 후 현재 선택된 카테고리 유지
        parentFragmentManager.setFragmentResultListener("item_registered", this) { _, bundle ->
            val isSuccess = bundle.getBoolean("success", false)
            val registeredDate = bundle.getString("registered_date")
            if (isSuccess) {
                // 🔥 현재 선택된 카테고리 유지하면서 새로고침
                refreshCurrentCategory()
                notifyCalendarFragmentOfNewItem(registeredDate)
                Toast.makeText(context, "아이템이 등록되었습니다", Toast.LENGTH_SHORT).show()
            }
        }

        // 🔥 NEW: 아이템 수정 결과 처리 - 강제 새로고침 추가
        parentFragmentManager.setFragmentResultListener("wardrobe_item_updated", this) { _, bundle ->
            val isSuccess = bundle.getBoolean("success", false)
            val forceRefresh = bundle.getBoolean("force_refresh", false)
            if (isSuccess) {
                if (forceRefresh) {
                    // 🔥 카테고리 정보도 다시 로드 (상위 카테고리 변경 반영)
                    viewModel.loadAllWardrobeItems()
                    // 전체 카테고리로 리셋
                    resetToAllCategory()
                } else {
                    refreshCurrentCategory()
                }
                Toast.makeText(context, "아이템이 수정되었습니다", Toast.LENGTH_SHORT).show()
            }
        }

        parentFragmentManager.setFragmentResultListener("search_results", this) { _, bundle ->
            val filteredIds = bundle.getIntArray("filtered_item_ids")
            val filterApplied = bundle.getBoolean("filter_applied", false)
            setSearchIconColor(filterApplied)

            if (filteredIds != null) {
                val currentItems = viewModel.uiState.value.wardrobeItems
                val filteredItems = currentItems.filter { it.id in filteredIds }
                val finalItems = applyLocalFiltering(bundle, filteredItems)
                adapter.updateWithApiData(finalItems)
            }
        }
    }

    // 🔥 NEW: 현재 카테고리 새로고침 함수
    private fun refreshCurrentCategory() {
        Log.d("WardrobeFragment", "현재 카테고리 새로고침: category=$currentSelectedCategory, subcategory=$currentSelectedSubcategory")

        when {
            currentSelectedCategory == null -> {
                // 전체 카테고리인 경우
                Log.d("WardrobeFragment", "전체 카테고리로 새로고침")
                viewModel.loadAllWardrobeItems()
            }
            currentSelectedSubcategory != null -> {
                // 세부 카테고리가 선택된 경우
                Log.d("WardrobeFragment", "세부 카테고리로 새로고침: ${currentSelectedSubcategory}")
                viewModel.loadWardrobeItemsByCategory(
                    category = currentSelectedCategory,
                    subcategory = currentSelectedSubcategory
                )
            }
            else -> {
                // 메인 카테고리만 선택된 경우
                Log.d("WardrobeFragment", "메인 카테고리로 새로고침: ${currentSelectedCategory}")
                viewModel.loadWardrobeItemsByCategory(category = currentSelectedCategory)
            }
        }
    }

    // 🔥 NEW: 전체 카테고리로 리셋하는 함수
    private fun resetToAllCategory() {
        // 전체 버튼 선택 상태로 변경
        selectedTopCategoryButton?.isSelected = false
        val allButton = view?.findViewById<Button>(R.id.btnTopCategory1)
        allButton?.isSelected = true
        selectedTopCategoryButton = allButton

        // 상태 초기화
        currentSelectedCategory = null
        currentSelectedSubcategory = null

        Log.d("WardrobeFragment", "전체 카테고리로 리셋됨")
    }

    private fun setSearchIconColor(applied: Boolean) {
        if (!::searchButton.isInitialized) return
        val colorRes = if (applied) R.color.search_icon_active else R.color.search_icon_default
        val color = ContextCompat.getColor(requireContext(), colorRes)
        ImageViewCompat.setImageTintList(searchButton, android.content.res.ColorStateList.valueOf(color))
        isFilterApplied = applied
    }

    private fun notifyCalendarFragmentOfNewItem(registeredDate: String?) {
        val bundle = Bundle().apply {
            putString("registered_date", registeredDate ?: getCurrentDate())
            putBoolean("wardrobe_updated", true)
            putLong("timestamp", System.currentTimeMillis())
        }
        parentFragmentManager.setFragmentResult("outfit_registered", bundle)
    }

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
                    it.brand?.contains(brand, ignoreCase = true) == true
                }
            }
        }

        return filteredItems
    }

    private fun initializeViews(view: View) {
        recyclerView = view.findViewById(R.id.wardrobeRecyclerView)
        subFilterLayout = view.findViewById(R.id.subFilterLayout)
        subFilterScrollView = view.findViewById(R.id.subFilterScrollView)
        searchButton = view.findViewById(R.id.ic_search)
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
        searchButton.setOnClickListener {
            try {
                findNavController().navigate(R.id.wardrobeSearchFragment)
            } catch (e: Exception) {
                showSimpleSearch()
            }
        }
        setSearchIconColor(isFilterApplied)
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
                selectedTopCategoryButton?.isSelected = false
                button.isSelected = true
                selectedTopCategoryButton = button
                setSearchIconColor(false)

                // 🔥 NEW: 현재 선택 상태 저장
                currentSelectedCategory = categoryId
                currentSelectedSubcategory = null // 세부 카테고리 초기화

                if (categoryName == "전체") {
                    Log.d("WardrobeFragment", "전체 카테고리 선택")
                    viewModel.loadAllWardrobeItems()
                } else {
                    Log.d("WardrobeFragment", "카테고리 선택: $categoryName (ID: $categoryId)")
                    viewModel.loadWardrobeItemsByCategory(category = categoryId)
                }
            }
            if (categoryName == "전체") {
                button.isSelected = true
                selectedTopCategoryButton = button
                // 🔥 초기 상태 설정
                currentSelectedCategory = null
                currentSelectedSubcategory = null
            }
        }
    }

    private fun updateCategoryButtonsWithCount(categories: List<CategoryDto>) {
        if (!isAdded || context == null) return
        try {
            val totalCount = viewModel.uiState.value.wardrobeItems.size
            view?.findViewById<Button>(R.id.btnTopCategory1)?.text = "전체 $totalCount"
            categories.forEach { category ->
                val buttonId = when (category.category) {
                    1 -> R.id.btnTopCategory2
                    2 -> R.id.btnTopCategory3
                    3 -> R.id.btnTopCategory5
                    4 -> R.id.btnTopCategory4
                    5 -> R.id.btnTopCategory6
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

    // 🔥 FIXED: 세부 카테고리 업데이트 로직 수정 - 디버깅 로그 강화
    private fun updateSubCategories(subcategories: List<SubcategoryDto>) {
        Log.d("WardrobeFragment", "=== updateSubCategories 시작 ===")
        Log.d("WardrobeFragment", "받은 세부카테고리 개수: ${subcategories.size}")
        Log.d("WardrobeFragment", "현재 선택 상태 - category: $currentSelectedCategory, subcategory: $currentSelectedSubcategory")
        Log.d("WardrobeFragment", "받은 세부카테고리 목록: ${subcategories.map { "${it.name}(${it.subcategory})" }}")

        when {
            // 1. 서버에서 세부 카테고리 목록을 보내준 경우
            subcategories.isNotEmpty() -> {
                Log.d("WardrobeFragment", "✅ 서버에서 세부카테고리 제공 - 목록 표시")
                updateSubFiltersWithApiData(subcategories)
            }

            // 2. 상위 카테고리가 선택되었지만 서버에서 세부카테고리를 안 보내준 경우
            currentSelectedCategory != null && currentSelectedSubcategory == null -> {
                Log.d("WardrobeFragment", "🔥 상위 카테고리 선택됨 - 클라이언트에서 세부카테고리 생성")
                createSubcategoriesFromClient(currentSelectedCategory!!)
            }

            // 3. 하위 카테고리까지 선택된 경우 또는 기타
            else -> {
                Log.d("WardrobeFragment", "⭐ 기본 전체 버튼 표시")
                createDefaultAllButton()
            }
        }
        Log.d("WardrobeFragment", "=== updateSubCategories 끝 ===")
    }

    // 🔥 FIXED: 디버깅 로그 추가
    private fun createSubcategoriesFromClient(categoryId: Int) {
        Log.d("WardrobeFragment", "🏭 createSubcategoriesFromClient 시작: categoryId=$categoryId")

        if (!isAdded || context == null) {
            Log.w("WardrobeFragment", "❌ Fragment가 attach되지 않았거나 context가 null")
            return
        }

        // 카테고리별 세부카테고리 정의 - 기존 SubcategoryDto 사용
        val subcategoryMap = mapOf(
            1 to listOf(
                SubcategoryDto(1, "반팔티셔츠"),
                SubcategoryDto(2, "긴팔티셔츠"),
                SubcategoryDto(3, "민소매"),
                SubcategoryDto(4, "셔츠/블라우스"),
                SubcategoryDto(5, "맨투맨"),
                SubcategoryDto(6, "후드티"),
                SubcategoryDto(7, "니트/스웨터"),
                SubcategoryDto(8, "기타")
            ),
            2 to listOf(
                SubcategoryDto(9, "반바지"),
                SubcategoryDto(10, "긴바지"),
                SubcategoryDto(11, "청바지"),
                SubcategoryDto(12, "트레이닝 팬츠"),
                SubcategoryDto(13, "레깅스"),
                SubcategoryDto(14, "스커트"),
                SubcategoryDto(15, "기타")
            ),
            3 to listOf(
                SubcategoryDto(16, "미니원피스"),
                SubcategoryDto(17, "롱 원피스"),
                SubcategoryDto(18, "끈 원피스"),
                SubcategoryDto(19, "니트 원피스"),
                SubcategoryDto(20, "기타")
            ),
            4 to listOf(
                SubcategoryDto(21, "바람막이"),
                SubcategoryDto(22, "가디건"),
                SubcategoryDto(23, "자켓"),
                SubcategoryDto(24, "코트"),
                SubcategoryDto(25, "패딩"),
                SubcategoryDto(26, "후드집업"),
                SubcategoryDto(27, "무스탕/퍼"),
                SubcategoryDto(28, "기타")
            ),
            5 to listOf(
                SubcategoryDto(29, "운동화"),
                SubcategoryDto(30, "부츠"),
                SubcategoryDto(31, "샌들"),
                SubcategoryDto(32, "슬리퍼"),
                SubcategoryDto(33, "구두"),
                SubcategoryDto(34, "로퍼"),
                SubcategoryDto(35, "기타")
            ),
            6 to listOf(
                SubcategoryDto(36, "모자"),
                SubcategoryDto(37, "머플러"),
                SubcategoryDto(38, "장갑"),
                SubcategoryDto(39, "양말"),
                SubcategoryDto(40, "안경/선글라스"),
                SubcategoryDto(41, "가방"),
                SubcategoryDto(42, "시계/팔찌/목걸이"),
                SubcategoryDto(43, "기타")
            )
        )

        val clientSubcategories = subcategoryMap[categoryId] ?: emptyList()
        Log.d("WardrobeFragment", "🎯 클라이언트 생성 세부카테고리: ${clientSubcategories.map { it.name }}")

        if (clientSubcategories.isNotEmpty()) {
            Log.d("WardrobeFragment", "✅ updateSubFiltersWithApiData 호출")
            updateSubFiltersWithApiData(clientSubcategories)
        } else {
            Log.d("WardrobeFragment", "❌ 세부카테고리 없음 - 기본 버튼 생성")
            createDefaultAllButton()
        }
    }

    // 🔥 NEW: 기본 '전체' 버튼만 생성하는 함수
    private fun createDefaultAllButton() {
        if (!isAdded || context == null) return

        subFilterLayout.removeAllViews()
        selectedIndex = 0

        val allButton = createFilterButton("전체", 0, 1)
        allButton.setOnClickListener {
            if (isAdded && context != null) {
                updateButtonSelection(0)
                moveUnderline(0)
                // 현재 선택된 메인 카테고리로 로드 (세부카테고리 null)
                currentSelectedSubcategory = null
                val currentCategory = getCurrentSelectedCategory()
                if (currentCategory == null) {
                    viewModel.loadAllWardrobeItems()
                } else {
                    viewModel.loadWardrobeItemsByCategory(category = currentCategory, subcategory = null)
                }
            }
        }
        subFilterLayout.addView(allButton)
        updateButtonSelection(0)

        // 🔥 더미 버전의 밑줄 위치 계산 방식 적용
        subFilterLayout.post {
            if (isAdded && view != null) {
                moveUnderline(0)
            }
        }
    }

    // 🔥 FIXED: API 데이터로 세부 필터 업데이트
    private fun updateSubFiltersWithApiData(subcategories: List<SubcategoryDto>) {
        if (!isAdded || context == null) return

        Log.d("WardrobeFragment", "updateSubFiltersWithApiData 시작, 세부카테고리: ${subcategories.map { it.name }}")

        subFilterLayout.removeAllViews()
        selectedIndex = 0

        // '전체' 버튼 추가
        val allButton = createFilterButton("전체", 0, subcategories.size + 1)
        allButton.setOnClickListener {
            if (isAdded && context != null) {
                updateButtonSelection(0)
                moveUnderline(0)
                val currentCategory = getCurrentSelectedCategory()

                // 🔥 현재 선택 상태 저장
                currentSelectedSubcategory = null

                if (currentCategory == null) {
                    viewModel.loadAllWardrobeItems()
                } else {
                    viewModel.loadWardrobeItemsByCategory(category = currentCategory, subcategory = null)
                }
                Log.d("WardrobeFragment", "전체 세부카테고리 선택: category=$currentCategory")
            }
        }
        subFilterLayout.addView(allButton)

        // 세부 카테고리 버튼들 추가
        subcategories.forEachIndexed { index, subcategoryDto ->
            val displayName = subcategoryDto.name
            val button = createFilterButton(displayName, index + 1, subcategories.size + 1)
            button.setOnClickListener {
                if (isAdded && context != null) {
                    updateButtonSelection(index + 1)
                    moveUnderline(index + 1)
                    val currentCategory = getCurrentSelectedCategory()

                    // 🔥 현재 선택 상태 저장
                    currentSelectedSubcategory = subcategoryDto.subcategory

                    viewModel.loadWardrobeItemsByCategory(
                        category = currentCategory,
                        subcategory = subcategoryDto.subcategory
                    )
                    Log.d("WardrobeFragment", "세부카테고리 선택: $displayName (ID: ${subcategoryDto.subcategory})")
                }
            }
            subFilterLayout.addView(button)
        }

        updateButtonSelection(0)

        // 🔥 더미 버전의 밑줄 위치 계산 방식 적용
        subFilterLayout.post {
            if (isAdded && view != null) {
                moveUnderline(0)
            }
        }
    }

    private fun getCurrentSelectedCategory(): Int? {
        selectedTopCategoryButton?.let { button ->
            return when (button.id) {
                R.id.btnTopCategory1 -> null
                R.id.btnTopCategory2 -> 1
                R.id.btnTopCategory3 -> 2
                R.id.btnTopCategory4 -> 4
                R.id.btnTopCategory5 -> 3
                R.id.btnTopCategory6 -> 5
                else -> null
            }
        }
        return null
    }

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

    private fun performLocalSearch(query: String) {
        val currentItems = viewModel.uiState.value.wardrobeItems
        val filteredItems = currentItems.filter { item ->
            item.brand?.contains(query, ignoreCase = true) == true ||
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

    private fun showLoading(isLoading: Boolean) {
        // 로딩 인디케이터 표시/숨기기
    }

    private fun showEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            Toast.makeText(context, "등록된 아이템이 없습니다", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        Log.e("WardrobeFragment", "Error: $message")
    }

    private fun getCurrentDate(): String {
        val calendar = Calendar.getInstance()
        return String.format(
            "%04d-%02d-%02d",
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    // 🔥 더미 버전에서 가져온 정확한 dp->px 변환
    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    // 🔥 더미 버전에서 가져온 정확한 버튼 생성 로직
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
            gravity = Gravity.CENTER // 🔥 더미 버전에서 가져온 Gravity 설정
        }

        // 🔥 더미 버전에서 가져온 정확한 레이아웃 파라미터 설정
        val buttonParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            dpToPx(31)
        ).apply {
            when (index) {
                0 -> leftMargin = dpToPx(0) // 첫 번째 버튼을 왼쪽으로 더 붙임
                totalCount - 1 -> {
                    leftMargin = dpToPx(0) // 마지막 버튼 간격
                    rightMargin = dpToPx(0) // 마지막 버튼
                }
                else -> leftMargin = dpToPx(-5) // 중간 버튼들 간격
            }
        }

        button.setPadding(dpToPx(0), 0, dpToPx(0), 0)
        button.layoutParams = buttonParams

        return button
    }

    // 🔥 더미 버전에서 가져온 정확한 버튼 선택 업데이트 로직
    private fun updateButtonSelection(newSelectedIndex: Int) {
        if (!isAdded || context == null) return

        try {
            // 이전 선택된 버튼을 회색으로 변경
            if (selectedIndex < subFilterLayout.childCount) {
                val previousButton = subFilterLayout.getChildAt(selectedIndex) as? Button
                previousButton?.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray))
            }

            // 새로 선택된 버튼을 검정색으로 변경
            if (newSelectedIndex < subFilterLayout.childCount) {
                val newButton = subFilterLayout.getChildAt(newSelectedIndex) as? Button
                newButton?.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
            }

            selectedIndex = newSelectedIndex
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // 🔥 더미 버전에서 가져온 정확한 밑줄 위치 계산 로직
    private fun moveUnderline(selectedIndex: Int) {
        if (!isAdded || view == null || context == null) return

        val activeUnderline = view?.findViewById<View>(R.id.activeUnderline) ?: return
        val selectedButton = subFilterLayout.getChildAt(selectedIndex) as? Button ?: return

        // 뷰가 완전히 렌더링된 후에 실행
        selectedButton.post {
            if (!isAdded || view == null || context == null) return@post

            try {
                // 🔥 더미 버전의 정확한 밑줄 위치 계산 로직
                val paint = selectedButton.paint
                val textWidth = paint.measureText(selectedButton.text.toString())
                val underlineWidth = textWidth.toInt() + dpToPx(8)

                // 선택된 버튼의 중앙 위치 계산
                val buttonWidth = selectedButton.width
                val buttonLeft = selectedButton.left
                val targetLeft = buttonLeft + (buttonWidth - underlineWidth) / 2 - dpToPx(8)

                // 밑줄 크기와 위치 조정
                val layoutParams = activeUnderline.layoutParams as? RelativeLayout.LayoutParams
                layoutParams?.let {
                    it.width = underlineWidth
                    it.leftMargin = targetLeft
                    activeUnderline.layoutParams = it
                }

                Log.d("WardrobeFragment", "밑줄 위치 조정: text='${selectedButton.text}', buttonLeft=$buttonLeft, textWidth=$textWidth, targetLeft=$targetLeft")
            } catch (e: Exception) {
                Log.e("WardrobeFragment", "밑줄 위치 조정 실패", e)
                e.printStackTrace()
            }
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