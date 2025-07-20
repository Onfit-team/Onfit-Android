package com.example.onfit

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.Gravity
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RelativeLayout

open class WardrobeFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: WardrobeAdapter
    private lateinit var subFilterLayout: LinearLayout
    private lateinit var subFilterScrollView: HorizontalScrollView

    // 전체 이미지 (원본 리스트)
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

    // 카테고리별 필터 목록
    private val categoryFilters = mapOf(
        "전체" to listOf("전체"),
        "상의" to listOf("전체", "반팔티", "긴팔티", "셔츠", "블라우스", "니트", "나시티"),
        "하의" to listOf("전체", "청바지", "반바지", "슬랙스", "치마"),
        "아우터" to listOf("전체", "자켓", "패딩", "코트", "바람막이"),
        "원피스" to listOf("전체", "미니", "미디", "롱"),
        "신발" to listOf("전체", "운동화", "샌들", "부츠", "워커")
    )

    // 현재 선택된 인덱스를 저장
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

        // 초기 하위 필터 표시 (전체)
        updateSubFilters("전체")

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 등록 버튼 클릭 리스너
        val registerBtn = view.findViewById<ImageButton>(R.id.wardrobe_register_btn)
        registerBtn.setOnClickListener {
            val bottomSheet = RegisterItemBottomSheet()
            bottomSheet.show(parentFragmentManager, bottomSheet.tag)
        }

        // 검색 아이콘 클릭 리스너 추가
        val searchIcon = view.findViewById<ImageButton>(R.id.ic_search)
        searchIcon?.setOnClickListener {
            navigateToWardrobeSearch()
        }
    }

    private fun navigateToWardrobeSearch() {
        // WardrobeSearchActivity로 이동
        val intent = Intent(requireContext(), WardrobeSearchActivity::class.java)
        startActivity(intent)
    }

    private fun initializeViews(view: View) {
        recyclerView = view.findViewById(R.id.wardrobeRecyclerView)
        subFilterLayout = view.findViewById(R.id.subFilterLayout)
        subFilterScrollView = view.findViewById(R.id.subFilterScrollView)
    }

    private fun setupRecyclerView() {
        adapter = WardrobeAdapter(allImageList) { imageResId ->
            // 옷 아이템 클릭 시 상세 페이지로 이동
            navigateToClothesDetail(imageResId)
        }
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        recyclerView.adapter = adapter
    }

    private fun navigateToClothesDetail(imageResId: Int) {
        // ClothesDetailActivity로 이동
        val intent = Intent(requireContext(), ClothesDetailActivity::class.java)
        intent.putExtra("image_res_id", imageResId) // 이미지 리소스 ID 전달
        startActivity(intent)
    }

    private fun setupTopCategoryButtons(view: View) {
        val topCategories = mapOf(
            R.id.btnTopCategory1 to "전체",
            R.id.btnTopCategory2 to "상의",
            R.id.btnTopCategory3 to "하의",
            R.id.btnTopCategory4 to "아우터",
            R.id.btnTopCategory5 to "원피스",
            R.id.btnTopCategory6 to "신발"
        )

        topCategories.forEach { (id, categoryName) ->
            val button = view.findViewById<Button>(id)
            button?.setOnClickListener {
                // 이전 선택된 버튼 상태 해제
                selectedTopCategoryButton?.isSelected = false

                // 새로 선택된 버튼 상태 설정
                button.isSelected = true
                selectedTopCategoryButton = button

                updateSubFilters(categoryName)
            }

            // 첫 번째 버튼(전체)을 기본 선택 상태로 설정
            if (categoryName == "전체") {
                button.isSelected = true
                selectedTopCategoryButton = button
            }
        }
    }

    // dp를 px로 변환하는 헬퍼 함수
    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    // 하위 필터 버튼 갱신
    private fun updateSubFilters(category: String) {
        val filters = categoryFilters[category] ?: return

        // Fragment가 여전히 유효한지 확인
        if (!isAdded || context == null) return

        // 기존 뷰들을 안전하게 제거
        subFilterLayout.removeAllViews()
        selectedIndex = 0 // 초기화

        // 버튼들을 생성하고 추가
        createSubFilterButtons(filters)

        // 초기 선택 상태 설정
        updateButtonSelection(0)

        // 초기 밑줄 위치 설정 (뷰가 완전히 렌더링된 후)
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

            // 클릭 리스너 설정
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

        // 버튼 레이아웃 파라미터 설정
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

    private fun moveUnderline(selectedIndex: Int) {
        if (!isAdded || view == null || context == null) return

        val activeUnderline = view?.findViewById<View>(R.id.activeUnderline) ?: return
        val selectedButton = subFilterLayout.getChildAt(selectedIndex) as? Button ?: return

        // 뷰가 완전히 렌더링된 후에 실행
        selectedButton.post {
            if (!isAdded || view == null || context == null) return@post

            try {
                // 텍스트 길이에 따른 밑줄 너비 계산
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
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        try {
            // RecyclerView 정리
            if (::recyclerView.isInitialized) {
                recyclerView.adapter = null
            }

            // 서브 필터 레이아웃 정리
            if (::subFilterLayout.isInitialized) {
                subFilterLayout.removeAllViews()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onPause() {
        super.onPause()
        // 필요시에만 GC 호출 (일반적으로 권장되지 않음)
        // System.gc()
    }
}