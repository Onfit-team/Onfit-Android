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
        val outfitId = arguments?.getInt("outfitId", -1)

        Log.d("CalendarSaveFragment", "받은 데이터:")
        Log.d("CalendarSaveFragment", "날짜: $selectedDate")
        Log.d("CalendarSaveFragment", "메인 이미지 URL: $mainImageUrl")
        Log.d("CalendarSaveFragment", "아이템 이미지 URLs: $itemImageUrls")
        Log.d("CalendarSaveFragment", "Outfit ID: $outfitId")

        selectedDateArg = arguments?.getString("selected_date") ?: arguments?.getString("selectedDate")
        outfitIdArg = arguments?.getInt("outfitId", -1) ?: -1

        Log.d("CalendarSaveFragment", "args keys=${arguments?.keySet()}")
        Log.d("CalendarSaveFragment", "resolved outfitIdArg=$outfitIdArg")

        // ⭐ 날짜 표시
        binding.calendarSaveDateTv.text = selectedDate ?: "날짜 없음"

        // id 없으면 날짜 비활성화
        binding.calendarSaveSendIv.isEnabled = isValidServerId(outfitIdArg)
        binding.calendarSaveSendIv.alpha = if (binding.calendarSaveSendIv.isEnabled) 1f else 0.3f

        binding.root.isEnabled = true
        binding.root.isClickable = true
        binding.calendarSaveBackBtn.isEnabled = true
        binding.calendarSaveEditIv.isEnabled  = true

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
}


