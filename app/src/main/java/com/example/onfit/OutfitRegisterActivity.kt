package com.example.onfit

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.onfit.databinding.ActivityOutfitRegisterBinding
import com.example.onfit.databinding.ActivitySaveBinding

class OutfitRegisterActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        lateinit var binding: ActivityOutfitRegisterBinding
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityOutfitRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

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