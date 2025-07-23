package com.example.onfit

import android.os.Bundle
import android.view.View
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