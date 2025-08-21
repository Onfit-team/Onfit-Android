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
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.onfit.databinding.FragmentCalendarSaveBinding

// ✅ 상세 API 재호출을 위한 import
import androidx.lifecycle.lifecycleScope
import com.example.onfit.KakaoLogin.util.TokenProvider
import com.example.onfit.network.RetrofitInstance
import kotlinx.coroutines.launch

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
    ): View {
        _binding = FragmentCalendarSaveBinding.inflate(inflater, container, false)
        return binding.root
    }

    // CalendarSaveFragment.kt - onViewCreated 함수 전체 코드

    // 🔥 CalendarSaveFragment의 onViewCreated 수정 - 실제 이미지 URL 우선 처리

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ⭐ 전달받은 데이터 확인
        val selectedDate = arguments?.getString("selected_date") ?: arguments?.getString("selectedDate")
        val mainImageUrl = arguments?.getString("main_image_url")
        val itemImageUrls = arguments?.getStringArrayList("item_image_urls")
        val outfitId = arguments?.getInt("outfit_id", -1) ?: -1
        val outfitNumber = arguments?.getInt("outfit_number") ?: -1
        val fromOutfitRecord = arguments?.getBoolean("from_outfit_record") ?: false
        val isDummyOutfit = arguments?.getBoolean("is_dummy_outfit") ?: false
        val isRealOutfit = arguments?.getBoolean("is_real_outfit") ?: false
        val memo = arguments?.getString("memo")

        Log.d("CalendarSaveFragment", "받은 데이터:")
        Log.d("CalendarSaveFragment", "날짜: $selectedDate")
        Log.d("CalendarSaveFragment", "메인 이미지 URL: $mainImageUrl")
        Log.d("CalendarSaveFragment", "Outfit ID: $outfitId")
        Log.d("CalendarSaveFragment", "Outfit Number: $outfitNumber")
        Log.d("CalendarSaveFragment", "From Outfit Record: $fromOutfitRecord")
        Log.d("CalendarSaveFragment", "Is Dummy Outfit: $isDummyOutfit")
        Log.d("CalendarSaveFragment", "Is Real Outfit: $isRealOutfit")
        Log.d("CalendarSaveFragment", "Memo: $memo")

        // ⭐ 날짜 표시
        binding.calendarSaveDateTv.text = selectedDate ?: "날짜 없음"

        // 🔥 더미 코디 판별
        val isStyleOutfitsDummy = outfitId in 1101..1107  // StyleOutfits + cody7
        val isCalendarDummy = outfitId in 1001..1004
        val isCody7Dummy = outfitId == 1107 || outfitNumber == 7

        when {
            // 🔥 NEW: 실제 이미지 URL이 있으면 최우선 처리 ✅
            !mainImageUrl.isNullOrBlank() && mainImageUrl.startsWith("http") -> {
                Log.d("CalendarSaveFragment", "🌟 실제 업로드 이미지 처리: $mainImageUrl")
                setupRealUploadedImage(mainImageUrl, memo)
            }

            // 코디 7번 처리
            isCody7Dummy -> {
                Log.d("CalendarSaveFragment", "🎯 코디 7번 처리: ID=$outfitId, Number=$outfitNumber")
                setupDummyOutfitData(7)
            }

            // StyleOutfits 더미 코디 (1101~1107)
            isStyleOutfitsDummy -> {
                Log.d("CalendarSaveFragment", "🎨 StyleOutfits 더미 코디 처리: ID=$outfitId")
                val actualOutfitNumber = outfitId - 1100
                setupDummyOutfitData(actualOutfitNumber)
            }

            // Calendar 더미 코디 (1001~1004)
            isCalendarDummy -> {
                Log.d("CalendarSaveFragment", "📅 Calendar 더미 코디 처리: ID=$outfitId")
                val actualOutfitNumber = outfitId - 1000
                setupDummyOutfitData(actualOutfitNumber)
            }

            // 코디 기록에서 온 더미 처리 (outfitNumber 5, 6, 7번)
            (fromOutfitRecord && outfitNumber in 5..7) -> {
                Log.d("CalendarSaveFragment", "🎯 코디 기록 더미 처리: outfitNumber=$outfitNumber")
                setupDummyOutfitData(outfitNumber)
            }

            // 기존 더미 처리 방식
            isDummyOutfit || (fromOutfitRecord && outfitNumber != -1) -> {
                Log.d("CalendarSaveFragment", "🎭 기존 더미 코디 처리: outfitNumber=$outfitNumber")
                setupDummyOutfitData(outfitNumber)
            }

            // isRealOutfit 플래그가 true인 경우
            isRealOutfit && !mainImageUrl.isNullOrBlank() -> {
                Log.d("CalendarSaveFragment", "🌐 실제 API 코디 처리")
                setupRealApiOutfitData(normalizeServerUrl(mainImageUrl), memo)
            }

            // 🔥 fallback을 더미가 아닌 기본 처리로 변경
            else -> {
                Log.d("CalendarSaveFragment", "⚠️ 조건 미일치 - 기본 이미지 처리 시도")
                if (!mainImageUrl.isNullOrBlank()) {
                    setupRealUploadedImage(mainImageUrl, memo)
                } else {
                    Log.d("CalendarSaveFragment", "🔄 최후 폴백: 더미 데이터 사용")
                    setupDummyRecyclerView()
                }
            }
        }

        // 🔥 실제 업로드는 서버 API 호출하지 않음
        if (outfitId > 0 && !isStyleOutfitsDummy && !isCalendarDummy && !isDummyOutfit && !isCody7Dummy && mainImageUrl.isNullOrBlank()) {
            val token = TokenProvider.getToken(requireContext())
            if (token.isNotBlank()) {
                viewLifecycleOwner.lifecycleScope.launch {
                    runCatching {
                        val res = RetrofitInstance.api.getOutfitDetail("Bearer $token", outfitId)
                        if (!res.isSuccessful) return@runCatching
                        val d = res.body()?.result ?: return@runCatching

                        val serverMain = d.mainImage?.trim()
                        if (!serverMain.isNullOrBlank()) {
                            setupMainImage(normalizeServerUrl(serverMain))
                        }

                        val urls = d.items
                            .mapNotNull { it.image }
                            .filter { it.isNotBlank() }
                            .map { normalizeServerUrl(it) }

                        if (urls.isNotEmpty()) {
                            setupItemRecyclerView(urls)
                        }
                    }.onFailure {
                        Log.d("CalendarSaveFragment", "상세 재조회 실패: ${it.message}")
                    }
                }
            }
        } else {
            Log.d("CalendarSaveFragment", "실제 이미지 URL 있음 - 서버 API 호출 생략")
        }

        // 버튼 리스너들
        binding.calendarSaveBackBtn.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.calendarSaveEditIv.setOnClickListener {
            if (isStyleOutfitsDummy || isCalendarDummy || isDummyOutfit || isCody7Dummy) {
                Toast.makeText(requireContext(), "더미 코디는 편집할 수 없습니다", Toast.LENGTH_SHORT).show()
            } else {
                findNavController().navigate(R.id.action_calendarSaveFragment_to_calendarRewriteFragment)
            }
        }

        binding.calendarSaveSendIv.setOnClickListener {
            if (isStyleOutfitsDummy || isCalendarDummy || isDummyOutfit || isCody7Dummy) {
                showDummyDeleteDialog()
            } else {
                showDeleteDialog()
            }
        }
    }

    // 🔥 NEW: 실제 업로드된 이미지 처리 함수
    private fun setupRealUploadedImage(imageUrl: String, memo: String?) {
        Log.d("CalendarSaveFragment", "🌟 실제 업로드 이미지 설정: $imageUrl")

        // 메인 이미지 표시
        setupMainImage(imageUrl)

        // RecyclerView는 숨김 (개별 아이템 없음)
        binding.calendarSaveRv.visibility = View.GONE

        // 메모가 있다면 표시 (선택사항)
        if (!memo.isNullOrBlank()) {
            Log.d("CalendarSaveFragment", "업로드 메모: $memo")
            // 필요시 메모 표시 로직 추가
        }

        Log.d("CalendarSaveFragment", "✅ 실제 업로드 이미지 설정 완료")
    }


    /**
     * ⭐ 서버 경로 보정:
     * - 절대 URL(http/https): 그대로
     * - file://, content:// : 그대로
     * - "/images/..." 같은 절대 경로: 베이스 도메인만 붙임
     * - "foo.jpg" 같은 파일명: /images/ prefix와 도메인 붙임
     */
    private fun normalizeServerUrl(raw: String): String {
        val s = raw.trim()
        return when {
            s.startsWith("http://") || s.startsWith("https://") -> s
            s.startsWith("file://") || s.startsWith("content://") -> s
            s.startsWith("/") -> "http://3.36.113.173$s"
            else -> "http://3.36.113.173/images/$s"
        }
    }

    /**
     * 🔥 실제 API 코디 데이터 설정
     */
    private fun setupRealApiOutfitData(mainImageUrl: String, memo: String?) {
        Log.d("CalendarSaveFragment", "🌐 실제 API 코디 설정: $mainImageUrl")
        setupMainImage(mainImageUrl)
        setupRealOutfitDescription(memo)
        binding.calendarSaveRv.visibility = View.GONE
    }

    /**
     * 🔥 실제 코디 설명 표시 (RecyclerView 대신)
     */
    private fun setupRealOutfitDescription(memo: String?) {
        binding.calendarSaveRv.visibility = View.GONE
        Log.d("CalendarSaveFragment", "실제 코디 메모: $memo")
        // 필요 시 memo를 별도 TextView에 표시하는 로직 추가 가능
    }

    /**
     * 🔥 더미 코디 삭제 다이얼로그 (더미 전용)
     */
    private fun showDummyDeleteDialog() {
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
            Toast.makeText(requireContext(), "더미 코디가 제거되었습니다", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
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

    /**
     * ⭐ 큰 메인 이미지 표시 (URL/파일 경로)
     */
    private fun setupMainImage(mainImageUrl: String) {
        Log.d("CalendarSaveFragment", "메인 이미지 로드 시작: $mainImageUrl")

        val layoutParams = binding.calendarSaveOutfitIv.layoutParams
        layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
        layoutParams.height = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 350f, resources.displayMetrics
        ).toInt()
        binding.calendarSaveOutfitIv.layoutParams = layoutParams

        binding.calendarSaveOutfitIv.scaleType = ImageView.ScaleType.CENTER_CROP

        Glide.with(this)
            .load(mainImageUrl)
            .placeholder(R.drawable.ic_launcher_background)
            .error(R.drawable.ic_launcher_background)
            .into(binding.calendarSaveOutfitIv)

        Log.d("CalendarSaveFragment", "Glide 로드 요청 완료")
    }

    /**
     * ⭐ 개별 아이템들을 작은 RecyclerView에 표시 (URL 리스트)
     */
    private fun setupItemRecyclerView(itemImageUrls: List<String>) {
        Log.d("CalendarSaveFragment", "개별 아이템들 로드: ${itemImageUrls.size}개")

        val itemList = itemImageUrls.map { url ->
            CalendarSaveItem(imageUrl = url)
        }

        val adapter = CalendarSaveAdapter(itemList)
        binding.calendarSaveRv.adapter = adapter
        binding.calendarSaveRv.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.calendarSaveRv.visibility = View.VISIBLE
    }

    /**
     * ⭐ 더미 데이터로 RecyclerView 설정 (fallback)
     */
    private fun setupDummyRecyclerView() {
        Log.d("CalendarSaveFragment", "더미 이미지 사용")

        val calendarSaveAdapter = CalendarSaveAdapter(calendarSaveList)
        binding.calendarSaveRv.adapter = calendarSaveAdapter
        binding.calendarSaveRv.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.calendarSaveRv.visibility = View.VISIBLE
    }

    /**
     * 실제(서버) 코디 삭제 다이얼로그
     */
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

        val width = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 294f, resources.displayMetrics
        ).toInt()
        dialog.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupDummyOutfitData(outfitNumber: Int) {
        Log.d("CalendarSaveFragment", "🎭 더미 코디 ${outfitNumber}번 데이터 설정")

        // 🔥 날짜는 arguments에서 받은 selected_date 사용 (계산하지 않음)
        val selectedDate = arguments?.getString("selected_date") ?: arguments?.getString("selectedDate")
        val outfitId = arguments?.getInt("outfit_id", -1) ?: -1

        if (!selectedDate.isNullOrBlank()) {
            binding.calendarSaveDateTv.text = selectedDate
            Log.d("CalendarSaveFragment", "전달받은 날짜 사용: $selectedDate")
        } else {
            binding.calendarSaveDateTv.text = "날짜 없음"
            Log.w("CalendarSaveFragment", "날짜 정보가 없음")
        }

        // 🔥 outfit_id 범위에 따라 다른 이미지 사용
        val isStyleOutfitsDummy = outfitId in 1101..1106  // 8월 1~6일: ccody 시리즈 + cody5, cody6
        val isCalendarDummy = outfitId in 1001..1005      // 8월 10~14일: cody 시리즈
        val isCody7Dummy = outfitId == 1107               // 🔥 NEW: 8월 16일: cody7

        val mainImageRes = when {
            // 🔥 NEW: 코디 7번 처리 (8월 16일)
            isCody7Dummy || outfitNumber == 7 -> {
                Log.d("CalendarSaveFragment", "🎯 코디 7번 이미지 설정")
                R.drawable.cody7  // 실제 cody7 이미지 사용
            }

            // StyleOutfits 더미 (8월 1~6일): ccody1~ccody4, cody5, cody6
            isStyleOutfitsDummy -> {
                when (outfitNumber) {
                    1 -> {
                        val ccody1Id = resources.getIdentifier("ccody1", "drawable", requireContext().packageName)
                        if (ccody1Id != 0) ccody1Id else R.drawable.cody1
                    }
                    2 -> {
                        val ccody2Id = resources.getIdentifier("ccody2", "drawable", requireContext().packageName)
                        if (ccody2Id != 0) ccody2Id else R.drawable.cody2
                    }
                    3 -> {
                        val ccody3Id = resources.getIdentifier("ccody3", "drawable", requireContext().packageName)
                        if (ccody3Id != 0) ccody3Id else R.drawable.cody3
                    }
                    4 -> {
                        val ccody4Id = resources.getIdentifier("ccody4", "drawable", requireContext().packageName)
                        if (ccody4Id != 0) ccody4Id else R.drawable.clothes8
                    }
                    5 -> R.drawable.cody5  // 8월 5일 - cody5
                    6 -> R.drawable.cody6  // 8월 14일 - cody6
                    else -> R.drawable.clothes8
                }
            }

            // Calendar 더미 (8월 10~14일)
            isCalendarDummy -> {
                when (outfitNumber) {
                    1 -> R.drawable.cody1  // 8월 13일 - cody1
                    2 -> R.drawable.cody2  // 8월 12일 - cody2
                    3 -> R.drawable.cody3  // 8월 11일 - cody3
                    4 -> R.drawable.cody4  // 8월 10일 - cody4
                    5 -> R.drawable.cody6  // 8월 14일 - cody6
                    else -> R.drawable.cody1
                }
            }

            // 🔥 기본값 (코디 기록에서 오는 경우) - 7번 추가
            else -> {
                when (outfitNumber) {
                    1 -> R.drawable.cody1  // 8월 13일
                    2 -> R.drawable.cody2  // 8월 12일
                    3 -> R.drawable.cody3  // 8월 11일
                    4 -> R.drawable.cody4  // 8월 10일
                    5 -> R.drawable.cody5  // 8월 5일 - cody5 이미지
                    6 -> R.drawable.cody6  // 8월 14일 - cody6 이미지
                    7 -> R.drawable.cody7  // 🔥 NEW: 8월 16일 - cody7 이미지 ✅
                    else -> R.drawable.clothes8
                }
            }
        }

        setupMainImageFromDrawable(mainImageRes)

        // 🔥 더미 코디별 개별 아이템 이미지 설정 (7번 추가)
        val itemList = when (outfitNumber) {
            1 -> listOf(
                CalendarSaveItem(imageResId = R.drawable.shirts1),
                CalendarSaveItem(imageResId = R.drawable.pants1),
                CalendarSaveItem(imageResId = R.drawable.shoes1)
            )
            2 -> listOf(
                CalendarSaveItem(imageResId = R.drawable.shirts2),
                CalendarSaveItem(imageResId = R.drawable.pants2),
                CalendarSaveItem(imageResId = R.drawable.shoes2)
            )
            3 -> listOf(
                CalendarSaveItem(imageResId = R.drawable.shirts3),
                CalendarSaveItem(imageResId = R.drawable.shoes3),
                CalendarSaveItem(imageResId = R.drawable.pants3),
                CalendarSaveItem(imageResId = R.drawable.acc3)
            )
            4 -> listOf(
                CalendarSaveItem(imageResId = R.drawable.shirts4),
                CalendarSaveItem(imageResId = R.drawable.pants4),
                CalendarSaveItem(imageResId = R.drawable.shoes4),
                CalendarSaveItem(imageResId = R.drawable.bag4)
            )
            5 -> listOf(  // 코디 5번 - 5시리즈 아이템들
                CalendarSaveItem(imageResId = R.drawable.shirts5),
                CalendarSaveItem(imageResId = R.drawable.pants5),
                CalendarSaveItem(imageResId = R.drawable.shoes5),
                CalendarSaveItem(imageResId = R.drawable.acc5)
            )
            6 -> listOf(  // 코디 6번 - 6시리즈 아이템들
                CalendarSaveItem(imageResId = R.drawable.shirts6),
                CalendarSaveItem(imageResId = R.drawable.pants6),
                CalendarSaveItem(imageResId = R.drawable.shoes6),
                CalendarSaveItem(imageResId = R.drawable.acc6)
            )
            7 -> listOf(  // 🔥 NEW: 코디 7번 - 캐주얼 코디 아이템들 ✅
                CalendarSaveItem(imageResId = R.drawable.shirts8),  // 체크 셔츠 (임시로 shirts1 사용)
                CalendarSaveItem(imageResId = R.drawable.check7),  // 화이트 이너 (임시로 shirts2 사용)
                CalendarSaveItem(imageResId = R.drawable.pants8),   // 네이비 청바지 (pants5 사용)
                CalendarSaveItem(imageResId = R.drawable.shoes2),   // 블랙 컨버스 (shoes1 사용)
                CalendarSaveItem(imageResId = R.drawable.bag7)      // 블랙 백팩 (acc5 사용)
            )
            else -> calendarSaveList
        }

        setupDummyItemRecyclerView(itemList)

        Log.d("CalendarSaveFragment", "✅ 코디 ${outfitNumber}번 설정 완료: 날짜=$selectedDate, 메인이미지=${mainImageRes}, 타입=${when {
            isCody7Dummy -> "Cody7"
            isStyleOutfitsDummy -> "StyleOutfits"
            isCalendarDummy -> "Calendar"
            else -> "기본"
        }}")
    }

    // 🔥 Drawable 리소스로 메인 이미지 설정
    private fun setupMainImageFromDrawable(imageResId: Int) {
        Log.d("CalendarSaveFragment", "메인 이미지 Drawable 설정: $imageResId")

        val layoutParams = binding.calendarSaveOutfitIv.layoutParams
        layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
        layoutParams.height = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 350f, resources.displayMetrics
        ).toInt()
        binding.calendarSaveOutfitIv.layoutParams = layoutParams

        binding.calendarSaveOutfitIv.scaleType = ImageView.ScaleType.CENTER_CROP
        binding.calendarSaveOutfitIv.setImageResource(imageResId)

        Log.d("CalendarSaveFragment", "✅ Drawable 메인 이미지 설정 완료")
    }

    // 🔥 더미 아이템들을 RecyclerView에 표시
    private fun setupDummyItemRecyclerView(itemList: List<CalendarSaveItem>) {
        Log.d("CalendarSaveFragment", "더미 개별 아이템들 설정: ${itemList.size}개")

        val adapter = CalendarSaveAdapter(itemList)
        binding.calendarSaveRv.adapter = adapter
        binding.calendarSaveRv.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.calendarSaveRv.visibility = View.VISIBLE

        Log.d("CalendarSaveFragment", "✅ 더미 아이템 RecyclerView 설정 완료")
    }
}
