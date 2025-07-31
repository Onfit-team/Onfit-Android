package com.example.onfit.KakaoLogin.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.onfit.R
import com.example.onfit.databinding.FragmentTermsBinding

class TermsFragment : Fragment() {

    private var _binding: FragmentTermsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTermsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // "전체 동의" 체크박스 클릭 시
        binding.checkboxAll.setOnCheckedChangeListener { _, isChecked ->
            binding.checkboxPrivacy.isChecked = isChecked
            binding.checkboxService.isChecked = isChecked
            binding.checkboxPolicy.isChecked = isChecked
        }

        // 개별 체크박스 클릭 시 전체 체크 여부 갱신
        val listener = CompoundButton.OnCheckedChangeListener { _, _ ->
            val allChecked = binding.checkboxPrivacy.isChecked &&
                    binding.checkboxService.isChecked &&
                    binding.checkboxPolicy.isChecked
            binding.checkboxAll.isChecked = allChecked
            binding.btnNext.isEnabled = allChecked
        }

        binding.checkboxPrivacy.setOnCheckedChangeListener(listener)
        binding.checkboxService.setOnCheckedChangeListener(listener)
        binding.checkboxPolicy.setOnCheckedChangeListener(listener)

        // 이용약관 보기 클릭
        binding.btnServiceDetail.setOnClickListener {
            findNavController().navigate(R.id.action_termsFragment_to_termsDetailFragment_service)
        }

        // 개인정보 처리방침 보기 클릭
        binding.btnPolicyDetail.setOnClickListener {
            findNavController().navigate(R.id.action_termsFragment_to_termsDetailFragment_policy)
        }

        // 다음 단계로 이동
        binding.btnNext.setOnClickListener {
            findNavController().navigate(R.id.action_termsFragment_to_nicknameFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
