package com.example.onfit.KakaoLogin.fragment

import android.os.Bundle
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

        binding.webView.settings.javaScriptEnabled = true
        binding.webView.webChromeClient = WebChromeClient()
        binding.webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)

                view?.evaluateJavascript(
                    "(function() { return document.body.innerText; })();"
                ) { html ->
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

                            // ✅ 토큰 저장
                            TokenProvider.saveToken(requireContext(), token)

                            // ✅ 약관 동의 화면으로 이동 (nickname 여부 상관없이)
                            findNavController().navigate(R.id.action_loginFragment_to_termsFragment)
                        } else {
                            showErrorDialog("로그인 실패: ${json.getString("message")}")
                        }

                    } catch (e: Exception) {
                        e.printStackTrace()
                        showErrorDialog("로그인 처리 중 오류가 발생했습니다.")
                    }
                }
            }
        }

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
