package com.example.onfit.KakaoLogin.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.onfit.databinding.FragmentTermsDetailBinding
import com.example.onfit.R

class TermsDetailFragment : Fragment() {

    private var _binding: FragmentTermsDetailBinding? = null
    private val binding get() = _binding!!

    companion object {
        const val TYPE_SERVICE = "service"
        const val TYPE_POLICY = "policy"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTermsDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 전달받은 type에 따라 텍스트 분기
        val type = arguments?.getString("type") ?: TYPE_SERVICE
        if (type == TYPE_SERVICE) {
            binding.tvTitle.text = "[필수] 서비스 이용약관"
            binding.tvContent.text = HtmlCompat.fromHtml(
                getString(R.string.service_terms_text),
                HtmlCompat.FROM_HTML_MODE_LEGACY
            )
        } else {
            binding.tvTitle.text = "[필수] 개인정보 처리방침"
            binding.tvContent.text = HtmlCompat.fromHtml(
                getString(R.string.privacy_policy_text),
                HtmlCompat.FROM_HTML_MODE_LEGACY
            )
        }


        // 뒤로가기 버튼
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
