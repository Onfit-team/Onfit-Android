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

class OutfitRegisterActivity : AppCompatActivity() {
    lateinit var binding: ActivityOutfitRegisterBinding
    private lateinit var adapter: OutfitAdapter
    private val outfitList = mutableListOf<OutfitItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityOutfitRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 더미 데이터 추가
        outfitList.addAll(
            listOf(
                OutfitItem(R.drawable.outfit_top),
                OutfitItem(R.drawable.outfit_pants),
                OutfitItem(R.drawable.outfit_shoes)
            )
        )
        adapter = OutfitAdapter(outfitList)
        binding.outfitRegisterRv.adapter = adapter
        binding.outfitRegisterRv.layoutManager = LinearLayoutManager(this)

        // + 버튼 누르면 이미지 추가
        binding.outfitRegisterAddButton.setOnClickListener {
            val newItem = OutfitItem(R.drawable.sun)
            adapter.addItem(newItem)
        }
    }
}