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
        if (state.isLoading) showLoading(true) else showLoading(false)

        if (state.hasData) {
            adapter.updateWithApiData(state.wardrobeItems)
            updateCategoryButtonsWithCount(state.categories)
            updateSubCategories(state.subcategories)
        }

        if (state.hasError) {
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
        parentFragmentManager.setFragmentResultListener("item_registered", this) { _, bundle ->
            val isSuccess = bundle.getBoolean("success", false)
            val registeredDate = bundle.getString("registered_date")
            if (isSuccess) {
                viewModel.refreshWardrobeItems()
                notifyCalendarFragmentOfNewItem(registeredDate)
                Toast.makeText(context, "아이템이 등록되었습니다", Toast.LENGTH_SHORT).show()
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
                // 빈 리스트도 정상적으로 반영(검색 결과 없음)
            }
        }
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
                if (categoryName == "전체") {
                    viewModel.loadAllWardrobeItems()
                } else {
                    viewModel.loadWardrobeItemsByCategory(category = categoryId)
                }
            }
            if (categoryName == "전체") {
                button.isSelected = true
                selectedTopCategoryButton = button
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

    private fun updateSubCategories(subcategories: List<SubcategoryDto>) {
        if (subcategories.isNotEmpty()) {
            updateSubFiltersWithApiData(subcategories)
        } else {
            subFilterLayout.removeAllViews()
            val allButton = createFilterButton("전체", 0, 1)
            subFilterLayout.addView(allButton)
            updateButtonSelection(0)
        }
    }

    private fun updateSubFiltersWithApiData(subcategories: List<SubcategoryDto>) {
        if (!isAdded || context == null) return
        subFilterLayout.removeAllViews()
        selectedIndex = 0
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
        subcategories.forEachIndexed { index, subcategoryDto ->
            val displayName = subcategoryDto.name
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
        // 필요시 ProgressBar 추가
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