package com.example.onfit.KakaoLogin.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
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

        // 1) 토큰이 이미 있으면 로그인 화면 건너뛰기
        val existingToken = TokenProvider.getToken(requireContext())
        if (!existingToken.isNullOrBlank()) {
            Log.d("LoginWebView", "기존 토큰 발견 → 로그인 화면 스킵")
            findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
            return
        }


        // 2) 토큰이 없을 때만 WebView 로그인 진행
        val webSettings = binding.webView.settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        // 혼합 콘텐츠 허용(HTTPS 페이지에서 HTTP 콜백 리다이렉트가 있을 수 있으므로)
        webSettings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

        binding.webView.webChromeClient = WebChromeClient()

        // 쿠키는 로그인 유지에 필요 → 전역 삭제는 하지 않음 (로그아웃에서 처리)
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(binding.webView, true)

        binding.webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                Log.d("LoginWebView", "onPageFinished URL: $url")

                // 콜백 URL 도달 시 JSON 파싱 (기존 로직 유지)
                if (url != null && url.contains("/user/auth/kakao/callback")) {
                    view?.evaluateJavascript(
                        "(function() { return document.body.innerText; })();"
                    ) { html ->
                        Log.d("LoginWebView", "최종 응답: $html")
                        try {
                            val cleanJson = html
                                .trim('"')
                                .replace("\\n", "")
                                .replace("\\", "")

                            val json = JSONObject(cleanJson)
                            val isSuccess = json.getBoolean("isSuccess")

                            if (isSuccess) {
                                val result = json.getJSONObject("result")
                                val token = result.getString("token")

                                // 토큰 저장
                                TokenProvider.saveToken(requireContext(), token)
                                Log.d("LoginWebView", "토큰 저장됨: $token")

                                // 홈 화면으로 이동
                                findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
                            }
                            else {
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

        // 3) 로그인 페이지 로딩 (기존 HTTPS 주소 유지)
        binding.webView.loadUrl("https://15.164.35.198:3000/user/auth/kakao")
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
