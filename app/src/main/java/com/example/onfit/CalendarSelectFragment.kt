package com.example.onfit

import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.example.onfit.databinding.FragmentCalendarSelectBinding
import java.io.File


class CalendarSelectFragment : Fragment() {
    private var _binding: FragmentCalendarSelectBinding? = null
    private val binding get() = _binding!!
    private val args: CalendarSelectFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentCalendarSelectBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // CalendarRewriteFragment에서 이미지 받기
        val iv = binding.calendarSelectOutfitIv
        val src = args.imageSource  // 번들 키: "imageSource"

        if (src.isNullOrBlank()) return

        when {
            src.startsWith("http") -> Glide.with(iv).load(src).into(iv)
            src.startsWith("content://") || src.startsWith("file://") -> iv.setImageURI(Uri.parse(src))
            src.startsWith("res://") -> src.removePrefix("res://").toIntOrNull()?.let { iv.setImageResource(it) }
            else -> iv.setImageURI(Uri.fromFile(File(src))) // 순수 파일 경로
        }

        // 처음에 WardrobeSelectFragment를 자식 프래그먼트로 붙임
        childFragmentManager.beginTransaction()
            .replace(R.id.calendar_select_fragment_container, WardrobeSelectFragment())
            .commit()

        // 저장 버튼에서 선택값(이미지) 돌려주고 뒤로가기
        binding.calendarSelectSaveBtn.setOnClickListener {
            val child = childFragmentManager
                .findFragmentById(R.id.calendar_select_fragment_container) as? WardrobeSelectFragment

            val selectedResIds = child?.getSelectedImages().orEmpty()
            if (selectedResIds.isEmpty()) {
                Toast.makeText(requireContext(), "옷을 선택해 주세요.", Toast.LENGTH_SHORT).show()
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