package com.example.onfit

import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.onfit.databinding.FragmentCalendarSaveBinding

class CalendarSaveFragment : Fragment() {
    private var _binding: FragmentCalendarSaveBinding? = null
    private val binding get() = _binding!!

    // 더미 데이터 (fallback용)
    private val calendarSaveList = listOf(
        CalendarSaveItem(imageResId = R.drawable.calendar_save_image2),
        CalendarSaveItem(imageResId = R.drawable.calendar_save_image3),
        CalendarSaveItem(imageResId = R.drawable.calendar_save_image4),
        CalendarSaveItem(imageResId = R.drawable.cloth2)
    )

    // ⭐ 현재 화면 값 보관(앞으로 전달에 사용)
    private var currentSelectedDate: String? = null
    private var currentMainImageUrl: String? = null
    private var currentItemImageUrls: ArrayList<String> = arrayListOf()
    private var currentWeatherText: String? = null
    private var currentMemoText: String? = null
    private var currentOutfitId: Int = -1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentCalendarSaveBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ⭐ 전달받은 데이터 확인
        val selectedDate = arguments?.getString("selected_date") ?: arguments?.getString("selectedDate")
        val mainImageUrl = arguments?.getString("main_image_url")
        val itemImageUrls = arguments?.getStringArrayList("item_image_urls")
        val outfitId = arguments?.getInt("outfit_id", -1)

        Log.d("CalendarSaveFragment", "받은 데이터:")
        Log.d("CalendarSaveFragment", "날짜: $selectedDate")
        Log.d("CalendarSaveFragment", "메인 이미지 URL: $mainImageUrl")
        Log.d("CalendarSaveFragment", "아이템 이미지 URLs: $itemImageUrls")
        Log.d("CalendarSaveFragment", "Outfit ID: $outfitId")

        // ⭐ 날짜 표시
        binding.calendarSaveDateTv.text = selectedDate ?: "날짜 없음"

        // ⭐ 메인 이미지 표시 (큰 영역)
        if (!mainImageUrl.isNullOrBlank()) {
            setupMainImage(mainImageUrl)
        } else {
            Log.d("CalendarSaveFragment", "메인 이미지 URL이 없음 - 기본 이미지 유지")
        }

        // ⭐ 개별 아이템들 표시 (작은 RecyclerView)
        if (!itemImageUrls.isNullOrEmpty()) {
            setupItemRecyclerView(itemImageUrls)
        } else {
            // 아이템 이미지들이 없으면 더미 데이터 사용
            setupDummyRecyclerView()
        }

        // ⭐ 현재 화면의 날씨/메모 텍스트도 보관
        currentWeatherText = binding.calendarSaveWeatherTv.text?.toString()
        currentMemoText = binding.calendarSaveMemoTv.text?.toString()

        // 뒤로가기
        binding.calendarSaveBackBtn.setOnClickListener {
            findNavController().popBackStack()
        }

        // ⭐ 수정 아이콘 클릭: 현재 값들을 Safe Args로 전달
        binding.calendarSaveEditIv.setOnClickListener {
            // 혹시 화면에서 사용자가 방금 수정했을 수도 있으니 다시 읽어서 최신화
            val selectedDateText = binding.calendarSaveDateTv.text?.toString()
            val weatherText      = binding.calendarSaveWeatherTv.text?.toString()
            val memoText         = binding.calendarSaveMemoTv.text?.toString()
            val mainUrl  = currentMainImageUrl      // String?
            val itemArr  = currentItemImageUrls.toTypedArray() // Array<String>

            val action = CalendarSaveFragmentDirections
                .actionCalendarSaveFragmentToCalendarRewriteFragment(
                    selectedDateText,
                    mainUrl,
                    itemArr, // string[] 전달
                    weatherText,
                    memoText
                )

            findNavController().navigate(action)
        }

        observeRewriteResults()

        binding.calendarSaveSendIv.setOnClickListener {
            showDeleteDialog()
        }
    }

    /**
     * ⭐ 큰 메인 이미지 표시
     */
    private fun setupMainImage(mainImageUrl: String) {
        Log.d("CalendarSaveFragment", "메인 이미지 로드 시작: $mainImageUrl")

        // ⭐ ImageView 크기를 코드에서 설정
        val layoutParams = binding.calendarSaveOutfitIv.layoutParams
        layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
        layoutParams.height = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 350f, resources.displayMetrics
        ).toInt()
        binding.calendarSaveOutfitIv.layoutParams = layoutParams

        // scaleType도 설정
        binding.calendarSaveOutfitIv.scaleType = ImageView.ScaleType.CENTER_CROP

        Glide.with(this)
            .load(mainImageUrl)
            .placeholder(R.drawable.ic_launcher_background)
            .error(R.drawable.ic_launcher_background)
            .into(binding.calendarSaveOutfitIv)

        Log.d("CalendarSaveFragment", "Glide 로드 요청 완료")
    }

    /**
     * ⭐ 개별 아이템들을 작은 RecyclerView에 표시
     */
    private fun setupItemRecyclerView(itemImageUrls: List<String>) {
        Log.d("CalendarSaveFragment", "개별 아이템들 로드: ${itemImageUrls.size}개")

        // URL 리스트를 CalendarSaveItem 리스트로 변환
        val itemList = itemImageUrls.map { url ->
            CalendarSaveItem(imageUrl = url)
        }

        val adapter = CalendarSaveAdapter(itemList)
        binding.calendarSaveRv.adapter = adapter
        binding.calendarSaveRv.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.calendarSaveRv.visibility = View.VISIBLE
    }

    /**
     * ⭐ 더미 데이터로 RecyclerView 설정 (fallback)
     */
    private fun setupDummyRecyclerView() {
        Log.d("CalendarSaveFragment", "더미 이미지 사용")

        val calendarSaveAdapter = CalendarSaveAdapter(calendarSaveList)
        binding.calendarSaveRv.adapter = calendarSaveAdapter
        binding.calendarSaveRv.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.calendarSaveRv.visibility = View.VISIBLE
    }

    private fun showDeleteDialog() {
        val dialog = AlertDialog.Builder(requireContext()).create()
        val dialogView = layoutInflater.inflate(R.layout.outfit_delete_dialog, null)
        dialog.setView(dialogView)
        dialog.setCancelable(false)

        dialog.window?.setBackgroundDrawable(
            ContextCompat.getDrawable(requireContext(), R.drawable.rounded_white_bg)
        )

        val yesBtn = dialogView.findViewById<Button>(R.id.delete_dialog_yes_btn)
        val noBtn = dialogView.findViewById<Button>(R.id.delete_dialog_no_btn)

        yesBtn.setOnClickListener {
            dialog.dismiss()
            activity?.finish() // 액티비티 종료
        }

        noBtn.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()

        // 다이얼로그 너비 294dp
        val width = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 294f, resources.displayMetrics
        ).toInt()
        dialog.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    private fun observeRewriteResults() {
        val handle = findNavController().currentBackStackEntry?.savedStateHandle ?: return

        // 날짜
        handle.getLiveData<String>("rewrite_date")
            .observe(viewLifecycleOwner) { newDate ->
                if (!newDate.isNullOrBlank()) {
                    binding.calendarSaveDateTv.text = newDate
                    // (선택) 상태 보관 변수도 갱신 중이면 함께 갱신
                    // currentSelectedDate = newDate
                }
                handle.remove<String>("rewrite_date") // 소비 후 제거 (중복 방지)
            }

        // 날씨
        handle.getLiveData<String>("rewrite_weather")
            .observe(viewLifecycleOwner) { weather ->
                binding.calendarSaveWeatherTv.text = weather ?: ""
                handle.remove<String>("rewrite_weather")
            }

        // 메모
        handle.getLiveData<String>("rewrite_memo")
            .observe(viewLifecycleOwner) { memo ->
                binding.calendarSaveMemoTv.text = memo ?: ""
                handle.remove<String>("rewrite_memo")
            }

        // 메인 이미지(URL/URI 문자열)
        handle.getLiveData<String>("rewrite_main_image_url")
            .observe(viewLifecycleOwner) { url ->
                if (!url.isNullOrBlank()) {
                    // currentMainImageUrl = url
                    setupMainImage(url)   // 네가 이미 만든 함수 재사용
                }
                handle.remove<String>("rewrite_main_image_url")
            }

        // 아이템 리스트(ArrayList<String> = URL/URI)
        handle.getLiveData<ArrayList<String>>("rewrite_item_image_urls")
            .observe(viewLifecycleOwner) { urls ->
                val list = urls ?: arrayListOf()
                if (list.isNotEmpty()) {
                    // currentItemImageUrls = ArrayList(list)
                    setupItemRecyclerView(list)     // 네가 이미 만든 함수 재사용
                } else {
                    // 실제 편집 결과를 존중해 비우고 싶다면 여기서 비우기:
                    // binding.calendarSaveRv.adapter = CalendarSaveAdapter(emptyList())
                    // binding.calendarSaveRv.visibility = View.GONE
                    // or 네가 쓰던 더미 유지 전략이라면:
                    setupDummyRecyclerView()
                }
                handle.remove<ArrayList<String>>("rewrite_item_image_urls")
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}