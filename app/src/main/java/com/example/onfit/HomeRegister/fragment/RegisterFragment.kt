package com.example.onfit.HomeRegister.fragment

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.onfit.KakaoLogin.util.TokenProvider
import com.example.onfit.OutfitRegister.ApiService
import com.example.onfit.OutfitRegister.RetrofitClient
import com.example.onfit.R
import com.example.onfit.TopSheetDialogFragment
import com.example.onfit.databinding.FragmentRegisterBinding
import com.google.android.material.chip.Chip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.coroutines.cancellation.CancellationException

class RegisterFragment : Fragment(), TopSheetDialogFragment.OnMemoDoneListener {
    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recvUrl = arguments?.getString("uploadedImageUrl")
        val recvPath = arguments?.getString("selectedImagePath")
        Log.d("RegisterArg", "uploadedImageUrl=$recvUrl, selectedImagePath=$recvPath")

        // HomeFragment 갤러리에서 선택한 사진 받아오기
        val imagePath = arguments?.getString("selectedImagePath")
        imagePath?.let {
            val bitmap = BitmapFactory.decodeFile(it)
            binding.registerOutfitIv.setImageBitmap(bitmap)
        }

        // ⭐ 날짜 설정: 전달받은 날짜가 있으면 그 날짜로, 없으면 오늘 날짜로
        val selectedDate = arguments?.getString("selectedDate") // CalendarFragment에서 전달받은 날짜
        val dateToDisplay = if (!selectedDate.isNullOrBlank()) {
            // 캘린더에서 선택한 날짜 사용 (yyyy-MM-dd → yyyy.MM.dd 변환)
            try {
                val parts = selectedDate.split("-")
                String.format("%04d.%02d.%02d", parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
            } catch (e: Exception) {
                // 변환 실패 시 오늘 날짜 사용
                Log.e("RegisterFragment", "날짜 변환 실패: $selectedDate", e)
                SimpleDateFormat("yyyy.MM.dd", Locale.KOREA).format(Calendar.getInstance().time)
            }
        } else {
            // 홈에서 왔거나 날짜 전달이 없으면 오늘 날짜 사용
            SimpleDateFormat("yyyy.MM.dd", Locale.KOREA).format(Calendar.getInstance().time)
        }

        Log.d("RegisterFragment", "표시할 날짜: $dateToDisplay (전달받은 날짜: $selectedDate)")
        binding.registerDateTv.text = dateToDisplay

        // 위에서 메모 TopSheetDialog 내려옴
        binding.registerMemoEt.setOnClickListener {
            val dialog = TopSheetDialogFragment()
            dialog.setOnMemoDoneListener(object : TopSheetDialogFragment.OnMemoDoneListener {
                override fun onMemoDone(memoText: String) {
                    binding.registerMemoEt.setText(memoText)
                }
            })
            dialog.show(parentFragmentManager, "TopSheet")
        }

        // 칩 3개까지만 선택(분위기)
        val chipGroup2 = binding.registerVibeChips
        chipGroup2.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.size > 3) {
                // 방금 선택한 Chip의 체크를 해제
                val lastCheckedChipId = checkedIds.last()
                group.findViewById<Chip>(lastCheckedChipId)?.isChecked = false

                Toast.makeText(requireContext(), "최대 3개까지만 선택할 수 있어요!", Toast.LENGTH_SHORT).show()
            }
        }

        // 칩 3개까지만 선택(욛도)
        val chipGroup3 = binding.registerUseChips
        chipGroup3.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.size > 3) {
                // 방금 선택한 Chip의 체크를 해제
                val lastCheckedChipId = checkedIds.last()
                group.findViewById<Chip>(lastCheckedChipId)?.isChecked = false

                Toast.makeText(requireContext(), "최대 3개까지만 선택할 수 있어요!", Toast.LENGTH_SHORT).show()
            }
        }

        // 날짜 수정
        binding.registerDropdownBtn.setOnClickListener {
            val parts = binding.registerDateTv.text.toString().split(".")
            val year = parts[0].toInt()
            val month = parts[1].toInt() - 1
            val day = parts[2].toInt()

            DatePickerDialog(requireContext(), { _, y, m, d ->
                binding.registerDateTv.text = String.format("%04d.%02d.%02d", y, m + 1, d)
            }, year, month, day).show()
        }

        // 저장 버튼 누르면 날짜, 이미지 Bundle로 전달, 코디 등록 API 연동
        binding.registerSaveBtn.setOnClickListener {
            // 날짜 변환(API 용)
            binding.registerSaveBtn.isEnabled = false

            // 1) 날짜 안전 파싱 ('.'는 정규식이라 반드시 이스케이프)
            val rawDate = binding.registerDateTv.text.toString().trim()
            val parts = rawDate.split("\\.".toRegex()).map { it.trim() }
            if (parts.size != 3 || parts.any { it.isBlank() }) {
                Toast.makeText(requireContext(), "날짜 형식이 올바르지 않습니다. (예: 2025.08.12)", Toast.LENGTH_SHORT).show()
                binding.registerSaveBtn.isEnabled = true
                return@setOnClickListener
            }
            // 날짜(API용)
            val formattedDateForAPI = "${parts[0]}-${parts[1]}-${parts[2]}"
            // 날짜(Bundle용)
            val formattedDate = "${parts[1].toInt()}월 ${parts[2].toInt()}일"

            // 2) 이미지 URL 필수 확인 (HomeFragment에서 전달한 키 이름 정확히 일치해야 함)
            val imageUrl = arguments?.getString("uploadedImageUrl")?.trim()
            Log.d("RegisterOutfit", "uploadedImageUrl='$imageUrl'")
            if (imageUrl.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "이미지 업로드 후 등록해 주세요.", Toast.LENGTH_SHORT).show()
                binding.registerSaveBtn.isEnabled = true
                return@setOnClickListener
            }

            // 메모 텍스트
            val memoText = binding.registerMemoEt.text.toString()

            // 날씨 칩 묶기
            val chipGroup1 = binding.registerWeatherChips
            val selectedWeatherTags = chipGroup1.checkedChipIds.mapNotNull { chipId ->
                chipGroup1.findViewById<Chip>(chipId).tag?.toString()?.toIntOrNull()
            }

            // 분위기 칩 묶기
            val selectedVibeTags = chipGroup2.checkedChipIds.mapNotNull { chipId ->
                chipGroup2.findViewById<Chip>(chipId).tag?.toString()?.toIntOrNull()
            }

            // 용도 칩 묶기
            val selectedUseTags = chipGroup3.checkedChipIds.mapNotNull { chipId ->
                chipGroup3.findViewById<Chip>(chipId).tag?.toString()?.toIntOrNull()
            }

            val finalMoodTags = selectedVibeTags.take(3)
            val finalPurposeTags = selectedUseTags.take(3)

            // Json body 생성
            val jsonBody = JSONObject().apply {
                put("date", formattedDateForAPI)
                put("mainImage", imageUrl) // 서버에서 받은 이미지 Url 생성
                put("memo", memoText)
                put("feelsLikeTemp", selectedWeatherTags.firstOrNull() ?: 0)
                put("moodTags", JSONArray(finalMoodTags))
                put("purposeTags", JSONArray(finalPurposeTags))
            }
            Log.d("RegisterOutfit", "jsonBody=$jsonBody")

            // RequestBody로 변환
            val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaTypeOrNull())

            // Retrofit API 호출(발급받은 임시 토큰 사용)
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val api = RetrofitClient.instance.create(ApiService::class.java)
                    val header = "Bearer ${TokenProvider.getToken(requireContext())}"
                    val response = withContext(Dispatchers.IO) { api.registerOutfit(header, requestBody) }

                    // errorBody 읽기
                    val errorText = response.errorBody()?.string()
                    Log.d("RegisterOutfit", "code=${response.code()}, error=$errorText, body=${response.body()}")

                    val body = response.body()
                    if (response.isSuccessful && body?.isSuccess == true) {
                        Toast.makeText(requireContext(), "아웃핏 등록 성공!", Toast.LENGTH_SHORT).show()

                        // ✅ 서버가 준 id 꺼내기 (Long/Int → String)
                        val outfitIdText = body.result?.id?.toString().orEmpty()
                        val registeredDate = formattedDateForAPI // "2025-08-06"

                        Log.d("RegisterFragment", "등록 성공 - 날짜: $registeredDate, outfit_id: $outfitIdText")

                        // ⭐ outfit_id와 날짜 매핑을 outfit_history에 저장
                        val historyPrefs = requireContext().getSharedPreferences("outfit_history", Context.MODE_PRIVATE)
                        val existingData = historyPrefs.getString("registered_outfits", "") ?: ""

                        val newEntry = "$registeredDate:$outfitIdText"
                        val updatedData = if (existingData.isBlank()) {
                            newEntry
                        } else {
                            "$existingData,$newEntry"
                        }

                        historyPrefs.edit().putString("registered_outfits", updatedData).apply()
                        Log.d("RegisterFragment", "등록 기록 저장: $newEntry")

                        // ⭐ 새로 등록된 정보를 CalendarFragment에 전달
                        val registrationPrefs = requireContext().getSharedPreferences("outfit_registration", Context.MODE_PRIVATE)
                        registrationPrefs.edit()
                            .putString("newly_registered_date", registeredDate)
                            .putInt("newly_registered_outfit_id", outfitIdText.toIntOrNull() ?: -1)
                            .putLong("registration_timestamp", System.currentTimeMillis())
                            .apply()

                        val bundle = Bundle().apply {
                            putString("save_date", formattedDate)
                            putString("outfit_image_path", imagePath)
                            putString("outfit_id", outfitIdText)
                        }
                        if (!isAdded) return@launch
                        findNavController().navigate(
                            R.id.action_registerFragment_to_saveFragment, bundle
                        )
                    }
                    else {
                        // 중복 날짜 실패 응답 처리 추가
                        val duplicateByBody =
                            (body?.isSuccess == false) &&
                                    (body.message?.contains("이미 해당 날짜에 등록된 코디가 있습니다") == true)
                        val duplicateByError = isDuplicateDateError(errorText)

                        if (duplicateByBody || duplicateByError) {
                            // 중복 날짜 다이얼로그 노출
                            showExistDialog()   // <- 네가 갖고 있는 다이얼로그 호출
                            // 버튼 재활성화만 하고 리턴
                            if (isAdded) binding.registerSaveBtn.isEnabled = true
                            return@launch
                        }
                        val msg = body?.message ?: parseServerReason(errorText) ?: "등록 실패"
                        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                    }
                } catch (ce: CancellationException) {
                    Log.w(
                        "RegisterOutfit",
                        "Job cancelled (lifecycle=${lifecycle.currentState}, isAdded=$isAdded)",
                        ce
                    )
                    return@launch
                }catch (e: Exception) {
                    Log.e("RegisterOutfit", "API 호출 실패: ${e.message}", e)
                    Toast.makeText(requireContext(), "서버 오류 발생", Toast.LENGTH_SHORT).show()
                } finally {
                    if (isAdded) binding.registerSaveBtn.isEnabled = true
                }
            }
        }

        // 뒤로가기 버튼
        binding.registerBackBtn.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun isDuplicateDateError(err: String?): Boolean {
        if (err.isNullOrBlank()) return false
        return try {
            val obj = org.json.JSONObject(err)
            val error = obj.optJSONObject("error")
            val code = error?.optString("errorCode")
            val reason = error?.optString("reason") ?: ""
            code == "INVALID_INPUT" && reason.contains("이미 해당 날짜에 등록된 코디가 있습니다")
        } catch (_: Exception) { false }
    }

    private fun parseServerReason(err: String?): String? =
        try {
            if (err.isNullOrBlank()) null
            else org.json.JSONObject(err).optJSONObject("error")
                ?.optString("reason")?.takeIf { it.isNotBlank() }
        } catch (_: Exception) { null }

    private fun showExistDialog() {
        val dialog = AlertDialog.Builder(requireContext()).create()
        val dialogView = layoutInflater.inflate(R.layout.outfit_exist_dialog, null)
        dialog.setView(dialogView)
        dialog.setCancelable(false)

        dialog.window?.setBackgroundDrawable(
            ContextCompat.getDrawable(requireContext(), R.drawable.rounded_white_bg)
        )

        val yesBtn = dialogView.findViewById<Button>(R.id.exist_dialog_yes_btn)

        // 액티비티 종료
        yesBtn.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()

        // 다이얼로그 너비 294dp
        val width = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 294f, resources.displayMetrics
        ).toInt()
        dialog.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun onMemoDone(memoText: String) {
        binding.registerMemoEt.setText(memoText)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}