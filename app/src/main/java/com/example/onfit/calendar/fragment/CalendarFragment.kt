package com.example.onfit.calendar.fragment

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.onfit.Community.model.CommunityOutfitsResponse
import com.example.onfit.KakaoLogin.util.TokenProvider
import com.example.onfit.OutfitRegister.ApiService
import com.example.onfit.OutfitRegister.RetrofitClient
import com.example.onfit.R
import com.example.onfit.calendar.adapter.CalendarAdapter
import com.example.onfit.calendar.viewmodel.CalendarViewModel
import com.example.onfit.calendar.viewmodel.CalendarUiState
import com.example.onfit.calendar.Network.CalendarService
import com.example.onfit.calendar.Network.OutfitImageResponse
import com.example.onfit.calendar.Network.MostUsedTagResponse
import com.example.onfit.network.RetrofitInstance
import com.example.onfit.Home.viewmodel.HomeViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class CalendarFragment : Fragment() {

    // 기존 UI 멤버 변수들
    private lateinit var rvCalendar: RecyclerView
    private lateinit var calendarAdapter: CalendarAdapter
    private lateinit var tvMostUsedStyle: TextView

    // 카메라 이미지 저장
    private lateinit var takePictureLauncher: ActivityResultLauncher<Uri>
    private var cameraImageUri: Uri? = null
    private var cameraImageFile: File? = null

    // 갤러리 이미지 저장
    private lateinit var pickImageLauncher: ActivityResultLauncher<Intent>
    private var selectedImageUri: Uri? = null

    // MVVM
    private lateinit var viewModel: CalendarViewModel
    private lateinit var homeViewModel: HomeViewModel

    // ⭐ 등록된 날짜와 outfit_id 매핑 저장
    private var registeredDates = mutableSetOf<String>()
    private var dateToOutfitIdMap = mutableMapOf<String, Int>() // 날짜 -> outfit_id 매핑

    // ⭐ 중복 실행 방지를 위한 플래그
    private var isLoadingDates = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this)[CalendarViewModel::class.java]
        homeViewModel = ViewModelProvider(this)[HomeViewModel::class.java]

        // 갤러리 Launcher
        pickImageLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == AppCompatActivity.RESULT_OK) {
                    selectedImageUri = result.data?.data

                    // 선택 이미지 URI -> 캐시 파일로 변환 후 업로드
                    selectedImageUri?.let { uri ->
                        Log.d("CalendarFragment", "선택된 이미지 URI: $uri")
                        val cacheFile = uriToCacheFile(requireContext(), uri)
                        Log.d("CalendarFragment", "파일 존재 여부: ${cacheFile.exists()}")
                        Log.d("CalendarFragment", "파일 크기: ${cacheFile.length()}")
                        uploadImageToServer(cacheFile)
                    }
                }
            }

        // 카메라 Launcher
        takePictureLauncher = registerForActivityResult(
            ActivityResultContracts.TakePicture()
        ) { success ->
            if (success) {
                val file = cameraImageFile
                if (file != null && file.exists()) {
                    // 갤러리와 동일하게 업로드 재사용
                    uploadImageToServer(file)
                } else {
                    Toast.makeText(requireContext(), "촬영 파일을 찾을 수 없어요.", Toast.LENGTH_SHORT).show()
                }
            } else {
                // 취소 시 임시파일 정리
                cameraImageFile?.takeIf { it.exists() }?.delete()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_calendar, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViews(view)
        setupCalendar()
        setupNavigationResultListener()
        observeViewModel()

        // ⭐ 기존 API를 활용한 방식으로 등록된 날짜 로드
        loadRegisteredDatesWithExistingAPI()

        // 가장 많이 사용된 태그 조회
        loadMostUsedTag()

        // ⭐ 실제 서버에 등록된 코디 로드
        loadRealRegisteredOutfits()
    }

    override fun onResume() {
        super.onResume()
        checkForNewRegistrations()

        rvCalendar.post {
            try {
                val currentMonthIndex = 24
                (rvCalendar.layoutManager as? LinearLayoutManager)?.scrollToPositionWithOffset(
                    currentMonthIndex,
                    0
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * ⭐ 기존 API를 활용해서 등록된 날짜를 찾는 방식
     * SharedPreferences에 저장된 등록 기록을 활용
     */
    private fun loadRegisteredDatesWithExistingAPI() {
        if (isLoadingDates) return
        isLoadingDates = true

        Log.d("CalendarFragment", "SharedPreferences에서 등록된 날짜 로드")

        // SharedPreferences에서 등록된 코디 정보 조회
        val prefs = requireContext().getSharedPreferences("outfit_history", Context.MODE_PRIVATE)
        val registeredOutfitsJson = prefs.getString("registered_outfits", null)

        if (!registeredOutfitsJson.isNullOrBlank()) {
            try {
                // JSON 파싱해서 등록된 날짜들 추출
                // 예: "2025-08-18:1,2025-08-17:2" 형태
                val outfitEntries = registeredOutfitsJson.split(",")

                registeredDates.clear()
                dateToOutfitIdMap.clear()

                outfitEntries.forEach { entry ->
                    val parts = entry.split(":")
                    if (parts.size == 2) {
                        val date = parts[0]
                        val outfitId = parts[1].toIntOrNull() ?: 1

                        registeredDates.add(date)
                        dateToOutfitIdMap[date] = outfitId
                    }
                }

                Log.d("CalendarFragment", "로드된 등록 날짜: $registeredDates")

                if (::calendarAdapter.isInitialized) {
                    calendarAdapter.updateRegisteredDates(registeredDates)
                }

            } catch (e: Exception) {
                Log.e("CalendarFragment", "등록 기록 파싱 실패", e)
            }
        } else {
            Log.d("CalendarFragment", "등록된 코디 없음 - 빈 캘린더 표시")
        }

        isLoadingDates = false
    }

    /**
     * ⭐ 특정 outfit_id로 코디 상세 정보 조회
     */
    private fun fetchOutfitDetails(outfitId: Int, onResult: (String?, String?) -> Unit) {
        val token = TokenProvider.getToken(requireContext())
        if (token.isBlank()) {
            Toast.makeText(requireContext(), "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val calendarService = RetrofitClient.instance.create(CalendarService::class.java)

                val response = calendarService.getOutfitText(
                    outfitId = outfitId,
                    authorization = "Bearer $token"
                )

                if (response.isSuccessful && response.body()?.result != null) {
                    val outfitDetails = response.body()?.result
                    val date = outfitDetails?.date
                    val memo = outfitDetails?.memo
                    onResult(date, memo)
                } else if (response.code() == 404) {
                    Log.e("CalendarFragment", "코디 상세 정보 조회 실패: 해당 Outfit이 없습니다.")
                    Toast.makeText(requireContext(), "해당 Outfit 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT)
                        .show()
                    onResult(null, null)
                } else {
                    Log.e("CalendarFragment", "코디 상세 정보 조회 실패: code=${response.code()}")
                    Toast.makeText(requireContext(), "코디 데이터를 불러올 수 없습니다.", Toast.LENGTH_SHORT)
                        .show()
                    onResult(null, null)
                }
            } catch (e: Exception) {
                Log.e("CalendarFragment", "코디 상세 정보 조회 오류", e)
                Toast.makeText(requireContext(), "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                onResult(null, null)
            }
        }
    }

    /**
     * ⭐ 날짜 클릭 이벤트 처리 - outfit_id로 코디 상세 데이터 확인 후 상세 화면으로 이동
     */
    private fun handleDateClick(dateString: String, hasOutfit: Boolean) {
        if (hasOutfit) {
            // 날짜에 등록된 코디 ID 가져오기
            val outfitId = dateToOutfitIdMap[dateString]
            if (outfitId != null) {
                fetchOutfitDetails(outfitId) { fetchedDate, memo ->
                    if (!fetchedDate.isNullOrBlank() && !memo.isNullOrBlank()) {
                        navigateToOutfitDetail(fetchedDate, outfitId, memo)
                    } else {
                        Toast.makeText(context, "해당 날짜의 코디 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            } else {
                Toast.makeText(context, "해당 날짜의 코디 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Log.d("CalendarFragment", "등록되지 않은 날짜 클릭: $dateString")
            showBottomSheet()
        }
    }

    /**
     * ⭐ 코디 상세 화면으로 이동 (날짜, outfit_id, 메모 전달)
     */
    private fun navigateToOutfitDetail(dateString: String, outfitId: Int, memo: String) {
        try {
            val bundle = Bundle().apply {
                putString("selected_date", dateString)
                putInt("outfit_id", outfitId)
                putString("memo", memo)
            }

            val navController = findNavController()

            runCatching {
                navController.navigate(R.id.action_calendarFragment_to_calendarSaveFragment, bundle)
            }.onFailure {
                runCatching {
                    navController.navigate(R.id.calendarSaveFragment, bundle)
                }.onFailure {
                    Log.e("CalendarFragment", "코디 상세 화면으로의 navigation이 정의되지 않음")
                    Toast.makeText(
                        context,
                        "$dateString 코디 (ID: $outfitId)\n메모: $memo",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

        } catch (e: Exception) {
            Log.e("CalendarFragment", "코디 상세 화면 이동 실패", e)
            Toast.makeText(context, "코디 상세 화면을 열 수 없습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * ⭐ HomeFragment와 동일한 방식으로 실제 등록된 코디 로드
     */
    private fun loadRealRegisteredOutfits() {
        val token = TokenProvider.getToken(requireContext())
        if (token.isBlank()) return

        Log.d("RealOutfits", "HomeViewModel을 통해 실제 코디 로드 시작")

        // HomeFragment와 동일한 방식
        homeViewModel.fetchRecentOutfits(token)
        homeViewModel.recentOutfits.observe(viewLifecycleOwner) { outfits ->
            val top7 = outfits?.take(7).orEmpty()

            Log.d("RealOutfits", "받은 코디 개수: ${top7.size}")

            top7.forEachIndexed { index, outfit ->
                val fullDate = outfit.date
                // ⭐ 서버에서 id를 안 보내므로 임시 고유 ID 생성
                val tempOutfitId = System.currentTimeMillis().toInt() + index

                if (!fullDate.isNullOrBlank() && fullDate.length >= 10) {
                    val date = fullDate.substring(0, 10)

                    Log.d("RealOutfits", "실제 코디: $date -> 임시 ID: $tempOutfitId")

                    addRegisteredDate(date, tempOutfitId)
                    saveOutfitRegistration(date, tempOutfitId)
                }
            }
        }

    }

    /**
     * ⭐ 새로 등록된 코디가 있는지 확인하는 함수
     */
    private fun checkForNewRegistrations() {
        val prefs =
            requireContext().getSharedPreferences("outfit_registration", Context.MODE_PRIVATE)
        val newlyRegisteredDate = prefs.getString("newly_registered_date", null)
        val newlyRegisteredId = prefs.getInt("newly_registered_outfit_id", -1)
        val timestamp = prefs.getLong("registration_timestamp", 0)

        // 5분 이내에 등록된 것만 처리 (중복 처리 방지)
        if (!newlyRegisteredDate.isNullOrBlank() &&
            System.currentTimeMillis() - timestamp < 5 * 60 * 1000
        ) {

            Log.d("CalendarFragment", "새로 등록된 코디 감지: $newlyRegisteredDate (ID: $newlyRegisteredId)")

            // SharedPreferences 클리어 (재처리 방지)
            prefs.edit()
                .remove("newly_registered_date")
                .remove("newly_registered_outfit_id")
                .remove("registration_timestamp")
                .apply()

            // ⭐ 새로 등록된 날짜 즉시 추가
            addRegisteredDate(newlyRegisteredDate, newlyRegisteredId)

            // ⭐ 등록 기록을 SharedPreferences에 저장
            saveOutfitRegistration(newlyRegisteredDate, newlyRegisteredId)

            Toast.makeText(requireContext(), "코디가 등록되었습니다!", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * ⭐ 코디 등록 기록을 SharedPreferences에 저장
     */
    private fun saveOutfitRegistration(date: String, outfitId: Int) {
        val prefs = requireContext().getSharedPreferences("outfit_history", Context.MODE_PRIVATE)
        val existingData = prefs.getString("registered_outfits", "") ?: ""

        val newEntry = "$date:$outfitId"
        val updatedData = if (existingData.isBlank()) {
            newEntry
        } else {
            "$existingData,$newEntry"
        }

        prefs.edit().putString("registered_outfits", updatedData).apply()
        Log.d("CalendarFragment", "등록 기록 저장: $newEntry")
    }

    /**
     * ⭐ Navigation 결과 수신 설정 (코디 등록 완료 감지)
     */
    private fun setupNavigationResultListener() {
        findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<String>("registered_date")
            ?.observe(viewLifecycleOwner) { registeredDate ->
                if (!registeredDate.isNullOrBlank()) {
                    Log.d("CalendarFragment", "Navigation 결과로 등록된 날짜 수신: $registeredDate")

                    // 임시 outfit_id 생성 (실제로는 등록 API 응답에서 받아야 함)
                    val tempOutfitId = System.currentTimeMillis().toInt() % 100000

                    addRegisteredDate(registeredDate, tempOutfitId)
                    saveOutfitRegistration(registeredDate, tempOutfitId)

                    Toast.makeText(requireContext(), "코디가 등록되었습니다!", Toast.LENGTH_SHORT).show()

                    findNavController().currentBackStackEntry?.savedStateHandle?.remove<String>("registered_date")
                }
            }
    }

    /**
     * ⭐ 새로운 날짜가 등록되었을 때 호출하는 함수
     */
    fun addRegisteredDate(dateString: String, outfitId: Int = -1) {
        val wasAdded = registeredDates.add(dateString)
        Log.d("CalendarDebug", "날짜 추가 시도: $dateString, 실제 추가됨: $wasAdded")

        if (wasAdded) {
            Log.d("CalendarFragment", "새로운 날짜 추가: $dateString")

            // outfit_id 매핑 저장
            if (outfitId != -1) {
                dateToOutfitIdMap[dateString] = outfitId
            }

            if (::calendarAdapter.isInitialized) {
                calendarAdapter.updateRegisteredDates(registeredDates)
            }
        }

        logCurrentState()
    }

    /**
     * ⭐ 특정 날짜의 코디 데이터를 서버에서 가져오는 함수 (기존 API 사용)
     */
    private fun loadOutfitForDate(dateString: String) {
        val token = TokenProvider.getToken(requireContext())
        if (token.isNullOrBlank()) {
            Toast.makeText(context, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
            return
        }

        // 해당 날짜의 outfit_id 찾기
        val outfitId = dateToOutfitIdMap[dateString]
        if (outfitId == null) {
            Toast.makeText(context, "해당 날짜의 코디 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val calendarService = RetrofitClient.instance.create(CalendarService::class.java)

                // ⭐ 기존 API 사용: 특정 outfit_id로 이미지 조회
                val response = calendarService.getOutfitImage(
                    outfitId = outfitId,
                    authorization = "Bearer $token"
                )

                if (response.isSuccessful) {
                    val outfitData = response.body()?.result
                    if (outfitData != null && !outfitData.mainImage.isNullOrBlank()) {
                        withContext(Dispatchers.Main) {
                            // 코디 상세 화면으로 이동 (outfit_id와 이미지 URL 전달)
                            navigateToOutfitDetail(dateString, outfitId, outfitData.mainImage!!)
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "해당 날짜에 등록된 코디 이미지가 없습니다.", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "코디 데이터를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("CalendarFragment", "특정 날짜 코디 로드 실패", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * 날짜를 outfit_id로 변환하는 함수
     */
    private fun dateToOutfitId(dateString: String): Int {
        return dateToOutfitIdMap[dateString] ?: run {
            // 매핑이 없으면 날짜를 숫자로 변환 (fallback)
            dateString.replace("-", "").toIntOrNull() ?: 1
        }
    }

    private fun loadRealOutfitIdsFromCommunity() {
        val token = TokenProvider.getToken(requireContext())
        if (token.isBlank()) return

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitInstance.api.getCommunityOutfits(
                    token = "Bearer $token",
                    order = "latest",
                    limit = 50  // 충분히 많이 가져오기
                )

                if (response.isSuccessful) {
                    val outfits = response.body()?.result?.outfits ?: emptyList()

                    withContext(Dispatchers.Main) {
                        // HomeViewModel에서 받은 이미지 URL과 Community API의 이미지 URL 매칭
                        matchOutfitIds(outfits)
                    }
                }
            } catch (e: Exception) {
                Log.e("CalendarFragment", "Community API 호출 실패", e)
            }
        }
    }

    private fun matchOutfitIds(communityOutfits: List<CommunityOutfitsResponse.Outfit>) {
        // HomeViewModel에서 받은 데이터와 Community 데이터를 이미지 URL로 매칭
        homeViewModel.recentOutfits.value?.forEach { recentOutfit ->
            val matchingCommunityOutfit = communityOutfits.find {
                it.mainImage == recentOutfit.image
            }

            if (matchingCommunityOutfit != null) {
                val date = recentOutfit.date.substring(0, 10)
                val realOutfitId = matchingCommunityOutfit.id

                Log.d("RealMapping", "실제 매핑: $date -> 진짜 ID: $realOutfitId")

                // 실제 ID로 업데이트
                dateToOutfitIdMap[date] = realOutfitId
                saveOutfitRegistration(date, realOutfitId)
            }
        }
    }

    /**
     * ⭐ 디버깅을 위한 현재 상태 로그 출력
     */
    private fun logCurrentState() {
        Log.d("CalendarDebug", "=== 캘린더 상태 디버깅 ===")
        Log.d("CalendarDebug", "등록된 날짜 개수: ${registeredDates.size}")
        Log.d("CalendarDebug", "등록된 날짜 목록: $registeredDates")
        Log.d("CalendarDebug", "날짜-ID 매핑: $dateToOutfitIdMap")
        Log.d("CalendarDebug", "어댑터 초기화 여부: ${::calendarAdapter.isInitialized}")
        Log.d("CalendarDebug", "========================")
    }

    // API에 갤러리, 카메라에서 고른 사진 업로드하고 Url 받아오기
    private fun uploadImageToServer(file: File) {
        Log.d("Calendar", "Step 1: 함수 진입")
        Log.d(
            "UploadDebug",
            "파일 존재=${file.exists()}, size=${file.length()}, path=${file.absolutePath}"
        )

        // 1. 토큰 체크
        val token = TokenProvider.getToken(requireContext())
        require(!token.isNullOrBlank()) { "토큰이 없다" }
        val header = "Bearer $token"
        Log.d("UploadDebug", "Step 2: 토큰=$token")

        // 2. 파일 검증 로그
        val exists = file.exists()
        val length = file.length()
        val canRead = file.canRead()
        val ext = file.extension.lowercase()
        val bmpTest = BitmapFactory.decodeFile(file.absolutePath) != null

        Log.d(
            "UploadCheck",
            "exists=$exists, canRead=$canRead, length=$length, ext=$ext, bitmapReadable=$bmpTest"
        )

        require(exists && length > 0 && bmpTest) { "이미지 파일이 손상되었거나 크기가 0입니다." }

        // 3. 확장자 기반 MIME 자동 지정
        var uploadFile = file
        var uploadMime = when (ext) {
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            else -> "application/octet-stream"
        }

        // 3-1. PNG -> JPG 변환
        if (ext == "png") {
            try {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                require(bitmap != null) { "PNG 디코딩 실패" }

                val jpgFile =
                    File(requireContext().cacheDir, "upload_temp_${System.currentTimeMillis()}.jpg")
                FileOutputStream(jpgFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                }
                uploadFile = jpgFile
                uploadMime = "image/jpeg"
                Log.d("UploadDebug", "PNG → JPG 변환 완료: ${jpgFile.absolutePath}")
            } catch (e: Exception) {
                Log.e("UploadDebug", "PNG → JPG 변환 실패", e)
                uploadFile = file
                uploadMime = "image/png"
            }
        }

        // 4. RequestBody + MultipartBody.Part 생성
        val requestFile = uploadFile.asRequestBody(uploadMime.toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData("image", uploadFile.name, requestFile)

        // 6. 업로드 실행
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val api = RetrofitClient.instance.create(ApiService::class.java)
                val response = api.uploadImage(header, body)

                val bodyObj = response.body()

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && bodyObj?.ok == true) {
                        val imageUrl = bodyObj.payload?.imageUrl

                        if (imageUrl.isNullOrBlank()) {
                            Toast.makeText(
                                requireContext(),
                                "이미지 URL을 받지 못했어요.",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@withContext
                        }

                        // RegisterFragment로 URL 전달
                        val bundle = Bundle().apply {
                            putString("selectedImagePath", uploadFile.absolutePath)
                            putString("uploadedImageUrl", imageUrl)
                        }

                        if (!isAdded || !viewLifecycleOwner.lifecycle.currentState.isAtLeast(
                                Lifecycle.State.STARTED
                            )
                        ) {
                            return@withContext
                        }

                        if (!isAdded || !viewLifecycleOwner.lifecycle.currentState.isAtLeast(
                                Lifecycle.State.STARTED
                            )
                        ) return@withContext
                        val nav = findNavController()

                        // 액션으로 시도
                        runCatching {
                            nav.navigate(R.id.action_calendarFragment_to_registerFragment, bundle)
                        }.onFailure {
                            runCatching {
                                nav.navigate(R.id.registerFragment, bundle)
                            }
                        }
                    } else {
                        val errorMsg = response.errorBody()?.string()
                        Log.e(
                            "HomeFragment",
                            "업로드 실패: code=${response.code()}, error=$errorMsg, body=$bodyObj"
                        )
                        Toast.makeText(
                            requireContext(),
                            bodyObj?.message ?: "업로드 실패",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "서버 오류", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupViews(view: View) {
        rvCalendar = view.findViewById(R.id.rvCalendar)
        tvMostUsedStyle = view.findViewById(R.id.tvMostUsedStyle)

        tvMostUsedStyle.text = "#포멀 스타일이 가장 많았어요!"

        view.findViewById<View>(R.id.btnStyleOutfits)?.setOnClickListener {
            navigateToStyleOutfits()
        }

        view.findViewById<View>(R.id.calendar_register_btn)?.setOnClickListener {
            try {
                showBottomSheet()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "이동 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupCalendar() {
        val months = generateMonths()

        calendarAdapter = CalendarAdapter(
            months = months,
            registeredDates = registeredDates,
            onDateClick = { dateString, hasOutfit ->
                handleDateClick(dateString, hasOutfit)
            }
        )

        rvCalendar.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = calendarAdapter
            PagerSnapHelper().attachToRecyclerView(this)
        }

        scrollToCurrentMonth()
    }

    private fun loadMostUsedTag() {
        viewModel.loadMostUsedTag()
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                updateTagUI(state)
            }
        }
    }

    private fun updateTagUI(state: CalendarUiState) {
        when {
            state.isTagLoading -> {
                tvMostUsedStyle.text = "데이터를 불러오는 중..."
            }

            state.mostUsedTag != null -> {
                val tag = state.mostUsedTag
                tvMostUsedStyle.text = "#${tag.tag} 스타일이 가장 많았어요! (${tag.count}개)"
            }

            state.tagErrorMessage != null -> {
                tvMostUsedStyle.text = "#포멀 스타일이 가장 많았어요!"
                viewModel.clearTagError()
            }

            else -> {
                tvMostUsedStyle.text = "#포멀 스타일이 가장 많았어요!"
            }
        }
    }

    private fun generateMonths(): List<MonthData> {
        val months = mutableListOf<MonthData>()
        val calendar = Calendar.getInstance()

        calendar.add(Calendar.MONTH, -24)

        repeat(37) {
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH) + 1
            val monthData = MonthData(year, month)
            months.add(monthData)
            calendar.add(Calendar.MONTH, 1)
        }

        return months
    }

    private fun scrollToCurrentMonth() {
        val currentMonthIndex = 24
        rvCalendar.post {
            rvCalendar.postDelayed({
                try {
                    (rvCalendar.layoutManager as? LinearLayoutManager)?.scrollToPositionWithOffset(
                        currentMonthIndex,
                        0
                    )
                } catch (e: Exception) {
                    rvCalendar.scrollToPosition(currentMonthIndex)
                }
            }, 100)
        }
    }

    private fun navigateToOutfitRegister(
        dateString: String,           // 필요 없으면 지워도 됨
        outfitId: Int?,               // ← 호출할 때 넘겨줘
        imageUrl: String?             // ← 호출할 때 넘겨줘
    ) {
        val b = Bundle().apply {
            putInt("outfit_id", outfitId ?: -1)     // ❗ nav_graph와 키 일치
            putString("image_url", imageUrl ?: "")  // ❗ nav_graph와 키 일치
        }
        findNavController().navigate(R.id.calendarSaveFragment, b)
    }

    private fun navigateToStyleOutfits() {
        try {
            val navController = findNavController()
            val targetDestination = navController.graph.findNode(R.id.styleOutfitsFragment)

            if (targetDestination != null) {
                navController.navigate(R.id.styleOutfitsFragment)
            } else {
                Toast.makeText(
                    requireContext(),
                    "StyleOutfitsFragment를 찾을 수 없습니다.",
                    Toast.LENGTH_SHORT
                ).show()
            }

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Navigation 오류: ${e.message}", Toast.LENGTH_LONG)
                .show()
        }
    }

    // bottom sheet
    private fun showBottomSheet() {
        val view = layoutInflater.inflate(R.layout.bottom_sheet_dialog, null)
        val dialog = BottomSheetDialog(requireContext())
        dialog.setContentView(view)

        // 카메라 버튼
        view.findViewById<LinearLayout>(R.id.camera_btn).setOnClickListener {
            ensureCameraPermission {
                openCamera()
                dialog.dismiss()
            }

        }
        // 갤러리 버튼
        view.findViewById<LinearLayout>(R.id.gallery_btn).setOnClickListener {
            ensurePhotoPermission { rescanPicturesAndOpenGallery() }
            dialog.dismiss()
        }
    }

        private fun uriToCacheFile(context: Context, uri: Uri): File {
            val inputStream = context.contentResolver.openInputStream(uri)
            val file = File(context.cacheDir, "selected_outfit.png")
            val outputStream = FileOutputStream(file)
            inputStream?.use { input ->
                outputStream.use { output -> input.copyTo(output) }
            }
            return file
        }


        // 카메라 권한
        private fun ensureCameraPermission(onGranted: () -> Unit) {
            val perm = android.Manifest.permission.CAMERA
            if (ContextCompat.checkSelfPermission(requireContext(), perm) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                onGranted()
            } else {
                // 재사용 가능하게 RequestPermission launcher 하나 더 써도 되고,
                // 여기선 간단히 임시로 런처 생성
                registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                    if (granted) onGranted() else
                        Toast.makeText(requireContext(), "카메라 권한이 필요해요", Toast.LENGTH_SHORT).show()
                }.launch(perm)
            }
        }

        // 카메라 열기
        private fun openCamera() {
            try {
                val (file, uri) = createCameraOutput(requireContext()) // ← 지역 val
                cameraImageFile = file
                cameraImageUri = uri
                takePictureLauncher.launch(uri) // 지역 val은 non-null
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "카메라 실행 실패: ${e.message}", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        private fun createCameraOutput(ctx: Context): Pair<File, Uri> {
            val baseDir = ctx.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: ctx.cacheDir
            val outDir = File(baseDir, "camera").apply { mkdirs() }
            val file = File(outDir, "camera_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
            return file to uri
        }

        // 갤러리 권한
        private fun ensurePhotoPermission(onGranted: () -> Unit) {
            val perm = if (Build.VERSION.SDK_INT >= 33)
                android.Manifest.permission.READ_MEDIA_IMAGES
            else
                android.Manifest.permission.READ_EXTERNAL_STORAGE

            if (ContextCompat.checkSelfPermission(requireContext(), perm) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                onGranted()
            } else {
                requestPermissionLauncher.launch(perm)
            }
        }

        // 권한 허용 시 갤러리 열기
        private val requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                if (granted) {
                    rescanPicturesAndOpenGallery()
                } else {
                    Toast.makeText(requireContext(), "사진 접근 권한이 필요해요", Toast.LENGTH_SHORT).show()
                }
            }


        // Pictures 폴더 스캔
        private fun rescanPicturesAndOpenGallery() {
            val picturesPath = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES
            ).absolutePath

            MediaScannerConnection.scanFile(
                requireContext(),
                arrayOf(picturesPath),
                null
            ) { _, _ ->
                requireActivity().runOnUiThread { openGallery() }
            }
        }

        // 갤러리 열기
        private fun openGallery() {
            val intent = Intent(Intent.ACTION_PICK).apply { type = "image/*" }
            pickImageLauncher.launch(intent)
        }

        /**
         * 외부에서 태그 통계 새로고침
         */
        fun refreshMostUsedTag() {
            loadMostUsedTag()
        }

        private fun loadOutfitDataInBackground(dateString: String) {
            viewModel.onDateSelected(dateString)
        }
    }

    data class MonthData(
        val year: Int,
        val month: Int
    )
