package com.example.onfit.HomeRegister.fragment

import android.app.DatePickerDialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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

        // HomeFragment 갤러리에서 선택한 사진 받아오기
        val imagePath = arguments?.getString("selectedImagePath")
        imagePath?.let {
            val bitmap = BitmapFactory.decodeFile(it)
            binding.registerOutfitIv.setImageBitmap(bitmap)
        }

        // 날짜 오늘 날짜로 기본 설정
        val today = Calendar.getInstance().time
        val dateFormat = SimpleDateFormat("yyyy.MM.dd", Locale.KOREA)
        binding.registerDateTv.text = dateFormat.format(today)

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

                        val bundle = Bundle().apply {
                            putString("save_date", formattedDate)
                            putString("outfit_image_path", imagePath)
                        }
                        if (!isAdded) return@launch
                        findNavController().navigate(
                            R.id.action_registerFragment_to_saveFragment, bundle
                        )
                    }
                    else {
                        val err = response.errorBody()?.string()
                        Log.d("RegisterOutfit", "code=${response.code()}, error=$err, body=$body")
                        Toast.makeText(requireContext(), body?.message ?: "등록 실패", Toast.LENGTH_SHORT).show()
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

    override fun onMemoDone(memoText: String) {
        binding.registerMemoEt.setText(memoText)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}