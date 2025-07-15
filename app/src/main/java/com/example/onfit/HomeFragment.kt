package com.example.onfit

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.onfit.databinding.FragmentHomeBinding
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.onfit.data.model.BestItem
import com.example.onfit.data.model.SimItem

// fragment_home.xml을 사용하는 HomeFragment 정의
class HomeFragment : Fragment(R.layout.fragment_home) {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private var isShortText = false

    //홈 화면 옷 리스트
    private val clothSuggestList = listOf(
        R.drawable.cloth1,
        R.drawable.cloth2,
        R.drawable.cloth3
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

    //홈 화면 옷 추천 3가지
    private fun setRandomImages() {
        val mix = clothSuggestList.shuffled().take(3)

        binding.suggestedCloth1Iv.setImageResource(mix[0])
        binding.suggestedCloth2Iv.setImageResource(mix[1])
        binding.suggestedCloth3Iv.setImageResource(mix[2])
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ViewBinding 객체 연결 (fragment_home.xml의 뷰들과 연결됨)
        _binding = FragmentHomeBinding.bind(view)
        val today = LocalDate.now()
        // 날짜를 "MM월 dd일" 형식으로 포맷 지정
        val formatter = DateTimeFormatter.ofPattern("MM월 dd일")
        // 포맷 적용된 문자열로 변환 (ex: "07월 05일")
        val formattedDate = today.format(formatter)
        binding.dateTv.text = formattedDate

        //리프레시 버튼 클릭시 이미지 교체
        binding.refreshIcon.setOnClickListener {
            setRandomImages()
        }

        // RecyclerView 어댑터 연결
        val simadapter = SimiliarStyleAdapter(similiarClothList)
        binding.similarStyleRecyclerView.adapter = simadapter
        binding.similarStyleRecyclerView.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        
        val lateadapter = LatestStyleAdapter(latestStyleList)
        binding.latestStyleRecyclerView.adapter = lateadapter
        binding.latestStyleRecyclerView.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        val bestadapter = BestOutfitAdapter(bestStyleList)
        binding.bestoutfitRecycleView.adapter = bestadapter
        binding.bestoutfitRecycleView.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        val scrollView = binding.homeSv
        val registerTv = binding.homeRegisterTv

        // 스크롤이 50 이상 내려갔을 때 버튼 텍스트 변경
        scrollView.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            if (scrollY > 50 && !isShortText) {
                animateTextChange(registerTv, "+") // 스크롤 시 "+등록하기" → "+"
                isShortText = true
            } else if (scrollY <= 50 && isShortText) {
                animateTextChange(registerTv, "+ 등록하기") // "+" → "+등록하기"
                isShortText = false
            }
        }
        
        binding.homeRegisterBtn.setOnClickListener { 
            //여기다가 add 작성
        }
    }

    // 텍스트 부드럽게 변하게 하는 애니메이션
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
