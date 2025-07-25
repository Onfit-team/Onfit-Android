package com.example.onfit.HomeRegister.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.onfit.databinding.FragmentHomeAiSaveBinding

class HomeAiSaveFragment : Fragment() {

    private var _binding: FragmentHomeAiSaveBinding? = null  // 기존 layout 이름 그대로 사용
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeAiSaveBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 뒤로가기 버튼
        binding.outfitSaveBackBtn.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // 저장 버튼 클릭 → 저장 완료 메시지만 출력
        binding.outfitSaveSaveBtn.setOnClickListener {
            Toast.makeText(requireContext(), "Outfit 저장 완료!", Toast.LENGTH_SHORT).show()
            requireActivity().finish() // 플로우 종료
        }

        // 기타: 카테고리/계절/색상 선택, ChipGroup, EditText 등은 그대로 작동
        // 필요한 로직은 기존 OutfitSaveActivity 그대로 옮기면 됨
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
