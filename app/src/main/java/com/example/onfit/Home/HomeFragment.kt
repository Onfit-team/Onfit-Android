package com.example.onfit.Home

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.media.session.MediaSession.Token
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.onfit.LatestStyleAdapter
import com.example.onfit.R
import com.example.onfit.Home.adapter.BestOutfitAdapter
import com.example.onfit.Home.adapter.SimiliarStyleAdapter
import com.example.onfit.Home.model.BestItem
import com.example.onfit.Home.model.SimItem
import com.example.onfit.Home.viewmodel.HomeViewModel
import com.example.onfit.KakaoLogin.util.TokenProvider
import com.example.onfit.databinding.FragmentHomeBinding
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
import com.example.onfit.network.RetrofitInstance
import java.text.SimpleDateFormat
import java.util.*

class HomeFragment : Fragment(R.layout.fragment_home) {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()
    private var isShortText = false
    private lateinit var pickImageLauncher: ActivityResultLauncher<Intent>
    private var selectedImageUri: Uri? = null

    //홈 화면 옷 리스트
    private val clothSuggestList = listOf(
        R.drawable.cloth1, R.drawable.cloth2, R.drawable.cloth3
    )

    private val similiarClothList = listOf(
        SimItem(R.drawable.simcloth1, "딱 좋음"),
        SimItem(R.drawable.simcloth2, "조금 추움"),
        SimItem(R.drawable.simcloth3, "많이 더움")
    )

    private val latestStyleList = listOf(
        SimItem(R.drawable.simcloth2, "4월 20일"),
        SimItem(R.drawable.simcloth3, "4월 19일"),
        SimItem(R.drawable.simcloth1, "4월 18일")
    )

    private val bestStyleList = listOf(
        BestItem(R.drawable.bestcloth1, "TOP 1", "큐야"),
        BestItem(R.drawable.bestcloth2, "TOP 2", "별이"),
        BestItem(R.drawable.bestcloth3, "TOP 3", "금이")
    )

    override fun onResume() {
        super.onResume()
        fetchCurrentWeather()
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

                    // 이미지 업로드 API 호출, Url 받아오기
                    uploadImageToServer(cacheFile)
                }
            }
        }
    }


    // API에 파일 업로드하고 Url 받아오기
    private fun uploadImageToServer(file: File) {
        Log.d("HomeFragment", "업로드 함수 실행됨: ${file.absolutePath}")
        val token = TokenProvider.getToken(requireContext())
        val header = "Bearer $token"
        val mime = requireContext().contentResolver.getType(Uri.fromFile(file)) ?: "image/jpeg"
        val requestFile = file.asRequestBody(mime.toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData("image", file.name, requestFile)

        // 📌 여기서 요청 정보 로그 찍기
        Log.d("Upload", "file=${file.name}, size=${file.length()}, mime=$mime")
        Log.d("Upload", "url=/items/upload, header=$header, fieldName=image")

        Log.d("UploadREQ",
            "url=http://15.164.35.198:3001/items/upload, " +
                    "auth=${header.take(20)}..., file=${file.name}, size=${file.length()}, mime=image/*"
        )

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val api = RetrofitClient.instance.create(ApiService::class.java)
                val response = api.uploadImage(header, body)

                // 서버에서 받은 전체 응답 로그로 출력
                val rawResponse = response.body()
                Log.d("HomeFragment", "서버 응답 바디: $rawResponse")

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        val imageUrl = response.body()!!.data?.imageUrl
                        Log.d("HomeFragment", "이미지 업로드 성공: $imageUrl")

                        // RegisterFragment로 URL 전달
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
        _binding = FragmentHomeBinding.bind(view)

        viewModel.fetchDate()
        viewModel.dateLiveData.observe(viewLifecycleOwner) {
            updateCombinedInfo(it, TokenProvider.getLocation(requireContext()))
        }
        viewModel.errorLiveData.observe(viewLifecycleOwner) {
            Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
        }

        setRandomImages()
        binding.refreshIcon.setOnClickListener { setRandomImages() }

        binding.similarStyleRecyclerView.apply {
            adapter = SimiliarStyleAdapter(similiarClothList)
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        }

        binding.latestStyleRecyclerView.apply {
            adapter = LatestStyleAdapter(latestStyleList)
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        }

        binding.bestoutfitRecycleView.apply {
            adapter = BestOutfitAdapter(bestStyleList)
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        }

        binding.homeSv.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            if (scrollY > 50 && !isShortText) {
                animateTextChange(binding.homeRegisterTv, "+")
                isShortText = true
            } else if (scrollY <= 50 && isShortText) {
                animateTextChange(binding.homeRegisterTv, "+ 등록하기")
                isShortText = false
            }
        }

        binding.locationBtn.setOnClickListener {
            val action = HomeFragmentDirections.actionHomeFragmentToLocationSettingFragment(true)
            findNavController().navigate(action)
        }

        binding.weatherBtn.setOnClickListener {
            fetchTomorrowWeather()
        }

        binding.homeRegisterBtn.setOnClickListener {
            showBottomSheet()
        }
    }

    private fun fetchCurrentWeather() {
        val token = TokenProvider.getToken(requireContext())
        val location = TokenProvider.getLocation(requireContext())
        lifecycleScope.launch {
            try {
                val response = RetrofitInstance.api.getCurrentWeather("Bearer $token")
                if (response.isSuccessful) {
                    val weather = response.body()?.result?.weather
                    val tempMax = weather?.tempMax?.toInt() ?: 0
                    val tempMin = weather?.tempMin?.toInt() ?: 0
                    val precipitation = weather?.precipitation?.toInt() ?: 0
                    val tempAvg = weather?.tempAvg?.toInt() ?: 0
                    val status = weather?.status ?: "Unknown"

                    updateCombinedInfo(getTodayDateString(), location)

                    binding.weatherInformTv.text = "최고 ${tempMax}°C · 최저 ${tempMin}°C · 강수확률 ${precipitation}%"
                    binding.tempTv.text = "${tempAvg}°C"

                    val fullText = "오늘 ${tempAvg}°C, 딱 맞는 스타일이에요!"
                    val targetText = "${tempAvg}°C"
                    val spannable = SpannableString(fullText)
                    val startIndex = fullText.indexOf(targetText)
                    val endIndex = startIndex + targetText.length
                    val color = ContextCompat.getColor(requireContext(), R.color.basic_blue)
                    spannable.setSpan(ForegroundColorSpan(color), startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    binding.weatherTitle.text = spannable
                    updateWeatherImages(status)
                } else {
                    binding.weatherInformTv.text = "날씨 정보를 가져오지 못했습니다."
                    binding.tempTv.text = ""
                }
            } catch (e: Exception) {
                binding.weatherInformTv.text = "날씨 오류: ${e.message}"
                binding.tempTv.text = ""
            }
        }
    }

    private fun fetchTomorrowWeather() {
        val token = TokenProvider.getToken(requireContext())
        val location = TokenProvider.getLocation(requireContext())
        lifecycleScope.launch {
            try {
                val response = RetrofitInstance.api.getTomorrowWeather("Bearer $token")
                if (response.isSuccessful) {
                    val weather = response.body()?.result?.weather
                    val tempMax = weather?.tempMax?.toInt() ?: 0
                    val tempMin = weather?.tempMin?.toInt() ?: 0
                    val precipitation = weather?.precipitation?.toInt() ?: 0
                    val tempAvg = weather?.tempAvg?.toInt() ?: 0
                    val status = weather?.status ?: "Unknown"

                    updateCombinedInfo(getTomorrowDateString(), location)

                    binding.tempTv.text = "${tempAvg}°C"
                    binding.weatherInformTv.text = "최고 ${tempMax}°C · 최저 ${tempMin}°C · 강수확률 ${precipitation}%"

                    val fullText = "내일 ${tempAvg}°C, 어떤 스타일일까요?"
                    val targetText = "${tempAvg}°C"
                    val spannable = SpannableString(fullText)
                    val startIndex = fullText.indexOf(targetText)
                    val endIndex = startIndex + targetText.length
                    val color = ContextCompat.getColor(requireContext(), R.color.basic_blue)
                    spannable.setSpan(ForegroundColorSpan(color), startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    binding.weatherTitle.text = spannable
                    updateWeatherImages(status)
                } else {
                    binding.weatherInformTv.text = "내일 날씨 정보를 가져오지 못했습니다."
                    binding.tempTv.text = ""
                }
            } catch (e: Exception) {
                binding.weatherInformTv.text = "내일 날씨 오류: ${e.message}"
                binding.tempTv.text = ""
            }
        }
    }

    private fun updateWeatherImages(status: String) {
        when (status) {
            "Storm" -> {
                binding.sunIv.setImageResource(R.drawable.weather_storm)
                binding.sunnyIv.setImageResource(R.drawable.weather_storm_bg)
            }
            "Snow" -> {
                binding.sunIv.setImageResource(R.drawable.weather_snow)
                binding.sunnyIv.setImageResource(R.drawable.weather_snow_bg)
            }
            "Rain" -> {
                binding.sunIv.setImageResource(R.drawable.weather_rain)
                binding.sunnyIv.setImageResource(R.drawable.weather_rain_bg)
            }
            "Fog" -> {
                binding.sunIv.setImageResource(R.drawable.weather_fog)
                binding.sunnyIv.setImageResource(R.drawable.weather_fog_bg)
            }
            "CloudFew" -> {
                binding.sunIv.setImageResource(R.drawable.weather_cloudfew)
                binding.sunnyIv.setImageResource(R.drawable.weather_cloudfew_bg)
            }
            "CloudMany" -> {
                binding.sunIv.setImageResource(R.drawable.weather_manycloud)
                binding.sunnyIv.setImageResource(R.drawable.weather_manycloud_bg)
            }
            "CloudBroken" -> {
                binding.sunIv.setImageResource(R.drawable.weather_brokencloud)
                binding.sunnyIv.setImageResource(R.drawable.weather_brokencloud_bg)
            }
            "Sun" -> {
                binding.sunIv.setImageResource(R.drawable.weather_sun)
                binding.sunnyIv.setImageResource(R.drawable.weather_sun_bg)
            }
            else -> {
                // 기본값
                binding.sunIv.setImageResource(R.drawable.weather_sun)
                binding.sunnyIv.setImageResource(R.drawable.weather_sun_bg)
            }
        }
    }


    private fun updateCombinedInfo(date: String, location: String) {
        binding.combinedInfoTv.text = "$date $location 날씨"
    }

    private fun getTodayDateString(): String {
        val calendar = Calendar.getInstance()
        val format = SimpleDateFormat("M월 d일", Locale.KOREA)
        return format.format(calendar.time)
    }

    private fun getTomorrowDateString(): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DATE, 1)
        val format = SimpleDateFormat("M월 d일", Locale.KOREA)
        return format.format(calendar.time)
    }

    private fun setRandomImages() {
        val mix = clothSuggestList.shuffled().take(3)
        binding.suggestedCloth1Iv.setImageResource(mix[0])
        binding.suggestedCloth2Iv.setImageResource(mix[1])
        binding.suggestedCloth3Iv.setImageResource(mix[2])
    }

    private fun showBottomSheet() {
        val view = layoutInflater.inflate(R.layout.bottom_sheet_dialog, null)
        val dialog = BottomSheetDialog(requireContext())
        dialog.setContentView(view)

        view.findViewById<LinearLayout>(R.id.camera_btn).setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_registerFragment)
            dialog.dismiss()
        }

        view.findViewById<LinearLayout>(R.id.gallery_btn).setOnClickListener {
            // 권한 확인 → Pictures 스캔 → 갤러리 열기
            ensurePhotoPermission { rescanPicturesAndOpenGallery() }
            dialog.dismiss()
        }

        dialog.show()
    }

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

    // 1) 권한 체크 (API33+ READ_MEDIA_IMAGES / 이하 READ_EXTERNAL_STORAGE)
    private fun ensurePhotoPermission(onGranted: () -> Unit) {
        val perm = if (Build.VERSION.SDK_INT >= 33)
            android.Manifest.permission.READ_MEDIA_IMAGES
        else
            android.Manifest.permission.READ_EXTERNAL_STORAGE

        if (ContextCompat.checkSelfPermission(requireContext(), perm) ==
            PackageManager.PERMISSION_GRANTED) {
            onGranted()
        } else {
            requestPermissionLauncher.launch(perm)
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                // 권한이 방금 허용되면 스캔 후 갤러리 열기
                rescanPicturesAndOpenGallery()
            } else {
                Toast.makeText(requireContext(), "사진 접근 권한이 필요해요", Toast.LENGTH_SHORT).show()
            }
        }

    // 2) Pictures 폴더 스캔 후 갤러리 열기
    private fun rescanPicturesAndOpenGallery() {
        // 에뮬레이터/Device Explorer로 넣은 파일을 인덱싱
        val picturesPath = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_PICTURES
        ).absolutePath

        MediaScannerConnection.scanFile(
            requireContext(),
            arrayOf(picturesPath),
            null
        ) { _, _ ->
            // 스캔 콜백에서 갤러리 열기 (스캔 완료 후)
            requireActivity().runOnUiThread { openGallery() }
        }
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
