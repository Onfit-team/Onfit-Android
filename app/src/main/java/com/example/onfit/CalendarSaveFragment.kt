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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.onfit.DeleteOutfit.DeleteOutfitService
import com.example.onfit.KakaoLogin.util.TokenProvider
import com.example.onfit.OutfitRegister.RetrofitClient
import com.example.onfit.calendar.Network.CalendarService
import com.example.onfit.databinding.FragmentCalendarSaveBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar as JavaCalendar // 이름 변경
import java.util.*

class CalendarSaveFragment : Fragment() {
    private var _binding: FragmentCalendarSaveBinding? = null
    private val binding get() = _binding!!

    // outfit_id 저장
    private var outfitIdArg: Int = -1
    private var selectedDateArg: String? = null

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
        Log.d("WhoAmI", "I am ${this::class.qualifiedName}")
        super.onViewCreated(view, savedInstanceState)

        // ⭐ 전달받은 데이터 확인
        val selectedDate = arguments?.getString("selected_date") ?: arguments?.getString("selectedDate")
        val mainImageUrl = arguments?.getString("main_image_url")
        val itemImageUrls = arguments?.getStringArrayList("item_image_urls")
        
        val outfitIdCamel = arguments?.getInt("outfitId", -1) ?: -1
        val outfitIdSnake = arguments?.getInt("outfit_id", -1) ?: -1
        val outfitId = arguments?.getInt("outfit_id", -1)
        
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

        selectedDateArg = arguments?.getString("selected_date") ?: arguments?.getString("selectedDate")
        outfitIdArg = arguments?.getInt("outfitId", -1) ?: -1

        Log.d("CalendarSaveFragment", "args keys=${arguments?.keySet()}")
        Log.d("CalendarSaveFragment", "resolved outfitIdArg=$outfitIdArg")

        // ⭐ 날짜 표시
        binding.calendarSaveDateTv.text = selectedDate ?: "날짜 없음"

        binding.calendarSaveSendIv.isEnabled = isValidServerId(outfitIdArg)
        binding.calendarSaveSendIv.alpha = if (binding.calendarSaveSendIv.isEnabled) 1f else 0.3f

        binding.root.isEnabled = true
        binding.root.isClickable = true
        binding.calendarSaveBackBtn.isEnabled = true
        binding.calendarSaveEditIv.isEnabled  = true

        // 🔥 코디 타입별 처리
        when {
            // 1) 더미 코디
            isDummyOutfit || (fromOutfitRecord && outfitNumber != -1) -> {
                Log.d("CalendarSaveFragment", "🎭 더미 코디 데이터 설정")
                setupDummyOutfitData(outfitNumber)
            }

            // 2) 실제 API 코디 (HomeViewModel에서 온 경우)
            isRealOutfit && !mainImageUrl.isNullOrBlank() -> {
                Log.d("CalendarSaveFragment", "🌐 실제 API 코디 데이터 설정")
                setupRealApiOutfitData(mainImageUrl, memo)
            }

            // 3) 기존 서버 코디 (메인 + 아이템들)
            !mainImageUrl.isNullOrBlank() && !itemImageUrls.isNullOrEmpty() -> {
                Log.d("CalendarSaveFragment", "📦 기존 서버 코디 데이터 설정")
                setupMainImage(mainImageUrl)
                setupItemRecyclerView(itemImageUrls)
            }

            // 4) 메인 이미지만 있는 경우
            !mainImageUrl.isNullOrBlank() -> {
                Log.d("CalendarSaveFragment", "🖼️ 메인 이미지만 설정")
                setupMainImage(mainImageUrl)
                setupDummyRecyclerView()
            }

            // 5) 폴백: 더미
            else -> {
                Log.d("CalendarSaveFragment", "🔄 폴백: 더미 데이터 사용")
                setupDummyRecyclerView()
            }
        }

        // 버튼 리스너들
        binding.calendarSaveBackBtn.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.calendarSaveEditIv.setOnClickListener {
            if (isDummyOutfit) {
                Toast.makeText(requireContext(), "더미 코디는 편집할 수 없습니다", Toast.LENGTH_SHORT).show()
            } else {
                findNavController().navigate(R.id.action_calendarSaveFragment_to_calendarRewriteFragment)
            }
        }

        binding.calendarSaveSendIv.setOnClickListener {
            if (isDummyOutfit) {
                showDummyDeleteDialog()
            } else {
                showDeleteDialog()
            }
        }
    }

    /**
     * 🔥 NEW: 실제 API 코디 데이터 설정
     */
    private fun setupRealApiOutfitData(mainImageUrl: String, memo: String?) {
        Log.d("CalendarSaveFragment", "🌐 실제 API 코디 설정: $mainImageUrl")

        // 메인 이미지 설정
        setupMainImage(mainImageUrl)

        // 🔥 실제 API 코디는 개별 아이템이 없으므로 메모나 설명을 표시
        setupRealOutfitDescription(memo)

        // RecyclerView는 숨기거나 간단한 설명으로 대체
        binding.calendarSaveRv.visibility = View.GONE
    }

    /**
     * 🔥 NEW: 실제 코디 설명 표시 (RecyclerView 대신)
     */
    private fun setupRealOutfitDescription(memo: String?) {
        // 실제 코디에는 개별 아이템 정보가 없으므로
        // RecyclerView 영역에 메모나 설명을 표시할 수 있음
        // 여기서는 단순히 RecyclerView를 숨김
        binding.calendarSaveRv.visibility = View.GONE

        Log.d("CalendarSaveFragment", "실제 코디 메모: $memo")
        // 필요하다면 memo를 별도 TextView에 표시하는 로직 추가
    }

    /**
     * 🔥 NEW: 더미 코디 삭제 다이얼로그
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
        val dialogView = layoutInflater.inflate(R.layout.outfit_delete_dialog, null, false)

        val yesBtn = dialogView.findViewById<Button>(R.id.delete_dialog_yes_btn)
        val noBtn = dialogView.findViewById<Button>(R.id.delete_dialog_no_btn)

        // ★ 핵심: setView를 Builder에 붙인 다음 create()
        val dialog = MaterialAlertDialogBuilder(requireContext()) // 없으면 AlertDialog.Builder 써도 OK
            .setView(dialogView)
            .setCancelable(false)
            .create()

        // (선택) 배경 라운드가 필요하면 show() 후 적용
        dialog.show()
        dialog.window?.setBackgroundDrawable(
            ContextCompat.getDrawable(requireContext(), R.drawable.rounded_white_bg)
        )
        dialog.window?.setLayout(
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 294f, resources.displayMetrics)
                .toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        yesBtn.setOnClickListener {
            Log.d("DeleteDialog", "YES clicked")
            lifecycleScope.launch {
                try {
                    val token = TokenProvider.getToken(requireContext())
                    if (token.isNullOrBlank()) {
                        Toast.makeText(requireContext(), "로그인이 필요합니다.", Toast.LENGTH_SHORT)
                            .show(); return@launch
                    }
                    val bearer = "Bearer $token"

                    Log.d("DeleteDialog", "1) before DELETE id=$outfitIdArg")

                    val resp = withContext(Dispatchers.IO) {
                        RetrofitClient.instance.create(DeleteOutfitService::class.java)
                            .deleteOutfit(outfitIdArg, bearer)
                    }
                    Log.d("DeleteDialog", "2) after DELETE code=${resp.code()}")
                    val success = resp.code() == 204 || (resp.isSuccessful && (resp.body()?.isSuccess == true))
                    if (success) {
                        // 성공 처리
                    } else {
                        val msg = resp.body()?.message ?: resp.errorBody()?.string() ?: "삭제 실패 (${resp.code()})"
                        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                    }

                    val ok =
                        resp.code() == 204 || (resp.isSuccessful && (resp.body()?.isSuccess == true))
                    if (ok) {
                        findNavController().previousBackStackEntry?.savedStateHandle?.apply {
                            set("deleted_outfit_id", outfitIdArg); set(
                            "deleted_date",
                            selectedDateArg
                        )
                        }
                        Toast.makeText(requireContext(), "코디 삭제 성공", Toast.LENGTH_SHORT).show()
                        dialog.dismiss(); findNavController().popBackStack()
                    } else {
                        val err = resp.errorBody()?.string()
                        Log.e("DeleteDialog", "DELETE failed code=${resp.code()} body=$err")
                        Toast.makeText(
                            requireContext(),
                            "삭제 실패 (${resp.code()})",
                            Toast.LENGTH_SHORT
                        ).show()
                        yesBtn.isEnabled = true; noBtn.isEnabled = true
                    }
                } catch (e: Exception) {
                    Log.e("DeleteDialog", "DELETE exception", e)
                    Toast.makeText(requireContext(), "네트워크 오류: ${e.message}", Toast.LENGTH_SHORT)
                        .show()
                    yesBtn.isEnabled = true; noBtn.isEnabled = true
                }
            }

            noBtn.setOnClickListener {
                Log.d("DeleteDialog", "NO clicked") // ← 클릭 확인용 로그
                dialog.dismiss()
            }
        }
    }

    private fun isValidServerId(id: Int) = id > 0

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // 🔥 NEW: 더미 코디 데이터 설정
    private fun setupDummyOutfitData(outfitNumber: Int) {
        Log.d("CalendarSaveFragment", "🎭 더미 코디 ${outfitNumber}번 데이터 설정")

        // 🔥 현재 년도/월 기준으로 날짜 생성
        val calendar = JavaCalendar.getInstance()
        val currentYear = calendar.get(JavaCalendar.YEAR)
        val currentMonth = calendar.get(JavaCalendar.MONTH) + 1

        // 날짜 매핑 수정
        val dateMap = mapOf(
            1 to "$currentYear-${String.format("%02d", currentMonth)}-13", // 이번 달 20일
            2 to "$currentYear-${String.format("%02d", currentMonth)}-12", // 이번 달 19일
            3 to "$currentYear-${String.format("%02d", currentMonth)}-11",  // 이번 달 18일
            4 to "$currentYear-${String.format("%02d", currentMonth)}-10"
        )

        val targetDate = dateMap[outfitNumber] ?: "날짜 없음"

        // 날짜 표시 업데이트
        binding.calendarSaveDateTv.text = targetDate

        // 코디별 메인 이미지
        val mainImageRes = when (outfitNumber) {
            1 -> R.drawable.cody1
            2 -> R.drawable.cody2
            3 -> R.drawable.cody3
            else -> R.drawable.clothes8
        }

        // 메인 이미지 표시
        binding.calendarSaveOutfitIv.setImageResource(mainImageRes)

        // 코디별 개별 아이템들
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
                CalendarSaveItem(imageResId = R.drawable.pants3)
            )
            else -> calendarSaveList
        }

        // RecyclerView 설정
        val adapter = CalendarSaveAdapter(itemList)
        binding.calendarSaveRv.adapter = adapter

        Log.d("CalendarSaveFragment", "✅ 코디 ${outfitNumber}번 설정 완료: 날짜=${targetDate}")
    }

    // 🔥 NEW: Drawable 리소스로 메인 이미지 설정
    private fun setupMainImageFromDrawable(imageResId: Int) {
        Log.d("CalendarSaveFragment", "메인 이미지 Drawable 설정: $imageResId")

        // ImageView 크기 설정
        val layoutParams = binding.calendarSaveOutfitIv.layoutParams
        layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
        layoutParams.height = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 350f, resources.displayMetrics
        ).toInt()
        binding.calendarSaveOutfitIv.layoutParams = layoutParams

        // scaleType 설정
        binding.calendarSaveOutfitIv.scaleType = ImageView.ScaleType.CENTER_CROP

        // Drawable 리소스 직접 설정
        binding.calendarSaveOutfitIv.setImageResource(imageResId)

        Log.d("CalendarSaveFragment", "✅ Drawable 메인 이미지 설정 완료")
    }

    // 🔥 NEW: 더미 아이템들을 RecyclerView에 표시
    private fun setupDummyItemRecyclerView(itemList: List<CalendarSaveItem>) {
        Log.d("CalendarSaveFragment", "더미 개별 아이템들 설정: ${itemList.size}개")

        val adapter = CalendarSaveAdapter(itemList)
        binding.calendarSaveRv.adapter = adapter
        binding.calendarSaveRv.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.calendarSaveRv.visibility = View.VISIBLE

        Log.d("CalendarSaveFragment", "✅ 더미 아이템 RecyclerView 설정 완료")
    }
}
