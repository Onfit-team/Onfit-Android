package com.example.onfit

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.example.onfit.databinding.FragmentCalendarSelectBinding


class CalendarSelectFragment : Fragment() {
    private var _binding: FragmentCalendarSelectBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentCalendarSelectBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 처음에 WardrobeSelectFragment를 자식 프래그먼트로 붙임
        childFragmentManager.beginTransaction()
            .replace(R.id.calendar_select_fragment_container, WardrobeSelectFragment())
            .commit()

        binding.calendarSelectSaveBtn.setOnClickListener {
            val child = childFragmentManager
                .findFragmentById(R.id.calendar_select_fragment_container) as? WardrobeSelectFragment

            val selectedResIds = child?.getSelectedImages().orEmpty()
            if (selectedResIds.isEmpty()) {
                Toast.makeText(requireContext(), "이미지를 선택해 주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // ✅ 이전 화면(CalendarRewriteFragment)으로 결과 전달
            findNavController().previousBackStackEntry
                ?.savedStateHandle
                ?.set("calendar_select_result", selectedResIds)

            // 이전 화면으로 복귀
            findNavController().popBackStack()
        }

        // 뒤로가기 버튼 눌렀을 때 이전 프래그먼트로 돌아감
        binding.calendarSelectBackBtn.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}