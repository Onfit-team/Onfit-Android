package com.example.onfit

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import com.example.onfit.databinding.FragmentHomeBinding
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.onfit.data.model.BestItem
import com.example.onfit.data.model.SimItem

// fragment_home.xml을 사용하는 HomeFragment 정의
class HomeFragment : Fragment(R.layout.fragment_home) {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    //홈 화면 옷 리스트
    private val clothSuggestList = listOf(
        R.drawable.cloth1,
        R.drawable.cloth2,
        R.drawable.cloth3
    )
