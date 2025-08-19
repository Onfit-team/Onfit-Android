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

    // ⭐ 캘린더에서 선택한 날짜를 저장할 변수
    private var selectedDateForRegistration: String? = null

    private var dateToImageUrlMap = mutableMapOf<String, String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this)[CalendarViewModel::class.java]
        homeViewModel = ViewModelProvider(this)[HomeViewModel::class.java]

        // 갤러리 Launcher
        pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
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
                (rvCalendar.layoutManager as? LinearLayoutManager)?.scrollToPositionWithOffset(currentMonthIndex, 0)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
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
                    Toast.makeText(requireContext(), "해당 Outfit 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                    onResult(null, null)
                } else {
                    Log.e("CalendarFragment", "코디 상세 정보 조회 실패: code=${response.code()}")
                    Toast.makeText(requireContext(), "코디 데이터를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show()
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
     * ⭐ 이미지 URL 기반으로 코디 상세 표시 (API 호출 없음)
     */
    private fun handleDateClick(dateString: String, hasOutfit: Boolean) {
        if (hasOutfit) {
            showOutfitWithImageUrl(dateString)
        } else {
            Log.d("CalendarFragment", "등록되지 않은 날짜 클릭: $dateString")

            // ⭐ 캘린더에서 클릭한 날짜 저장
            selectedDateForRegistration = dateString

            showBottomSheet()
        }
    }

    /**
     * ⭐ showOutfitWithImageUrl 함수 - CalendarService API 사용
     */
    private fun showOutfitWithImageUrl(dateString: String) {
        Log.d("OutfitDebug", "=== 코디 상세 찾기 ===")
        Log.d("OutfitDebug", "찾는 날짜: $dateString")

        // 1. 실제 이미지 URL 값 확인
        val savedImageUrl = dateToImageUrlMap[dateString]
        Log.d("OutfitDebug", "저장된 이미지 URL 실제 값: '$savedImageUrl'") // ⭐ 추가

        if (!savedImageUrl.isNullOrBlank()) {
            Log.d("OutfitDebug", "✅ 저장된 이미지 사용: $dateString -> $savedImageUrl")
            navigateToOutfitDetailWithImage(dateString, savedImageUrl)
            return
        }

        // 2. HomeViewModel에서 찾기
        val allOutfits = homeViewModel.recentOutfits.value
        val matchingOutfit = allOutfits?.find {
            it.date.substring(0, 10) == dateString
        }

        if (matchingOutfit != null) {
            Log.d("OutfitDebug", "✅ HomeViewModel 매칭 성공: $dateString -> ${matchingOutfit.image}")
            navigateToOutfitDetailWithImage(dateString, matchingOutfit.image)
            return
        }

        // 3. ⭐ CalendarService API로 이미지 가져오기 (7일 이전 데이터용)
        val outfitId = dateToOutfitIdMap[dateString]
        if (outfitId != null) {
            Log.d("OutfitDebug", "CalendarService API로 이미지 조회: $dateString, outfitId: $outfitId")
            loadImageFromCalendarService(dateString, outfitId)
        } else {
            Log.d("OutfitDebug", "❌ outfit_id 없음: $dateString")
            Toast.makeText(context, "해당 날짜의 코디 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * ⭐ CalendarService API로 이미지 URL 가져오기
     */
    private fun loadImageFromCalendarService(dateString: String, outfitId: Int) {
        val token = TokenProvider.getToken(requireContext())
        if (token.isBlank()) {
            Toast.makeText(requireContext(), "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val calendarService = RetrofitClient.instance.create(CalendarService::class.java)
                val response = calendarService.getOutfitImage(
                    outfitId = outfitId,
                    authorization = "Bearer $token"
                )

                if (response.isSuccessful && response.body()?.result?.mainImage != null) {
                    val imageUrl = response.body()!!.result!!.mainImage!!

                    withContext(Dispatchers.Main) {
                        Log.d("OutfitDebug", "✅ CalendarService에서 이미지 조회 성공: $dateString -> $imageUrl")

                        // 다음번을 위해 저장
                        dateToImageUrlMap[dateString] = imageUrl
                        saveOutfitRegistration(dateString, outfitId, imageUrl)

                        // 상세 화면으로 이동
                        navigateToOutfitDetailWithImage(dateString, imageUrl)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Log.d("OutfitDebug", "❌ CalendarService에서 이미지 없음: $dateString")
                        Toast.makeText(context, "해당 날짜의 코디 이미지를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("OutfitDebug", "CalendarService API 호출 실패", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * ⭐ 기존 등록된 데이터에 이미지 URL 추가 (한 번만 실행)
     */
    private fun migrateExistingDataWithImageUrls() {
        val prefs = requireContext().getSharedPreferences("outfit_history", Context.MODE_PRIVATE)
        val migrationDone = prefs.getBoolean("image_url_migration_done", false)

        if (migrationDone) {
            Log.d("CalendarFragment", "이미지 URL 마이그레이션 이미 완료")
            return
        }

        Log.d("CalendarFragment", "기존 데이터 이미지 URL 마이그레이션 시작")

        // 이미지 URL이 없는 날짜들을 CalendarService API로 조회
        val datesNeedingImageUrls = registeredDates.filter { !dateToImageUrlMap.containsKey(it) }

        if (datesNeedingImageUrls.isEmpty()) {
            prefs.edit().putBoolean("image_url_migration_done", true).apply()
            return
        }

        val token = TokenProvider.getToken(requireContext())
        if (token.isBlank()) return

        viewLifecycleOwner.lifecycleScope.launch {
            var successCount = 0

            datesNeedingImageUrls.forEach { date ->
                val outfitId = dateToOutfitIdMap[date]
                if (outfitId != null) {
                    try {
                        val calendarService = RetrofitClient.instance.create(CalendarService::class.java)
                        val response = calendarService.getOutfitImage(
                            outfitId = outfitId,
                            authorization = "Bearer $token"
                        )

                        if (response.isSuccessful && response.body()?.result?.mainImage != null) {
                            val imageUrl = response.body()!!.result!!.mainImage!!

                            withContext(Dispatchers.Main) {
                                dateToImageUrlMap[date] = imageUrl
                                saveOutfitRegistration(date, outfitId, imageUrl)
                                successCount++

                                Log.d("CalendarFragment", "마이그레이션 성공: $date -> $imageUrl")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("CalendarFragment", "마이그레이션 실패: $date", e)
                    }
                }
            }

            withContext(Dispatchers.Main) {
                prefs.edit().putBoolean("image_url_migration_done", true).apply()
                Log.d("CalendarFragment", "마이그레이션 완료: $successCount/${datesNeedingImageUrls.size}")

                if (successCount > 0) {
                    Toast.makeText(requireContext(), "기존 코디 이미지 로드 완료!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    /**
     * ⭐ 이미지 URL로 상세 화면 이동 (outfit_id 없이)
     */
    private fun navigateToOutfitDetailWithImage(dateString: String, imageUrl: String) {
        try {
            Log.d("OutfitDebug", "상세 화면 이동 - 날짜: '$dateString', 이미지: '$imageUrl'")

            if (imageUrl.isBlank() || imageUrl == "null") {
                Log.e("OutfitDebug", "❌ 잘못된 이미지 URL: '$imageUrl'")
                Toast.makeText(context, "이미지 URL이 올바르지 않습니다.", Toast.LENGTH_SHORT).show()
                return
            }

            val bundle = Bundle().apply {
                putString("selected_date", dateString)
                putString("main_image_url", imageUrl)
            }

            Log.d("OutfitDebug", "Bundle 생성 완료: ${bundle.keySet()}")

            val navController = findNavController()
            Log.d("OutfitDebug", "NavController 획득 완료")

            // ⭐ Fragment가 활성 상태인지 확인
            if (!isAdded || !viewLifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                Log.e("OutfitDebug", "❌ Fragment가 비활성 상태")
                return
            }

            Log.d("OutfitDebug", "Navigation 시도 중...")

            runCatching {
                navController.navigate(R.id.action_calendarFragment_to_calendarSaveFragment, bundle)
                Log.d("OutfitDebug", "✅ action navigation 성공")
            }.onFailure { actionError ->
                Log.e("OutfitDebug", "Action navigation 실패: ${actionError.message}")

                runCatching {
                    navController.navigate(R.id.calendarSaveFragment, bundle)
                    Log.d("OutfitDebug", "✅ direct navigation 성공")
                }.onFailure { directError ->
                    Log.e("OutfitDebug", "Direct navigation도 실패: ${directError.message}")

                    // ⭐ 최후의 수단: 임시 토스트로 확인
                    Toast.makeText(context, "$dateString 코디\n이미지: $imageUrl", Toast.LENGTH_LONG).show()
                }
            }

        } catch (e: Exception) {
            Log.e("CalendarFragment", "코디 상세 화면 이동 실패", e)
            Toast.makeText(context, "코디 상세 화면을 열 수 없습니다: ${e.message}", Toast.LENGTH_SHORT).show()
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
                    Toast.makeText(context, "$dateString 코디 (ID: $outfitId)\n메모: $memo", Toast.LENGTH_LONG).show()
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

            // 중복 제거: 날짜별로 그룹핑해서 하나씩만 처리
            val uniqueOutfits = top7.groupBy { it.date.substring(0, 10) }
                .mapValues { it.value.first() }

            uniqueOutfits.forEach { (date, outfit) ->
                Log.d("RealOutfits", "실제 코디: $date -> 이미지: ${outfit.image}")

                // outfit_id는 필요 없으므로 임시 ID 사용
                addRegisteredDate(date, date.hashCode())
            }
        }
    }

    /**
     * ⭐ 새로 등록된 코디가 있는지 확인하는 함수
     */
    private fun checkForNewRegistrations() {
        val prefs = requireContext().getSharedPreferences("outfit_registration", Context.MODE_PRIVATE)
        val newlyRegisteredDate = prefs.getString("newly_registered_date", null)
        val newlyRegisteredId = prefs.getInt("newly_registered_outfit_id", -1)
        val newlyRegisteredImageUrl = prefs.getString("newly_registered_image_url", null)
        val timestamp = prefs.getLong("registration_timestamp", 0)

        // 5분 이내에 등록된 것만 처리 (중복 처리 방지)
        if (!newlyRegisteredDate.isNullOrBlank() &&
            System.currentTimeMillis() - timestamp < 5 * 60 * 1000) {

            Log.d("CalendarFragment", "새로 등록된 코디 감지: $newlyRegisteredDate (ID: $newlyRegisteredId)")

            // SharedPreferences 클리어 (재처리 방지)
            prefs.edit()
                .remove("newly_registered_date")
                .remove("newly_registered_outfit_id")
                .remove("newly_registered_image_url")
                .remove("registration_timestamp")
                .apply()

            // ⭐ 새로 등록된 날짜 즉시 추가
            addRegisteredDate(newlyRegisteredDate, newlyRegisteredId)

            // ⭐ 등록 기록을 SharedPreferences에 저장
            saveOutfitRegistration(newlyRegisteredDate, newlyRegisteredId, newlyRegisteredImageUrl)

            Toast.makeText(requireContext(), "코디가 등록되었습니다!", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * ⭐ 코디 등록 기록을 SharedPreferences에 저장 (데이터 손실 방지)
     */
    private fun saveOutfitRegistration(date: String, outfitId: Int, imageUrl: String? = null) {
        val prefs = requireContext().getSharedPreferences("outfit_history", Context.MODE_PRIVATE)
        val existingData = prefs.getString("registered_outfits", "") ?: ""

        Log.d("CalendarFragment", "saveOutfitRegistration 호출: $date, $outfitId, $imageUrl")
        Log.d("CalendarFragment", "기존 데이터: $existingData")

        // ⭐ 기존 데이터를 파싱해서 중복 체크
        val existingEntries = if (existingData.isNotBlank()) {
            existingData.split(",").toMutableList()
        } else {
            mutableListOf()
        }

        // 해당 날짜의 기존 엔트리 제거 (업데이트를 위해)
        existingEntries.removeAll { it.startsWith("$date:") }

        // 새로운 엔트리 추가
        val newEntry = if (!imageUrl.isNullOrBlank()) {
            "$date:$outfitId:$imageUrl"
        } else {
            "$date:$outfitId"
        }
        existingEntries.add(newEntry)

        val updatedData = existingEntries.joinToString(",")

        Log.d("CalendarFragment", "업데이트된 데이터: $updatedData")

        prefs.edit().putString("registered_outfits", updatedData).apply()
        Log.d("CalendarFragment", "등록 기록 저장 완료: $newEntry")
    }

    /**
     * ⭐ loadRegisteredDatesWithExistingAPI 수정 (saveOutfitRegistration 호출 제거)
     */
    private fun loadRegisteredDatesWithExistingAPI() {
        if (isLoadingDates) return
        isLoadingDates = true

        Log.d("CalendarFragment", "SharedPreferences에서 등록된 날짜 로드")

        val prefs = requireContext().getSharedPreferences("outfit_history", Context.MODE_PRIVATE)
        val registeredOutfitsJson = prefs.getString("registered_outfits", null)

        Log.d("CalendarFragment", "저장된 데이터: $registeredOutfitsJson")

        if (!registeredOutfitsJson.isNullOrBlank()) {
            try {
                val outfitEntries = registeredOutfitsJson.split(",")

                registeredDates.clear()
                dateToOutfitIdMap.clear()
                dateToImageUrlMap.clear()

                outfitEntries.forEach { entry ->
                    val parts = entry.split(":")
                    Log.d("CalendarFragment", "파싱 중: '$entry' -> parts: ${parts.toList()}") // ⭐ 수정

                    if (parts.size >= 2) {
                        val date = parts[0].trim() // ⭐ trim 추가
                        val outfitId = parts[1].trim().toIntOrNull() ?: 1 // ⭐ trim 추가
                        val imageUrl = if (parts.size >= 3) {
                            val url = parts.drop(2).joinToString(":").trim() // ⭐ URL에 ':'가 포함될 수 있음
                            if (url.isNotBlank()) url else null
                        } else null

                        registeredDates.add(date)
                        dateToOutfitIdMap[date] = outfitId

                        if (!imageUrl.isNullOrBlank()) {
                            dateToImageUrlMap[date] = imageUrl
                            Log.d("CalendarFragment", "이미지 URL 저장: '$date' -> '$imageUrl'") // ⭐ 수정
                        } else {
                            Log.d("CalendarFragment", "이미지 URL 없음: '$date'") // ⭐ 수정
                        }
                    }
                }
                // ... 나머지 코드
            } catch (e: Exception) {
                Log.e("CalendarFragment", "등록 기록 파싱 실패", e)
            }
        }
        isLoadingDates = false
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

                    val tempOutfitId = System.currentTimeMillis().toInt() % 100000
                    addRegisteredDate(registeredDate, tempOutfitId)
                    saveOutfitRegistration(registeredDate, tempOutfitId, null) // ⭐ null 추가

                    Toast.makeText(requireContext(), "코디가 등록되었습니다!", Toast.LENGTH_SHORT).show()
                    findNavController().currentBackStackEntry?.savedStateHandle?.remove<String>("registered_date")
                }
            }

        // ⭐ 새로 추가: SaveFragment에서 돌아올 때 처리
        findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<String>("newly_registered_date")
            ?.observe(viewLifecycleOwner) { newlyRegisteredDate ->
                if (!newlyRegisteredDate.isNullOrBlank()) {
                    Log.d("CalendarFragment", "SaveFragment에서 등록된 날짜 수신: $newlyRegisteredDate")

                    val outfitId = findNavController().currentBackStackEntry?.savedStateHandle?.get<Int>("newly_registered_outfit_id") ?: -1

                    // 새로 등록된 날짜를 캘린더에 추가
                    addRegisteredDate(newlyRegisteredDate, outfitId)
                    saveOutfitRegistration(newlyRegisteredDate, outfitId, null) // ⭐ null 추가

                    Toast.makeText(requireContext(), "코디가 캘린더에 추가되었습니다!", Toast.LENGTH_SHORT).show()

                    // 사용 후 제거
                    findNavController().currentBackStackEntry?.savedStateHandle?.remove<String>("newly_registered_date")
                    findNavController().currentBackStackEntry?.savedStateHandle?.remove<Int>("newly_registered_outfit_id")
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
                            Toast.makeText(context, "해당 날짜에 등록된 코디 이미지가 없습니다.", Toast.LENGTH_SHORT).show()
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

        // ⭐ 이미지 URL 상세 정보
        Log.d("CalendarDebug", "이미지 URL 매핑 개수: ${dateToImageUrlMap.size}")
        dateToImageUrlMap.forEach { (date, url) ->
            Log.d("CalendarDebug", "이미지 매핑: '$date' -> '$url' (길이: ${url.length})")
        }

        val prefs = requireContext().getSharedPreferences("outfit_history", Context.MODE_PRIVATE)
        val actualData = prefs.getString("registered_outfits", "없음")
        Log.d("CalendarDebug", "실제 저장된 데이터: '$actualData'")

        // ⭐ 특정 날짜 확인 (예: 2025-07-27)
        val testDate = "2025-07-27"
        val hasUrl = dateToImageUrlMap.containsKey(testDate)
        val urlValue = dateToImageUrlMap[testDate]
        Log.d("CalendarDebug", "테스트 날짜 '$testDate': hasUrl=$hasUrl, value='$urlValue'")

        Log.d("CalendarDebug", "========================")
    }

    // API에 갤러리에서 고른 사진 업로드하고 Url 받아오기
    private fun uploadImageToServer(file: File) {
        Log.d("Calendar", "Step 1: 함수 진입")
        Log.d("UploadDebug", "파일 존재=${file.exists()}, size=${file.length()}, path=${file.absolutePath}")

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

        Log.d("UploadCheck", "exists=$exists, canRead=$canRead, length=$length, ext=$ext, bitmapReadable=$bmpTest")

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

                val jpgFile = File(requireContext().cacheDir, "upload_temp_${System.currentTimeMillis()}.jpg")
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
                            Toast.makeText(requireContext(), "이미지 URL을 받지 못했어요.", Toast.LENGTH_SHORT).show()
                            return@withContext
                        }

                        // ⭐ 캘린더에서 선택한 날짜가 있으면 그 날짜로, 없으면 오늘 날짜로
                        val dateToRegister = selectedDateForRegistration
                            ?: SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

                        Log.d("CalendarFragment", "등록할 날짜: $dateToRegister (선택된 날짜: $selectedDateForRegistration)")

                        // RegisterFragment로 URL 전달
                        val bundle = Bundle().apply {
                            putString("selectedImagePath", uploadFile.absolutePath)
                            putString("uploadedImageUrl", imageUrl)
                            putString("selectedDate", dateToRegister) // ⭐ 선택한 날짜 전달
                        }

                        if (!isAdded || !viewLifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                            return@withContext
                        }

                        val nav = findNavController()

                        runCatching {
                            nav.navigate(R.id.action_calendarFragment_to_registerFragment, bundle)
                        }.onFailure {
                            runCatching {
                                nav.navigate(R.id.registerFragment, bundle)
                            }
                        }

                        // ⭐ 사용 후 선택 날짜 초기화
                        selectedDateForRegistration = null

                    } else {
                        val errorMsg = response.errorBody()?.string()
                        Log.e("HomeFragment", "업로드 실패: code=${response.code()}, error=$errorMsg, body=$bodyObj")
                        Toast.makeText(requireContext(), bodyObj?.message ?: "업로드 실패", Toast.LENGTH_SHORT).show()
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
                    (rvCalendar.layoutManager as? LinearLayoutManager)?.scrollToPositionWithOffset(currentMonthIndex, 0)
                } catch (e: Exception) {
                    rvCalendar.scrollToPosition(currentMonthIndex)
                }
            }, 100)
        }
    }

    private fun navigateToOutfitRegister(dateString: String) {
        val action = CalendarFragmentDirections.actionCalendarFragmentToCalendarSaveFragment(dateString)
        findNavController().navigate(action)
    }

    private fun navigateToStyleOutfits() {
        try {
            val navController = findNavController()
            val targetDestination = navController.graph.findNode(R.id.styleOutfitsFragment)

            if (targetDestination != null) {
                navController.navigate(R.id.styleOutfitsFragment)
            } else {
                Toast.makeText(requireContext(), "StyleOutfitsFragment를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
            }

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Navigation 오류: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun showBottomSheet() {
        val view = layoutInflater.inflate(R.layout.bottom_sheet_dialog, null)
        val dialog = BottomSheetDialog(requireContext())
        dialog.setContentView(view)

        view.findViewById<LinearLayout>(R.id.camera_btn).setOnClickListener {
            dialog.dismiss()
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            navigateToOutfitRegister(today)
        }

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
                rescanPicturesAndOpenGallery()
            } else {
                Toast.makeText(requireContext(), "사진 접근 권한이 필요해요", Toast.LENGTH_SHORT).show()
            }
        }

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