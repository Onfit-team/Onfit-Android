package com.example.onfit.Wardrobe.fragment

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
import androidx.navigation.fragment.findNavController
import com.example.onfit.R
import com.example.onfit.Wardrobe.adapter.WardrobeAdapter

class StyleOutfitsFragment : Fragment() {

    private lateinit var rvOutfitItems: RecyclerView
    private lateinit var wardrobeAdapter: WardrobeAdapter
    private lateinit var styleFilterLayout: LinearLayout
    private lateinit var styleFilterScrollView: HorizontalScrollView

    // 전체 의류 아이템 (WardrobeFragment와 완전히 동일)
    private val allImageList = listOf(
        R.drawable.clothes1,
        R.drawable.clothes2,
        R.drawable.clothes3,
        R.drawable.clothes4,
        R.drawable.clothes5,
        R.drawable.clothes6,
        R.drawable.clothes7,
        R.drawable.clothes8,
        R.drawable.cody_image1,
        R.drawable.cody_image4,
        R.drawable.cody_image5
    )

    // 스타일 필터 목록
    private val styleFilters = listOf("포멀", "빈티지", "미니멀", "캐주얼", "꾸안꾸")
    private val styleTagCounts = mapOf(
        "포멀" to 10,
        "빈티지" to 10,
        "미니멀" to 10,
        "캐주얼" to 10,
        "꾸안꾸" to 10
    )

    // 현재 선택된 인덱스
    private var selectedStyleIndex = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_style_outfits, container, false)

        initializeViews(view)
        setupRecyclerView()
        setupStyleFilters()
        setupBackButton(view)

        return view
    }

    private fun initializeViews(view: View) {
        rvOutfitItems = view.findViewById(R.id.rvOutfitItems)
        styleFilterLayout = view.findViewById(R.id.styleFilterLayout)
        styleFilterScrollView = view.findViewById(R.id.styleFilterScrollView)
    }

    private fun setupRecyclerView() {
        // 기존 WardrobeAdapter 사용 - 클래스 중복 문제 해결
        wardrobeAdapter = WardrobeAdapter(allImageList) { imageResId ->
            navigateToClothesDetail(imageResId)
        }
        rvOutfitItems.layoutManager = GridLayoutManager(requireContext(), 2)
        rvOutfitItems.adapter = wardrobeAdapter
    }


    private fun navigateToClothesDetail(imageResId: Int) {
        // ClothesDetailFragment로 이동 (Fragment Navigation 사용)
        val bundle = Bundle().apply {
            putInt("image_res_id", imageResId) // 이미지 리소스 ID 전달
        }
        findNavController().navigate(R.id.clothesDetailFragment, bundle)
    }

    private fun setupStyleFilters() {
        // 스타일 필터 버튼들 생성
        styleFilters.forEachIndexed { index, styleName ->
            val button = createStyleFilterButton(styleName, index)
            styleFilterLayout.addView(button)
        }

        // 첫 번째 버튼을 기본 선택으로 설정
        updateStyleButtonSelection(0)
    }

    private fun createStyleFilterButton(styleName: String, index: Int): Button {
        val count = styleTagCounts[styleName] ?: 0
        val buttonText = "$styleName $count"

        val button = Button(requireContext()).apply {
            text = buttonText
            setTextColor(ContextCompat.getColor(context, android.R.color.black))
            background = try {
                ContextCompat.getDrawable(context, R.drawable.style_tag_background)
            } catch (e: Exception) {
                // 리소스가 없는 경우 기본 배경 사용
                null
            }
            setPadding(dpToPx(1), dpToPx(0), dpToPx(1), dpToPx(0)) // 좌우 8px, 상하 7px
            textSize = 14f
            isAllCaps = false
            minWidth = dpToPx(49)
            minHeight = 0
            setSingleLine(true)
            gravity = Gravity.CENTER

            // intermedium 폰트 적용
            try {
                typeface = resources.getFont(R.font.intermedium)
            } catch (e: Exception) {
                // 폰트가 없는 경우 기본 폰트 사용
                e.printStackTrace()
            }
        }

        // 버튼 레이아웃 파라미터 설정
        val buttonParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            if (index > 0) leftMargin = dpToPx(8) // 버튼 간격
        }
        button.layoutParams = buttonParams

        // 클릭 리스너
        button.setOnClickListener {
            updateStyleButtonSelection(index)
            filterOutfitsByStyle(styleName)
        }

        return button
    }

    private fun updateStyleButtonSelection(newSelectedIndex: Int) {
        if (!isAdded || context == null) return

        try {
            // 모든 버튼을 기본 상태로 변경
            for (i in 0 until styleFilterLayout.childCount) {
                val button = styleFilterLayout.getChildAt(i) as? Button
                button?.isSelected = false
                button?.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))
            }

            // 선택된 버튼 상태 변경
            if (newSelectedIndex < styleFilterLayout.childCount) {
                val selectedButton = styleFilterLayout.getChildAt(newSelectedIndex) as? Button
                selectedButton?.isSelected = true
                selectedButton?.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
            }

            selectedStyleIndex = newSelectedIndex
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun filterOutfitsByStyle(styleName: String) {
        // TODO: 스타일에 따른 필터링 로직 구현
        // 현재는 모든 아이템 표시
        // 예시: 특정 스타일에 따라 이미지 리스트 필터링
    }

    private fun setupBackButton(view: View) {
        view.findViewById<View>(R.id.ic_back)?.setOnClickListener {
            try {
                requireActivity().onBackPressed()
            } catch (e: Exception) {
                // Fragment가 attach되지 않은 경우 처리
                e.printStackTrace()
            }
        }
    }

    // dp를 px로 변환하는 헬퍼 함수
    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        try {
            if (::rvOutfitItems.isInitialized) {
                rvOutfitItems.adapter = null
            }
            if (::styleFilterLayout.isInitialized) {
                styleFilterLayout.removeAllViews()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onStart() {
        super.onStart()
        // StyleOutfitsFragment가 시작될 때 bottom navigation 숨기기
        try {
            val activity = requireActivity()
            // MainActivity에서 실제 사용하는 bottomNavigationView
            val bottomNav = activity.findViewById<View>(R.id.bottomNavigationView)
            bottomNav?.visibility = View.GONE
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onStop() {
        super.onStop()
        // StyleOutfitsFragment가 멈출 때 bottom navigation 다시 보이기
        try {
            val activity = requireActivity()
            val bottomNav = activity.findViewById<View>(R.id.bottomNavigationView)
            bottomNav?.visibility = View.VISIBLE
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}