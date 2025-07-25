package com.example.onfit

import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.ViewGroup
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.onfit.databinding.ActivityCalendarSaveBinding
import com.example.onfit.databinding.ActivityRegisterBinding

class CalendarSaveActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCalendarSaveBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityCalendarSaveBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 처음 실행 시에만 프래그먼트 추가
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.register_container, CalendarSaveFragment())
                .commit()
        }
    }

}