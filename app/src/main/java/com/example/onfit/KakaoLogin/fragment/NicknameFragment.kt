package com.example.onfit.KakaoLogin.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.onfit.KakaoLogin.api.KakaoAuthService
import com.example.onfit.KakaoLogin.util.NicknameProvider
import com.example.onfit.databinding.FragmentNicknameBinding
import kotlinx.coroutines.launch
import com.example.onfit.R
import com.example.onfit.network.RetrofitInstance

class NicknameFragment : Fragment() {

    private var _binding: FragmentNicknameBinding? = null
    private val binding get() = _binding!!

    private var isNicknameAvailable = false
    private lateinit var nickname: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNicknameBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val api = RetrofitInstance.kakaoApi

        binding.btnCheckNickname.setOnClickListener {
            val input = binding.editNickname.text.toString().trim()
            if (isValidNickname(input)) {
                lifecycleScope.launch {
                    try {
                        val response = api.checkNickname(input)
                        if (response.isSuccessful) {
                            val body = response.body()
                            if (body?.result?.available == true) {
                                isNicknameAvailable = true
                                nickname = input
                                Toast.makeText(requireContext(), "사용 가능한 닉네임입니다.", Toast.LENGTH_SHORT).show()
                            } else {
                                isNicknameAvailable = false
                                Toast.makeText(requireContext(), "이미 사용 중인 닉네임입니다.", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(requireContext(), "서버 오류", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "네트워크 오류", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(requireContext(), "닉네임 형식이 올바르지 않습니다.", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnNext.setOnClickListener {
            if (isNicknameAvailable) {
                NicknameProvider.nickname = nickname
                findNavController().navigate(R.id.action_nicknameFragment_to_locationSettingFragment)
            } else {
                Toast.makeText(requireContext(), "닉네임 중복 확인을 해주세요.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun isValidNickname(nickname: String): Boolean {
        val regex = "^[가-힣a-zA-Z0-9]{2,10}$".toRegex()
        return nickname.matches(regex)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}