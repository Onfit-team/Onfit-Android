package com.example.onfit

import android.os.Bundle
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
    }
}