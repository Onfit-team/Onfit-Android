package com.example.onfit.Wardrobe.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.onfit.R

class AddItemFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_add_item, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 뒤로가기 버튼
        val backButton = view.findViewById<ImageButton>(R.id.ic_back)
        backButton?.setOnClickListener {
            findNavController().navigateUp()
        }

        // 드롭다운 설정
        setupDropdowns(view)

        // 더미 버튼 클릭 기능 설정
        setupTagButtons(view)

        // 저장 버튼
        val saveButton = view.findViewById<Button>(R.id.btn_save)
        saveButton?.setOnClickListener {
            // 저장 로직 구현
            Toast.makeText(requireContext(), "저장되었습니다", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
        }
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

    private fun setupDropdowns(view: View) {
        // 카테고리 스피너 설정
        setupSpinnerWithContainer(
            view,
            R.id.spinner_category,
            arrayOf("상의", "하의", "아우터", "원피스", "신발", "악세서리")
        )

        // 세부 카테고리 스피너 설정
        setupSpinnerWithContainer(
            view,
            R.id.spinner_detail_category,
            arrayOf("반팔티", "긴팔티", "셔츠", "블라우스", "니트", "후드티")
        )

        // 계절 스피너 설정
        setupSpinnerWithContainer(
            view,
            R.id.spinner_season,
            arrayOf("봄", "여름", "가을", "겨울", "사계절")
        )

        // 색상 스피너 설정
        setupSpinnerWithContainer(
            view,
            R.id.spinner_color,
            arrayOf("블랙", "화이트", "그레이", "네이비", "브라운", "베이지", "레드", "핑크", "옐로우", "그린", "블루", "퍼플")
        )
    }

    private fun setupSpinnerWithContainer(view: View, spinnerId: Int, data: Array<String>) {
        val spinner = view.findViewById<Spinner>(spinnerId)

        // 기본 어댑터 사용
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, data)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner?.adapter = adapter

        // 스피너가 포함된 LinearLayout 찾기 (전체 컨테이너)
        val spinnerContainer = spinner?.parent as? LinearLayout

        // 드롭다운 위치 조정
        spinner?.setOnTouchListener { _, _ ->
            spinner.post {
                adjustDropdownPosition(spinner, spinnerContainer)
            }
            false
        }

        // 전체 컨테이너 클릭 시 스피너 열기
        spinnerContainer?.setOnClickListener {
            spinner.performClick()
        }

        // 컨테이너 내의 ImageView(화살표) 클릭 시도 스피너 열기
        spinnerContainer?.let { container ->
            for (i in 0 until container.childCount) {
                val child = container.getChildAt(i)
                if (child is ImageView) {
                    child.setOnClickListener {
                        spinner.performClick()
                    }
                }
            }
        }
    }

    private fun adjustDropdownPosition(spinner: Spinner, spinnerContainer: LinearLayout?) {
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

            // 스피너가 컨테이너 왼쪽 경계로부터 얼마나 떨어져 있는지 계산
            val offsetToContainerLeft = spinnerLocation[0] - containerLocation[0]

            android.util.Log.d(
                "Spinner",
                "Spinner pos: ${spinnerLocation[0]}, Container pos: ${containerLocation[0]}"
            )
            android.util.Log.d("Spinner", "Calculated offset: ${-offsetToContainerLeft}")

            // 컨테이너 너비로 드롭다운 너비 설정
            val containerWidth = spinnerContainer.width
            val setWidthMethod = popupWindow.javaClass.getMethod("setWidth", Int::class.java)
            setWidthMethod.invoke(popupWindow, containerWidth)

            // 높이 제한
            val maxHeight = (250 * resources.displayMetrics.density).toInt()
            val setHeightMethod = popupWindow.javaClass.getMethod("setHeight", Int::class.java)
            setHeightMethod.invoke(popupWindow, maxHeight)

            // 컨테이너 왼쪽 경계에 맞춰 offset 설정
            val setHorizontalOffsetMethod =
                popupWindow.javaClass.getMethod("setHorizontalOffset", Int::class.java)
            setHorizontalOffsetMethod.invoke(popupWindow, -offsetToContainerLeft)

            android.util.Log.d("Spinner", "Dropdown adjusted successfully")

        } catch (e: Exception) {
            android.util.Log.e("Spinner", "Failed to adjust dropdown: ${e.message}")
        }
    }

    private fun setupTagButtons(view: View) {
        // 분위기 태그 버튼들 (Layout1, Layout2)
        setupFlexboxLayout(view, R.id.topCategoryLayout1)
        setupFlexboxLayout(view, R.id.topCategoryLayout2)

        // 용도 태그 버튼들 (Layout3, Layout4)
        setupFlexboxLayout(view, R.id.topCategoryLayout3)
        setupFlexboxLayout(view, R.id.topCategoryLayout4)
    }

    private fun setupFlexboxLayout(view: View, layoutId: Int) {
        val flexboxLayout = view.findViewById<com.google.android.flexbox.FlexboxLayout>(layoutId)

        // FlexboxLayout 내의 모든 버튼에 클릭 리스너 추가
        for (i in 0 until flexboxLayout.childCount) {
            val child = flexboxLayout.getChildAt(i)
            if (child is Button) {
                child.setOnClickListener { button ->
                    // 버튼 선택 상태 토글
                    button.isSelected = !button.isSelected

                    // 로그로 확인
                    android.util.Log.d(
                        "TagButton",
                        "${(button as Button).text} selected: ${button.isSelected}"
                    )
                }
            }
        }
    }
}