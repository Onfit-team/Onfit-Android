package com.example.onfit

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.onfit.databinding.ActivityOutfitRegisterBinding
import com.example.onfit.databinding.ActivitySaveBinding

class OutfitRegisterActivity : AppCompatActivity() {
    private lateinit var adapter: OutfitAdapter
    private lateinit var outfitList: MutableList<OutfitItem>
    lateinit var binding: ActivityOutfitRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityOutfitRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // RecyclerView 세팅
        outfitList = mutableListOf(
            OutfitItem(R.drawable.outfit_top, isInCloset = true),
            OutfitItem(R.drawable.outfit_pants, isInCloset = true),
            OutfitItem(R.drawable.outfit_shoes, isInCloset = true)
        ) // 초기 빈 리스트
        adapter = OutfitAdapter(outfitList)

        val recyclerView = findViewById<RecyclerView>(R.id.outfit_register_rv)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        // 추가 버튼 클릭 시 아이템 추가
        binding.outfitRegisterAddButton.setOnClickListener {
            adapter.addItem(OutfitItem(R.drawable.sun, isInCloset = true)) // 기본 이미지로 추가
        }

        // 아이템 저장 화면으로 이동
        binding.outfitRegisterSaveBtn.setOnClickListener {
            val intent = Intent(this, OutfitSaveActivity::class.java)
            startActivity(intent)
        }

        // 뒤로가기 버튼
        binding.outfitRegisterBackBtn.setOnClickListener {
            finish()
        }
    }
}