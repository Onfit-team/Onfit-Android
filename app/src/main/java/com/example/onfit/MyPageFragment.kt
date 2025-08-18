package com.example.onfit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.example.onfit.KakaoLogin.util.NicknameProvider
import com.example.onfit.KakaoLogin.util.TokenProvider
import com.example.onfit.databinding.FragmentMyPageBinding

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
        renderNickname()
        // 레이아웃에 itemLogout가 존재하는 디자인이면 클릭 연결
        val itemLogoutId = resources.getIdentifier("itemLogout", "id", requireContext().packageName)
        if (itemLogoutId != 0) {
            binding.root.findViewById<View>(itemLogoutId)?.setOnClickListener { confirmLogout() }
        }
    }

    private fun renderNickname() {
        val nicknameLocal = TokenProvider.getNickname(requireContext()).trim()
        val nicknameFallback = NicknameProvider.nickname?.trim().orEmpty()
        val nickname = if (nicknameLocal.isNotEmpty()) nicknameLocal else nicknameFallback

        val tvTitleId = resources.getIdentifier("tvTitle", "id", requireContext().packageName)
        if (tvTitleId != 0 && nickname.isNotEmpty()) {
            val tvTitle = binding.root.findViewById<android.widget.TextView>(tvTitleId)
            tvTitle?.text = "${nickname}님의 Outfit"
        }
    }

    private fun confirmLogout() {
        AlertDialog.Builder(requireContext())
            .setMessage("로그아웃 하시겠습니까?")
            .setNegativeButton("아니오", null)
            .setPositiveButton("예") { _, _ ->
                // 저장된 로그인 정보 삭제
                TokenProvider.saveToken(requireContext(), "")
                TokenProvider.saveNickname(requireContext(), "")
                TokenProvider.setLocation(requireContext(), "")
                NicknameProvider.nickname = null

                // (선택) 쿠키도 정리
                runCatching {
                    val cm = CookieManager.getInstance()
                    cm.removeAllCookies(null)
                    cm.flush()
                }

                // 백스택 비우고 시작화면으로
                val nav = findNavController()
                val options = NavOptions.Builder()
                    .setPopUpTo(nav.graph.id, true)
                    .build()
                nav.navigate(R.id.loginStartFragment, null, options)
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
