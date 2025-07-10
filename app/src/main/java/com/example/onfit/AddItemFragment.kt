package com.example.onfit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.onfit.R

class AddItemFragment : Fragment() {

    private lateinit var spinnerCategory: Spinner
    private lateinit var spinnerDetailCategory: Spinner
    private lateinit var spinnerSeason: Spinner
    private lateinit var spinnerColor: Spinner

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_add_item, container, false)

        initViews(view)
        setupSpinners()

        return view
    }

    private fun initViews(view: View) {
        spinnerCategory = view.findViewById(R.id.spinner_category)
        spinnerDetailCategory = view.findViewById(R.id.spinner_detail_category)
        spinnerSeason = view.findViewById(R.id.spinner_season)
        spinnerColor = view.findViewById(R.id.spinner_color)

        // 뒤로가기 버튼
        val btnBack = view.findViewById<ImageButton>(R.id.ic_back)
        btnBack.setOnClickListener {
            activity?.onBackPressed()
        }

        // 저장하기 버튼
        val btnSave = view.findViewById<Button>(R.id.btn_save)
        btnSave.setOnClickListener {
            saveClothingItem()
        }
    }

    private fun setupSpinners() {
        // 카테고리 스피너
        val categories = arrayOf("상의", "하의", "아우터", "신발", "가방", "액세서리")
        val categoryAdapter = ArrayAdapter(requireContext(), R.layout.spinner_item, categories)
        categoryAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        spinnerCategory.adapter = categoryAdapter

        // 세부 카테고리 스피너
        val detailCategories = arrayOf("반팔티", "긴팔티", "셔츠", "블라우스", "후드티", "맨투맨")
        val detailAdapter = ArrayAdapter(requireContext(), R.layout.spinner_item, detailCategories)
        detailAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        spinnerDetailCategory.adapter = detailAdapter

        // 계절 스피너
        val seasons = arrayOf("봄", "여름", "가을", "겨울")
        val seasonAdapter = ArrayAdapter(requireContext(), R.layout.spinner_item, seasons)
        seasonAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        spinnerSeason.adapter = seasonAdapter

        // 색상 스피너
        val colors = arrayOf("검은색", "흰색", "빨간색", "주황색", "노란색", "초록색", "파란색", "남색", "보라색", "아이보리색", "회색", "베이지색")
        val colorAdapter = ArrayAdapter(requireContext(), R.layout.spinner_item, colors)
        colorAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        spinnerColor.adapter = colorAdapter

        // 스피너 선택 리스너
        spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedCategory = categories[position]
                updateDetailCategory(selectedCategory)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun updateDetailCategory(category: String) {
        val detailCategories = when (category) {
            "상의" -> arrayOf("반팔티", "긴팔티", "셔츠", "블라우스", "후드티", "맨투맨")
            "하의" -> arrayOf("청바지", "슬랙스", "치마", "반바지", "레깅스")
            "아우터" -> arrayOf("코트", "재킷", "가디건", "점퍼", "패딩")
            "신발" -> arrayOf("스니커즈", "구두", "부츠", "샌들", "슬리퍼")
            "가방" -> arrayOf("백팩", "토트백", "크로스백", "클러치", "숄더백")
            "액세서리" -> arrayOf("모자", "벨트", "시계", "목걸이", "귀걸이")
            else -> arrayOf("반팔티", "긴팔티", "셔츠")
        }

        val detailAdapter = ArrayAdapter(requireContext(), R.layout.spinner_item, detailCategories)
        detailAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        spinnerDetailCategory.adapter = detailAdapter
    }

    private fun saveClothingItem() {
        // 선택된 값들 가져오기
        val category = spinnerCategory.selectedItem.toString()
        val detailCategory = spinnerDetailCategory.selectedItem.toString()
        val season = spinnerSeason.selectedItem.toString()
        val color = spinnerColor.selectedItem.toString()

        // 저장 로직 구현 (데이터베이스, SharedPreferences 등)
        // 예: 토스트 메시지 표시
        Toast.makeText(requireContext(), "저장되었습니다!", Toast.LENGTH_SHORT).show()
    }
}