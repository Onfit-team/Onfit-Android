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
import java.util.Calendar as JavaCalendar
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
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

        // 🔥 더미 데이터 즉시 추가
        addDummyDataToCalendar()

        // arguments 처리
        handleNavigationArguments()

    }

    private fun addDummyDataToCalendar() {
        Log.d("CalendarFragment", "🎭 더미 데이터를 캘린더에 추가")

        // 🔥 JavaCalendar로 변경
        val calendar = JavaCalendar.getInstance()
        val currentYear = calendar.get(JavaCalendar.YEAR)
        val currentMonth = calendar.get(JavaCalendar.MONTH) + 1

        // 하드코딩된 더미 코디 데이터 (현재 월 기준)
        val dummyOutfits = mapOf(
            "$currentYear-${String.format("%02d", currentMonth)}-13" to 1001, // 이번 달 20일
            "$currentYear-${String.format("%02d", currentMonth)}-12" to 1002, // 이번 달 19일
            "$currentYear-${String.format("%02d", currentMonth)}-11" to 1003,  // 이번 달 18일
            "$currentYear-${String.format("%02d", currentMonth)}-10" to 1004,
            )

        dummyOutfits.forEach { (date, outfitId) ->
            // 등록된 날짜에 추가
            registeredDates.add(date)
            dateToOutfitIdMap[date] = outfitId

            // SharedPreferences에 저장
            saveOutfitRegistration(date, outfitId)

            Log.d("CalendarFragment", "더미 코디 추가: $date -> ID: $outfitId")
        }

        // 어댑터 업데이트
        if (::calendarAdapter.isInitialized) {
            calendarAdapter.updateRegisteredDates(registeredDates)
        }

        Log.d("CalendarFragment", "✅ 더미 데이터 추가 완료: ${dummyOutfits.size}개")
    }

    private fun handleNavigationArguments() {
        arguments?.let { bundle ->
            val targetDate = bundle.getString("target_date")
            val outfitNumber = bundle.getInt("outfit_number", -1)
            val fromOutfitRecord = bundle.getBoolean("from_outfit_record", false)

            if (!targetDate.isNullOrBlank() && outfitNumber != -1 && fromOutfitRecord) {
                Log.d("CalendarFragment", "🎯 ClothesDetail에서 전달받은 날짜: $targetDate, 코디: $outfitNumber")

                view?.post {
                    // 해당 날짜로 스크롤 (간단한 버전)
                    Toast.makeText(requireContext(), "${targetDate}의 코디 ${outfitNumber}번", Toast.LENGTH_SHORT).show()
                }
            }
        }
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
    // 🔥 CalendarFragment에서 실제 ID 찾는 방법 추가

    /**
     * 날짜 클릭 시 처리 - 실제 ID 찾기 로직 추가
     */
    private fun handleDateClick(dateString: String, hasOutfit: Boolean) {
        if (hasOutfit) {
            val storedOutfitId = dateToOutfitIdMap[dateString]
            Log.d("CalendarFragment", "날짜 클릭: $dateString, 저장된 ID: $storedOutfitId")

            when {
                // 1. 더미 코디 (1001~1004)
                storedOutfitId != null && isDummyOutfitId(storedOutfitId) -> {
                    Log.d("CalendarFragment", "🎭 더미 코디 감지")
                    navigateToDummyOutfitDetail(dateString, storedOutfitId)
                }

                // 2. 실제 코디 - 하지만 임시 ID일 수 있음
                storedOutfitId != null -> {
                    Log.d("CalendarFragment", "📱 실제 코디 감지 - ID 유효성 확인")

                    // 🔥 먼저 API 호출해서 유효한지 확인
                    fetchOutfitDetails(storedOutfitId) { fetchedDate, memo ->
                        if (!fetchedDate.isNullOrBlank()) {
                            // 유효한 ID - 바로 이동
                            navigateToOutfitDetail(fetchedDate, storedOutfitId, memo ?: "등록된 코디입니다.")
                        } else {
                            // 404 오류 - 실제 ID 찾기 시도
                            Log.w("CalendarFragment", "⚠️ 저장된 ID가 유효하지 않음. 실제 ID 검색 시작")
                            findRealOutfitIdForDate(dateString)
                        }
                    }
                }

                else -> {
                    Log.w("CalendarFragment", "⚠️ 저장된 ID가 없음")
                    Toast.makeText(context, "해당 날짜의 코디 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Log.d("CalendarFragment", "등록되지 않은 날짜 클릭: $dateString")
            showBottomSheet()
        }
    }

    /**
     * 🔥 NEW: HomeViewModel 데이터로 실제 ID 찾기
     */
    /**
     * 🔥 실제 ID 찾기 로직 개선 - HomeViewModel 데이터 사용
     */
    private fun findRealOutfitIdForDate(dateString: String) {
        Log.d("CalendarFragment", "🔍 $dateString 의 실제 ID 검색 시작")

        // HomeViewModel에서 해당 날짜의 코디 찾기
        homeViewModel.recentOutfits.value?.let { outfits ->
            val matchingOutfit = outfits.find { outfit ->
                val outfitDate = outfit.date?.take(10) // "2025-08-20T..." -> "2025-08-20"
                outfitDate == dateString
            }

            if (matchingOutfit != null) {
                Log.d("CalendarFragment", "✅ HomeViewModel에서 해당 날짜 코디 발견")

                // 🔥 방법 1: Calendar API로 이미지 URL 매칭
                findOutfitIdByImageUrl(matchingOutfit.image ?: "", dateString)

            } else {
                Log.w("CalendarFragment", "❌ HomeViewModel에서 해당 날짜 코디를 찾을 수 없음")

                // 🔥 방법 2: 직접 날짜 기반으로 최신 코디 찾기
                findLatestOutfitForDate(dateString)
            }
        } ?: run {
            Log.w("CalendarFragment", "❌ HomeViewModel에 코디 데이터가 없음")
            // HomeViewModel 데이터가 없어도 날짜 기반으로 검색 시도
            findLatestOutfitForDate(dateString)
        }
    }

    /**
     * 🔥 NEW: 날짜 주변 최신 코디 찾기 (fallback)
     */
    private fun findLatestOutfitAroundDate(dateString: String) {
        val token = TokenProvider.getToken(requireContext())
        if (token.isBlank()) return

        Log.d("CalendarFragment", "📅 $dateString 주변 최신 코디 검색")

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val calendarService = RetrofitClient.instance.create(CalendarService::class.java)

                // 최근 등록된 코디 ID 역순으로 확인 (가장 최근 것부터)
                for (id in 100 downTo 1) {
                    try {
                        val response = calendarService.getOutfitText(
                            outfitId = id,
                            authorization = "Bearer $token"
                        )

                        if (response.isSuccessful && response.body()?.result != null) {
                            val outfitDetails = response.body()?.result
                            val outfitDate = outfitDetails?.date?.take(10)

                            // 날짜가 일치하거나 비슷한 시기의 코디 찾기
                            if (outfitDate == dateString || isDateClose(outfitDate, dateString)) {
                                Log.d("CalendarFragment", "✅ 주변 날짜 코디 발견: ID=$id, 날짜=$outfitDate")

                                withContext(Dispatchers.Main) {
                                    // 실제 ID로 업데이트
                                    dateToOutfitIdMap[dateString] = id
                                    saveOutfitRegistration(dateString, id)

                                    // 상세 화면으로 이동
                                    navigateToOutfitDetail(outfitDate ?: dateString, id, outfitDetails?.memo ?: "등록된 코디입니다.")
                                }
                                return@launch
                            }
                        }
                    } catch (e: Exception) {
                        continue
                    }
                }

                // 모든 검색 실패
                withContext(Dispatchers.Main) {
                    Log.w("CalendarFragment", "❌ 주변 날짜에서도 코디를 찾을 수 없음")
                    Toast.makeText(context, "해당 날짜의 코디 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                Log.e("CalendarFragment", "주변 날짜 검색 실패", e)
            }
        }
    }

    /**
     * 🔥 NEW: 실제 API Outfit ID 판별 함수 (음수 임시 ID)
     */
    private fun isRealApiOutfitId(outfitId: Int): Boolean {
        // 실제 API에서 받은 코디는 임시로 음수 ID 부여됨
        return outfitId < 0
    }

    /**
     * 🔥 NEW: 더미 코디 메모 저장 (수정된 버전)
     */
    private fun saveDummyOutfitMemo(targetDate: String, outfitNumber: Int) {
        val prefs = requireContext().getSharedPreferences("outfit_memos", Context.MODE_PRIVATE)

        val dummyMemo = getDummyMemoForOutfit(outfitNumber)

        prefs.edit().putString("memo_$targetDate", dummyMemo).apply()
        Log.d("CalendarFragment", "더미 메모 저장: $targetDate -> $dummyMemo")
    }

    /**
     * 🔥 NEW: 실제 API 코디 상세 화면으로 이동 (memo를 설명으로 변경)
     */
    private fun navigateToRealOutfitDetail(dateString: String, imageUrl: String, description: String) {
        try {
            val bundle = Bundle().apply {
                putString("selected_date", dateString)
                putString("main_image_url", imageUrl)
                putString("memo", description) // 고정된 설명 사용
                putBoolean("is_real_outfit", true) // 실제 코디 표시

                // 🔥 실제 코디는 개별 아이템 이미지가 없으므로 메인 이미지만 표시
                putStringArrayList("item_image_urls", arrayListOf()) // 빈 리스트
            }

            val navController = findNavController()
            runCatching {
                navController.navigate(R.id.action_calendarFragment_to_calendarSaveFragment, bundle)
            }.onFailure {
                runCatching {
                    navController.navigate(R.id.calendarSaveFragment, bundle)
                }.onFailure {
                    Log.e("CalendarFragment", "실제 코디 상세 화면으로의 navigation 실패")
                    Toast.makeText(context, "실제 코디 ($dateString)", Toast.LENGTH_LONG).show()
                }
            }

            Log.d("CalendarFragment", "✅ 실제 코디 상세 화면으로 이동: $dateString")

        } catch (e: Exception) {
            Log.e("CalendarFragment", "실제 코디 상세 화면 이동 실패", e)
            Toast.makeText(context, "코디 상세 화면을 열 수 없습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 🔥 NEW: 실제 API 코디의 진짜 ID 찾아서 이동
     */
    private fun findRealOutfitIdAndNavigate(dateString: String, tempOutfitId: Int) {
        Log.d("CalendarFragment", "실제 코디 ID 검색 시작: 임시ID=$tempOutfitId, 날짜=$dateString")

        // HomeViewModel에서 해당 날짜의 코디 찾기
        homeViewModel.recentOutfits.value?.let { outfits ->
            val matchingOutfit = outfits.find { outfit ->
                val outfitDate = outfit.date?.take(10) // "2025-08-18T..." -> "2025-08-18"
                outfitDate == dateString
            }

            if (matchingOutfit != null) {
                Log.d("CalendarFragment", "매칭된 실제 코디 발견: ${matchingOutfit.image}")

                // 🔥 실제 코디는 이미지 URL만 사용 (존재하지 않는 필드 참조 제거)
                navigateToRealOutfitDetail(dateString, matchingOutfit.image ?: "", "실제 등록된 코디")
            } else {
                Log.w("CalendarFragment", "해당 날짜의 실제 코디를 찾을 수 없음")
                Toast.makeText(context, "해당 날짜의 코디 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
            }
        } ?: run {
            Log.w("CalendarFragment", "HomeViewModel에 코디 데이터가 없음")
            Toast.makeText(context, "코디 데이터를 불러오는 중입니다. 잠시 후 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
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

        homeViewModel.fetchRecentOutfits(token)
        homeViewModel.recentOutfits.observe(viewLifecycleOwner) { outfits ->
            val top7 = outfits?.take(7).orEmpty()

            Log.d("RealOutfits", "받은 코디 개수: ${top7.size}")

            top7.forEachIndexed { index, outfit ->
                val fullDate = outfit.date

                if (!fullDate.isNullOrBlank() && fullDate.length >= 10) {
                    val date = fullDate.substring(0, 10) // "2025-08-18T..." -> "2025-08-18"

                    // 🔥 음수 임시 ID 생성 (실제 코디 구분용)
                    val tempOutfitId = -(System.currentTimeMillis().toInt() + index)

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
        val calendar = JavaCalendar.getInstance()

        calendar.add(JavaCalendar.MONTH, -24)

        repeat(37) {
            val year = calendar.get(JavaCalendar.YEAR)
            val month = calendar.get(JavaCalendar.MONTH) + 1
            val monthData = MonthData(year, month)
            months.add(monthData)
            calendar.add(JavaCalendar.MONTH, 1)
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

    private fun navigateToOutfitRegister(dateString: String) {
        val action =
            CalendarFragmentDirections.actionCalendarFragmentToCalendarSaveFragment(dateString)
        findNavController().navigate(action)
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
        dialog.show()
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

    // 🔥 NEW: ClothesDetailFragment에서 호출되는 함수 - CalendarFragment에 추가
    fun navigateToCalendarWithOutfit(outfitNumber: Int) {
        try {
            // 🔥 하드코딩된 코디별 등록 날짜
            val outfitDateMap = mapOf(
                1 to "2024-08-13", // cody1 -> 8/13
                2 to "2024-08-12", // cody2 -> 8/12
                3 to "2024-08-11", // cody3 -> 8/11
                4 to "2024-08-10"  // cody4 -> 8/10
            )

            val targetDate = outfitDateMap[outfitNumber]

            if (targetDate != null) {
                Log.d("CalendarFragment", "🗓️ 외부에서 코디 ${outfitNumber}번 요청 -> ${targetDate}로 이동")

                // 🔥 더미 데이터 추가 (실제 캘린더에 표시되도록)
                addDummyOutfitData(targetDate, outfitNumber)

                // 🔥 해당 날짜로 캘린더 스크롤
                scrollToSpecificDate(targetDate)

                Toast.makeText(requireContext(), "코디 ${outfitNumber}번이 등록된 ${targetDate}로 이동합니다", Toast.LENGTH_SHORT).show()

            } else {
                Log.e("CalendarFragment", "❌ 코디 ${outfitNumber}번의 날짜 정보 없음")
                Toast.makeText(requireContext(), "해당 코디의 등록 날짜를 찾을 수 없습니다", Toast.LENGTH_SHORT).show()
            }

        } catch (e: Exception) {
            Log.e("CalendarFragment", "💥 캘린더 이동 실패", e)
            Toast.makeText(requireContext(), "캘린더로 이동할 수 없습니다: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // 🔥 NEW: 더미 코디 데이터를 캘린더에 추가
    private fun addDummyOutfitData(targetDate: String, outfitNumber: Int) {
        // 임시 outfit_id 생성 (코디 번호 기반)
        val dummyOutfitId = 1000 + outfitNumber

        Log.d("CalendarFragment", "더미 코디 데이터 추가: $targetDate -> 더미 ID: $dummyOutfitId")

        // 등록된 날짜에 추가
        addRegisteredDate(targetDate, dummyOutfitId)

        // SharedPreferences에 저장
        saveOutfitRegistration(targetDate, dummyOutfitId)

        // 더미 메모 데이터도 추가
        saveDummyOutfitMemo(targetDate, outfitNumber)
    }

    // 🔥 NEW: 특정 날짜로 캘린더 스크롤
    private fun scrollToSpecificDate(targetDate: String) {
        try {
            // targetDate: "2024-08-13" 형식
            val dateParts = targetDate.split("-")
            val year = dateParts[0].toInt()
            val month = dateParts[1].toInt()

            // 현재 캘린더의 기준점 (2023년 1월부터 시작한다고 가정)
            val baseYear = 2023
            val baseMonth = 1

            // 해당 날짜의 월 인덱스 계산
            val targetMonthIndex = (year - baseYear) * 12 + (month - baseMonth)

            Log.d("CalendarFragment", "날짜 스크롤: $targetDate -> 월 인덱스: $targetMonthIndex")

            // 캘린더를 해당 월로 스크롤
            rvCalendar.post {
                try {
                    (rvCalendar.layoutManager as? LinearLayoutManager)?.scrollToPositionWithOffset(
                        targetMonthIndex,
                        0
                    )

                    // 잠시 후 해당 날짜 강조 표시
                    rvCalendar.postDelayed({
                        highlightSpecificDate(targetDate)
                    }, 500)

                } catch (e: Exception) {
                    Log.e("CalendarFragment", "스크롤 실패", e)
                    rvCalendar.scrollToPosition(targetMonthIndex.coerceAtLeast(0))
                }
            }

        } catch (e: Exception) {
            Log.e("CalendarFragment", "날짜 파싱 실패", e)
            Toast.makeText(requireContext(), "날짜 형식 오류", Toast.LENGTH_SHORT).show()
        }
    }

    // 🔥 NEW: 특정 날짜 강조 표시 (선택사항)
    private fun highlightSpecificDate(targetDate: String) {
        // 어댑터에 특정 날짜 강조 기능이 있다면 호출
        if (::calendarAdapter.isInitialized) {
            // calendarAdapter.highlightDate(targetDate) // 구현되어 있다면
            Log.d("CalendarFragment", "날짜 강조: $targetDate")
        }
    }


    /**
     * 🔥 더미 코디 판별 (1001~1004만 더미로 인식)
     */
    private fun isDummyOutfitId(outfitId: Int): Boolean {
        return outfitId in 1001..1004
    }

    /**
     * 🔥 더미 코디 상세 화면으로 이동 (기존 코드 유지)
     */
    private fun navigateToDummyOutfitDetail(dateString: String, dummyOutfitId: Int) {
        try {
            val outfitNumber = dummyOutfitId - 1000

            Log.d("CalendarFragment", "🎭 더미 코디 상세 이동: 날짜=$dateString, 번호=$outfitNumber")

            val bundle = Bundle().apply {
                putString("selected_date", dateString)
                putInt("outfit_id", dummyOutfitId)
                putInt("outfit_number", outfitNumber)
                putBoolean("from_outfit_record", true)
                putBoolean("is_dummy_outfit", true)
                putString("memo", getDummyMemoForOutfit(outfitNumber))
            }

            val navController = findNavController()
            runCatching {
                navController.navigate(R.id.action_calendarFragment_to_calendarSaveFragment, bundle)
            }.onFailure {
                runCatching {
                    navController.navigate(R.id.calendarSaveFragment, bundle)
                }.onFailure {
                    Log.e("CalendarFragment", "더미 코디 navigation 실패")
                    val fallbackDescription = getDummyMemoForOutfit(outfitNumber)
                    Toast.makeText(context, "더미 코디 $outfitNumber 번 ($dateString)\n$fallbackDescription", Toast.LENGTH_LONG).show()
                }
            }

        } catch (e: Exception) {
            Log.e("CalendarFragment", "더미 코디 이동 실패", e)
            Toast.makeText(context, "코디 상세 화면을 열 수 없습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 🔥 HomeViewModel에서 검색 (Community API 실패 시 fallback)
     */
    private fun searchInHomeViewModel(dateString: String) {
        Log.d("CalendarFragment", "🏠 HomeViewModel에서 $dateString 코디 검색")

        homeViewModel.recentOutfits.value?.let { outfits ->
            Log.d("CalendarFragment", "HomeViewModel에 ${outfits.size}개 코디 있음")

            val matchingOutfit = outfits.find { outfit ->
                val outfitDate = outfit.date?.take(10)
                Log.d("CalendarFragment", "HomeViewModel 코디 날짜 비교: $outfitDate vs $dateString")
                outfitDate == dateString
            }

            if (matchingOutfit != null) {
                Log.d("CalendarFragment", "✅ HomeViewModel에서 코디 발견: ${matchingOutfit.image}")

                // 이미지 URL로 Community API에서 실제 ID 찾기
                findOutfitIdByImageUrl(matchingOutfit.image ?: "", dateString)
            } else {
                Log.w("CalendarFragment", "❌ HomeViewModel에서도 코디를 찾을 수 없음")
                showNotFoundMessage(dateString)
            }
        } ?: run {
            Log.w("CalendarFragment", "❌ HomeViewModel에 코디 데이터가 없음")
            showNotFoundMessage(dateString)
        }
    }

    /**
     * 🔥 이미지 URL로 실제 outfit_id 찾기
     */
    private fun findOutfitIdByImageUrl(imageUrl: String, dateString: String) {
        val token = TokenProvider.getToken(requireContext())
        if (token.isBlank()) {
            Toast.makeText(requireContext(), "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d("CalendarFragment", "🖼️ 병렬 검색 시작: $imageUrl")

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val calendarService = RetrofitClient.instance.create(CalendarService::class.java)

                // 🔥 병렬 처리: 10개씩 묶어서 동시 실행
                val ranges = listOf(
                    (100 downTo 91),  // 최신 10개
                    (90 downTo 81),
                    (80 downTo 71),
                    (70 downTo 61),
                    (60 downTo 51),
                    (50 downTo 41),
                    (40 downTo 31),
                    (30 downTo 21),
                    (20 downTo 11),
                    (10 downTo 1)     // 가장 오래된 10개
                )

                for (range in ranges) {
                    Log.d("CalendarFragment", "검색 범위: ${range.first}~${range.last}")

                    val deferredResults = range.map { id ->
                        async(Dispatchers.IO) {
                            try {
                                val response = calendarService.getOutfitImage(
                                    outfitId = id,
                                    authorization = "Bearer $token"
                                )

                                if (response.isSuccessful) {
                                    val outfitData = response.body()?.result
                                    if (outfitData?.mainImage == imageUrl) {
                                        return@async id // 찾았음!
                                    }
                                }
                                return@async null
                            } catch (e: Exception) {
                                return@async null
                            }
                        }
                    }

                    // 병렬 실행 결과 기다리기
                    val results = deferredResults.awaitAll()
                    val foundId = results.firstOrNull { it != null }

                    if (foundId != null) {
                        Log.d("CalendarFragment", "✅ 병렬 검색으로 ID 발견: $foundId")

                        withContext(Dispatchers.Main) {
                            // 실제 ID로 업데이트
                            dateToOutfitIdMap[dateString] = foundId
                            saveOutfitRegistration(dateString, foundId)

                            // 상세 정보 조회 후 이동
                            fetchOutfitDetails(foundId) { fetchedDate, memo ->
                                navigateToOutfitDetail(
                                    fetchedDate ?: dateString,
                                    foundId,
                                    memo ?: "등록된 코디입니다."
                                )
                            }
                        }
                        return@launch
                    }

                    // 각 범위 완료 후 잠깐 대기 (서버 부하 방지)
                    delay(100)
                }

                // 모든 병렬 검색 실패 - 빠른 날짜 검색
                withContext(Dispatchers.Main) {
                    Log.w("CalendarFragment", "❌ 병렬 이미지 검색 실패 - 빠른 날짜 검색")
                    findLatestOutfitFast(dateString)
                }

            } catch (e: Exception) {
                Log.e("CalendarFragment", "병렬 검색 실패", e)
                withContext(Dispatchers.Main) {
                    findLatestOutfitFast(dateString)
                }
            }
        }
    }

    private fun findLatestOutfitFast(dateString: String) {
        val token = TokenProvider.getToken(requireContext())
        if (token.isBlank()) return

        Log.d("CalendarFragment", "📅 빠른 날짜 검색: $dateString")

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val calendarService = RetrofitClient.instance.create(CalendarService::class.java)

                // 🔥 최신 20개만 빠르게 확인 (가장 가능성 높은 범위)
                val recentIds = (50 downTo 31).toList()

                val deferredResults = recentIds.map { id ->
                    async(Dispatchers.IO) {
                        try {
                            val response = calendarService.getOutfitText(
                                outfitId = id,
                                authorization = "Bearer $token"
                            )

                            if (response.isSuccessful && response.body()?.result != null) {
                                val outfitDetails = response.body()?.result
                                val outfitDate = outfitDetails?.date?.take(10)

                                if (outfitDate == dateString) {
                                    return@async Pair(id, outfitDetails)
                                }
                            }
                            return@async null
                        } catch (e: Exception) {
                            return@async null
                        }
                    }
                }

                // 병렬 실행
                val results = deferredResults.awaitAll()
                val found = results.firstOrNull { it != null }

                if (found != null) {
                    val (foundId, outfitDetails) = found
                    Log.d("CalendarFragment", "✅ 빠른 날짜 검색으로 발견: ID=$foundId")

                    withContext(Dispatchers.Main) {
                        // 실제 ID로 업데이트
                        dateToOutfitIdMap[dateString] = foundId
                        saveOutfitRegistration(dateString, foundId)

                        // 상세 화면으로 이동
                        navigateToOutfitDetail(
                            outfitDetails.date?.take(10) ?: dateString,
                            foundId,
                            outfitDetails.memo ?: "등록된 코디입니다."
                        )
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Log.w("CalendarFragment", "❌ 빠른 검색도 실패 - 임시 뷰 생성")
                        createTemporaryOutfitView(dateString)
                    }
                }

            } catch (e: Exception) {
                Log.e("CalendarFragment", "빠른 검색 실패", e)
                withContext(Dispatchers.Main) {
                    createTemporaryOutfitView(dateString)
                }
            }
        }
    }

    /**
     * 🔥 임시 방편: 검색 실패 시 임시 코디 뷰 생성
     */
    private fun createTemporaryOutfitView(dateString: String) {
        Log.d("CalendarFragment", "🚨 임시 뷰: $dateString")

        // HomeViewModel에서 이미지만 가져와서 간단히 표시
        homeViewModel.recentOutfits.value?.find {
            it.date?.take(10) == dateString
        }?.let { outfit ->

            val bundle = Bundle().apply {
                putString("selected_date", dateString)
                putString("main_image_url", outfit.image)
                putString("memo", "등록된 코디입니다.")
                putBoolean("is_temporary_view", true)
            }

            runCatching {
                findNavController().navigate(R.id.action_calendarFragment_to_calendarSaveFragment, bundle)
            }.onFailure {
                runCatching {
                    findNavController().navigate(R.id.calendarSaveFragment, bundle)
                }.onFailure {
                    Toast.makeText(context, "$dateString 코디 정보", Toast.LENGTH_SHORT).show()
                }
            }

        } ?: Toast.makeText(context, "해당 날짜의 코디를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
    }

    private fun findLatestOutfitForDate(dateString: String) {
        val token = TokenProvider.getToken(requireContext())
        if (token.isBlank()) return

        Log.d("CalendarFragment", "📅 $dateString 최신 코디 검색")

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val calendarService = RetrofitClient.instance.create(CalendarService::class.java)

                // 🔥 최근 30개 코디를 확인해서 날짜가 일치하는 것 찾기
                for (id in 30 downTo 1) {
                    try {
                        val response = calendarService.getOutfitText(
                            outfitId = id,
                            authorization = "Bearer $token"
                        )

                        if (response.isSuccessful && response.body()?.result != null) {
                            val outfitDetails = response.body()?.result
                            val outfitDate = outfitDetails?.date?.take(10)

                            Log.d("CalendarFragment", "ID $id 날짜 확인: $outfitDate vs $dateString")

                            // 날짜가 정확히 일치하는 코디 찾기
                            if (outfitDate == dateString) {
                                Log.d("CalendarFragment", "✅ 날짜 일치하는 코디 발견: ID=$id")

                                withContext(Dispatchers.Main) {
                                    // 실제 ID로 업데이트
                                    dateToOutfitIdMap[dateString] = id
                                    saveOutfitRegistration(dateString, id)

                                    Log.d("CalendarFragment", "🔄 최신 코디로 업데이트: $dateString -> $id")

                                    // 상세 화면으로 이동
                                    navigateToOutfitDetail(
                                        outfitDate,
                                        id,
                                        outfitDetails?.memo ?: "등록된 코디입니다."
                                    )
                                }
                                return@launch
                            }
                        }
                    } catch (e: Exception) {
                        Log.d("CalendarFragment", "ID $id 텍스트 조회 실패: ${e.message}")
                        continue
                    }
                }

                // 모든 검색 실패
                withContext(Dispatchers.Main) {
                    Log.w("CalendarFragment", "❌ 모든 방법으로 실제 ID를 찾을 수 없음")
                    Toast.makeText(
                        context,
                        "해당 날짜의 코디 정보를 찾을 수 없습니다.\n잠시 후 다시 시도해주세요.",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } catch (e: Exception) {
                Log.e("CalendarFragment", "최신 코디 검색 실패", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * 🔥 간단한 날짜 근접성 확인 (기존 코드에서 사용 안 함)
     */
    private fun isDateClose(date1: String?, date2: String?): Boolean {
        if (date1.isNullOrBlank() || date2.isNullOrBlank()) return false

        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val d1 = sdf.parse(date1)?.time ?: 0
            val d2 = sdf.parse(date2)?.time ?: 0

            // 1일 이내면 근접한 것으로 판단
            Math.abs(d1 - d2) <= 24 * 60 * 60 * 1000
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 🔥 실제 outfit_id로 상세 정보 조회 후 이동
     */
    private fun fetchOutfitDetailsAndNavigate(realOutfitId: Int, dateString: String) {
        Log.d("CalendarFragment", "📋 실제 코디 상세 정보 조회: ID=$realOutfitId")

        fetchOutfitDetails(realOutfitId) { fetchedDate, memo ->
            if (!fetchedDate.isNullOrBlank() && !memo.isNullOrBlank()) {
                Log.d("CalendarFragment", "✅ 실제 코디 상세 정보 조회 성공")
                navigateToOutfitDetail(fetchedDate, realOutfitId, memo)
            } else {
                Log.w("CalendarFragment", "⚠️ 실제 코디 상세 정보가 비어있음")
                // 빈 메모라도 이동은 가능하도록
                navigateToOutfitDetail(dateString, realOutfitId, "등록된 코디입니다.")
            }
        }
    }

    /**
     * 🔥 코디를 찾을 수 없을 때 메시지
     */
    private fun showNotFoundMessage(dateString: String) {
        Toast.makeText(
            requireContext(),
            "해당 날짜($dateString)의 코디 정보를 찾을 수 없습니다.\n잠시 후 다시 시도해주세요.",
            Toast.LENGTH_LONG
        ).show()
    }

    /**
     * 🔥 더미 코디별 메모 반환 (기존 코드 유지)
     */
    private fun getDummyMemoForOutfit(outfitNumber: Int): String {
        return when (outfitNumber) {
            1 -> "화이트 셔츠와 베이지 팬츠로 깔끔한 오피스 룩 (8월 13일)"
            2 -> "블랙 반팔과 베이지 반바지로 시원한 여름 코디 (8월 12일)"
            3 -> "블랙 셔츠와 화이트 신발로 모던하고 세련된 스타일 (8월 11일)"
            4 -> "그레이 셔츠와 블랙 팬츠로 미니멀한 데일리 코디 (8월 10일)"
            else -> "스타일리시한 데일리 코디"
        }
    }

    /**
     * 🔥 수정된 Navigation 결과 수신 설정
     */
    private fun setupNavigationResultListener() {
        // Fragment Result 방식
        parentFragmentManager.setFragmentResultListener("outfit_registered", this) { _, bundle ->
            val registeredDate = bundle.getString("registered_date")
            val success = bundle.getBoolean("success", false)
            val realOutfitId = bundle.getInt("real_outfit_id", -1) // 🔥 실제 서버 ID

            if (success && !registeredDate.isNullOrBlank()) {
                Log.d("CalendarFragment", "Fragment 결과로 등록된 날짜 수신: $registeredDate, 실제 ID: $realOutfitId")

                // 🔥 실제 ID가 있으면 사용, 없으면 기존 방식대로 임시 ID
                val outfitId = if (realOutfitId > 0) {
                    realOutfitId
                } else {
                    System.currentTimeMillis().toInt() % 100000
                }

                addRegisteredDate(registeredDate, outfitId)
                saveOutfitRegistration(registeredDate, outfitId)

                Toast.makeText(requireContext(), "코디가 등록되었습니다!", Toast.LENGTH_SHORT).show()
            }
        }

        // 기존 Navigation 방식도 유지
        findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<String>("registered_date")
            ?.observe(viewLifecycleOwner) { registeredDate ->
                if (!registeredDate.isNullOrBlank()) {
                    Log.d("CalendarFragment", "Navigation 결과로 등록된 날짜 수신: $registeredDate")

                    // 🔥 실제 ID도 받기 시도
                    val handle = findNavController().currentBackStackEntry?.savedStateHandle
                    val realOutfitId = handle?.get<Int>("real_outfit_id") ?: -1

                    val outfitId = if (realOutfitId > 0) {
                        realOutfitId
                    } else {
                        System.currentTimeMillis().toInt() % 100000
                    }

                    addRegisteredDate(registeredDate, outfitId)
                    saveOutfitRegistration(registeredDate, outfitId)

                    Toast.makeText(requireContext(), "코디가 등록되었습니다!", Toast.LENGTH_SHORT).show()

                    findNavController().currentBackStackEntry?.savedStateHandle?.remove<String>("registered_date")
                    findNavController().currentBackStackEntry?.savedStateHandle?.remove<Int>("real_outfit_id")
                }
            }
    }

    fun addRegisteredDate(dateString: String, outfitId: Int = -1) {
        val wasAdded = registeredDates.add(dateString)
        Log.d("CalendarDebug", "날짜 추가 시도: $dateString, 실제 추가됨: $wasAdded, outfit_id: $outfitId")

        if (outfitId != -1) {
            val existingId = dateToOutfitIdMap[dateString]

            // 🔥 더미 코디는 실제 코디로 덮어쓰지 않도록 보호
            val shouldUpdate = when {
                existingId == null -> true // 기존 ID가 없으면 추가
                isDummyOutfitId(existingId) && !isDummyOutfitId(outfitId) -> false // 더미 -> 실제 변경 방지
                !isDummyOutfitId(existingId) && isDummyOutfitId(outfitId) -> false // 실제 -> 더미 변경 방지
                existingId == outfitId -> false // 같은 ID면 스킵
                else -> true // 그 외는 업데이트
            }

            if (shouldUpdate) {
                Log.d("CalendarDebug", "ID 업데이트: $dateString -> $outfitId")
                dateToOutfitIdMap[dateString] = outfitId
            } else {
                Log.d("CalendarDebug", "ID 업데이트 스킵: $dateString -> 기존 $existingId 유지")
            }
        }

        if (::calendarAdapter.isInitialized) {
            calendarAdapter.updateRegisteredDates(registeredDates)
        }

        logCurrentState()
    }
}

data class MonthData(
    val year: Int,
    val month: Int
)