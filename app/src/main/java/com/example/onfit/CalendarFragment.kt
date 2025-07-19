package com.example.onfit

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.onfit.databinding.FragmentCalendarBinding
import com.example.onfit.databinding.FragmentHomeBinding

class CalendarFragment : Fragment(R.layout.fragment_calendar) {
    private var _binding: FragmentCalendarBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentCalendarBinding.bind(view)

        // CalendarSaveActivity로 이동
        binding.calendarRegisterBtn.setOnClickListener {
            val intent = Intent(requireContext(), CalendarSaveActivity::class.java)
            startActivity(intent)
        }
    }
}