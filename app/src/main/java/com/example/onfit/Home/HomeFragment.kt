package com.example.onfit.Home

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.onfit.LatestStyleAdapter
import com.example.onfit.R
import com.example.onfit.Home.model.BestItem
import com.example.onfit.Home.model.SimItem
import com.example.onfit.databinding.FragmentHomeBinding
import com.example.onfit.Home.adapter.BestOutfitAdapter
import com.example.onfit.Home.adapter.SimiliarStyleAdapter
import com.example.onfit.OutfitRegister.ApiService
import com.example.onfit.OutfitRegister.RetrofitClient
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// fragment_home.xml을 사용하는 HomeFragment 정의
class HomeFragment : Fragment(R.layout.fragment_home) {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private var isShortText = false

    private lateinit var pickImageLauncher: ActivityResultLauncher<Intent>
    private var selectedImageUri: Uri? = null

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 갤러리 Launcher
        pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == AppCompatActivity.RESULT_OK) {
                selectedImageUri = result.data?.data

                // RegisterFragment로 이동하면서 선택한 이미지 URI -> File로 변환하여 파일 경로 전달
                selectedImageUri?.let { uri ->
                    Log.d("HomeFragment", "선택된 이미지 URI: $uri")
                    // uri -> 캐시 파일 변환
                    val cacheFile = uriToCacheFile(requireContext(), uri)
                    Log.d("HomeFragment", "파일 존재 여부: ${cacheFile.exists()}")
                    Log.d("HomeFragment", "파일 크기: ${cacheFile.length()}")

                    // 이미지 업로드 API 호출
                    uploadImageToServer(cacheFile)
                }
            }
        }
    }

    // API에 파일 업로드하고 Url 받아오기
    private fun uploadImageToServer(file: File) {
        Log.d("HomeFragment", "업로드 함수 실행됨: ${file.absolutePath}")
        val token = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VySWQiOjMsImlhdCI6MTc1NDA0NTA2NiwiZXhwIjoxNzU0NjQ5ODY2fQ.jF6SDERUqPs2_Qiv204CpgxN037D8mGaYq1g7a0fDb8"
        val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData("image", file.name, requestFile)

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val api = RetrofitClient.instance.create(ApiService::class.java)
                val response = api.uploadImage(token, body)

                val rawResponse = response.body()
                Log.d("HomeFragment", "서버 응답 바디: $rawResponse")

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body()?.isSuccess == true) {
                        val imageUrl = response.body()!!.result?.imageUrl
                        Log.d("HomeFragment", "이미지 업로드 성공: $imageUrl")

                        // 3️RegisterFragment로 URL 전달
                        val bundle = Bundle().apply {
                            putString("selectedImagePath", file.absolutePath)
                            putString("uploadedImageUrl", imageUrl) // URL 전달
                        }
                        findNavController().navigate(R.id.action_homeFragment_to_registerFragment, bundle)

                    } else {
                        val errorMsg = response.errorBody()?.string()
                        Log.e("HomeFragment", "업로드 실패: code=${response.code()}, error=$errorMsg")
                        Toast.makeText(requireContext(), "업로드 실패", Toast.LENGTH_SHORT).show()
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
                // 백그라운드에서는 Toast 호출 금지
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "서버 오류", Toast.LENGTH_SHORT).show()
                }
            }
        }
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

        // 등록 버튼 클릭 시 bottom sheet 올라옴
        binding.homeRegisterBtn.setOnClickListener {
            showBottomSheet()
        }
    }

    private fun showBottomSheet() {
        val view = layoutInflater.inflate(R.layout.bottom_sheet_dialog, null)
        val dialog = BottomSheetDialog(requireContext())
        dialog.setContentView(view)

        // 카메라 버튼 클릭
        view.findViewById<LinearLayout>(R.id.camera_btn).setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_registerFragment)
            dialog.dismiss()
        }

        // 사진첩 버튼 클릭
        view.findViewById<LinearLayout>(R.id.gallery_btn).setOnClickListener {
            openGallery()
            dialog.dismiss()
        }
        dialog.show()
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

    private fun uriToCacheFile(context: Context, uri: Uri): File {
        val inputStream = context.contentResolver.openInputStream(uri)
        val file = File(context.cacheDir, "selected_outfit.png") // 파일 이름 고정
        val outputStream = FileOutputStream(file)

        inputStream?.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }
        return file
    }

    // gallery_btn 클릭 시 실행
    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
        }
        pickImageLauncher.launch(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}