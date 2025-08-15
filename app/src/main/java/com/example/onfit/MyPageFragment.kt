package com.example.onfit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.example.onfit.databinding.FragmentMyPageBinding
import com.example.onfit.KakaoLogin.util.TokenProvider
import com.example.onfit.KakaoLogin.util.NicknameProvider

class MyPageFragment : Fragment() {

    private var _binding: FragmentMyPageBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyPageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        renderNickname()
        binding.itemLogout.setOnClickListener { showLogoutDialog() }
    }

    private fun renderNickname() {
        val local = TokenProvider.getNickname(requireContext()).trim()
        val fallback = NicknameProvider.nickname?.trim().orEmpty()
        val nickname = if (local.isNotEmpty()) local else fallback
        if (nickname.isNotEmpty()) binding.tvTitle.text = "${nickname}님의 Outfit"
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(requireContext())
            .setMessage("로그아웃 하시겠습니까?")
            .setNegativeButton("아니오", null)
            .setPositiveButton("예") { _, _ ->
                // 1) 로컬 상태 초기화 (팀에서 사용하는 저장소만 호출)
                TokenProvider.saveToken(requireContext(), "")
                TokenProvider.saveNickname(requireContext(), "")
                TokenProvider.setLocation(requireContext(), "")
                NicknameProvider.nickname = null

                // 2) 백스택 전체 제거 후 로그인 시작 화면으로 이동
                val navController = findNavController()
                val navOptions = NavOptions.Builder()
                    .setPopUpTo(navController.graph.id, true)
                    .build()
                navController.navigate(R.id.loginStartFragment, null, navOptions)
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
