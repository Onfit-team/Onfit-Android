package com.example.onfit.KakaoLogin.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.onfit.R
import com.example.onfit.databinding.FragmentLoginStartBinding
import com.example.onfit.KakaoLogin.util.TokenProvider

class LoginStartFragment : Fragment() {

    private var _binding: FragmentLoginStartBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginStartBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val nav = findNavController()
        val hasToken = TokenProvider.getToken(requireContext()).isNotBlank()

        if (hasToken) {
            // 저장된 로그인 정보가 있으면 바로 홈으로
            if (nav.currentDestination?.id == R.id.loginStartFragment) {
                nav.navigate(R.id.action_loginStartFragment_to_homeFragment)
            }
            return
        }

        // 토큰이 없으면 로그인 시작
        binding.KakaoLoginBtn.setOnClickListener {
            if (nav.currentDestination?.id == R.id.loginStartFragment) {
                nav.navigate(R.id.action_loginStartFragment_to_loginFragment)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
