package com.example.onfit.KakaoLogin.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.onfit.KakaoLogin.api.KakaoAuthService
import com.example.onfit.KakaoLogin.model.SignUpRequest
import com.example.onfit.KakaoLogin.util.NicknameProvider
import com.example.onfit.KakaoLogin.util.TokenProvider
import com.example.onfit.R
import com.example.onfit.databinding.FragmentLoginBinding
import com.example.onfit.network.RetrofitInstance
import kotlinx.coroutines.launch
import org.json.JSONObject

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val kakaoAuthService: KakaoAuthService by lazy {
        RetrofitInstance.kakaoApi
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.webView.settings.javaScriptEnabled = true
        binding.webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)

                // 웹 페이지 로딩 완료 후 HTML 내 JSON 파싱
                view?.evaluateJavascript(
                    "(function() { return document.body.innerText; })();"
                ) { html ->
                    try {
                        val json = JSONObject(html.trim('"').replace("\\n", "").replace("\\", ""))
                        val token = json.getString("token")

                        // 토큰 저장
                        TokenProvider.saveToken(requireContext(), token)

                        // 닉네임이 이미 설정된 경우 서버에 회원가입 요청
                        val nickname = NicknameProvider.nickname
                        if (nickname != null) {
                            val request = SignUpRequest(nickname = nickname)
                            lifecycleScope.launch {
                                val response = kakaoAuthService.signUp("Bearer $token", request)
                                if (response.isSuccessful) {
                                    // 닉네임 등록 성공 후 위치 설정 화면으로 이동
                                    findNavController().navigate(R.id.action_loginFragment_to_nicknameFragment)
                                } else {
                                    showErrorDialog("회원가입 실패\n잠시 후 다시 시도해주세요.")
                                }
                            }
                        } else {
                            // 닉네임 설정 화면으로 이동
                            findNavController().navigate(R.id.action_loginFragment_to_nicknameFragment)
                        }
                    } catch (e: Exception) {
                        showErrorDialog("로그인 처리 중 오류가 발생했습니다.")
                    }
                }
            }
        }

        binding.webView.webChromeClient = WebChromeClient()
        binding.webView.loadUrl("http://15.164.35.198:3000/user/auth/kakao")
    }

    private fun showErrorDialog(message: String) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setMessage(message)
            .setPositiveButton("확인", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
