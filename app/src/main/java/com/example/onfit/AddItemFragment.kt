package com.example.onfit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment

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

        // 뒤로가기 버튼 (있다면)
        val backButton = view.findViewById<ImageButton>(R.id.ic_back)
        backButton?.setOnClickListener {
            requireActivity().finish()
        }

        // 드롭다운 더미데이터 설정
        setupDropdowns(view)
    }

    private fun setupDropdowns(view: View) {
        // 카테고리 스피너
        val categorySpinner = view.findViewById<Spinner>(R.id.spinner_category)
        val categoryData = arrayOf("상의", "하의", "아우터", "원피스", "신발", "악세서리")
        val categoryAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categoryData)
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner?.adapter = categoryAdapter

        // 세부 카테고리 스피너
        val detailCategorySpinner = view.findViewById<Spinner>(R.id.spinner_detail_category)
        val detailCategoryData = arrayOf("반팔티", "긴팔티", "셔츠", "블라우스", "니트", "후드티")
        val detailCategoryAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, detailCategoryData)
        detailCategoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        detailCategorySpinner?.adapter = detailCategoryAdapter

        // 계절 스피너
        val seasonSpinner = view.findViewById<Spinner>(R.id.spinner_season)
        val seasonData = arrayOf("봄", "여름", "가을", "겨울", "사계절")
        val seasonAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, seasonData)
        seasonAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        seasonSpinner?.adapter = seasonAdapter

        // 색상 스피너
        val colorSpinner = view.findViewById<Spinner>(R.id.spinner_color)
        val colorData = arrayOf("블랙", "화이트", "그레이", "네이비", "브라운", "베이지", "레드", "핑크", "옐로우", "그린", "블루", "퍼플")
        val colorAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, colorData)
        colorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        colorSpinner?.adapter = colorAdapter
    }
}