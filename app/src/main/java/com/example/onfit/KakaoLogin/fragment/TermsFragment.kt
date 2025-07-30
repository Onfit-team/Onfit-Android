package com.example.onfit.KakaoLogin.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.onfit.R
import com.example.onfit.databinding.FragmentTermsBinding

class TermsFragment : Fragment() {

    private var _binding: FragmentTermsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTermsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 전체 동의 → 개별 체크 모두 체크
        binding.checkboxAll.setOnCheckedChangeListener { _, isChecked ->
            binding.checkboxService.isChecked = isChecked
            binding.checkboxPrivacy.isChecked = isChecked
        }

        // 개별 체크 시 → 다음 버튼 활성화 여부만 갱신
        val listener = View.OnClickListener {
            val allChecked = binding.checkboxService.isChecked && binding.checkboxPrivacy.isChecked
            binding.btnNext.isEnabled = allChecked
        }
        binding.checkboxService.setOnClickListener(listener)
        binding.checkboxPrivacy.setOnClickListener(listener)

        // 전체 동의 → 모든 체크박스 + 다음 버튼 활성화
        binding.checkboxAll.setOnCheckedChangeListener { _, isChecked ->
            binding.checkboxService.isChecked = isChecked
            binding.checkboxPrivacy.isChecked = isChecked
            binding.btnNext.isEnabled = isChecked
        }


        // 약관 상세 보기 TODO
        binding.textService.setOnClickListener {
            // TODO: 약관 상세보기 Fragment로 이동
        }
        binding.textPrivacy.setOnClickListener {
            // TODO: 개인정보 처리방침 Fragment로 이동
        }

        //다음 버튼 클릭 시 NicknameFragment로 이동
        binding.btnNext.setOnClickListener {
            findNavController().navigate(R.id.action_termsFragment_to_nicknameFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
