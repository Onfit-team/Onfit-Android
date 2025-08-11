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
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

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
            val parts = binding.registerDateTv.text.toString().split(".")
            val formattedDateForAPI = "${parts[0]}-${parts[1]}-${parts[2]}"

            //  HomeFragment의 갤러리에서 고른 이미지의 URL
            val imageUrl = arguments?.getString("uploadedImageUrl")

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

            // RequestBody로 변환
            val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaTypeOrNull())

            // Retrofit API 호출(발급받은 임시 토큰 사용)
            lifecycleScope.launch {
                try {
                    val api = RetrofitClient.instance.create(ApiService::class.java)

                    val token = TokenProvider.getToken(requireContext())
                    val header = "Bearer $token"
                    val response = api.registerOutfit(header, requestBody)

                    if (response.isSuccessful && response.body()?.isSuccess == true) {
                        Toast.makeText(requireContext(), "아웃핏 등록 성공!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "등록 실패", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("RegisterOutfit", "API 호출 실패: ${e.message}", e)
                    e.printStackTrace()
                    Toast.makeText(requireContext(), "서버 오류 발생", Toast.LENGTH_SHORT).show()
                }
            }

            // 날짜 변환(Bundle용)
            val formattedDate = "${parts[1].toInt()}월 ${parts[2].toInt()}일"
            // Fragment에서 전달받은 이미지 파일 경로 사용
            val imagePath = arguments?.getString("selectedImagePath")

            // 파일 경로만 전달
            val bundle = Bundle().apply {
                putString("save_date", formattedDate)
                putString("outfit_image_path", imagePath)
            }
            findNavController().navigate(R.id.action_registerFragment_to_saveFragment, bundle)
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