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
        Log.d("WardrobeFragment", "  - currentSelectedCategory: $currentSelectedCategory")
        Log.d("WardrobeFragment", "  - 아이템 수: ${if (state.hasData) state.wardrobeItems.size else "no data"}")

        if (state.isLoading) {
            Log.d("WardrobeFragment", "⏳ 로딩 상태")
            showLoading(true)
        } else {
            showLoading(false)
        }

        if (state.hasData) {
            Log.d("WardrobeFragment", "📊 데이터 있음 - 아이템 개수: ${state.wardrobeItems.size}")

            // 🔥 CRITICAL: 서버 데이터 상세 분석
            Log.d("WardrobeFragment", "🔍 서버 데이터 분석:")
            state.wardrobeItems.forEach { item ->
                Log.d("WardrobeFragment", "  📱 서버아이템: ID=${item.id}, category=${item.category}, subcategory=${item.subcategory}, brand='${item.brand}'")
            }

            // 🔥 CRITICAL FIX: API 아이템 중 카테고리 정보가 없거나 0인 것들 확인
            val needDetailItems = state.wardrobeItems.filter {
                it.category == null || it.category == 0 || it.subcategory == null || it.subcategory == 0
            }

            if (needDetailItems.isNotEmpty()) {
                Log.w("WardrobeFragment", "⚠️ ${needDetailItems.size}개 API 아이템이 상세 정보 필요:")
                needDetailItems.forEach { item ->
                    Log.w("WardrobeFragment", "  상세필요: ID=${item.id}, category=${item.category}, subcategory=${item.subcategory}")
                }

                // 🔥 검색 상태가 아닐 때만 상세 정보 로드
                if (!isFilterApplied) {
                    Log.d("WardrobeFragment", "🔧 API 아이템 상세 정보 로드 시작")
                    loadItemDetailsForAPIItems(needDetailItems, state)
                    return // 상세 정보 로드 후 다시 호출됨
                } else {
                    Log.d("WardrobeFragment", "🔴 검색 상태 - 상세 정보 로드 건너뛰고 기본 처리")
                }
            }

            // 🔥 서버 데이터와 더미 데이터 결합
            val allItems = if (USE_WARDROBE_DUMMY) {
                combineServerAndDummyData(state.wardrobeItems)
            } else {
                state.wardrobeItems
            }

            Log.d("WardrobeFragment", "🔗 결합 후 전체 아이템:")
            allItems.forEach { item ->
                Log.d("WardrobeFragment", "  🔍 전체아이템: ID=${item.id}, category=${item.category}, subcategory=${item.subcategory}")
            }

            // 🔥 검색 상태가 아닐 때만 어댑터 업데이트
            if (!isFilterApplied) {
                Log.d("WardrobeFragment", "🟢 정상 상태 - 어댑터 업데이트")

                val itemsToShow = if (currentSelectedCategory == null) {
                    Log.d("WardrobeFragment", "📋 전체 카테고리 표시: ${allItems.size}개")
                    allItems
                } else {
                    val filtered = allItems.filter { it.category == currentSelectedCategory }
                    Log.d("WardrobeFragment", "📋 카테고리 $currentSelectedCategory 필터링: ${filtered.size}개")
                    filtered
                }

                adapter.updateWithApiData(itemsToShow)
            } else {
                Log.d("WardrobeFragment", "🔴 검색 상태 - 어댑터 업데이트 건너뜀")

                if (::searchButton.isInitialized) {
                    view?.post {
                        setSearchIconColor(true)
                    }
                }
            }

            // 🔥 카테고리 버튼 개수 업데이트 (항상 전체 데이터 기준)
            updateCategoryButtonsWithCount(state.categories, allItems)

            // 🔥 서브카테고리 업데이트 로직
            val shouldUpdateSubcategories = when {
                state.subcategories.isNotEmpty() -> {
                    Log.d("WardrobeFragment", "✅ 서버 세부카테고리 있음")
                    true
                }
                currentSelectedCategory != null -> {
                    Log.d("WardrobeFragment", "✅ 상위 카테고리 선택됨")
                    true
                }
                currentSelectedCategory == null && allItems.isNotEmpty() && !isFilterApplied -> {
                    Log.d("WardrobeFragment", "✅ 전체 상태 + 아이템 있음 + 검색 아님")
                    true
                }
                subFilterLayout.childCount == 0 && !isFilterApplied -> {
                    Log.d("WardrobeFragment", "✅ 세부카테고리 없음 + 검색 아님")
                    true
                }
                else -> {
                    Log.d("WardrobeFragment", "❌ 세부카테고리 업데이트 불필요")
                    false
                }
            }

            if (shouldUpdateSubcategories) {
                Log.d("WardrobeFragment", "🚀 세부카테고리 업데이트 시작")
                // 🔥 FIXED: 함수명 수정
                updateSubCategories(state.subcategories)
            }

        } else {
            Log.d("WardrobeFragment", "❌ 서버 데이터 없음")

            // 🔥 서버 데이터가 없어도 더미 데이터 표시
            if (USE_WARDROBE_DUMMY) {
                val dummyItems = loadDummyWardrobeFromAssets()
                if (dummyItems.isNotEmpty()) {
                    Log.d("WardrobeFragment", "🎭 더미 데이터만 표시: ${dummyItems.size}개")
                    adapter.updateWithApiData(dummyItems)
                    updateCategoryButtonsWithCount(emptyList(), dummyItems)
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

    private fun loadItemDetailsForAPIItems(needDetailItems: List<WardrobeItemDto>, originalState: WardrobeUiState) {
        Log.d("WardrobeFragment", "🔧 loadItemDetailsForAPIItems 시작 - ${needDetailItems.size}개 API 아이템")

        lifecycleScope.launch {
            val correctedAPIItems = mutableListOf<WardrobeItemDto>()

            // 🔥 기존 서버 아이템들을 베이스로 시작
            val baseItems = originalState.wardrobeItems.toMutableList()

            // 🔥 상세 정보가 필요한 API 아이템들만 처리
            needDetailItems.forEach { item ->
                if (isFilterApplied) {
                    Log.w("WardrobeFragment", "🚫 검색 상태 감지 - API 상세 로드 중단")
                    return@launch
                }

                try {
                    Log.d("WardrobeFragment", "📡 API 아이템 ${item.id} 상세정보 요청")

                    val detailResult = repository.getWardrobeItemDetail(item.id)
                    if (detailResult.isSuccess) {
                        val detail = detailResult.getOrNull()
                        if (detail != null) {
                            val correctedItem = WardrobeItemDto(
                                id = detail.id,
                                image = detail.image ?: item.image,
                                brand = detail.brand ?: item.brand,
                                season = detail.season,
                                color = detail.color,
                                category = detail.category,
                                subcategory = detail.subcategory
                            )

                            // 🔥 기존 리스트에서 해당 아이템 교체
                            val index = baseItems.indexOfFirst { it.id == item.id }
                            if (index != -1) {
                                baseItems[index] = correctedItem
                                Log.d("WardrobeFragment", "✅ API 아이템 ${item.id} 교체: category=${item.category}→${detail.category}, subcategory=${item.subcategory}→${detail.subcategory}")
                            }
                        } else {
                            Log.w("WardrobeFragment", "⚠️ API 아이템 ${item.id} 상세정보가 null")
                        }
                    } else {
                        Log.e("WardrobeFragment", "❌ API 아이템 ${item.id} 상세정보 실패: ${detailResult.exceptionOrNull()?.message}")
                    }
                } catch (e: Exception) {
                    Log.e("WardrobeFragment", "❌ API 아이템 ${item.id} 상세정보 예외", e)
                }
            }

            // 🔥 검색 상태 재확인
            if (isFilterApplied) {
                Log.w("WardrobeFragment", "🚫 최종 업데이트 전 검색 상태 감지 - 업데이트 건너뜀")
                return@launch
            }

            Log.d("WardrobeFragment", "📊 API 아이템 상세 정보 로드 후:")
            baseItems.forEach { item ->
                Log.d("WardrobeFragment", "  보정완료: ID=${item.id}, category=${item.category}, subcategory=${item.subcategory}")
            }

            // 🔥 더미 데이터와 결합
            val allItems = if (USE_WARDROBE_DUMMY) {
                val dummyItems = loadDummyWardrobeFromAssets()
                baseItems + dummyItems
            } else {
                baseItems
            }

            Log.d("WardrobeFragment", "🔗 더미 결합 후 전체 아이템: ${allItems.size}개")
            allItems.forEach { item ->
                Log.d("WardrobeFragment", "  전체아이템: ID=${item.id}, category=${item.category}, subcategory=${item.subcategory}")
            }

            // 🔥 CRITICAL: 현재 선택된 카테고리에 따라 표시할 아이템 결정
            val itemsToShow = when {
                currentSelectedCategory == null -> {
                    Log.d("WardrobeFragment", "📋 전체 카테고리 표시: ${allItems.size}개")
                    allItems
                }
                else -> {
                    val filtered = allItems.filter { it.category == currentSelectedCategory }
                    Log.d("WardrobeFragment", "📋 카테고리 $currentSelectedCategory 필터링: ${filtered.size}개")
                    filtered.forEach { item ->
                        Log.d("WardrobeFragment", "  카테고리필터결과: ID=${item.id}, cat=${item.category}, subcat=${item.subcategory}")
                    }
                    filtered
                }
            }

            Log.d("WardrobeFragment", "🔄 API 상세 로드 후 어댑터 업데이트: ${itemsToShow.size}개")
            itemsToShow.forEach { item ->
                Log.d("WardrobeFragment", "  어댑터업데이트아이템: ID=${item.id}, cat=${item.category}, subcat=${item.subcategory}")
            }

            adapter.updateWithApiData(itemsToShow)

            // 🔥 카테고리 버튼 업데이트 (전체 아이템 기준)
            updateCategoryButtonsWithCount(originalState.categories, allItems)

            // 🔥 CRITICAL: 현재 카테고리가 선택되어 있으면 서브카테고리도 업데이트
            if (currentSelectedCategory != null && itemsToShow.isNotEmpty()) {
                Log.d("WardrobeFragment", "🚀 API 상세 로드 후 서브카테고리 업데이트")
                Log.d("WardrobeFragment", "서브카테고리 생성 대상: ${itemsToShow.size}개 아이템")
                createSubcategoriesFromClientWithItems(currentSelectedCategory!!, itemsToShow, allItems)
            } else if (currentSelectedCategory == null) {
                Log.d("WardrobeFragment", "⭐ 전체 카테고리 - 기본 전체 버튼 생성")
                createDefaultAllButton()
            }

            Log.d("WardrobeFragment", "✅ loadItemDetailsForAPIItems 완료")
        }
    }

    private fun loadItemDetails(items: List<WardrobeItemDto>) {
        Log.d("WardrobeFragment", "🔧 loadItemDetails 호출됨 - 대신 loadItemDetailsForAPIItems 사용")

        // API 아이템들만 필터링
        val apiItems = items.filter { it.id > 0 && (it.category == null || it.category == 0 || it.subcategory == null || it.subcategory == 0) }

        if (apiItems.isNotEmpty()) {
            val currentState = viewModel.uiState.value
            loadItemDetailsForAPIItems(apiItems, currentState)
        } else {
            Log.d("WardrobeFragment", "✅ 상세 정보가 필요한 API 아이템 없음")
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
        parentFragmentManager.setFragmentResultListener(
            "wardrobe_item_updated",
            this
        ) { _, bundle ->
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
                Log.d("WardrobeFragment", "받은 ID들: ${filteredIds?.contentToString()}")

                if (filterApplied && filteredIds != null && filteredIds.isNotEmpty()) {
                    // 🔥 CRITICAL FIX: 서버 + 더미 데이터 모두에서 검색
                    val serverItems = viewModel.uiState.value.wardrobeItems
                    val allItems = if (USE_WARDROBE_DUMMY) {
                        combineServerAndDummyData(serverItems)
                    } else {
                        serverItems
                    }

                    Log.d("WardrobeFragment", "🔍 검색 대상 전체 아이템: ${allItems.size}개")
                    allItems.forEach { item ->
                        Log.d(
                            "WardrobeFragment",
                            "  전체아이템: ID=${item.id}, category=${item.category}, brand='${item.brand}'"
                        )
                    }

                    // 🔥 ID로 필터링
                    val filteredItems = allItems.filter { item ->
                        val matches = item.id in filteredIds
                        Log.d(
                            "WardrobeFragment",
                            "🔍 검색필터: 아이템 ${item.id} in ${filteredIds.contentToString()} ? $matches"
                        )
                        matches
                    }

                    Log.d("WardrobeFragment", "✅ 필터링된 검색 결과: ${filteredItems.size}개")
                    filteredItems.forEach { item ->
                        Log.d(
                            "WardrobeFragment",
                            "  결과아이템: ID=${item.id}, category=${item.category}, brand='${item.brand}'"
                        )
                    }

                    // 🔥 어댑터 업데이트
                    if (::adapter.isInitialized) {
                        adapter.updateWithApiData(filteredItems)

                        // 🔥 강제 새로고침
                        if (::recyclerView.isInitialized) {
                            recyclerView.post {
                                adapter.notifyDataSetChanged()
                                Log.d("WardrobeFragment", "✅ 검색 결과 어댑터 새로고침 완료")
                            }
                        }
                    }

                    // 🔥 검색 상태 설정
                    isFilterApplied = true
                    setSearchIconColor(true)

                    // 🔥 결과 메시지
                    val message = if (filteredItems.isNotEmpty()) {
                        "${filteredItems.size}개 아이템 검색됨"
                    } else {
                        "검색 조건에 맞는 아이템이 없습니다"
                    }
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()

                } else {
                    // 🔥 필터 해제
                    Log.d("WardrobeFragment", "🔄 필터 해제 - 원래 상태 복원")
                    restoreOriginalItems()
                    isFilterApplied = false
                    setSearchIconColor(false)
                }

            } catch (e: Exception) {
                Log.e("WardrobeFragment", "💥 검색 결과 처리 실패", e)
                Toast.makeText(context, "검색 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun restoreOriginalItems() {
        Log.d("WardrobeFragment", "🔄 원래 아이템 상태 복원")

        // 🔥 CRITICAL: 더미 데이터 포함한 전체 아이템 사용
        val serverItems = viewModel.uiState.value.wardrobeItems
        val allItems = if (USE_WARDROBE_DUMMY) {
            combineServerAndDummyData(serverItems)
        } else {
            serverItems
        }

        Log.d("WardrobeFragment", "📦 복원용 전체 아이템: ${allItems.size}개")

        when {
            currentSelectedCategory == null -> {
                // 전체 카테고리인 경우
                Log.d("WardrobeFragment", "🌐 전체 카테고리 복원: ${allItems.size}개")
                adapter.updateWithApiData(allItems)
            }

            currentSelectedSubcategory != null -> {
                // 세부 카테고리가 선택된 경우
                val filteredItems = allItems.filter {
                    it.category == currentSelectedCategory && it.subcategory == currentSelectedSubcategory
                }
                Log.d("WardrobeFragment", "📂 세부카테고리 $currentSelectedSubcategory 복원: ${filteredItems.size}개")
                adapter.updateWithApiData(filteredItems)
            }

            else -> {
                // 메인 카테고리만 선택된 경우
                val filteredItems = allItems.filter { it.category == currentSelectedCategory }
                Log.d("WardrobeFragment", "📁 메인카테고리 $currentSelectedCategory 복원: ${filteredItems.size}개")
                adapter.updateWithApiData(filteredItems)
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
        Log.d("WardrobeFragment", "아이템 개수 (더미 포함): ${items.size}")

        try {
            val totalCount = items.size
            val allButton = view?.findViewById<Button>(R.id.btnTopCategory1)
            allButton?.text = "전체 $totalCount"
            Log.d("WardrobeFragment", "전체 버튼 업데이트: 전체 $totalCount")

            // 🔥 FIXED: 원피스(3) -> 아우터(4) 순서로 수정, 더미 데이터 포함
            val categoryMapping = mapOf(
                1 to Pair(R.id.btnTopCategory2, "상의"),
                2 to Pair(R.id.btnTopCategory3, "하의"),
                3 to Pair(R.id.btnTopCategory4, "원피스"),
                4 to Pair(R.id.btnTopCategory5, "아우터"),
                5 to Pair(R.id.btnTopCategory6, "신발"),
                6 to Pair(R.id.btnTopCategory7, "액세서리") // 레이아웃 추가 후 활성화
            )

            categoryMapping.forEach { (categoryId, buttonInfo) ->
                val (buttonId, categoryName) = buttonInfo
                val button = view?.findViewById<Button>(buttonId)

                // 🔥 클라이언트에서 직접 개수 계산 (서버 + 더미 포함)
                val count = items.count { it.category == categoryId }

                button?.text = "$categoryName $count"
                Log.d("WardrobeFragment", "버튼 업데이트: $categoryName $count (더미 포함)")
            }

            Log.d("WardrobeFragment", "✅ 카테고리 버튼 업데이트 완료 (더미 포함)")
        } catch (e: Exception) {
            Log.e("WardrobeFragment", "Error updating category buttons", e)
        }
    }

    private fun updateSubCategories(subcategories: List<SubcategoryDto>) {
        Log.d("WardrobeFragment", "=== updateSubCategories 시작 ===")
        Log.d("WardrobeFragment", "받은 세부카테고리 개수: ${subcategories.size}")
        Log.d("WardrobeFragment", "현재 선택 상태 - category: $currentSelectedCategory, subcategory: $currentSelectedSubcategory")

        when {
            // 1. 서버에서 세부 카테고리 목록을 보내준 경우
            subcategories.isNotEmpty() -> {
                Log.d("WardrobeFragment", "✅ 서버에서 세부카테고리 제공")
                subcategories.forEach { subcategory ->
                    Log.d("WardrobeFragment", "📋 세부카테고리: ${subcategory.name} (ID: ${subcategory.subcategory})")
                }
                updateSubFiltersWithApiData(subcategories)
            }

            // 2. 상위 카테고리가 선택되었고 서버에서 세부카테고리를 안 보내준 경우
            currentSelectedCategory != null -> {
                Log.d("WardrobeFragment", "🔥 상위 카테고리 선택됨 - 클라이언트에서 세부카테고리 생성")
                createSubcategoriesFromClient(currentSelectedCategory!!)
            }

            // 3. 전체 카테고리인 경우
            else -> {
                Log.d("WardrobeFragment", "⭐ 전체 카테고리 - 기본 전체 버튼만 표시")
                createDefaultAllButton()
            }
        }
        Log.d("WardrobeFragment", "=== updateSubCategories 끝 ===")
    }

    // 🔥 FIXED: updateSubCategories 함수 완전히 수정
    private fun createSubcategoriesFromClient(categoryId: Int) {
        Log.d("WardrobeFragment", "=== createSubcategoriesFromClient 시작 ===")
        Log.d("WardrobeFragment", "🏭 categoryId=$categoryId")

        if (!isAdded || context == null) {
            Log.w("WardrobeFragment", "❌ Fragment가 attach되지 않았거나 context가 null")
            return
        }

        // 🔥 CRITICAL: 서버 + 더미 데이터 결합하여 사용
        val serverItems = viewModel.uiState.value.wardrobeItems
        val allItems = if (USE_WARDROBE_DUMMY) {
            combineServerAndDummyData(serverItems)
        } else {
            serverItems
        }

        Log.d("WardrobeFragment", "🔍 전체 아이템 개수 (서버+더미): ${allItems.size}")

        // 🔥 해당 카테고리의 아이템들 필터링
        val categoryItems = allItems.filter { item ->
            val matches = item.category == categoryId
            Log.d("WardrobeFragment", "🔍 아이템 ${item.id} 카테고리 비교: ${item.category} == $categoryId ? $matches")
            matches
        }

        Log.d("WardrobeFragment", "카테고리 $categoryId 보유 아이템 수: ${categoryItems.size}")
        categoryItems.forEach { item ->
            Log.d("WardrobeFragment", "  카테고리아이템: ID=${item.id}, cat=${item.category}, subcat=${item.subcategory}")
        }

        if (categoryItems.isEmpty()) {
            Log.d("WardrobeFragment", "❌ 해당 카테고리에 아이템 없음 - 모든 서브카테고리 표시")
            showAllSubcategoriesForCategory(categoryId)
            return
        }

        // 🔥 카테고리별 서브카테고리 정의
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
        Log.d("WardrobeFragment", "🔍 가능한 서브카테고리: ${allSubcategories.map { "${it.name}(${it.subcategory})" }}")

        // 🔥 보유한 서브카테고리만 표시
        val availableSubcategories = allSubcategories.filter { subcategoryDto ->
            val hasItem = itemSubcategories.contains(subcategoryDto.subcategory)
            Log.d("WardrobeFragment", "🔍 서브카테고리 ${subcategoryDto.name}(${subcategoryDto.subcategory}) 보유 여부: $hasItem")
            hasItem
        }

        if (availableSubcategories.isNotEmpty()) {
            Log.d("WardrobeFragment", "✅ 보유 서브카테고리로 필터 생성: ${availableSubcategories.map { it.name }}")
            updateSubFiltersWithDetailedData(availableSubcategories, categoryItems)
        } else {
            Log.d("WardrobeFragment", "❌ 매칭되는 서브카테고리 없음 - 모든 서브카테고리 표시")
            showAllSubcategoriesForCategory(categoryId)
        }

        Log.d("WardrobeFragment", "=== createSubcategoriesFromClient 끝 ===")
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

    // 🔥 WardrobeFragment의 updateSubFiltersWithApiData 함수를 수정하세요

    private fun updateSubFiltersWithApiData(subcategories: List<SubcategoryDto>) {
        if (!isAdded || context == null) return

        Log.d("WardrobeFragment", "=== updateSubFiltersWithApiData 시작 ===")
        Log.d("WardrobeFragment", "서브카테고리 개수: ${subcategories.size}")
        Log.d("WardrobeFragment", "현재 선택된 카테고리: $currentSelectedCategory")

        subFilterLayout.removeAllViews()
        selectedIndex = 0

        // 🔥 CRITICAL FIX: 최신 상태의 모든 아이템 가져오기
        val serverItems = viewModel.uiState.value.wardrobeItems
        Log.d("WardrobeFragment", "🔍 서버 아이템들:")
        serverItems.forEach { item ->
            Log.d("WardrobeFragment", "  서버아이템: ID=${item.id}, cat=${item.category}, subcat=${item.subcategory}")
        }

        val allItems = if (USE_WARDROBE_DUMMY) {
            combineServerAndDummyData(serverItems)
        } else {
            serverItems
        }

        Log.d("WardrobeFragment", "🔍 결합 후 전체 아이템들:")
        allItems.forEach { item ->
            Log.d("WardrobeFragment", "  전체아이템: ID=${item.id}, cat=${item.category}, subcat=${item.subcategory}")
        }

        // 🔥 CRITICAL FIX: 카테고리 필터링 시 더 상세한 로깅
        val categoryItems = if (currentSelectedCategory != null) {
            Log.d("WardrobeFragment", "🔍 카테고리 $currentSelectedCategory 필터링 시작...")
            val filtered = allItems.filter { item ->
                val matches = item.category == currentSelectedCategory
                Log.d("WardrobeFragment", "카테고리 필터: 아이템 ${item.id} (cat=${item.category}) == $currentSelectedCategory ? $matches")
                matches
            }
            Log.d("WardrobeFragment", "✅ 카테고리 $currentSelectedCategory 필터링 완료: ${filtered.size}개")

            if (filtered.isEmpty()) {
                Log.e("WardrobeFragment", "❌❌❌ 카테고리 필터링 결과가 비어있습니다!")
                Log.e("WardrobeFragment", "전체 아이템 중 카테고리 $currentSelectedCategory 가 없는지 확인:")
                allItems.forEach { item ->
                    Log.e("WardrobeFragment", "  체크: ID=${item.id}, category=${item.category}")
                }
            }

            filtered.forEach { item ->
                Log.d("WardrobeFragment", "  카테고리필터결과: ID=${item.id}, cat=${item.category}, subcat=${item.subcategory}")
            }
            filtered
        } else {
            Log.d("WardrobeFragment", "⭐ 전체 카테고리 아이템: ${allItems.size}개")
            allItems
        }

        // 🔥 카테고리 필터링 결과가 비어있으면 강제로 전체 아이템 사용
        val finalCategoryItems = if (categoryItems.isEmpty() && currentSelectedCategory != null) {
            Log.w("WardrobeFragment", "⚠️ 카테고리 필터링 결과가 비어있어서 전체 아이템 사용")
            allItems
        } else {
            categoryItems
        }

        Log.d("WardrobeFragment", "📊 최종 카테고리 아이템: ${finalCategoryItems.size}개")
        finalCategoryItems.forEach { item ->
            Log.d("WardrobeFragment", "  최종카테고리아이템: ID=${item.id}, cat=${item.category}, subcat=${item.subcategory}")
        }

        // 🔥 '전체' 버튼 생성
        val allButton = createFilterButton("전체", 0, subcategories.size + 1)
        allButton.setOnClickListener {
            if (isAdded && context != null) {
                Log.d("WardrobeFragment", "🔵 '전체' 서브카테고리 버튼 클릭")
                updateButtonSelection(0)
                moveUnderline(0)
                currentSelectedSubcategory = null

                Log.d("WardrobeFragment", "전체 서브카테고리 선택 - ${finalCategoryItems.size}개 아이템 표시")
                finalCategoryItems.forEach { item ->
                    Log.d("WardrobeFragment", "  전체표시: ID=${item.id}, cat=${item.category}, subcat=${item.subcategory}")
                }
                adapter.updateWithApiData(finalCategoryItems)
            }
        }
        subFilterLayout.addView(allButton)

        // 🔥 각 서브카테고리 버튼 생성
        subcategories.forEachIndexed { index, subcategoryDto ->
            val button = createFilterButton(subcategoryDto.name, index + 1, subcategories.size + 1)
            button.setOnClickListener {
                if (isAdded && context != null) {
                    Log.d("WardrobeFragment", "🔵 서브카테고리 '${subcategoryDto.name}' 버튼 클릭")
                    Log.d("WardrobeFragment", "  선택된 서브카테고리 ID: ${subcategoryDto.subcategory}")

                    updateButtonSelection(index + 1)
                    moveUnderline(index + 1)
                    currentSelectedSubcategory = subcategoryDto.subcategory

                    // 🔥 서브카테고리 필터링 전에 상태 확인
                    Log.d("WardrobeFragment", "🔍 서브카테고리 필터링 대상: ${finalCategoryItems.size}개")
                    finalCategoryItems.forEach { item ->
                        Log.d("WardrobeFragment", "  필터링대상: ID=${item.id}, subcat=${item.subcategory}")
                    }

                    val filteredItems = finalCategoryItems.filter { item ->
                        val matches = item.subcategory == subcategoryDto.subcategory
                        Log.d("WardrobeFragment", "서브카테고리 필터: 아이템 ${item.id} (subcat=${item.subcategory}) == ${subcategoryDto.subcategory} ? $matches")
                        matches
                    }

                    Log.d("WardrobeFragment", "✅ 서브카테고리 ${subcategoryDto.name} 필터링 결과: ${filteredItems.size}개")
                    filteredItems.forEach { item ->
                        Log.d("WardrobeFragment", "  필터결과: ID=${item.id}, cat=${item.category}, subcat=${item.subcategory}")
                    }

                    if (filteredItems.isEmpty()) {
                        Log.w("WardrobeFragment", "⚠️ 서브카테고리 ${subcategoryDto.name}에 해당하는 아이템이 없습니다")
                        Log.w("WardrobeFragment", "📋 현재 카테고리의 모든 서브카테고리:")
                        finalCategoryItems.forEach { item ->
                            Log.w("WardrobeFragment", "  가능한아이템: ID=${item.id}, subcat=${item.subcategory}")
                        }
                    }

                    adapter.updateWithApiData(filteredItems)
                }
            }
            subFilterLayout.addView(button)
        }

        // 🔥 초기 선택: 전체
        updateButtonSelection(0)
        Log.d("WardrobeFragment", "🚀 초기 선택: 전체 서브카테고리 - ${finalCategoryItems.size}개 아이템 표시")
        adapter.updateWithApiData(finalCategoryItems)

        subFilterLayout.post {
            if (isAdded && view != null) {
                moveUnderline(0)
            }
        }

        Log.d("WardrobeFragment", "=== updateSubFiltersWithApiData 완료 ===")
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
    private fun updateSubFiltersWithDetailedData(subcategories: List<SubcategoryDto>, categoryItems: List<WardrobeItemDto>) {
        if (!isAdded || context == null) return

        Log.d("WardrobeFragment", "=== updateSubFiltersWithDetailedData 시작 ===")
        Log.d("WardrobeFragment", "서브카테고리: ${subcategories.map { it.name }}")
        Log.d("WardrobeFragment", "카테고리 아이템: ${categoryItems.size}개")

        subFilterLayout.removeAllViews()
        selectedIndex = 0

        // 🔥 '전체' 버튼 생성
        val allButton = createFilterButton("전체", 0, subcategories.size + 1)
        allButton.setOnClickListener {
            if (isAdded && context != null) {
                Log.d("WardrobeFragment", "🔵 상세 '전체' 서브카테고리 버튼 클릭")
                updateButtonSelection(0)
                moveUnderline(0)
                currentSelectedSubcategory = null

                Log.d("WardrobeFragment", "상세 전체 서브카테고리 선택: ${categoryItems.size}개 아이템 표시")
                adapter.updateWithApiData(categoryItems)
            }
        }
        subFilterLayout.addView(allButton)

        // 🔥 각 서브카테고리 버튼 생성
        subcategories.forEachIndexed { index, subcategoryDto ->
            val button = createFilterButton(subcategoryDto.name, index + 1, subcategories.size + 1)
            button.setOnClickListener {
                if (isAdded && context != null) {
                    Log.d("WardrobeFragment", "🔵 상세 서브카테고리 '${subcategoryDto.name}' 버튼 클릭")

                    updateButtonSelection(index + 1)
                    moveUnderline(index + 1)
                    currentSelectedSubcategory = subcategoryDto.subcategory

                    val filteredItems = categoryItems.filter { item ->
                        val matches = item.subcategory == subcategoryDto.subcategory
                        Log.d("WardrobeFragment", "상세 서브카테고리 필터: 아이템 ${item.id} (subcat=${item.subcategory}) == ${subcategoryDto.subcategory} ? $matches")
                        matches
                    }

                    Log.d("WardrobeFragment", "✅ 상세 서브카테고리 ${subcategoryDto.name} 선택: ${filteredItems.size}개 아이템 표시")
                    adapter.updateWithApiData(filteredItems)
                }
            }
            subFilterLayout.addView(button)
        }

        // 🔥 초기 선택: 전체
        updateButtonSelection(0)
        Log.d("WardrobeFragment", "🚀 상세 초기 선택: ${categoryItems.size}개 아이템 표시")
        adapter.updateWithApiData(categoryItems)

        subFilterLayout.post {
            if (isAdded && view != null) {
                moveUnderline(0)
            }
        }

        Log.d("WardrobeFragment", "=== updateSubFiltersWithDetailedData 완료 ===")
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

    // 🔥 WardrobeFragment의 setupTopCategoryButtons 함수를 완전히 교체하세요

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
                Log.d("WardrobeFragment", "🎯 카테고리 버튼 클릭: $categoryName (ID: $categoryId)")

                selectedTopCategoryButton?.isSelected = false
                button.isSelected = true
                selectedTopCategoryButton = button

                setSearchIconColor(false)
                isFilterApplied = false

                currentSelectedCategory = categoryId
                currentSelectedSubcategory = null
                lastSubcategoriesSize = -1

                // 🔥 CRITICAL: 현재 상태의 최신 아이템들 가져오기
                val serverItems = viewModel.uiState.value.wardrobeItems
                val allItems = if (USE_WARDROBE_DUMMY) {
                    combineServerAndDummyData(serverItems)
                } else {
                    serverItems
                }

                Log.d("WardrobeFragment", "📦 카테고리 클릭 시 전체 아이템: ${allItems.size}개")
                allItems.forEach { item ->
                    Log.d("WardrobeFragment", "  전체아이템: ID=${item.id}, category=${item.category}, subcategory=${item.subcategory}")
                }

                if (categoryName == "전체") {
                    Log.d("WardrobeFragment", "🌐 전체 카테고리 선택 - 모든 아이템 표시")

                    adapter.updateWithApiData(allItems)
                    Log.d("WardrobeFragment", "✅ 전체 아이템 어댑터 업데이트 완료: ${allItems.size}개")

                    createDefaultAllButton()
                    // viewModel.loadAllWardrobeItems() // 🔥 중복 호출 제거
                } else {
                    Log.d("WardrobeFragment", "📂 카테고리 '$categoryName' (ID: $categoryId) 선택")

                    // 🔥 CRITICAL: 해당 카테고리 아이템만 필터링
                    val categoryItems = allItems.filter { item ->
                        val matches = item.category == categoryId
                        Log.d("WardrobeFragment", "🔍 카테고리 필터: 아이템 ${item.id} (cat=${item.category}) == $categoryId ? $matches")
                        matches
                    }

                    Log.d("WardrobeFragment", "✅ '$categoryName' 카테고리 필터링 결과: ${categoryItems.size}개")
                    categoryItems.forEach { item ->
                        Log.d("WardrobeFragment", "  ✓ 포함: ID=${item.id}, category=${item.category}, subcategory=${item.subcategory}, brand='${item.brand}'")
                    }

                    // 🔥 CRITICAL: 필터링된 아이템 즉시 표시
                    adapter.updateWithApiData(categoryItems)
                    Log.d("WardrobeFragment", "✅ 카테고리 아이템 어댑터 업데이트 완료: ${categoryItems.size}개")

                    // 🔥 CRITICAL: 서브카테고리 생성 시 필터링된 아이템들 전달
                    if (categoryItems.isNotEmpty()) {
                        Log.d("WardrobeFragment", "🚀 서브카테고리 생성 시작 - ${categoryItems.size}개 아이템으로")
                        createSubcategoriesFromClientWithItems(categoryId!!, categoryItems, allItems)
                    } else {
                        Log.w("WardrobeFragment", "⚠️ 해당 카테고리에 아이템 없음 - 기본 서브카테고리 표시")
                        showAllSubcategoriesForCategory(categoryId!!)
                    }

                    // 🔥 서버에서 해당 카테고리 데이터 다시 로드 (백그라운드)
                    viewModel.loadWardrobeItemsByCategory(category = categoryId)
                }
            }

            // 🔥 초기 선택 상태 설정
            if (categoryName == "전체") {
                button.isSelected = true
                selectedTopCategoryButton = button
                currentSelectedCategory = null
                currentSelectedSubcategory = null
            }
        }
    }

    // 🔥 NEW: 카테고리 아이템들을 직접 전달받는 서브카테고리 생성 함수
    private fun createSubcategoriesFromClientWithItems(categoryId: Int, categoryItems: List<WardrobeItemDto>, allItems: List<WardrobeItemDto>) {
        Log.d("WardrobeFragment", "=== createSubcategoriesFromClientWithItems 시작 ===")
        Log.d("WardrobeFragment", "🏭 categoryId=$categoryId, 카테고리아이템=${categoryItems.size}개")

        if (!isAdded || context == null) {
            Log.w("WardrobeFragment", "❌ Fragment가 attach되지 않았거나 context가 null")
            return
        }

        Log.d("WardrobeFragment", "📋 전달받은 카테고리 아이템들:")
        categoryItems.forEach { item ->
            Log.d("WardrobeFragment", "  카테고리아이템: ID=${item.id}, cat=${item.category}, subcat=${item.subcategory}")
        }

        if (categoryItems.isEmpty()) {
            Log.d("WardrobeFragment", "❌ 카테고리 아이템이 비어있음 - 모든 서브카테고리 표시")
            showAllSubcategoriesForCategory(categoryId)
            return
        }

        // 🔥 카테고리별 서브카테고리 정의
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

        Log.d("WardrobeFragment", "🔍 실제 카테고리 아이템들의 서브카테고리 ID: $itemSubcategories")
        Log.d("WardrobeFragment", "🔍 가능한 서브카테고리: ${allSubcategories.map { "${it.name}(${it.subcategory})" }}")

        // 🔥 보유한 서브카테고리만 표시
        val availableSubcategories = allSubcategories.filter { subcategoryDto ->
            val hasItem = itemSubcategories.contains(subcategoryDto.subcategory)
            Log.d("WardrobeFragment", "🔍 서브카테고리 ${subcategoryDto.name}(${subcategoryDto.subcategory}) 보유 여부: $hasItem")
            hasItem
        }

        if (availableSubcategories.isNotEmpty()) {
            Log.d("WardrobeFragment", "✅ 보유 서브카테고리로 필터 생성: ${availableSubcategories.map { it.name }}")
            updateSubFiltersWithDetailedData(availableSubcategories, categoryItems)
        } else {
            Log.d("WardrobeFragment", "❌ 매칭되는 서브카테고리 없음 - 모든 서브카테고리 표시")
            showAllSubcategoriesForCategory(categoryId)
        }

        Log.d("WardrobeFragment", "=== createSubcategoriesFromClientWithItems 끝 ===")
    }

    // 🔥 NEW: 더미 데이터를 포함한 서브카테고리 생성
    private fun createSubcategoriesFromClientWithDummy(categoryId: Int, allItems: List<WardrobeItemDto>) {
        Log.d("WardrobeFragment", "🏭 createSubcategoriesFromClientWithDummy 시작: categoryId=$categoryId")

        if (!isAdded || context == null) {
            Log.w("WardrobeFragment", "❌ Fragment가 attach되지 않았거나 context가 null")
            return
        }

        // 🔥 FIXED: 서버 + 더미 데이터 모두 사용
        Log.d("WardrobeFragment", "🔍 전체 아이템 개수 (서버+더미): ${allItems.size}")

        allItems.forEach { item ->
            Log.d("WardrobeFragment", "📱 아이템 ID: ${item.id}, 카테고리: ${item.category}, 서브카테고리: ${item.subcategory}")
        }

        // 🔥 정상적인 카테고리 정보가 있는 경우 기존 로직 사용
        val categoryItems = allItems.filter { item ->
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
     * 🔥 수정된 더미 데이터 생성 함수 (태그 번호 방식)
     */
    private fun loadDummyWardrobeFromAssets(): List<WardrobeItemDto> {
        if (!USE_WARDROBE_DUMMY) return emptyList()

        try {
            Log.d("WardrobeFragment", "🎭 하드코딩된 더미 옷장 아이템 생성 시작")

            val hardcodedItems = listOf(
                // 새로 추가한 더미데이터
                HardcodedWardrobeItem(
                    imageName = "shirts5",
                    category = 1, subcategory = 4,
                    brand = "H&M", season = 2, color = 1,
                    tag1 = 1, tag2 = 10, tag3 = null,
                    purchasePlace = "H&M 온라인",
                    purchasePrice = "69,800원",
                    purchaseDate = "2025-07-10"
                ),
                HardcodedWardrobeItem(
                    imageName = "pants5",
                    category = 2, subcategory = 11,
                    brand = "무신사", season = 2, color = 1,
                    tag1 = 2, tag2 = 8, tag3 = null,
                    purchasePlace = "무신사 온라인",
                    purchasePrice = "39,900원",
                    purchaseDate = "2025-06-12"
                ),
                HardcodedWardrobeItem(
                    imageName = "shoes5",
                    category = 5, subcategory = 32,
                    brand = "무지", season = 2, color = 1,
                    tag1 = 1, tag2 = 8, tag3 = null, // 캐주얼, 데일리
                    purchasePlace = "무지 온라인",
                    purchasePrice = "29,900원",
                    purchaseDate = "2024-07-10"
                ),
                HardcodedWardrobeItem(
                    imageName = "acc5",
                    category = 6, subcategory = 41,
                    brand = "아디다스", season = 2, color = 1,
                    tag1 = 1, tag2 = 10, tag3 = null, // 캐주얼, 데일리
                    purchasePlace = "아디다스 온라인",
                    purchasePrice = "86,900원",
                    purchaseDate = "2025-08-12"
                ),
                // 새로 추가한 더미데이터
                HardcodedWardrobeItem(
                    imageName = "shirts6",
                    category = 1, subcategory = 4,
                    brand = "무지", season = 2, color = 1,
                    tag1 = 1, tag2 = 10, tag3 = null, // 캐주얼, 데일리
                    purchasePlace = "무지 온라인",
                    purchasePrice = "69,900원",
                    purchaseDate = "2025-07-10"
                ),
                HardcodedWardrobeItem(
                    imageName = "pants6",
                    category = 2, subcategory = 10,
                    brand = "무신사", season = 2, color = 2,
                    tag1 = 3, tag2 = 11, tag3 = null, // 미니멀, 출근룩
                    purchasePlace = "무신사",
                    purchasePrice = "49,900원",
                    purchaseDate = "2024-03-08"
                ),
                HardcodedWardrobeItem(
                    imageName = "shoes6",
                    category = 5, subcategory = 34,
                    brand = "무지", season = 1, color = 1,
                    tag1 = 11, tag2 = 16, tag3 = null, // 출근룩, 하객룩
                    purchasePlace = "무지 온라인",
                    purchasePrice = "29,900원",
                    purchaseDate = "2024-07-10"
                ),
                HardcodedWardrobeItem(
                    imageName = "acc6",
                    category = 6, subcategory = 43,
                    brand = "H&M", season = 2, color = 1,
                    tag1 = 4, tag2 = null, tag3 = null, // 클래식
                    purchasePlace = "H&M",
                    purchasePrice = "39,900원",
                    purchaseDate = "2024-07-12"
                ),
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
                    tag1 = 1, tag2 = 4, tag3 = null, // 예: 미니멀, 데일리 (2개만)
                    purchasePlace = "유니클로 온라인",
                    purchasePrice = "29,900원",
                    purchaseDate = "2024-02-20"
                ),
                HardcodedWardrobeItem(
                    imageName = "shoes1",
                    category = 5, subcategory = 29,
                    brand = "나이키", season = 2, color = 6,
                    tag1 = 2, tag2 = 4, tag3 = null, // 예: 스포티, 액티브, 편안함
                    purchasePlace = "나이키 공식몰",
                    purchasePrice = "139,000원",
                    purchaseDate = "2024-01-10"
                ),
                HardcodedWardrobeItem(
                    imageName = "shirts2",
                    category = 1, subcategory = 1,
                    brand = "자라", season = 2, color = 1,
                    tag1 = 10, tag2 = null, tag3 = null, // 예: 미니멀, 심플
                    purchasePlace = "자라 홍대점",
                    purchasePrice = "19,900원",
                    purchaseDate = "2024-06-05"
                ),
                HardcodedWardrobeItem(
                    imageName = "pants2",
                    category = 2, subcategory = 9,
                    brand = "리바이스", season = 2, color = 6,
                    tag1 = null, tag2 = null, tag3 = null, // 예: 빈티지, 캐주얼, 데님
                    purchasePlace = "리바이스 매장",
                    purchasePrice = "89,000원",
                    purchaseDate = "2024-05-12"
                ),
                HardcodedWardrobeItem(
                    imageName = "shoes2",
                    category = 4, subcategory = 29,
                    brand = "아디다스", season = 1, color = 1,
                    tag1 = 2, tag2 = 13, tag3 = null, // 예: 스포티, 스트릿
                    purchasePlace = "아디다스 온라인",
                    purchasePrice = "119,000원",
                    purchaseDate = "2024-04-08"
                ),
                HardcodedWardrobeItem(
                    imageName = "shirts3",
                    category = 1, subcategory = 4,
                    brand = "H&M", season = 2, color = 1,
                    tag1 = 3, tag2 = 11, tag3 = null, // 예: 미니멀, 모던, 심플
                    purchasePlace = "H&M 명동점",
                    purchasePrice = "24,900원",
                    purchaseDate = "2024-07-01"
                ),
                HardcodedWardrobeItem(
                    imageName = "shoes3",
                    category = 5, subcategory = 29,
                    brand = "닥터마틴", season = 1, color = 2,
                    tag1 = 3, tag2 = 17, tag3 = null, // 예: 록, 개성
                    purchasePlace = "닥터마틴 강남점",
                    purchasePrice = "259,000원",
                    purchaseDate = "2024-03-20"
                ),
                HardcodedWardrobeItem(
                    imageName = "pants3",
                    category = 2, subcategory = 10,
                    brand = "MCM", season = 1, color = 1,
                    tag1 = 9, tag2 = 11, tag3 = null, // 예: 럭셔리, 출근룩
                    purchasePlace = "MCM 백화점",
                    purchasePrice = "189,000원",
                    purchaseDate = "2024-02-14"
                ),
                HardcodedWardrobeItem(
                    imageName = "acc3",
                    category = 6, subcategory = 40,
                    brand = "무지", season = 2, color = 1,
                    tag1 = 9, tag2 = null, tag3 = null, // 예: 액세서리, 여름
                    purchasePlace = "무지 매장",
                    purchasePrice = "39,000원",
                    purchaseDate = "2024-06-20"
                ),
                HardcodedWardrobeItem(
                    imageName = "shirts4",
                    category = 1, subcategory = 4,
                    brand = "유니클로", season = 2, color = 3,
                    tag1 = 2, tag2 = 11, tag3 = null, // 예: 미니멀, 베이직, 심플
                    purchasePlace = "유니클로 홍대점",
                    purchasePrice = "29,900원",
                    purchaseDate = "2024-06-15"
                ),
                HardcodedWardrobeItem(
                    imageName = "pants4",
                    category = 2, subcategory = 10,
                    brand = "자라", season = 1, color = 1,
                    tag1 = 4, tag2 = 15, tag3 = null, // 예: 페미닌, 로맨틱
                    purchasePlace = "자라 온라인",
                    purchasePrice = "39,900원",
                    purchaseDate = "2024-04-25"
                ),
                HardcodedWardrobeItem(
                    imageName = "bag4",
                    category = 6, subcategory = 41,
                    brand = "무지", season = 2, color = 1,
                    tag1 = 4, tag2 = 11, tag3 = null, // 예: 미니멀, 데일리
                    purchasePlace = "무지 매장",
                    purchasePrice = "49,000원",
                    purchaseDate = "2024-05-30"
                ),
                HardcodedWardrobeItem(
                    imageName = "shoes4",
                    category = 5, subcategory = 31,
                    brand = "무지", season = 2, color = 1,
                    tag1 = 13, tag2 = null, tag3 = null, // 예: 여름, 편안함
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

            Log.d("WardrobeFragment", "✅ 하드코딩된 더미 옷장 아이템 ${dummyItems.size}개 생성")

            return dummyItems

        } catch (e: Exception) {
            Log.e("WardrobeFragment", "하드코딩된 더미 데이터 생성 실패", e)
            return emptyList()
        }
    }

    /**
     * 🔥 태그 번호를 태그 이름으로 변환하는 함수
     */
    fun getTagNameById(tagId: Int): String {
        return when (tagId) {
            1 -> "캐주얼"
            2 -> "스트릿"
            3 -> "미니멀"
            4 -> "클래식"
            5 -> "빈티지"
            6 -> "러블리"
            7 -> "페미닌"
            8 -> "보이시"
            9 -> "모던"
            10 -> "데일리"
            11 -> "출근룩"
            12 -> "데이트룩"
            13 -> "나들이룩"
            14 -> "운동복"
            15 -> "하객룩"
            16 -> "파티룩"
            17 -> "여행룩"
            else -> "기타"
        }
    }

    /**
     * 🔥 NEW: 하드코딩된 옷장 아이템 데이터 클래스
     */
    data class HardcodedWardrobeItem(
        val imageName: String,
        val category: Int,
        val subcategory: Int,
        val brand: String,
        val season: Int,
        val color: Int,
        val tag1: Int?, // 🔥 첫 번째 태그 ID (null 가능)
        val tag2: Int?, // 🔥 두 번째 태그 ID (null 가능)
        val tag3: Int?, // 🔥 세 번째 태그 ID (null 가능)
        val purchasePlace: String, // 구매처
        val purchasePrice: String, // 구매 가격
        val purchaseDate: String // 구매 날짜
    )

    /**
     * 🔥 handleUiState에서 더미 데이터와 서버 데이터 결합
     */
    private fun combineServerAndDummyData(serverItems: List<WardrobeItemDto>): List<WardrobeItemDto> {
        Log.d("WardrobeFragment", "🔗 데이터 결합 시작")
        Log.d("WardrobeFragment", "📦 서버 아이템: ${serverItems.size}개")
        serverItems.forEach { item ->
            Log.d("WardrobeFragment", "  서버: ID=${item.id}, cat=${item.category}, subcat=${item.subcategory}")
        }

        val dummyItems = loadDummyWardrobeFromAssets()
        Log.d("WardrobeFragment", "🎭 더미 아이템: ${dummyItems.size}개")

        val combined = serverItems + dummyItems
        Log.d("WardrobeFragment", "✅ 데이터 결합 완료: 서버 ${serverItems.size}개 + 더미 ${dummyItems.size}개 = 총 ${combined.size}개")

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