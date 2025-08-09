package com.example.onfit.Home

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.onfit.Home.adapter.BestOutfitAdapter
import com.example.onfit.Home.adapter.LatestStyleAdapter
import com.example.onfit.Home.adapter.SimiliarStyleAdapter
import com.example.onfit.Home.model.SimItem
import com.example.onfit.Home.viewmodel.HomeViewModel
import com.example.onfit.KakaoLogin.util.TokenProvider
import com.example.onfit.R
import com.example.onfit.RegisterActivity
import com.example.onfit.databinding.FragmentHomeBinding
import com.example.onfit.network.RetrofitInstance
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

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
        SimItem(R.drawable.simcloth3, "많이 더움")
    )

    override fun onResume() {
        super.onResume()
        fetchCurrentWeather()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHomeBinding.bind(view)

        val token = TokenProvider.getToken(requireContext())


        // 닉네임 가져와서 sim_text_tv에 표시
        val nickname = TokenProvider.getNickname(requireContext())
        if (nickname.isNotEmpty()) {
            binding.simTextTv.text = "비슷한 날, ${nickname}님의 스타일"
        } else {
            binding.simTextTv.text = "비슷한 날, 회원님의 스타일"
        }


        // 지난 7일 스타일
        viewModel.fetchRecentOutfits(token)
        viewModel.recentOutfits.observe(viewLifecycleOwner) { outfits ->
            if (outfits.isNullOrEmpty()) {
                binding.latestStyleEmptyTv.visibility = View.VISIBLE
                binding.latestStyleRecyclerView.visibility = View.GONE
            } else {
                binding.latestStyleEmptyTv.visibility = View.GONE
                binding.latestStyleRecyclerView.visibility = View.VISIBLE
                binding.latestStyleRecyclerView.apply {
                    adapter = LatestStyleAdapter(outfits)
                    layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                }
            }
        }


        // BEST OUTFIT 3
        viewModel.fetchBestOutfits(token)
        viewModel.bestOutfitList.observe(viewLifecycleOwner) { outfitList ->
            Log.d("BestOutfit", "bestOutfit size=${outfitList.size}")

            // 빈 상태/리스트 토글
            if (outfitList.isNullOrEmpty()) {
                binding.bestOutfitEmptyTv.visibility = View.VISIBLE
                binding.bestoutfitRecycleView.visibility = View.GONE
            } else {
                binding.bestOutfitEmptyTv.visibility = View.GONE
                binding.bestoutfitRecycleView.visibility = View.VISIBLE
                binding.bestoutfitRecycleView.apply {
                    adapter = BestOutfitAdapter(outfitList)
                    layoutManager = LinearLayoutManager(
                        context,
                        LinearLayoutManager.HORIZONTAL,
                        false
                    )
                }
            }
        }

        // 날짜 표시
        viewModel.fetchDate()
        viewModel.dateLiveData.observe(viewLifecycleOwner) {
            updateCombinedInfo(it, TokenProvider.getLocation(requireContext()))
        }

        // 에러 표시
        viewModel.errorLiveData.observe(viewLifecycleOwner) {
            Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
        }

        // 비슷한 스타일 RecyclerView
        binding.similarStyleRecyclerView.apply {
            adapter = SimiliarStyleAdapter(similiarClothList)
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        }

        // 이미지 추천 3개
        setRandomImages()
        binding.refreshIcon.setOnClickListener { setRandomImages() }

        // 스크롤에 따라 FAB 텍스트 변경
        binding.homeSv.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            if (scrollY > 50 && !isShortText) {
                animateTextChange(binding.homeRegisterTv, "+")
                isShortText = true
            } else if (scrollY <= 50 && isShortText) {
                animateTextChange(binding.homeRegisterTv, "+ 등록하기")
                isShortText = false
            }
        }

        // 위치 버튼
        binding.locationBtn.setOnClickListener {
            val action = HomeFragmentDirections.actionHomeFragmentToLocationSettingFragment(true)
            findNavController().navigate(action)
        }

        // 내일 날씨 버튼
        binding.weatherBtn.setOnClickListener {
            fetchTomorrowWeather()
        }

        // 등록하기 버튼
        binding.homeRegisterBtn.setOnClickListener {
            showBottomSheet()
        }
    }

    private fun fetchCurrentWeather() {
        val token = TokenProvider.getToken(requireContext())
        val location = TokenProvider.getLocation(requireContext())
        lifecycleScope.launch {
            try {
                val response = RetrofitInstance.api.getCurrentWeather("Bearer $token")
                if (response.isSuccessful) {
                    val weather = response.body()?.result?.weather
                    val tempMax = weather?.tempMax?.toInt() ?: 0
                    val tempMin = weather?.tempMin?.toInt() ?: 0
                    val precipitation = weather?.precipitation?.toInt() ?: 0
                    val tempAvg = weather?.tempAvg?.toInt() ?: 0
                    val status = weather?.status ?: "Unknown"

                    updateCombinedInfo(getTodayDateString(), location)

                    binding.weatherInformTv.text = "최고 ${tempMax}°C · 최저 ${tempMin}°C · 강수확률 ${precipitation}%"
                    binding.tempTv.text = "${tempAvg}°C"

                    val fullText = "오늘 ${tempAvg}°C, 딱 맞는 스타일이에요!"
                    val targetText = "${tempAvg}°C"
                    val spannable = SpannableString(fullText)
                    val startIndex = fullText.indexOf(targetText)
                    val endIndex = startIndex + targetText.length
                    val color = ContextCompat.getColor(requireContext(), R.color.basic_blue)
                    spannable.setSpan(
                        ForegroundColorSpan(color),
                        startIndex,
                        endIndex,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    binding.weatherTitle.text = spannable

                    updateWeatherImages(status)
                } else {
                    binding.weatherInformTv.text = "날씨 정보를 가져오지 못했습니다."
                    binding.tempTv.text = ""
                }
            } catch (e: Exception) {
                binding.weatherInformTv.text = "날씨 오류: ${e.message}"
                binding.tempTv.text = ""
            }
        }
    }

    private fun fetchTomorrowWeather() {
        val token = TokenProvider.getToken(requireContext())
        val location = TokenProvider.getLocation(requireContext())
        lifecycleScope.launch {
            try {
                val response = RetrofitInstance.api.getTomorrowWeather("Bearer $token")
                if (response.isSuccessful) {
                    val weather = response.body()?.result?.weather
                    val tempMax = weather?.tempMax?.toInt() ?: 0
                    val tempMin = weather?.tempMin?.toInt() ?: 0
                    val precipitation = weather?.precipitation?.toInt() ?: 0
                    val tempAvg = weather?.tempAvg?.toInt() ?: 0
                    val status = weather?.status ?: "Unknown"

                    updateCombinedInfo(getTomorrowDateString(), location)

                    binding.weatherInformTv.text = "최고 ${tempMax}°C · 최저 ${tempMin}°C · 강수확률 ${precipitation}%"
                    binding.tempTv.text = "${tempAvg}°C"

                    val fullText = "내일 ${tempAvg}°C, 어떤 스타일일까요?"
                    val targetText = "${tempAvg}°C"
                    val spannable = SpannableString(fullText)
                    val startIndex = fullText.indexOf(targetText)
                    val endIndex = startIndex + targetText.length
                    val color = ContextCompat.getColor(requireContext(), R.color.basic_blue)
                    spannable.setSpan(
                        ForegroundColorSpan(color),
                        startIndex,
                        endIndex,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    binding.weatherTitle.text = spannable

                    updateWeatherImages(status)
                } else {
                    binding.weatherInformTv.text = "내일 날씨 정보를 가져오지 못했습니다."
                    binding.tempTv.text = ""
                }
            } catch (e: Exception) {
                binding.weatherInformTv.text = "내일 날씨 오류: ${e.message}"
                binding.tempTv.text = ""
            }
        }
    }

    private fun updateWeatherImages(status: String) {
        when (status) {
            "Storm" -> {
                binding.sunIv.setImageResource(R.drawable.weather_storm)
                binding.sunnyIv.setImageResource(R.drawable.weather_storm_bg)
            }
            "Snow" -> {
                binding.sunIv.setImageResource(R.drawable.weather_snow)
                binding.sunnyIv.setImageResource(R.drawable.weather_snow_bg)
            }
            "Rain" -> {
                binding.sunIv.setImageResource(R.drawable.weather_rain)
                binding.sunnyIv.setImageResource(R.drawable.weather_rain_bg)
            }
            "Fog" -> {
                binding.sunIv.setImageResource(R.drawable.weather_fog)
                binding.sunnyIv.setImageResource(R.drawable.weather_fog_bg)
            }
            "CloudFew" -> {
                binding.sunIv.setImageResource(R.drawable.weather_cloudfew)
                binding.sunnyIv.setImageResource(R.drawable.weather_cloudfew_bg)
            }
            "CloudMany" -> {
                binding.sunIv.setImageResource(R.drawable.weather_manycloud)
                binding.sunnyIv.setImageResource(R.drawable.weather_manycloud_bg)
            }
            "CloudBroken" -> {
                binding.sunIv.setImageResource(R.drawable.weather_brokencloud)
                binding.sunnyIv.setImageResource(R.drawable.weather_brokencloud_bg)
            }
            "Sun" -> {
                binding.sunIv.setImageResource(R.drawable.weather_sun)
                binding.sunnyIv.setImageResource(R.drawable.weather_sun_bg)
            }
            else -> {
                binding.sunIv.setImageResource(R.drawable.weather_sun)
                binding.sunnyIv.setImageResource(R.drawable.weather_sun_bg)
            }
        }
    }

    private fun updateCombinedInfo(date: String, location: String) {
        binding.combinedInfoTv.text = "$date $location 날씨"
    }

    private fun getTodayDateString(): String {
        val calendar = Calendar.getInstance()
        val format = SimpleDateFormat("M월 d일", Locale.KOREA)
        return format.format(calendar.time)
    }

    private fun getTomorrowDateString(): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DATE, 1)
        val format = SimpleDateFormat("M월 d일", Locale.KOREA)
        return format.format(calendar.time)
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