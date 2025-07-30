package com.example.onfit.KakaoLogin.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.onfit.KakaoLogin.util.TokenManager
import com.example.onfit.KakaoLogin.util.TokenProvider
import com.example.onfit.R
import com.example.onfit.databinding.FragmentLoginBinding
import org.json.JSONObject
import android.webkit.CookieManager

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val kakaoLoginUrl = "http://15.164.35.198:3000/user/auth/kakao"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }



    private fun clearWebViewSession() {
        // 웹뷰 세션, 쿠키, 캐시 제거 → 자동 로그인 방지
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
        binding.kakaoWebView.clearCache(true)
        binding.kakaoWebView.clearHistory()
    }



    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //앱 실행시 마다 카카오톡 재로그인 할 수 있게 해놓음
        clearWebViewSession()

        val webView = binding.kakaoWebView
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true

        webView.webViewClient = object : WebViewClient() {

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)

                // HTML 내부 <pre> 태그의 텍스트(JSON)를 추출
                view?.evaluateJavascript(
                    "(function() { return document.getElementsByTagName('pre')[0].innerText; })();"
                ) { result ->
                    try {
                        // result는 문자열로 넘어오므로 양쪽 큰따옴표 제거
                        val cleanedJson = result.removePrefix("\"").removeSuffix("\"")
                            .replace("\\n", "") // 줄바꿈 제거
                            .replace("\\", "")  // 이스케이프 제거

                        val jsonObject = JSONObject(cleanedJson)
                        val success = jsonObject.optBoolean("isSuccess")
                        val token = jsonObject.optJSONObject("result")?.optString("token")

                        if (success && !token.isNullOrEmpty()) {
                            TokenManager.saveToken(requireContext(), token)

                            TokenProvider.token = token

                            findNavController().navigate(R.id.action_loginFragment_to_termsFragment)
                        } else {
                            Toast.makeText(requireContext(), "로그인 실패", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Log.e("KakaoWeb", "JSON 파싱 오류: ${e.message}")
                    }
                }
            }
        }

        webView.loadUrl(kakaoLoginUrl)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
