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


// 더미 데이터 사용 여부 플래그
private const val USE_WARDROBE_DUMMY = true

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

    // 🔥 WardrobeFragment의 onViewCreated 함수를 이렇게 수정

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Log.d("WardrobeFragment", "🎯 onViewCreated 시작")

        initializeViews(view)
        setupRecyclerView()
        setupTopCategoryButtons(view)

        // 🔥 IMPORTANT: FragmentResultListener를 먼저 설정
        setupFragmentResultListeners()
        Log.d("WardrobeFragment", "✅ FragmentResultListeners 설정 완료")

        // 🔥 FIXED: observeViewModel() 대신 기존 코드 사용
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                handleUiState(state)
            }
        }

        // 🔥 NEW: 초기 로드 시 기본 전체 버튼 표시
        createDefaultAllButton()

        viewModel.loadAllWardrobeItems()

        Log.d("WardrobeFragment", "🎯 onViewCreated 완료")
    }

    private fun handleUiState(state: WardrobeUiState) {
        Log.d("WardrobeFragment", "🔄 handleUiState 호출됨")
        Log.d("WardrobeFragment", "  - isFilterApplied: $isFilterApplied")
        Log.d("WardrobeFragment", "  - 아이템 수: ${if (state.hasData) state.wardrobeItems.size else "no data"}")

        if (state.isLoading) {
            Log.d("WardrobeFragment", "⏳ 로딩 상태")
            showLoading(true)
        } else {
            showLoading(false)
        }

        if (state.hasData) {
            Log.d("WardrobeFragment", "📊 데이터 있음 - 아이템 개수: ${state.wardrobeItems.size}")

            // 🔥 서버 데이터와 더미 데이터 결합
            val allItems = if (USE_WARDROBE_DUMMY) {
                combineServerAndDummyData(state.wardrobeItems)
            } else {
                state.wardrobeItems
            }

            val hasValidCategories = allItems.any { it.category != null && it.category != 0 }
            Log.d("WardrobeFragment", "유효한 카테고리 정보 존재: $hasValidCategories")

            allItems.forEach { item ->
                Log.d("WardrobeFragment", "🔍 아이템: ID=${item.id}, category=${item.category}, subcategory=${item.subcategory}")
            }

            // 🔥 검색 상태가 아닐 때만 어댑터 업데이트
            if (!isFilterApplied) {
                Log.d("WardrobeFragment", "🟢 검색 상태 아님 - 어댑터 업데이트 허용")

                val isInitialLoad = currentSelectedCategory == null
                val isServerResponse = allItems.isNotEmpty()

                if (isInitialLoad && isServerResponse) {
                    Log.d("WardrobeFragment", "초기 로드 또는 전체 카테고리 - 모든 아이템 표시")
                    adapter.updateWithApiData(allItems) // 🔥 결합된 데이터 사용
                }
            } else {
                Log.d("WardrobeFragment", "🔴 검색 상태임 - 어댑터 업데이트 건너뜀")
                Log.d("WardrobeFragment", "  현재 어댑터 아이템 수: ${if (::adapter.isInitialized) adapter.itemCount else "not initialized"}")

                // 🔥 검색 상태일 때 아이콘 색상 강제 유지
                Log.d("WardrobeFragment", "🔍 검색 상태 - 아이콘 색상 강제 유지")
                if (::searchButton.isInitialized) {
                    view?.post {
                        setSearchIconColor(true)
                        Log.d("WardrobeFragment", "🔍 검색 아이콘 색상 강제 설정 완료")
                    }
                }
            }

            // 카테고리 개수 업데이트 (결합된 데이터 사용)
            updateCategoryButtonsWithCount(state.categories, allItems)

            if (!hasValidCategories && allItems.isNotEmpty()) {
                Log.w("WardrobeFragment", "⚠️ 서버에서 카테고리 정보 누락 - 상세 정보 로드 시작")
                // 검색 상태가 아닐 때만 상세 정보 로드
                if (!isFilterApplied) {
                    Log.d("WardrobeFragment", "🟢 검색 상태 아님 - 상세 정보 로드 진행")
                    loadItemDetails(allItems)
                } else {
                    Log.d("WardrobeFragment", "🔴 검색 상태임 - 상세 정보 로드 건너뜀")
                }
            } else {
                // 서브카테고리 업데이트 로직
                val shouldUpdateSubcategories = when {
                    state.subcategories.isNotEmpty() -> {
                        Log.d("WardrobeFragment", "✅ 서버 세부카테고리 있음 - 업데이트 필요")
                        true
                    }
                    currentSelectedCategory != null -> {
                        Log.d("WardrobeFragment", "✅ 상위 카테고리 선택됨 - 강제 업데이트")
                        true
                    }
                    currentSelectedCategory == null && allItems.isNotEmpty() && !isFilterApplied -> {
                        Log.d("WardrobeFragment", "✅ 전체 상태 + 아이템 있음 + 검색 아님 - 업데이트")
                        true
                    }
                    subFilterLayout.childCount == 0 && !isFilterApplied -> {
                        Log.d("WardrobeFragment", "✅ 세부카테고리 없음 + 검색 아님 - 업데이트 필요")
                        true
                    }
                    else -> {
                        Log.d("WardrobeFragment", "❌ 세부카테고리 업데이트 불필요")
                        false
                    }
                }

                if (shouldUpdateSubcategories) {
                    Log.d("WardrobeFragment", "🚀 세부카테고리 업데이트 시작")
                    updateSubCategories(state.subcategories)
                }
            }
        } else {
            Log.d("WardrobeFragment", "❌ 데이터 없음")

            // 🔥 서버 데이터가 없어도 더미 데이터 표시
            if (USE_WARDROBE_DUMMY) {
                val dummyItems = loadDummyWardrobeFromAssets()
                if (dummyItems.isNotEmpty()) {
                    Log.d("WardrobeFragment", "🎭 서버 데이터 없음 - 더미 데이터만 표시: ${dummyItems.size}개")
                    adapter.updateWithApiData(dummyItems)
                    updateCategoryButtonsWithCount(emptyList(), dummyItems)

                    // 기본 서브카테고리 생성
                    createDefaultAllButton()
                    return
                }
            }
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

    private fun loadItemDetails(items: List<WardrobeItemDto>) {
        Log.d("WardrobeFragment", "🔧 loadItemDetails 시작 - ${items.size}개 아이템")

        // 🔥 CRITICAL: 검색 상태 체크
        if (isFilterApplied) {
            Log.w("WardrobeFragment", "🚫 검색 상태이므로 loadItemDetails 중단")
            return
        }

        lifecycleScope.launch {
            val detailedItems = mutableListOf<WardrobeItemDto>()

            items.forEach { item ->
                // 각 아이템 처리 전에도 검색 상태 체크
                if (isFilterApplied) {
                    Log.w("WardrobeFragment", "🚫 검색 상태 감지 - loadItemDetails 중단")
                    return@launch
                }

                try {
                    // 개별 아이템 상세 정보 가져오기 (API 호출)
                    val detailResult = repository.getWardrobeItemDetail(item.id)
                    if (detailResult.isSuccess) {
                        val detail = detailResult.getOrNull()
                        if (detail != null) {
                            // WardrobeItemDetail을 WardrobeItemDto로 변환
                            val detailedItem = WardrobeItemDto(
                                id = detail.id,
                                image = detail.image ?: "",
                                brand = detail.brand ?: "",
                                season = detail.season,
                                color = detail.color,
                                category = detail.category,
                                subcategory = detail.subcategory
                            )
                            detailedItems.add(detailedItem)
                            Log.d("WardrobeFragment", "✅ 아이템 ${item.id} 상세정보: category=${detail.category}, subcategory=${detail.subcategory}")
                        } else {
                            detailedItems.add(item) // 실패시 원본 사용
                        }
                    } else {
                        Log.e("WardrobeFragment", "❌ 아이템 ${item.id} 상세정보 가져오기 실패: ${detailResult.exceptionOrNull()?.message}")
                        detailedItems.add(item) // 실패시 원본 사용
                    }
                } catch (e: Exception) {
                    Log.e("WardrobeFragment", "❌ 아이템 ${item.id} 상세정보 가져오기 실패", e)
                    detailedItems.add(item) // 실패시 원본 사용
                }
            }

            // 최종 UI 업데이트 전에도 검색 상태 체크
            if (isFilterApplied) {
                Log.w("WardrobeFragment", "🚫 최종 UI 업데이트 전 검색 상태 감지 - 업데이트 건너뜀")
                return@launch
            }

            // 검색 상태가 아닐 때만 어댑터 업데이트
            adapter.updateWithApiData(detailedItems)

            // 🔥 FIXED: 에러 나는 코드 완전 제거 - 필요하면 나중에 추가
            Log.d("WardrobeFragment", "✅ loadItemDetails 완료 - ${detailedItems.size}개 아이템 업데이트")
        }
    }

    // 🔥 NEW: 상세 데이터로 서브 필터 업데이트
    private fun updateSubFiltersWithDetailedData(subcategories: List<SubcategoryDto>, categoryItems: List<WardrobeItemDto>) {
        if (!isAdded || context == null) return

        Log.d("WardrobeFragment", "updateSubFiltersWithDetailedData 시작")

        subFilterLayout.removeAllViews()
        selectedIndex = 0

        // '전체' 버튼 추가
        val allButton = createFilterButton("전체 ${categoryItems.size}", 0, subcategories.size + 1)
        allButton.setOnClickListener {
            if (isAdded && context != null) {
                updateButtonSelection(0)
                moveUnderline(0)
                currentSelectedSubcategory = null

                // 🔥 FIXED: 즉시 현재 카테고리의 모든 아이템 표시
                adapter.updateWithApiData(categoryItems)
                Log.d("WardrobeFragment", "전체 세부카테고리 선택: ${categoryItems.size}개 아이템 표시")
            }
        }
        subFilterLayout.addView(allButton)

        // 서브카테고리 버튼들 추가
        subcategories.forEachIndexed { index, subcategoryDto ->
            val subcategoryItemCount = categoryItems.count { it.subcategory == subcategoryDto.subcategory }
            val displayName = "${subcategoryDto.name} $subcategoryItemCount"

            val button = createFilterButton(displayName, index + 1, subcategories.size + 1)
            button.setOnClickListener {
                if (isAdded && context != null) {
                    updateButtonSelection(index + 1)
                    moveUnderline(index + 1)
                    currentSelectedSubcategory = subcategoryDto.subcategory

                    // 🔥 FIXED: 즉시 해당 서브카테고리 아이템만 필터링해서 표시
                    val filteredItems = categoryItems.filter { it.subcategory == subcategoryDto.subcategory }
                    adapter.updateWithApiData(filteredItems)

                    Log.d("WardrobeFragment", "서브카테고리 ${subcategoryDto.name} 선택: ${filteredItems.size}개 아이템 표시")
                }
            }
            subFilterLayout.addView(button)
        }

        // 🔥 FIXED: 초기 선택 시 바로 전체 아이템 표시
        updateButtonSelection(0)
        adapter.updateWithApiData(categoryItems)
        Log.d("WardrobeFragment", "초기 로드: 전체 ${categoryItems.size}개 아이템 표시")

        subFilterLayout.post {
            if (isAdded && view != null) {
                moveUnderline(0)
            }
        }
    }

    // 🔥 WardrobeFragment의 setupFragmentResultListeners() 함수를 완전히 교체하세요

    private fun setupFragmentResultListeners() {
        Log.d("WardrobeFragment", "🎯 setupFragmentResultListeners 시작")

        // 아이템 등록 리스너 (기존 그대로)
        parentFragmentManager.setFragmentResultListener("item_registered", this) { _, bundle ->
            val isSuccess = bundle.getBoolean("success", false)
            val registeredDate = bundle.getString("registered_date")
            if (isSuccess) {
                refreshCurrentCategory()
                notifyCalendarFragmentOfNewItem(registeredDate)
                Toast.makeText(context, "아이템이 등록되었습니다", Toast.LENGTH_SHORT).show()
            }
        }

        // 아이템 수정 리스너 (기존 그대로)
        parentFragmentManager.setFragmentResultListener("wardrobe_item_updated", this) { _, bundle ->
            val isSuccess = bundle.getBoolean("success", false)
            val forceRefresh = bundle.getBoolean("force_refresh", false)
            if (isSuccess) {
                if (forceRefresh) {
                    viewModel.loadAllWardrobeItems()
                    resetToAllCategory()
                } else {
                    refreshCurrentCategory()
                }
                Toast.makeText(context, "아이템이 수정되었습니다", Toast.LENGTH_SHORT).show()
            }
        }

        // 🔥 검색 결과 리스너 - 간단하고 안전한 버전
        parentFragmentManager.setFragmentResultListener("search_results", this) { _, bundle ->
            Log.d("WardrobeFragment", "🎯 검색 결과 받음")

            try {
                val filteredIds = bundle.getIntArray("filtered_item_ids")
                val filterApplied = bundle.getBoolean("filter_applied", false)

                Log.d("WardrobeFragment", "필터 적용: $filterApplied, ID 개수: ${filteredIds?.size}")

                if (filterApplied && filteredIds != null && filteredIds.isNotEmpty()) {
                    // 현재 아이템들 가져오기
                    val allItems = viewModel.uiState.value.wardrobeItems
                    Log.d("WardrobeFragment", "전체 아이템: ${allItems.size}개")

                    // ID로 필터링
                    val filteredItems = allItems.filter { it.id in filteredIds }
                    Log.d("WardrobeFragment", "필터링된 아이템: ${filteredItems.size}개")

                    // 어댑터 업데이트 - 가장 기본적인 방법
                    if (::adapter.isInitialized) {
                        adapter.updateWithApiData(filteredItems)

                        // 강제 새로고침
                        if (::recyclerView.isInitialized) {
                            recyclerView.post {
                                adapter.notifyDataSetChanged()
                                Log.d("WardrobeFragment", "어댑터 새로고침 완료")
                            }
                        }
                    }

                    // 검색 상태 설정
                    isFilterApplied = true
                    setSearchIconColor(true)

                    // 결과 메시지
                    Toast.makeText(context, "${filteredItems.size}개 아이템 검색됨", Toast.LENGTH_SHORT).show()

                } else {
                    // 필터 해제
                    restoreOriginalItems()
                    isFilterApplied = false
                    setSearchIconColor(false)
                }

            } catch (e: Exception) {
                Log.e("WardrobeFragment", "검색 결과 처리 실패", e)
                Toast.makeText(context, "검색 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        Log.d("WardrobeFragment", "✅ setupFragmentResultListeners 완료")
    }

    private fun restoreOriginalItems() {
        Log.d("WardrobeFragment", "🔄 원래 아이템 상태 복원")

        when {
            currentSelectedCategory == null -> {
                // 전체 카테고리인 경우
                val allItems = viewModel.uiState.value.wardrobeItems
                adapter.updateWithApiData(allItems)
                Log.d("WardrobeFragment", "전체 아이템 복원: ${allItems.size}개")
            }

            currentSelectedSubcategory != null -> {
                // 세부 카테고리가 선택된 경우
                val allItems = viewModel.uiState.value.wardrobeItems
                val filteredItems = allItems.filter {
                    it.category == currentSelectedCategory && it.subcategory == currentSelectedSubcategory
                }
                adapter.updateWithApiData(filteredItems)
                Log.d("WardrobeFragment", "세부카테고리 아이템 복원: ${filteredItems.size}개")
            }

            else -> {
                // 메인 카테고리만 선택된 경우
                val allItems = viewModel.uiState.value.wardrobeItems
                val filteredItems = allItems.filter { it.category == currentSelectedCategory }
                adapter.updateWithApiData(filteredItems)
                Log.d("WardrobeFragment", "메인카테고리 아이템 복원: ${filteredItems.size}개")
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
        Log.d("WardrobeFragment", "🔍 setSearchIconColor 호출: applied=$applied")

        try {
            if (!::searchButton.isInitialized) {
                Log.e("WardrobeFragment", "❌ searchButton이 초기화되지 않음!")
                return
            }

            val iconRes = if (applied) {
                R.drawable.ic_search_selected  // 파란색 아이콘
            } else {
                R.drawable.ic_search_default // 회색 아이콘
            }

            searchButton.setImageResource(iconRes)
            isFilterApplied = applied

            Log.d("WardrobeFragment", "✅ 검색 아이콘 변경 완료: ${if (applied) "활성화" else "비활성화"}")

        } catch (e: Exception) {
            Log.e("WardrobeFragment", "💥 setSearchIconColor 에러", e)
        }
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
        Log.d("WardrobeFragment", "🔍 로컬 필터링 시작: ${items.size}개 아이템")

        // 🔥 시즌 필터링
        bundle.getString("filter_season")?.let { season ->
            if (season.isNotEmpty()) {
                Log.d("WardrobeFragment", "시즌 필터: $season")
                val seasonId = when (season) {
                    "봄ㆍ가을" -> 1
                    "여름" -> 2
                    "겨울" -> 4
                    else -> null
                }
                seasonId?.let { id ->
                    filteredItems = filteredItems.filter { it.season == id }
                    Log.d("WardrobeFragment", "시즌 필터링 후: ${filteredItems.size}개")
                }
            }
        }

        // 🔥 브랜드 필터링
        bundle.getString("filter_brand")?.let { brand ->
            if (brand.isNotEmpty()) {
                Log.d("WardrobeFragment", "브랜드 필터: $brand")
                filteredItems = filteredItems.filter {
                    it.brand.contains(brand, ignoreCase = true)
                }
                Log.d("WardrobeFragment", "브랜드 필터링 후: ${filteredItems.size}개")
            }
        }

        // 🔥 색상 필터링 (만약 있다면)
        bundle.getInt("filter_color", -1).let { color ->
            if (color != -1) {
                Log.d("WardrobeFragment", "색상 필터: $color")
                filteredItems = filteredItems.filter { it.color == color }
                Log.d("WardrobeFragment", "색상 필터링 후: ${filteredItems.size}개")
            }
        }

        Log.d("WardrobeFragment", "🎯 최종 필터링 결과: ${filteredItems.size}개")
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

    private fun updateCategoryButtonsWithCount(categories: List<CategoryDto>, items: List<WardrobeItemDto>) {
        if (!isAdded || context == null) return

        Log.d("WardrobeFragment", "🔄 updateCategoryButtonsWithCount 시작")
        Log.d("WardrobeFragment", "받은 카테고리 정보: ${categories.map { "${it.name}(${it.category}): ${it.count}" }}")
        Log.d("WardrobeFragment", "아이템 개수: ${items.size}")

        try {
            val totalCount = items.size
            val allButton = view?.findViewById<Button>(R.id.btnTopCategory1)
            allButton?.text = "전체 $totalCount"
            Log.d("WardrobeFragment", "전체 버튼 업데이트: 전체 $totalCount")

            // 🔥 FIXED: 액세서리 버튼 추가
            val categoryMapping = mapOf(
                1 to Pair(R.id.btnTopCategory2, "상의"),
                2 to Pair(R.id.btnTopCategory3, "하의"),
                4 to Pair(R.id.btnTopCategory4, "원피스"),
                3 to Pair(R.id.btnTopCategory5, "아우터"),
                5 to Pair(R.id.btnTopCategory6, "신발"),
                6 to Pair(R.id.btnTopCategory7, "액세서리")
            )

            categoryMapping.forEach { (categoryId, buttonInfo) ->
                val (buttonId, categoryName) = buttonInfo
                val button = view?.findViewById<Button>(buttonId)

                // 🔥 클라이언트에서 직접 개수 계산 (서버 + 더미 포함)
                val count = items.count { it.category == categoryId }

                button?.text = "$categoryName $count"
                Log.d("WardrobeFragment", "버튼 업데이트: $categoryName $count")
            }

            Log.d("WardrobeFragment", "✅ 카테고리 버튼 업데이트 완료")
        } catch (e: Exception) {
            Log.e("WardrobeFragment", "Error updating category buttons", e)
        }
    }

    // 🔥 FIXED: updateSubCategories 함수 완전히 수정
    private fun updateSubCategories(subcategories: List<SubcategoryDto>) {
        Log.d("WardrobeFragment", "=== updateSubCategories 시작 ===")
        Log.d("WardrobeFragment", "받은 세부카테고리 개수: ${subcategories.size}")
        Log.d("WardrobeFragment", "현재 선택 상태 - category: $currentSelectedCategory, subcategory: $currentSelectedSubcategory")

        // 🔥 중복 방지 로직 수정 - 상태도 함께 비교
        val stateKey = "${subcategories.size}-${currentSelectedCategory}-${currentSelectedSubcategory}"
        val lastStateKey = "${lastSubcategoriesSize}-${currentSelectedCategory}-${currentSelectedSubcategory}"

        Log.d("WardrobeFragment", "상태 키 비교: current=$stateKey, last=$lastStateKey")

        when {
            // 1. 서버에서 세부 카테고리 목록을 보내준 경우
            subcategories.isNotEmpty() -> {
                Log.d("WardrobeFragment", "✅ 서버에서 세부카테고리 제공")
                subcategories.forEach { subcategory ->
                    Log.d("WardrobeFragment", "📋 세부카테고리: ${subcategory.name} (ID: ${subcategory.subcategory})")
                }
                updateSubFiltersWithApiData(subcategories)
                lastSubcategoriesSize = subcategories.size
            }

            // 2. 상위 카테고리가 선택되었고 서버에서 세부카테고리를 안 보내준 경우
            currentSelectedCategory != null -> {
                Log.d("WardrobeFragment", "🔥 상위 카테고리 선택됨 - 클라이언트에서 세부카테고리 생성")
                createSubcategoriesFromClient(currentSelectedCategory!!)
                lastSubcategoriesSize = 0 // 서버에서 온 건 0개
            }

            // 3. 전체 카테고리인 경우
            else -> {
                Log.d("WardrobeFragment", "⭐ 전체 카테고리 - 기본 전체 버튼만 표시")
                createDefaultAllButton()
                lastSubcategoriesSize = 0
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

        // 🔥 FIXED: 서버에서 카테고리 정보가 0으로 오는 문제 해결
        val currentItems = viewModel.uiState.value.wardrobeItems
        Log.d("WardrobeFragment", "🔍 전체 아이템 개수: ${currentItems.size}")

        currentItems.forEach { item ->
            Log.d("WardrobeFragment", "📱 아이템 ID: ${item.id}, 카테고리: ${item.category}, 서브카테고리: ${item.subcategory}")
        }

        // 🔥 NEW: 서버에서 카테고리 정보가 0으로 오는 경우 모든 서브카테고리 표시
        val hasValidCategories = currentItems.any { it.category != null && it.category != 0 }

        if (!hasValidCategories) {
            Log.w("WardrobeFragment", "⚠️ 서버에서 유효한 카테고리 정보 없음 - 모든 서브카테고리 표시")
            showAllSubcategoriesForCategory(categoryId)
            return
        }

        // 🔥 정상적인 카테고리 정보가 있는 경우 기존 로직 사용
        val categoryItems = currentItems.filter { item ->
            Log.d("WardrobeFragment", "🔍 아이템 ${item.id} 카테고리 비교: ${item.category} == $categoryId ?")
            item.category == categoryId
        }

        Log.d("WardrobeFragment", "카테고리 $categoryId 보유 아이템 수: ${categoryItems.size}")

        if (categoryItems.isEmpty()) {
            Log.d("WardrobeFragment", "❌ 해당 카테고리에 아이템 없음 - 모든 서브카테고리 표시")
            showAllSubcategoriesForCategory(categoryId)
            return
        }

        // 카테고리별 서브카테고리 정의
        val allSubcategoryMap = mapOf(
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

        val allSubcategories = allSubcategoryMap[categoryId] ?: emptyList()

        val itemSubcategories = categoryItems.mapNotNull { it.subcategory }.distinct()
        Log.d("WardrobeFragment", "🔍 실제 아이템들의 서브카테고리 ID: $itemSubcategories")

        val availableSubcategories = allSubcategories.filter { subcategoryDto ->
            val hasItem = itemSubcategories.contains(subcategoryDto.subcategory)
            Log.d("WardrobeFragment", "🔍 서브카테고리 ${subcategoryDto.name}(${subcategoryDto.subcategory}) 보유 여부: $hasItem")
            hasItem
        }

        if (availableSubcategories.isNotEmpty()) {
            Log.d("WardrobeFragment", "✅ 보유 서브카테고리로 필터 생성")
            updateSubFiltersWithApiData(availableSubcategories)
        } else {
            Log.d("WardrobeFragment", "❌ 매칭되는 서브카테고리 없음 - 모든 서브카테고리 표시")
            showAllSubcategoriesForCategory(categoryId)
        }
    }

    // 🔥 NEW: 모든 서브카테고리를 표시하는 함수 (서버 응답 문제 대응용)
    private fun showAllSubcategoriesForCategory(categoryId: Int) {
        Log.d("WardrobeFragment", "🌟 showAllSubcategoriesForCategory: $categoryId")

        val allSubcategoryMap = mapOf(
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

        val subcategories = allSubcategoryMap[categoryId] ?: emptyList()
        if (subcategories.isNotEmpty()) {
            Log.d("WardrobeFragment", "✅ 모든 서브카테고리 표시: ${subcategories.map { it.name }}")
            updateSubFiltersWithApiData(subcategories)
        } else {
            createDefaultAllButton()
        }
    }

    // 🔥 NEW: 기본 '전체' 버튼만 생성하는 함수
    private fun createDefaultAllButton() {
        if (!isAdded || context == null) return

        Log.d("WardrobeFragment", "🔧 createDefaultAllButton 호출")

        subFilterLayout.removeAllViews()
        selectedIndex = 0

        // 🔥 FIXED: 전체 버튼에는 개수 표시 안 함
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
                    viewModel.loadWardrobeItemsByCategory(
                        category = currentCategory,
                        subcategory = null
                    )
                }
            }
        }
        subFilterLayout.addView(allButton)
        updateButtonSelection(0)

        // 밑줄 위치 계산
        subFilterLayout.post {
            if (isAdded && view != null) {
                moveUnderline(0)
            }
        }
    }

    // 🔥 WardrobeFragment의 updateSubFiltersWithApiData 함수 수정
    private fun updateSubFiltersWithApiData(subcategories: List<SubcategoryDto>) {
        if (!isAdded || context == null) return

        Log.d("WardrobeFragment", "updateSubFiltersWithApiData 시작, 세부카테고리: ${subcategories.map { it.name }}")

        subFilterLayout.removeAllViews()
        selectedIndex = 0

        // 🔥 FIXED: 현재 선택된 카테고리의 아이템만 가져오기
        val currentItems = viewModel.uiState.value.wardrobeItems
        val currentCategory = getCurrentSelectedCategory()

        val categoryItems = if (currentCategory == null) {
            // 전체 카테고리인 경우
            currentItems
        } else {
            // 🔥 FIX: 선택된 카테고리의 아이템만 필터링
            val filteredItems = currentItems.filter { item ->
                Log.d("WardrobeFragment", "🔍 아이템 ${item.id} 카테고리 체크: ${item.category} == $currentCategory")
                item.category == currentCategory
            }

            Log.d("WardrobeFragment", "🎯 선택된 카테고리($currentCategory) 아이템: ${filteredItems.size}개")
            filteredItems.forEach { item ->
                Log.d("WardrobeFragment", "  - 아이템 ${item.id}: category=${item.category}, subcategory=${item.subcategory}")
            }

            filteredItems
        }

        Log.d("WardrobeFragment", "📊 카테고리 아이템 개수: ${categoryItems.size}")

        // '전체' 버튼 추가 (해당 카테고리의 모든 아이템 개수 표시)
        val allButton = createFilterButton("전체 ${categoryItems.size}", 0, subcategories.size + 1)
        allButton.setOnClickListener {
            if (isAdded && context != null) {
                updateButtonSelection(0)
                moveUnderline(0)
                currentSelectedSubcategory = null

                // 🔥 FIXED: 현재 카테고리의 아이템만 표시
                adapter.updateWithApiData(categoryItems)
                Log.d("WardrobeFragment", "전체 세부카테고리 선택: ${categoryItems.size}개 아이템 표시")
            }
        }
        subFilterLayout.addView(allButton)

        // 세부 카테고리 버튼들 추가
        subcategories.forEachIndexed { index, subcategoryDto ->
            // 해당 서브카테고리의 아이템 개수 계산
            val subcategoryItemCount = categoryItems.count { it.subcategory == subcategoryDto.subcategory }
            val displayName = if (subcategoryItemCount > 0) {
                "${subcategoryDto.name} $subcategoryItemCount"
            } else {
                subcategoryDto.name
            }

            val button = createFilterButton(displayName, index + 1, subcategories.size + 1)
            button.setOnClickListener {
                if (isAdded && context != null) {
                    updateButtonSelection(index + 1)
                    moveUnderline(index + 1)
                    currentSelectedSubcategory = subcategoryDto.subcategory

                    // 🔥 FIXED: 현재 카테고리 + 서브카테고리로 필터링
                    val filteredItems = categoryItems.filter { it.subcategory == subcategoryDto.subcategory }
                    adapter.updateWithApiData(filteredItems)
                    Log.d("WardrobeFragment", "서브카테고리 ${subcategoryDto.name} 선택: ${filteredItems.size}개 아이템 표시")
                }
            }
            subFilterLayout.addView(button)
        }

        // 🔥 FIXED: 초기 로드 시 현재 카테고리 아이템만 표시
        updateButtonSelection(0)
        adapter.updateWithApiData(categoryItems)
        Log.d("WardrobeFragment", "초기 로드: 현재 카테고리 ${categoryItems.size}개 아이템 표시")

        // 밑줄 위치 계산
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
                R.id.btnTopCategory4 -> 3
                R.id.btnTopCategory5 -> 4
                R.id.btnTopCategory6 -> 5
                R.id.btnTopCategory7 -> 6
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
        Log.d("WardrobeFragment", "🔍 로컬 검색 시작: '$query'")

        val currentItems = viewModel.uiState.value.wardrobeItems
        val filteredItems = currentItems.filter { item ->
            val matchesBrand = item.brand.contains(query, ignoreCase = true)
            val matchesId = item.id.toString().contains(query)
            matchesBrand || matchesId
        }

        Log.d("WardrobeFragment", "로컬 검색 결과: ${filteredItems.size}개")

        if (filteredItems.isNotEmpty()) {
            adapter.updateWithApiData(filteredItems)
            setSearchIconColor(true) // 🔥 돋보기 색상 변경
            Toast.makeText(context, "${filteredItems.size}개의 아이템을 찾았습니다", Toast.LENGTH_SHORT).show()
        } else {
            adapter.updateWithApiData(emptyList())
            setSearchIconColor(true) // 🔥 검색했지만 결과 없음도 active 상태
            Toast.makeText(context, "검색 결과가 없습니다", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupTopCategoryButtons(view: View) {
        val topCategories = mapOf(
            R.id.btnTopCategory1 to Pair("전체", null),
            R.id.btnTopCategory2 to Pair("상의", 1),
            R.id.btnTopCategory3 to Pair("하의", 2),
            R.id.btnTopCategory4 to Pair("원피스", 3),
            R.id.btnTopCategory5 to Pair("아우터", 4),
            R.id.btnTopCategory6 to Pair("신발", 5),
            R.id.btnTopCategory7 to Pair("액세서리", 6)
        )

        topCategories.forEach { (id, categoryData) ->
            val button = view.findViewById<Button>(id)
            val (categoryName, categoryId) = categoryData

            button?.setOnClickListener {
                selectedTopCategoryButton?.isSelected = false
                button.isSelected = true
                selectedTopCategoryButton = button

                setSearchIconColor(false)
                isFilterApplied = false

                currentSelectedCategory = categoryId
                currentSelectedSubcategory = null
                lastSubcategoriesSize = -1

                Log.d("WardrobeFragment", "카테고리 클릭: $categoryName (ID: $categoryId)")

                if (categoryName == "전체") {
                    Log.d("WardrobeFragment", "전체 카테고리 선택")

                    // 🔥 서버 + 더미 데이터 모두 포함
                    val allItems = if (USE_WARDROBE_DUMMY) {
                        combineServerAndDummyData(viewModel.uiState.value.wardrobeItems)
                    } else {
                        viewModel.uiState.value.wardrobeItems
                    }

                    adapter.updateWithApiData(allItems)
                    Log.d("WardrobeFragment", "전체 아이템 표시: ${allItems.size}개")

                    createDefaultAllButton()
                    viewModel.loadAllWardrobeItems()
                } else {
                    Log.d("WardrobeFragment", "카테고리 선택: $categoryName (ID: $categoryId)")

                    // 🔥 서버 + 더미 데이터에서 해당 카테고리만 필터링
                    val allItems = if (USE_WARDROBE_DUMMY) {
                        combineServerAndDummyData(viewModel.uiState.value.wardrobeItems)
                    } else {
                        viewModel.uiState.value.wardrobeItems
                    }

                    val categoryItems = allItems.filter { item ->
                        Log.d("WardrobeFragment", "아이템 ${item.id} 카테고리 체크: ${item.category} == $categoryId")
                        item.category == categoryId
                    }

                    Log.d("WardrobeFragment", "필터링된 카테고리 아이템: ${categoryItems.size}개")
                    adapter.updateWithApiData(categoryItems)

                    createSubcategoriesFromClient(categoryId!!)
                    viewModel.loadWardrobeItemsByCategory(category = categoryId)
                }
            }

            if (categoryName == "전체") {
                button.isSelected = true
                selectedTopCategoryButton = button
                currentSelectedCategory = null
                currentSelectedSubcategory = null
            }
        }
    }

    private fun navigateToClothesDetail(itemId: Int) {
        try {
            Log.d("WardrobeFragment", "🔗 아이템 상세보기 이동: ID=$itemId")

            // 🔥 FIXED: 더미 아이템 (음수 ID)도 허용
            if (itemId < 0) {
                Log.d("WardrobeFragment", "🎭 더미 아이템 클릭됨: ID=$itemId - 상세보기로 이동")
                // 더미 아이템도 상세보기로 이동
                val bundle = Bundle().apply {
                    putInt("image_res_id", itemId)
                    putInt("item_category", 0) // 더미 데이터 표시
                    putInt("item_subcategory", 0)
                }

                try {
                    findNavController().navigate(R.id.clothesDetailFragment, bundle)
                    Log.d("WardrobeFragment", "✅ 더미 아이템 네비게이션 성공")
                } catch (e: Exception) {
                    Log.e("WardrobeFragment", "❌ 더미 아이템 네비게이션 실패", e)
                    Toast.makeText(context, "상세보기를 열 수 없습니다: ${e.message}", Toast.LENGTH_SHORT).show()
                }
                return
            }

            // 🔥 실제 아이템 ID인지 확인
            val currentItems = viewModel.uiState.value.wardrobeItems
            val targetItem = currentItems.find { it.id == itemId }

            if (targetItem == null) {
                Log.e("WardrobeFragment", "❌ 아이템을 찾을 수 없음: ID=$itemId")
                Toast.makeText(context, "아이템 정보를 찾을 수 없습니다", Toast.LENGTH_SHORT).show()
                return
            }

            Log.d("WardrobeFragment", "✅ 아이템 발견: ${targetItem.brand}, 카테고리: ${targetItem.category}")

            val bundle = Bundle().apply {
                putInt("image_res_id", itemId)
                putInt("item_category", targetItem.category)
                putInt("item_subcategory", targetItem.subcategory)
            }

            try {
                findNavController().navigate(R.id.clothesDetailFragment, bundle)
                Log.d("WardrobeFragment", "✅ 실제 아이템 네비게이션 성공")
            } catch (e: Exception) {
                Log.e("WardrobeFragment", "❌ 실제 아이템 네비게이션 실패", e)
                Toast.makeText(context, "상세보기를 열 수 없습니다: ${e.message}", Toast.LENGTH_SHORT).show()
            }

        } catch (e: Exception) {
            Log.e("WardrobeFragment", "💥 navigateToClothesDetail 전체 실패: ${e.message}", e)
            Toast.makeText(context, "상세보기를 열 수 없습니다: ${e.message}", Toast.LENGTH_SHORT).show()
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

    /**
     * Assets 폴더에서 더미 옷장 아이템 생성
     */
    private fun loadDummyWardrobeFromAssets(): List<WardrobeItemDto> {
        if (!USE_WARDROBE_DUMMY) return emptyList()

        try {
            val am = requireContext().assets
            val all = am.list("dummy_recommend")
                ?.filter { name ->
                    val l = name.lowercase()
                    val isImageFile = l.endsWith(".png") || l.endsWith(".jpg") || l.endsWith(".jpeg") || l.endsWith(".jfif") || l.endsWith(".webp")

                    // 🔥 FIXED: 코디 기록과 옷장 아이템 구분
                    val isOutfitRecord = isOutfitRecordFile(name) // 코디 기록 파일 체크
                    val isWardrobeItem = !isOutfitRecord // 코디 기록이 아니면 옷장 아이템

                    Log.d("WardrobeFragment", "파일 검사: $name")
                    Log.d("WardrobeFragment", "  - 이미지파일: $isImageFile")
                    Log.d("WardrobeFragment", "  - 코디기록: $isOutfitRecord")
                    Log.d("WardrobeFragment", "  - 옷장아이템: $isWardrobeItem")

                    isImageFile && isWardrobeItem // 이미지 파일이면서 옷장 아이템인 것만
                } ?: emptyList()

            if (all.isEmpty()) {
                Log.d("WardrobeFragment", "Assets 폴더에 옷장 아이템 이미지가 없습니다")
                return emptyList()
            }

            Log.d("WardrobeFragment", "필터링된 옷장 아이템 파일들: ${all.joinToString(", ")}")

            // 더미 아이템 생성 (파일명 기반으로 카테고리 추정)
            val dummyItems = all.mapIndexed { index, fileName ->
                val (category, subcategory) = estimateCategoryFromFileName(fileName, index)
                val brand = extractBrandFromFileName(fileName) ?: "더미브랜드"

                WardrobeItemDto(
                    id = -(1000 + index), // 음수 ID로 더미 표시
                    image = "file:///android_asset/dummy_recommend/$fileName",
                    brand = brand,
                    season = 1, // 봄가을
                    color = estimateColorFromFileName(fileName),
                    category = category,
                    subcategory = subcategory
                )
            }

            Log.d("WardrobeFragment", "✅ Assets에서 더미 옷장 아이템 ${dummyItems.size}개 생성")
            return dummyItems

        } catch (e: Exception) {
            Log.e("WardrobeFragment", "더미 데이터 로드 실패", e)
            return emptyList()
        }
    }



    /**
     * 🔥 NEW: 코디 기록 파일인지 판별하는 함수
     */
    private fun isOutfitRecordFile(fileName: String): Boolean {
        val name = fileName.lowercase()

        // 날짜.온도(체감온도) 패턴: "6월8.14(26.4).jpg" 형태
        val dateTemperaturePattern = Regex("\\d+월\\d+\\.\\d+\\(\\d+\\.\\d+\\)")

        return name.contains(dateTemperaturePattern)
    }

    /**
     * 🔥 NEW: Assets 폴더에서 코디 기록 로드하는 함수 (나중에 사용)
     */
    private fun loadOutfitRecordsFromAssets(): List<OutfitRecordDto> {
        try {
            val am = requireContext().assets
            val outfitFiles = am.list("dummy_recommend")
                ?.filter { name ->
                    val l = name.lowercase()
                    val isImageFile = l.endsWith(".png") || l.endsWith(".jpg") || l.endsWith(".jpeg") || l.endsWith(".jfif") || l.endsWith(".webp")
                    val isOutfitRecord = isOutfitRecordFile(name)

                    isImageFile && isOutfitRecord
                } ?: emptyList()

            Log.d("WardrobeFragment", "코디 기록 파일들: ${outfitFiles.joinToString(", ")}")

            // 여기서 코디 기록 데이터 생성 로직 구현
            // 현재는 빈 리스트 반환
            return emptyList()

        } catch (e: Exception) {
            Log.e("WardrobeFragment", "코디 기록 로드 실패", e)
            return emptyList()
        }
    }

    /**
     * 파일명에서 카테고리 추정
     */
    private fun estimateCategoryFromFileName(fileName: String, index: Int): Pair<Int, Int> {
        val name = fileName.lowercase()

        return when {
            // 상의 관련 키워드
            name.contains("후드") || name.contains("hood") || name.contains("맨투맨") ||
                    name.contains("티셔츠") || name.contains("셔츠") || name.contains("shirt") -> {
                val subcategory = when {
                    name.contains("후드") || name.contains("hood") -> 6 // 후드티
                    name.contains("셔츠") || name.contains("shirt") -> 4 // 셔츠/블라우스
                    name.contains("맨투맨") -> 5 // 맨투맨
                    else -> 1 // 반팔티셔츠
                }
                Pair(1, subcategory) // 상의
            }

            // 하의 관련 키워드
            name.contains("바지") || name.contains("pants") || name.contains("jean") ||
                    name.contains("슬랙스") || name.contains("팬츠") -> {
                val subcategory = when {
                    name.contains("청바지") || name.contains("jean") -> 11 // 청바지
                    name.contains("슬랙스") -> 10 // 긴바지
                    else -> 10 // 긴바지
                }
                Pair(2, subcategory) // 하의
            }

            // 아우터 관련 키워드
            name.contains("자켓") || name.contains("jacket") || name.contains("코트") ||
                    name.contains("아우터") || name.contains("outer") -> {
                Pair(4, 23) // 아우터 - 자켓
            }

            // 신발 관련 키워드
            name.contains("신발") || name.contains("shoes") || name.contains("운동화") ||
                    name.contains("sneakers") -> {
                Pair(5, 29) // 신발 - 운동화
            }

            // 액세서리 관련 키워드
            name.contains("안경") || name.contains("glasses") || name.contains("가방") ||
                    name.contains("bag") || name.contains("모자") || name.contains("hat") -> {
                val subcategory = when {
                    name.contains("안경") || name.contains("glasses") -> 40 // 안경/선글라스
                    name.contains("가방") || name.contains("bag") -> 41 // 가방
                    name.contains("모자") || name.contains("hat") -> 36 // 모자
                    else -> 43 // 기타
                }
                Pair(6, subcategory) // 액세서리
            }

            // 기본값: 인덱스 기반으로 순환 배치
            else -> {
                val categories = listOf(
                    Pair(1, 1), // 상의 - 반팔티셔츠
                    Pair(2, 10), // 하의 - 긴바지
                    Pair(4, 23), // 아우터 - 자켓
                    Pair(5, 29), // 신발 - 운동화
                    Pair(6, 43)  // 액세서리 - 기타
                )
                categories[index % categories.size]
            }
        }
    }

    /**
     * 파일명에서 브랜드 추정
     */
    private fun extractBrandFromFileName(fileName: String): String? {
        // 브랜드 관련 키워드가 있으면 추출, 없으면 null
        val brands = listOf("nike", "adidas", "uniqlo", "zara", "h&m", "무지", "엠씨엠")
        val name = fileName.lowercase()

        return brands.find { brand -> name.contains(brand) }
    }

    /**
     * 파일명에서 색상 추정
     */
    private fun estimateColorFromFileName(fileName: String): Int {
        val name = fileName.lowercase()

        return when {
            name.contains("black") || name.contains("블랙") || name.contains("검정") -> 1 // 블랙
            name.contains("white") || name.contains("화이트") || name.contains("흰색") -> 2 // 화이트
            name.contains("gray") || name.contains("grey") || name.contains("그레이") -> 3 // 그레이
            name.contains("navy") || name.contains("네이비") -> 4 // 네이비
            name.contains("brown") || name.contains("브라운") || name.contains("갈색") -> 5 // 브라운
            name.contains("beige") || name.contains("베이지") -> 6 // 베이지
            name.contains("red") || name.contains("빨강") || name.contains("레드") -> 7 // 레드
            name.contains("pink") || name.contains("핑크") -> 8 // 핑크
            name.contains("yellow") || name.contains("노랑") || name.contains("옐로우") -> 9 // 옐로우
            name.contains("green") || name.contains("초록") || name.contains("그린") -> 10 // 그린
            name.contains("blue") || name.contains("파랑") || name.contains("블루") -> 11 // 블루
            name.contains("purple") || name.contains("보라") || name.contains("퍼플") -> 12 // 퍼플
            else -> 1 // 기본값: 블랙
        }
    }

    /**
     * 🔥 handleUiState에서 더미 데이터와 서버 데이터 결합
     */
    private fun combineServerAndDummyData(serverItems: List<WardrobeItemDto>): List<WardrobeItemDto> {
        val dummyItems = loadDummyWardrobeFromAssets()
        val combined = serverItems + dummyItems

        Log.d("WardrobeFragment", "📊 데이터 결합: 서버 ${serverItems.size}개 + 더미 ${dummyItems.size}개 = 총 ${combined.size}개")

        return combined
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

    // 🔥 임시 데이터 클래스 (실제 코디 기록용)
    data class OutfitRecordDto(
        val id: Int,
        val date: String,
        val temperature: String,
        val imagePath: String,
        val items: List<Int> // 포함된 아이템 ID들
    )
}