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
import androidx.navigation.fragment.findNavController
import com.example.onfit.R
import com.example.onfit.TopSheetDialogFragment
import com.example.onfit.databinding.FragmentRegisterBinding
import com.google.android.material.chip.Chip
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
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

        // 갤러리에서 선택한 사진 받아오기
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

        // 날씨 칩 묶기
        val chipGroup1 = binding.registerWeatherChips
        val selectedWeatherTags = chipGroup1.checkedChipIds.mapNotNull { chipId ->
            val chip = chipGroup1.findViewById<Chip>(chipId)
            chip?.tag?.toString()?.toIntOrNull() // tag를 Int로 변환
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

        // 분위기 칩 묶기
        val selectedVibeTags = chipGroup2.checkedChipIds.mapNotNull { chipId ->
            val chip = chipGroup2.findViewById<Chip>(chipId)
            chip?.tag?.toString()?.toIntOrNull() // tag를 Int로 변환
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

        // 용도 칩 묶기
        val selectedUseTags = chipGroup3.checkedChipIds.mapNotNull { chipId ->
            val chip = chipGroup3.findViewById<Chip>(chipId)
            chip?.tag?.toString()?.toIntOrNull() // tag를 Int로 변환
        }

//        val feelsLikeTemp = selectedWeatherTags.firstOrNull() ?: 0
//        val jsonBody = JSONObject().apply {
//            put("date", formattedDate)
//            put("mainImage", imageUrl)
//            put("memo", memoText)
//            put("feelsLikeTemp", feelsLikeTemp)
//            put("moodTags", JSONArray(selectedVibeTags))
//            put("purposeTags", JSONArray(selectedUseTags))
//        }

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

        // 저장 버튼 누르면 날짜, 이미지 Bundle로 전달
        binding.registerSaveBtn.setOnClickListener {
            // 날짜 변환(API 용)
            val parts = binding.registerDateTv.text.toString().split(".")
            val formattedDateForAPI = "${parts[0]}-${parts[1]}-${parts[2]}"

            // 메모 텍스트
            val memoText = binding.registerMemoEt.text.toString()

            //  이미지 URL


            // Json body 생성
//            val jsonBody = JSONObject().apply {
//                put("date", formattedDateForAPI)
//                put("mainImage", imageUrl) // URL 변수 사용
//                put("memo", memoText)
//                put("feelsLikeTemp", selectedWeatherTags.firstOrNull() ?: 0)
//                put("moodTags", JSONArray(selectedVibeTags))
//                put("purposeTags", JSONArray(selectedUseTags))
//            }

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

    override fun onResume() {
        super.onResume()
        // 실행 중 bottom navigation view 보이지 않게
        activity?.findViewById<View>(R.id.bottomNavigationView)?.visibility = View.GONE
    }

    override fun onPause() {
        super.onPause()
        // 실행 안 할 때 bottom navigation view 다시 보이게
        activity?.findViewById<View>(R.id.bottomNavigationView)?.visibility = View.VISIBLE
    }
}