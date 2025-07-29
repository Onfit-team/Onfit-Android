package com.example.onfit.HomeRegister.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import com.example.onfit.R
import com.example.onfit.TopInfoDialogFragment
import com.example.onfit.databinding.FragmentOutfitSaveBinding

class OutfitSaveFragment : Fragment() {
    private var _binding: FragmentOutfitSaveBinding? = null
    private val binding get() = _binding!!

    private val categoryMap = mapOf(
        "상의" to listOf("반팔티", "긴팔티", "민소매", "셔츠/블라우스", "맨투맨", "후드티", "니트/스웨터", "기타"),
        "하의" to listOf("반바지", "긴바지", "청바지", "트레이닝 팬츠", "레깅스", "스커트", "기타"),
        "원피스" to listOf("미니 원피스", "롱 원피스", "끈 원피스", "니트 원피스", "기타"),
        "아우터" to listOf("바람막이", "가디건", "자켓", "코트", "패딩", "후드집업", "무스탕/퍼", "기타"),
        "신발" to listOf("운동화", "부츠", "샌들", "슬리퍼", "구두", "로퍼", "기타"),
        "악세사리" to listOf("모자", "머플러", "장갑", "양말", "안경/선글라스", "가방", "시계/팔찌/목걸이", "기타")
    )

    private val seasonList = listOf("봄", "여름", "가을", "겨울")
    private val colorList = listOf("화이트", "블랙", "그레이", "베이지/브라운", "네이비/블루", "레드/핑크", "오렌지/옐로우", "그린", "퍼플", "멀티/패턴")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentOutfitSaveBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val spinner1 = binding.outfitSaveSpinner1
        val spinner2 = binding.outfitSaveSpinner2
        val parentCategories = categoryMap.keys.toList()

        val spinner3 = binding.outfitSaveSpinner3
        val spinner4 = binding.outfitSaveSpinner4

        // 상위 카테고리 어댑터
        val parentAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, parentCategories)
        parentAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner1.adapter = parentAdapter

        // 하위 카테고리 어댑터
        val subCategoryList = mutableListOf<String>()
        val subAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, subCategoryList)
        subAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner2.adapter = subAdapter

        spinner1.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedParent = parentCategories[position]
                val subList = categoryMap[selectedParent] ?: emptyList()
                subAdapter.clear()
                subAdapter.addAll(subList)
                subAdapter.notifyDataSetChanged()
                spinner2.setSelection(0)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // 계절, 색상 스피너 설정
        val seasonAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, seasonList)
        seasonAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner3.adapter = seasonAdapter

        val colorAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, colorList)
        colorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner4.adapter = colorAdapter

        // 구매 정보 다이얼로그 연결
        val commonClickListener = View.OnClickListener {
            val dialog = TopInfoDialogFragment()
            dialog.setOnTopInfoSavedListener(object : TopInfoDialogFragment.OnTopInfoSavedListener {
                override fun onTopInfoSaved(brand: String, price: String, size: String, site: String) {
                    binding.outfitSaveEt1.setText(brand)
                    binding.outfitSaveEt2.setText(price)
                    binding.outfitSaveEt3.setText(size)
                    binding.outfitSaveEt4.setText(site)
                }
            })
            dialog.show(parentFragmentManager, "TopInfoDialog")
        }

        listOf(
            binding.outfitSaveEt1,
            binding.outfitSaveEt2,
            binding.outfitSaveEt3,
            binding.outfitSaveEt4
        ).forEach {
            it.setOnClickListener(commonClickListener)
        }

        // 뒤로가기 버튼
        binding.outfitSaveBackBtn.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        // 실행 중 bottom navigation view 보이지 않게
        activity?.findViewById<View>(R.id.bottomNavigationView)?.visibility = View.GONE
    }

    override fun onPause() {
        super.onPause()
        // 실행 안 할 때 bottom navigation view 다시 보이게
        activity?.findViewById<View>(R.id.bottomNavigationView)?.visibility = View.VISIBLE
    }

}