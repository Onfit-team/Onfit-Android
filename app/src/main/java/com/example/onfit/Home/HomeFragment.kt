package com.example.onfit.Home

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.onfit.LatestStyleAdapter
import com.example.onfit.R
import com.example.onfit.Home.model.BestItem
import com.example.onfit.Home.model.SimItem
import com.example.onfit.databinding.FragmentHomeBinding
import com.example.onfit.Home.adapter.BestOutfitAdapter
import com.example.onfit.Home.adapter.SimiliarStyleAdapter
import com.example.onfit.Home.viewmodel.HomeViewModel
import com.example.onfit.KakaoLogin.util.TokenProvider
import com.example.onfit.RegisterActivity
import com.google.android.material.bottomsheet.BottomSheetDialog

class HomeFragment : Fragment(R.layout.fragment_home) {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()
    private var isShortText = false

    private val clothSuggestList = listOf(
        R.drawable.cloth1, R.drawable.cloth2, R.drawable.cloth3
    )

    private val similiarClothList = listOf(
        SimItem(R.drawable.simcloth1, "딱 좋음"),
        SimItem(R.drawable.simcloth2, "조금 추움"),
        SimItem(R.drawable.simcloth3, "많이 더움"),
        SimItem(R.drawable.simcloth1, "딱 좋음"),
        SimItem(R.drawable.simcloth2, "조금 추움"),
        SimItem(R.drawable.simcloth3, "많이 더움")
    )

    private val latestStyleList = listOf(
        SimItem(R.drawable.simcloth2, "4월 20일"),
        SimItem(R.drawable.simcloth3, "4월 19일"),
        SimItem(R.drawable.simcloth1, "4월 18일"),
        SimItem(R.drawable.simcloth2, "4월 17일"),
        SimItem(R.drawable.simcloth3, "4월 16일"),
        SimItem(R.drawable.simcloth1, "4월 15일")
    )

    private val bestStyleList = listOf(
        BestItem(R.drawable.bestcloth1, "TOP 1", "큐야"),
        BestItem(R.drawable.bestcloth2, "TOP 2", "별이"),
        BestItem(R.drawable.bestcloth3, "TOP 3", "금이")
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHomeBinding.bind(view)

        val token = TokenProvider.getToken(requireContext())

        // ✅ 홈 진입 시 현재 날씨 자동 호출
        viewModel.fetchCurrentWeather(token)

        // ✅ 내일 날씨 버튼 클릭 시
        binding.weatherBtn.setOnClickListener {
            viewModel.fetchTomorrowWeather(token)
        }

        viewModel.weatherLiveData.observe(viewLifecycleOwner) { weatherResult ->
            val location = weatherResult.location
            val weather = weatherResult.weather

            binding.locateTv.text = "${location.sido} ${location.sigungu} ${location.dong}"
            binding.weatherInformTv.text = when (weather.status) {
                "Storm" -> "뇌우"
                "Snow" -> "눈"
                "Rain" -> "비"
                "Fog" -> "안개"
                "CloudFew" -> "구름 조금"
                "CloudMany" -> "구름 많음"
                "CloudBroken" -> "흐림"
                "Sun" -> "맑음"
                else -> "알 수 없음"
            }
            binding.tempTv.text = "평균 기온 ${weather.tempAvg}°C"
        }

        viewModel.errorLiveData.observe(viewLifecycleOwner) { error ->
            Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
        }

        // 날짜 표시
        viewModel.fetchDate()
        viewModel.dateLiveData.observe(viewLifecycleOwner) { date ->
            binding.dateTv.text = date
        }

        setRandomImages()
        binding.refreshIcon.setOnClickListener { setRandomImages() }

        binding.similarStyleRecyclerView.apply {
            adapter = SimiliarStyleAdapter(similiarClothList)
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        }

        binding.latestStyleRecyclerView.apply {
            adapter = LatestStyleAdapter(latestStyleList)
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        }

        binding.bestoutfitRecycleView.apply {
            adapter = BestOutfitAdapter(bestStyleList)
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        }

        val scrollView = binding.homeSv
        val registerTv = binding.homeRegisterTv
        scrollView.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            if (scrollY > 50 && !isShortText) {
                animateTextChange(registerTv, "+")
                isShortText = true
            } else if (scrollY <= 50 && isShortText) {
                animateTextChange(registerTv, "+ 등록하기")
                isShortText = false
            }
        }

        binding.homeRegisterBtn.setOnClickListener {
            showBottomSheet()
        }
    }

    private fun setRandomImages() {
        val mix = clothSuggestList.shuffled().take(3)
        binding.suggestedCloth1Iv.setImageResource(mix[0])
        binding.suggestedCloth2Iv.setImageResource(mix[1])
        binding.suggestedCloth3Iv.setImageResource(mix[2])
    }

    private fun showBottomSheet() {
        val view = layoutInflater.inflate(R.layout.bottom_sheet_dialog, null)
        val dialog = BottomSheetDialog(requireContext())
        dialog.setContentView(view)

        view.findViewById<LinearLayout>(R.id.camera_btn).setOnClickListener {
            val intent = Intent(requireContext(), RegisterActivity::class.java)
            startActivity(intent)
        }

        view.findViewById<LinearLayout>(R.id.gallery_btn).setOnClickListener {
            Toast.makeText(requireContext(), "사진첩 클릭됨", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun animateTextChange(textView: TextView, newText: String) {
        val fadeOut = ObjectAnimator.ofFloat(textView, "alpha", 1f, 0f)
        val fadeIn = ObjectAnimator.ofFloat(textView, "alpha", 0f, 1f)
        fadeOut.duration = 150
        fadeIn.duration = 150
        fadeOut.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                textView.text = newText
                fadeIn.start()
            }
        })
        fadeOut.start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
