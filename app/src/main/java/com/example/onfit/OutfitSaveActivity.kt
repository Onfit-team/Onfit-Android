package com.example.onfit

import android.R
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.onfit.databinding.ActivityOutfitSaveBinding

class OutfitSaveActivity : AppCompatActivity() {

    lateinit var binding: ActivityOutfitSaveBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityOutfitSaveBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 스피너 카테고리 매핑
        val categoryMap = mapOf(
            "상의" to listOf("반팔티", "긴팔티", "민소매", "셔츠/블라우스", "맨투맨", "후드티", "니트/스웨터", "기타"),
            "하의" to listOf("반바지", "긴바지", "청바지", "트레이닝 팬츠", "레깅스", "스커트", "기타"),
            "원피스" to listOf("미니 원피스", "롱 원피스", "끈 원피스", "니트 원피스", "기타"),
            "아우터" to listOf("바람막이", "가디건", "자켓", "코트", "패딩", "후드집업", "무스탕/퍼", "기타"),
            "신발" to listOf("운동화", "부츠", "샌들", "슬리퍼", "구두", "로퍼", "기타"),
            "악세사리" to listOf("모자", "머플러", "장갑", "양말", "안경/선글라스", "가방", "시계/팔찌/목걸이", "기타")
        )

        val seasonList = listOf("봄", "여름", "가을", "겨울")
        val colorList = listOf("화이트", "블랙", "그레이", "베이지/브라운", "네이비/블루", "레드/핑크", "오렌지/옐로우", "그린", "퍼플", "멀티/패턴")

        val spinner1 = binding.outfitSaveSpinner1 // 상위 카테고리
        val spinner2 = binding.outfitSaveSpinner2 // 하위 카테고리
        val parentCategories = categoryMap.keys.toList()
        val spinner3 = binding.outfitSaveSpinner3 // 계절 카테고리
        val spinner4 = binding.outfitSaveSpinner4 // 색 카테고리

        // 상위 카테고리 어댑터 설정
        val parentAdapter = ArrayAdapter(this, R.layout.simple_spinner_item, parentCategories)
        parentAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner1.adapter = parentAdapter

        // 하위 카테고리 어댑터는 수정 가능한 리스트로 시작
        val subCategoryList = mutableListOf<String>()
        val subAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, subCategoryList)
        subAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner2.adapter = subAdapter

        // 상위 스피너 선택 시 하위 카테고리 갱신
        spinner1.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selectedParent = parentCategories[position]
                val subList = categoryMap[selectedParent] ?: emptyList()

                subAdapter.clear()
                subAdapter.addAll(subList)
                subAdapter.notifyDataSetChanged()

                spinner2.setSelection(0) // 하위 스피너 첫 항목 선택
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // 계절 카테고리 어댑터
        val seasonAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, seasonList)
        seasonAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        // 색 카테고리 어댑터
        val colorAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, colorList)
        colorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        // 어댑터 연결
        spinner3.adapter = seasonAdapter
        spinner4.adapter = colorAdapter

        val outfitSaveEt1 = binding.outfitSaveEt1
        val outfitSaveEt2 = binding.outfitSaveEt2
        val outfitSaveEt3 = binding.outfitSaveEt3
        val outfitSaveEt4 = binding.outfitSaveEt4

        // 구매정보 입력 top sheet dialog 띄우기
        val commonClickListener = View.OnClickListener {
            val dialog = TopInfoDialogFragment()

            dialog.setOnTopInfoSavedListener(object : TopInfoDialogFragment.OnTopInfoSavedListener {
                override fun onTopInfoSaved(brand: String, price: String, size: String, site: String) {
                    outfitSaveEt1.setText(brand)
                    outfitSaveEt2.setText(price)
                    outfitSaveEt3.setText(size)
                    outfitSaveEt4.setText(site)
                }
            })
            dialog.show(supportFragmentManager, "TopInfoDialog")
        }

        // EditText 네 개에 한꺼번에 리스너 붙이기
        listOf(outfitSaveEt1, outfitSaveEt2, outfitSaveEt3, outfitSaveEt4).forEach {
            it.setOnClickListener(commonClickListener)
        }

        // 뒤로가기
        binding.outfitSaveBackBtn.setOnClickListener {
            finish()
        }
    }
}