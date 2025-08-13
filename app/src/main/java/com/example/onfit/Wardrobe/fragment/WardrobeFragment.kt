package com.example.onfit.Wardrobe.fragment

import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.Gravity
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.RelativeLayout
import android.widget.Toast
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import com.example.onfit.KakaoLogin.util.TokenProvider
import com.example.onfit.R
import com.example.onfit.RegisterItemBottomSheet
import com.example.onfit.Wardrobe.adapter.WardrobeAdapter
import com.example.onfit.Wardrobe.Network.RetrofitClient
import com.example.onfit.Wardrobe.Network.WardrobeItemDto
import com.example.onfit.Wardrobe.Network.CategoryDto
import com.example.onfit.Wardrobe.Network.RegisterItemRequestDto
import com.example.onfit.Wardrobe.Network.SubcategoryDto

open class WardrobeFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: WardrobeAdapter
    private lateinit var subFilterLayout: LinearLayout
    private lateinit var subFilterScrollView: HorizontalScrollView

    private var wardrobeItems = listOf<WardrobeItemDto>()
    private var categories = listOf<CategoryDto>()

    private val allImageList = listOf(
        R.drawable.clothes1,
        R.drawable.clothes2,
        R.drawable.clothes3,
        R.drawable.clothes4,
        R.drawable.clothes5,
        R.drawable.clothes6,
        R.drawable.clothes7,
        R.drawable.clothes8
    )

    private val categoryFilters = mapOf(
        "전체" to listOf("전체"),
        "상의" to listOf("전체", "반팔티", "긴팔티", "셔츠", "블라우스", "니트", "후드티", "탱크톱", "나시티"),
        "하의" to listOf("전체", "청바지", "반바지", "슬랙스", "치마", "레깅스", "조거팬츠"),
        "아우터" to listOf("전체", "자켓", "패딩", "코트", "바람막이", "가디건", "점퍼", "블레이저", "후드집업"),
        "원피스" to listOf("전체", "미니", "미디", "롱", "니트", "셔츠"),
        "신발" to listOf("전체", "운동화","구두", "부츠", "샌들", "슬리퍼","하이힐", "플랫슈즈", "워커")
    )

    private var selectedIndex = 0
    private var selectedTopCategoryButton: Button? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_wardrobe, container, false)
        initializeViews(view)
        setupRecyclerView()
        setupTopCategoryButtons(view)
        loadWardrobeData()
        updateSubFilters("전체")
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 아이템 수정 결과 받기
        parentFragmentManager.setFragmentResultListener("search_results", this) { _, bundle ->
            val filteredIds = bundle.getIntArray("filtered_item_ids")
            if (filteredIds != null) {
                val filteredItems = wardrobeItems.filter { it.id in filteredIds }

                // 🔥 추가 로컬 검증 - 실제 데이터와 필터 조건 비교
                val season = bundle.getString("filter_season")
                val finalItems = if (!season.isNullOrEmpty()) {
                    val seasonId = when (season) {
                        "봄ㆍ가을" -> 1
                        "여름" -> 2
                        "겨울" -> 4
                        else -> null
                    }

                    if (seasonId != null) {
                        filteredItems.filter { it.season == seasonId }
                    } else {
                        filteredItems
                    }
                } else {
                    filteredItems
                }

                adapter.updateWithApiData(finalItems)
                Toast.makeText(context, "${finalItems.size}개 아이템 검색됨", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun applyLocalFiltering(bundle: Bundle) {
        var filteredItems = wardrobeItems

        bundle.getString("filter_season")?.let { season ->
            if (season.isNotEmpty()) {
                val seasonId = when (season) {
                    "봄ㆍ가을" -> 1
                    "여름" -> 2
                    "겨울" -> 4
                    else -> null
                }
                seasonId?.let { filteredItems = filteredItems.filter { it.season == seasonId } }
            }
        }

        bundle.getString("filter_brand")?.let { brand ->
            if (brand.isNotEmpty()) {
                filteredItems = filteredItems.filter {
                    it.brand.contains(brand, ignoreCase = true)
                }
            }
        }

        adapter.updateWithApiData(filteredItems)
    }


    private fun testBasicSetup() {
        Log.d("WardrobeFragment", "기본 설정 테스트 시작")

        lifecycleScope.launch {
            try {
                Log.d("WardrobeFragment", "RetrofitClient 접근 테스트")
                val service = RetrofitClient.wardrobeService
                Log.d("WardrobeFragment", "Service 생성 성공: $service")

            } catch (e: Exception) {
                Log.e("WardrobeFragment", "기본 설정 에러: ${e.message}", e)
            }
        }
    }

    private fun initializeViews(view: View) {
        recyclerView = view.findViewById(R.id.wardrobeRecyclerView)
        subFilterLayout = view.findViewById(R.id.subFilterLayout)
        subFilterScrollView = view.findViewById(R.id.subFilterScrollView)

        // + 버튼 설정 추가
        setupAddButton(view)

        // 검색 버튼도 설정
        setupSearchButton(view)
    }

    private fun setupAddButton(view: View) {
        val addButton = view.findViewById<ImageButton>(R.id.wardrobe_register_btn)
        addButton?.setOnClickListener {
            // BottomSheet를 직접 호출 (Navigation 사용 안 함)
            val bottomSheet = RegisterItemBottomSheet()
            bottomSheet.show(parentFragmentManager, "RegisterItemBottomSheet")
        }
    }

    private fun setupSearchButton(view: View) {
        val searchButton = view.findViewById<ImageButton>(R.id.ic_search)
        searchButton?.setOnClickListener {
            try {
                Log.d("WardrobeFragment", "검색 버튼 클릭 - Navigation 시도")
                findNavController().navigate(R.id.wardrobeSearchFragment)
            } catch (e: Exception) {
                Log.e("WardrobeFragment", "Navigation 실패: ${e.message}")
                Toast.makeText(context, "검색 화면으로 이동할 수 없습니다", Toast.LENGTH_SHORT).show()

                // 임시로 다이얼로그 검색 사용
                showSearchDialog()
            }
        }
    }

    private fun showSearchDialog() {
        // 간단한 검색 다이얼로그
        val builder = AlertDialog.Builder(requireContext())
        val input = EditText(requireContext())
        input.hint = "브랜드, 색상, 계절로 검색"

        builder.setTitle("아이템 검색")
            .setView(input)
            .setPositiveButton("검색") { _, _ ->
                val query = input.text.toString().trim()
                if (query.isNotEmpty()) {
                    performSearch(query)
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun performSearch(query: String) {
        lifecycleScope.launch {
            try {
                val token = "Bearer " + TokenProvider.getToken(requireContext())

                // 브랜드로 검색 (API가 아직 구현되지 않았다면 임시로 로컬 필터링)
                val filteredItems = wardrobeItems.filter { item ->
                    item.id.toString().contains(query, ignoreCase = true)  // 임시로 ID로 검색
                }

                if (filteredItems.isNotEmpty()) {
                    adapter.updateWithApiData(filteredItems)
                    Toast.makeText(context, "${filteredItems.size}개의 아이템을 찾았습니다", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "검색 결과가 없습니다", Toast.LENGTH_SHORT).show()
                    // 검색 결과가 없으면 전체 데이터 다시 표시
                    adapter.updateWithApiData(wardrobeItems)
                }

            } catch (e: Exception) {
                showError("검색 실패: ${e.message}")
            }
        }
    }

    private fun loadBrandsList() {
        lifecycleScope.launch {
            try {
                val token = "Bearer " + TokenProvider.getToken(requireContext())
                // 브랜드 목록 API가 구현되면 사용
                // val response = RetrofitClient.wardrobeService.getBrandsList(token)

                // 임시로 현재 옷장에서 브랜드 추출
                val brands = wardrobeItems.map { it.brand }.distinct()
                showBrandsDialog(brands)

            } catch (e: Exception) {
                Log.e("WardrobeFragment", "브랜드 목록 로드 실패", e)
            }
        }
    }

    private fun showBrandsDialog(brands: List<String>) {
        if (brands.isEmpty()) {
            Toast.makeText(context, "등록된 브랜드가 없습니다", Toast.LENGTH_SHORT).show()
            return
        }

        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("브랜드 선택")
            .setItems(brands.toTypedArray()) { _, which ->
                val selectedBrand = brands[which]
                performSearch(selectedBrand)
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun performAdvancedFilter(
        season: Int? = null,
        color: Int? = null,
        brand: String? = null,
        tagIds: List<Int>? = null
    ) {
        lifecycleScope.launch {
            try {
                val token = "Bearer " + TokenProvider.getToken(requireContext())
                val tagIdsString = tagIds?.joinToString(",")

                // 필터 API가 구현되면 사용
                // val response = RetrofitClient.wardrobeService.filterWardrobeItems(...)

                // 임시로 로컬 필터링
                var filteredItems = wardrobeItems

                brand?.let { brandQuery ->
                    filteredItems = filteredItems.filter {
                        it.brand.contains(brandQuery, ignoreCase = true)
                    }
                }

                season?.let { seasonId ->
                    filteredItems = filteredItems.filter { it.season == seasonId }
                }

                color?.let { colorId ->
                    filteredItems = filteredItems.filter { it.color == colorId }
                }

                adapter.updateWithApiData(filteredItems)

            } catch (e: Exception) {
                showError("필터링 실패: ${e.message}")
            }
        }
    }

    private fun setupRecyclerView() {
        // 빈 리스트로 시작 - API 데이터로만 채움
        adapter = WardrobeAdapter(
            itemList = emptyList<Any>(),
            onItemClick = { item: Any ->
                when (item) {
                    is WardrobeItemDto -> navigateToClothesDetailWithId(item.id)
                    is Int -> navigateToClothesDetail(item) // 혹시 더미 데이터용
                }
            }
        )
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        recyclerView.adapter = adapter
    }

    /**
     * 옷장 데이터 API 호출
     */
    /**
     * 옷장 데이터 API 호출
     */
    private fun loadWardrobeData() {
        lifecycleScope.launch {
            try {
                val token = "Bearer " + TokenProvider.getToken(requireContext())
                Log.d("WardrobeFragment", "사용할 토큰: $token")

                val response = RetrofitClient.wardrobeService.getAllWardrobeItems(token)

                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    if (apiResponse?.isSuccess == true && apiResponse.result != null) {
                        // API 데이터 저장
                        wardrobeItems = apiResponse.result.items
                        categories = apiResponse.result.categories

                        // UI 업데이트 (카테고리 개수 포함)
                        updateUIWithApiData()

                        // 🔥 전체 개수 즉시 업데이트
                        val totalCount = wardrobeItems.size
                        view?.findViewById<Button>(R.id.btnTopCategory1)?.text = "전체 $totalCount"

                        Toast.makeText(context, "옷장 데이터를 불러왔습니다.", Toast.LENGTH_SHORT).show()
                    } else {
                        showError("데이터를 불러오는데 실패했습니다: ${apiResponse?.message}")
                    }
                } else {
                    showError("서버 오류: ${response.code()}")
                }

            } catch (e: Exception) {
                showError("네트워크 오류: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    /**
     * 카테고리별 데이터 로드
     */
    private fun loadWardrobeDataByCategory(category: Int? = null, subcategory: Int? = null) {
        lifecycleScope.launch {
            try {
                val token = "Bearer " + TokenProvider.getToken(requireContext())
                val response = RetrofitClient.wardrobeService.getWardrobeItemsByCategory(
                    token = token,
                    category = category,
                    subcategory = subcategory
                )
                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    if (apiResponse?.isSuccess == true && apiResponse.result != null) {
                        wardrobeItems = apiResponse.result.items
                        categories = apiResponse.result.categories
                        updateSubCategories(apiResponse.result.subcategories)
                        updateUIWithApiData()

                        // 🔥 전체 버튼 개수 항상 유지
                        maintainTotalCount()

                        apiResponse.result.appliedFilter?.let { filter ->
                            Log.d("WardrobeFragment",
                                "Applied filter - Category: ${filter.categoryName}, Subcategory: ${filter.subcategoryName}")
                        }
                    } else {
                        showError("데이터를 불러오는데 실패했습니다: ${apiResponse?.message}")
                    }
                } else {
                    showError("서버 오류: ${response.code()}")
                }
            } catch (e: Exception) {
                showError("네트워크 오류: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    /**
     * 전체 개수를 항상 유지하는 함수
     */
    private fun maintainTotalCount() {
        lifecycleScope.launch {
            try {
                val token = "Bearer " + TokenProvider.getToken(requireContext())
                val allResponse = RetrofitClient.wardrobeService.getAllWardrobeItems(token)

                if (allResponse.isSuccessful && allResponse.body()?.isSuccess == true) {
                    val totalCount = allResponse.body()?.result?.items?.size ?: 0

                    // UI 스레드에서 업데이트
                    view?.post {
                        if (isAdded && context != null) {
                            val btnAll = view?.findViewById<Button>(R.id.btnTopCategory1)
                            btnAll?.text = "전체 $totalCount"
                            Log.d("WardrobeFragment", "전체 개수 업데이트: $totalCount")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("WardrobeFragment", "전체 개수 업데이트 실패", e)
            }
        }
    }

    /**
     * API 데이터로 카테고리 버튼 텍스트 업데이트
     */
    private fun updateCategoryButtonsWithCount(categories: List<CategoryDto>, totalCount: Int) {
        if (!isAdded || context == null) return

        try {
            // 🔥 전체 개수는 별도 함수에서 관리
            // maintainTotalCount()는 이미 loadWardrobeDataByCategory에서 호출됨

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

            Log.d("WardrobeFragment", "Category buttons updated with counts")

        } catch (e: Exception) {
            Log.e("WardrobeFragment", "Error updating category buttons", e)
        }
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
                    // 전체 버튼 클릭 시 전체 데이터 로드
                    loadWardrobeData()
                } else {
                    // 특정 카테고리 선택 시
                    loadWardrobeDataByCategory(category = categoryId)
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
     * API 데이터로 UI 업데이트
     */
    private fun updateUIWithApiData() {
        // API 데이터로만 어댑터 업데이트
        adapter.updateWithApiData(wardrobeItems)

        // 카테고리 버튼 개수 업데이트
        val totalCount = wardrobeItems.size
        updateCategoryButtonsWithCount(categories, totalCount)

        // 카테고리 정보 로그 출력 (개발용)
        categories.forEach { category ->
            println("Category: ${category.name}, Count: ${category.count}")
        }
    }

    /**
     * API 데이터로 하위 필터 업데이트
     */
    // updateSubFiltersWithApiData 함수에서
    private fun updateSubFiltersWithApiData(filterNames: List<String>, subcategories: List<SubcategoryDto>) {
        if (!isAdded || context == null) return
        subFilterLayout.removeAllViews()
        selectedIndex = 0

        val allButton = createFilterButton("전체", 0, filterNames.size + 1)
        allButton.setOnClickListener {
            if (isAdded && context != null) {
                updateButtonSelection(0)
                moveUnderline(0)
                val currentCategory = getCurrentSelectedCategory()
                loadWardrobeDataByCategory(category = currentCategory, subcategory = null)
            }
        }
        subFilterLayout.addView(allButton)

        // API에서 받은 실제 서브카테고리만 사용하고 getSubcategoryName으로 이름 변환
        subcategories.forEachIndexed { index, subcategoryDto ->
            // 서버에서 온 이름 대신 로컬 매핑 함수 사용
            val displayName = getSubcategoryName(subcategoryDto.subcategory)
            val button = createFilterButton(displayName, index + 1, subcategories.size + 1)

            button.setOnClickListener {
                if (isAdded && context != null) {
                    updateButtonSelection(index + 1)
                    moveUnderline(index + 1)
                    val currentCategory = getCurrentSelectedCategory()
                    loadWardrobeDataByCategory(category = currentCategory, subcategory = subcategoryDto.subcategory)
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
     * 하위 카테고리 업데이트
     */
    // updateSubCategories 함수에 로그 추가
    private fun updateSubCategories(subcategories: List<SubcategoryDto>) {
        if (subcategories.isNotEmpty()) {
            Log.d("WardrobeFragment", "서브카테고리 업데이트: ${subcategories.map { "${getSubcategoryName(it.subcategory)}(${it.subcategory})" }}")
            updateSubFiltersWithApiData(emptyList(), subcategories)
        } else {
            // 서브카테고리가 없으면 전체만 표시
            subFilterLayout.removeAllViews()
            val allButton = createFilterButton("전체", 0, 1)
            subFilterLayout.addView(allButton)
            updateButtonSelection(0)
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
     * 아이템 등록 후 데이터 새로고침
     */
    fun refreshData() {
        loadWardrobeData() // 등록 후 전체 데이터 다시 로드
    }

    private fun navigateToWardrobeSearch() {
        findNavController().navigate(R.id.wardrobeSearchFragment)
    }

    private fun navigateToClothesDetail(imageResId: Int) {
        val bundle = Bundle().apply {
            putInt("image_res_id", imageResId)
        }
        findNavController().navigate(R.id.clothesDetailFragment, bundle)
    }

    private fun navigateToClothesDetailWithId(item: Any) {
        try {
            when (item) {
                is WardrobeItemDto -> {
                    // API 데이터인 경우 - item.id를 image_res_id로 전달
                    if (item.id > 0) {
                        val bundle = Bundle().apply {
                            putInt("image_res_id", item.id) // API item ID를 image_res_id로 전달
                        }
                        findNavController().navigate(R.id.clothesDetailFragment, bundle)
                    } else {
                        Log.e("WardrobeFragment", "잘못된 item ID: ${item.id}")
                    }
                }
                is Int -> {
                    // 더미 데이터인 경우 - drawable 리소스 ID 전달
                    val bundle = Bundle().apply {
                        putInt("image_res_id", item)
                    }
                    findNavController().navigate(R.id.clothesDetailFragment, bundle)
                }
                else -> {
                    Log.e("WardrobeFragment", "알 수 없는 아이템 타입: ${item::class.java}")
                }
            }
        } catch (e: Exception) {
            Log.e("WardrobeFragment", "Navigation 실패: ${e.message}")
        }
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private fun updateSubFilters(category: String) {
        val filters = categoryFilters[category] ?: return
        if (!isAdded || context == null) return
        subFilterLayout.removeAllViews()
        selectedIndex = 0
        createSubFilterButtons(filters)
        updateButtonSelection(0)
        subFilterLayout.post {
            if (isAdded && view != null) {
                moveUnderline(0)
            }
        }
    }

    private fun createSubFilterButtons(filters: List<String>) {
        for (i in filters.indices) {
            val filter = filters[i]
            val button = createFilterButton(filter, i, filters.size)
            button.setOnClickListener {
                if (isAdded && context != null) {
                    updateButtonSelection(i)
                    moveUnderline(i)
                }
            }
            subFilterLayout.addView(button)
        }
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
            gravity = Gravity.CENTER
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

    private fun collectFormData(): RegisterItemRequestDto? {
        try {
            // 임시로 더미 값들 사용 (실제로는 UI에서 가져와야 함)
            val category = 1
            val subcategory = 1
            val season = 1
            val color = 1
            val brand = "테스트브랜드"
            val size = "M"
            val purchaseDate = "2024-08-01"
            val imageUrl = "https://example.com/image.jpg"
            val price = 50000
            val purchaseSite = "테스트쇼핑몰"
            val selectedTags = listOf(1, 2)

            return RegisterItemRequestDto(
                category = category,
                subcategory = subcategory,
                season = season,
                color = color,
                brand = brand,
                size = size,
                purchaseDate = purchaseDate,
                image = imageUrl,
                price = price,
                purchaseSite = purchaseSite,
                tagIds = selectedTags
            )
        } catch (e: Exception) {
            Log.e("WardrobeFragment", "Error collecting form data", e)
            return null
        }
    }

    private fun getSubcategoryName(subcategoryId: Int): String {
        return when (subcategoryId) {
            // 상의 (category 1)
            1 -> "반팔티"
            2 -> "긴팔티"
            3 -> "셔츠"
            4 -> "블라우스"
            5 -> "니트"
            6 -> "후드티"
            7 -> "탱크톱"
            8 -> "나시티"

            // 하의 (category 2)
            9 -> "청바지"
            10 -> "면바지"
            11 -> "반바지"
            12 -> "슬랙스"
            13 -> "치마"
            14 -> "레깅스"
            15 -> "조거팬츠"

            // 원피스 (category 3)
            16 -> "미니원피스"
            17 -> "미디원피스"
            18 -> "롱원피스"
            19 -> "니트원피스"
            20 -> "셔츠원피스"

            // 아우터 (category 4)
            21 -> "자켓"
            22 -> "패딩"
            23 -> "코트"
            24 -> "바람막이"
            25 -> "가디건"
            26 -> "점퍼"
            27 -> "블레이저"

            // 신발 (category 5)
            28 -> "운동화"
            29 -> "구두"
            30 -> "부츠"
            31 -> "샌들"
            32 -> "슬리퍼"
            33 -> "하이힐"
            34 -> "플랫슈즈"

            // 악세서리 (category 6)
            35 -> "가방"
            36 -> "모자"
            37 -> "벨트"
            38 -> "목걸이"
            39 -> "귀걸이"
            40 -> "시계"
            41 -> "반지"

            else -> {
                Log.w("WardrobeFragment", "알 수 없는 서브카테고리 ID: $subcategoryId")
                "기타"
            }
        }
    }
}