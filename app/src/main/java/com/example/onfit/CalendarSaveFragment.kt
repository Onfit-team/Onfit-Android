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

        // 버튼 리스너들
        binding.calendarSaveBackBtn.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.calendarSaveEditIv.setOnClickListener {
            findNavController().navigate(R.id.action_calendarSaveFragment_to_calendarRewriteFragment)
        }

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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}