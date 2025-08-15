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
        super.onViewCreated(view, savedInstanceState)

        // 1) 현재 상태 조회
        val token = TokenProvider.getToken(requireContext())
        val hasToken = !token.isNullOrBlank()
        val hasNickname = TokenProvider.getNickname(requireContext()).isNotBlank()
        val hasLocation = TokenProvider.getLocation(requireContext()).isNotBlank()

        // 2) 자동 분기 (버튼 누를 필요 없이 즉시 이동)
        val nav = findNavController()
        if (hasToken && hasNickname && hasLocation) {
            if (nav.currentDestination?.id == R.id.loginStartFragment) {
                nav.navigate(R.id.action_loginStartFragment_to_homeFragment)
            }
            return
        }
        if (hasToken && !hasNickname) {
            if (nav.currentDestination?.id == R.id.loginStartFragment) {
                // 닉네임 설정 화면으로
                nav.navigate(R.id.action_loginStartFragment_to_nicknameFragment)
            }
            return
        }
        if (hasToken && hasNickname && !hasLocation) {
            if (nav.currentDestination?.id == R.id.loginStartFragment) {
                // 위치 설정 화면으로 (온보딩 플로우이므로 fromHome = false)
                val action = LoginStartFragmentDirections.actionLoginStartFragmentToLocationSettingFragment()
                action.fromHome = false // 또는 action.setFromHome(false)
                nav.navigate(action)
            }
            return
        }

        // 3) 토큰이 없을 때만 "카카오 로그인" 버튼 노출 → 로그인 화면으로
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
