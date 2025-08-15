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

        // 1) 기존 토큰 있으면 홈으로
        val existingToken = TokenProvider.getToken(requireContext())
        if (!existingToken.isNullOrBlank()) {
            Log.d("LoginWebView", "기존 토큰 발견 → 로그인 화면 스킵")
            findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
            return
        }

        // 2) WebView 설정
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

        binding.webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                Log.d("LoginWebView", "onPageFinished URL: $url")

                if (url != null && url.contains("/user/auth/kakao/callback")) {
                    view?.evaluateJavascript("(function() { return document.body.innerText; })();") { html ->
                        Log.d("LoginWebView", "최종 응답: $html")
                        try {
                            val cleanJson = html.trim('"')
                                .replace("\\n", "")
                                .replace("\\", "")
                            val json = JSONObject(cleanJson)
                            val isSuccess = json.getBoolean("isSuccess")
                            if (isSuccess) {
                                val result = json.getJSONObject("result")
                                val token = result.getString("token")
                                // ↓↓↓ 닉네임이 있으면 저장(없으면 무시)
                                val nickname = result.optString("nickname", "").trim()

                                TokenProvider.saveToken(requireContext(), token)
                                if (nickname.isNotEmpty()) {
                                    TokenProvider.saveNickname(requireContext(), nickname)
                                }
                                Log.d("LoginWebView", "토큰/닉네임 저장됨: token=${token.take(10)}..., nickname=$nickname")

                                findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
                            } else {
                                val message = json.optString("message", "로그인 실패")
                                showErrorDialog("로그인 실패: $message")
                            }
                        } catch (e: Exception) {
                            Log.e("LoginWebView", "JSON 파싱 오류", e)
                            showErrorDialog("로그인 처리 중 오류가 발생했습니다.")
                        }
                    }
                }
            }
        }

        // 3) 로그인 페이지 로딩 (개발용 http)
        //binding.webView.loadUrl("http://15.164.35.198:3000/user/auth/kakao")
        binding.webView.loadUrl("http://3.36.113.173/user/auth/kakao")
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
