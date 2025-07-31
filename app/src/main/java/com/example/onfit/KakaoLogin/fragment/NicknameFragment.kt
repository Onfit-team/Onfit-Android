package com.example.onfit.KakaoLogin.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.onfit.KakaoLogin.api.KakaoAuthService
import com.example.onfit.KakaoLogin.util.NicknameProvider
import com.example.onfit.R
import com.example.onfit.databinding.FragmentNicknameBinding
import com.example.onfit.network.RetrofitInstance
import kotlinx.coroutines.launch

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

        val api: KakaoAuthService = RetrofitInstance.kakaoApi

        // 닉네임 입력 시 유효성 검사 및 버튼 초기화
        binding.etNickname.addTextChangedListener {
            val input = it.toString()
            isNicknameAvailable = false
            val isValid = isValidNickname(input)
            binding.btnCheckNickname.isEnabled = isValid
            binding.btnNext.isEnabled = false
        }

        // 뒤로가기
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        // 중복확인 버튼
        binding.btnCheckNickname.setOnClickListener {
            val input = binding.etNickname.text.toString().trim()
            if (isValidNickname(input)) {
                lifecycleScope.launch {
                    try {
                        val response = api.checkNickname(input)
                        if (response.isSuccessful) {
                            val result = response.body()
                            if (result?.result?.available == true) {
                                isNicknameAvailable = true
                                nickname = input
                                showNicknameDialog(true)
                            } else {
                                isNicknameAvailable = false
                                showNicknameDialog(false)
                            }
                        } else {
                            showErrorDialog("서버 오류가 발생했습니다.\n잠시 후 다시 시도해주세요.")
                        }
                    } catch (e: Exception) {
                        showErrorDialog("네트워크 오류가 발생했습니다.\n인터넷 연결을 확인해주세요.")
                    }
                }
            } else {
                showErrorDialog("닉네임 형식이 올바르지 않습니다.\n2~10자, 한글/영어/숫자만 입력 가능해요.")
            }
        }

        // 가입하기 버튼
        binding.btnNext.setOnClickListener {
            if (isNicknameAvailable) {
                NicknameProvider.nickname = nickname
                findNavController().navigate(R.id.action_nicknameFragment_to_locationSettingFragment)
            } else {
                showErrorDialog("닉네임 중복 확인을 해주세요.")
            }
        }
    }

    private fun isValidNickname(nickname: String): Boolean {
        val regex = "^[가-힣a-zA-Z0-9]{2,10}$".toRegex()
        return nickname.matches(regex)
    }

    private fun showNicknameDialog(isAvailable: Boolean) {
        val message = if (isAvailable) {
            "사용 가능한 닉네임입니다"
        } else {
            "중복된 닉네임입니다.\n다시 입력해주세요."
        }

        AlertDialog.Builder(requireContext())
            .setMessage(message)
            .setPositiveButton("확인") { dialog, _ -> dialog.dismiss() }
            .show()

        // 사용 가능할 때만 가입하기 버튼 활성화
        binding.btnNext.isEnabled = isAvailable
    }

    private fun showErrorDialog(message: String) {
        AlertDialog.Builder(requireContext())
            .setMessage(message)
            .setPositiveButton("확인", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
