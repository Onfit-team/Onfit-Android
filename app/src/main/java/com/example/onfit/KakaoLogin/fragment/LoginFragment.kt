package com.example.onfit.KakaoLogin.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.onfit.KakaoLogin.util.TokenProvider
import com.example.onfit.R
import com.example.onfit.databinding.FragmentLoginBinding
import org.json.JSONObject
import android.webkit.CookieManager
import android.webkit.WebResourceRequest

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

        // 쿠키 초기화 (로그인 캐시 제거)
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()

        // WebView 설정
        binding.webView.settings.javaScriptEnabled = true
        binding.webView.settings.domStorageEnabled = true
        binding.webView.webChromeClient = WebChromeClient()
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(binding.webView, true)

        // WebViewClient 설정
        binding.webView.webViewClient = object : WebViewClient() {

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                Log.d("LoginWebView", "onPageFinished URL: $url")

                // 로그인 완료 후 리디렉트 URL 도달 시 JSON 파싱
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

                                // 약관 동의 화면으로 이동
                                findNavController().navigate(R.id.action_loginFragment_to_termsFragment)

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

        // 로그인 페이지 로딩
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
