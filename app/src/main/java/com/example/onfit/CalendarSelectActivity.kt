package com.example.onfit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.onfit.databinding.ActivityCalendarSelectBinding

class CalendarSelectActivity : AppCompatActivity() {
    lateinit var binding: ActivityCalendarSelectBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityCalendarSelectBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportFragmentManager.beginTransaction()
            .replace(R.id.calendar_select_fragment_container, WardrobeSelectFragment())
            .commit()

        binding.calendarSelectBackBtn.setOnClickListener {
            finish()
        }
    }
}