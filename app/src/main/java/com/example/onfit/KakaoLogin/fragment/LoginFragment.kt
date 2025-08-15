package com.example.onfit.KakaoLogin.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.onfit.KakaoLogin.util.TokenProvider
import com.example.onfit.R
import com.example.onfit.databinding.FragmentLoginBinding
import org.json.JSONObject

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 이미 토큰이 있다면(재방문) 로그인 과정 건너뛰고 홈으로
        val existingToken = TokenProvider.getToken(requireContext())
        if (existingToken.isNotBlank()) {
            Log.d("LoginWebView", "기존 토큰 발견 → 로그인 화면 스킵")
            findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
            return
        }

        // WebView 설정
        binding.webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }
        binding.webView.webChromeClient = WebChromeClient()
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(binding.webView, true)
        }

        // 콜백 처리: 로그인 성공 → 토큰 저장 → 약관 화면으로 이동
        binding.webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                if (url != null && url.contains("/user/auth/kakao/callback")) {
                    view?.evaluateJavascript("(function() { return document.body.innerText; })();") { html ->
                        try {
                            val clean = html.trim('"').replace("\\n", "").replace("\\", "")
                            val root = JSONObject(clean)
                            if (!root.getBoolean("isSuccess")) {
                                val msg = root.optString("message", "로그인 실패")
                                showErrorDialog("로그인 실패: $msg")
                                return@evaluateJavascript
                            }

                            val result = root.getJSONObject("result")
                            val token = result.getString("token")
                            val nickname = result.optString("nickname", "").trim() // 있으면 저장

                            // 토큰/닉네임 로컬 저장
                            TokenProvider.saveToken(requireContext(), token)
                            if (nickname.isNotEmpty()) {
                                TokenProvider.saveNickname(requireContext(), nickname)
                            }
                            Log.d("LoginWebView", "토큰/닉네임 저장 완료")

                            // 신규 가입 플로우로 강제 진입: 약관 화면
                            findNavController().navigate(R.id.action_loginFragment_to_termsFragment)

                        } catch (e: Exception) {
                            Log.e("LoginWebView", "콜백 파싱 오류", e)
                            showErrorDialog("로그인 처리 중 오류가 발생했습니다.")
                        }
                    }
                }
            }
        }

        // 로그인 페이지 로딩 (개발용 HTTP; manifest의 networkSecurityConfig로 허용되어 있어야 함)
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
