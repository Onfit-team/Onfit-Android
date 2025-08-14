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
import com.example.onfit.KakaoLogin.util.TokenProvider
import com.example.onfit.OutfitRegister.ApiService
import com.example.onfit.OutfitRegister.RetrofitClient
import com.example.onfit.R
import com.example.onfit.calendar.adapter.CalendarAdapter
import com.example.onfit.calendar.viewmodel.CalendarViewModel
import com.example.onfit.calendar.viewmodel.CalendarUiState
import com.example.onfit.calendar.Network.*
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
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

    // 기존 데이터들
    private val outfitRegisteredDates = setOf(
        "2025-04-03", "2025-04-04", "2025-04-05", "2025-04-06", "2025-04-07",
        "2025-04-08", "2025-04-09", "2025-04-10", "2025-04-11", "2025-04-12",
        "2025-04-13", "2025-04-14", "2025-04-15", "2025-04-16", "2025-04-17",
        "2025-04-18", "2025-04-19", "2025-04-20", "2025-04-21", "2025-04-22",
        "2025-04-23", "2025-04-24", "2025-04-25", "2025-04-26", "2025-04-27",
        "2025-04-28", "2025-04-29",
        "2025-07-03", "2025-07-04", "2025-07-05", "2025-07-06", "2025-07-07",
        "2025-07-08", "2025-07-09", "2025-07-10", "2025-07-11", "2025-07-12",
        "2025-07-13", "2025-07-14", "2025-07-15", "2025-07-16", "2025-07-17",
        "2025-07-18", "2025-07-19", "2025-07-20", "2025-07-21", "2025-07-22",
        "2025-07-23", "2025-07-24", "2025-07-25", "2025-07-26", "2025-07-27",
        "2025-07-28", "2025-07-29"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this)[CalendarViewModel::class.java]

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
        observeViewModel()

        // 🔥 새 API로 가장 많이 사용된 태그 조회
        loadMostUsedTag()
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
        val mime = when (ext) {
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            else -> "application/octet-stream" // 기타 확장자
        }
        Log.d("UploadDebug", "Step 3: MIME=$mime")

        // 확장자 기반 MIME 자동 지정
        var uploadFile = file
        var uploadMime = when (ext) {
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            else -> "application/octet-stream"
        }
        Log.d("UploadDebug", "Step 3: MIME=$uploadMime")

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
                uploadMime = "image/jpeg" // ← 변환했으니 MIME도 함께 변경
                Log.d("UploadDebug", "PNG → JPG 변환 완료: ${jpgFile.absolutePath}")
            } catch (e: Exception) {
                Log.e("UploadDebug", "PNG → JPG 변환 실패", e)
                // 변환 실패 시 원본 PNG 그대로 보낼 수도 있음(원하면 return으로 중단)
                uploadFile = file
                uploadMime = "image/png"
            }
        }

        // 4. RequestBody + MultipartBody.Part 생성
        val requestFile = uploadFile.asRequestBody(uploadMime.toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData("image", uploadFile.name, requestFile)
        Log.d("UploadREQ", "file=${uploadFile.name}, size=${uploadFile.length()}, mime=$uploadMime, fieldName=image")

        // 5. 업로드 요청 정보 로그
        Log.d("UploadREQ",
            "url=/items/upload, header=$header, file=${file.name}, size=$length, mime=$mime, fieldName=image"
        )

        // 6. 업로드 실행
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val api = RetrofitClient.instance.create(ApiService::class.java)
                val response = api.uploadImage(header, body)

                Log.d("UploadDebug", "Step 5: API 호출 완료, 응답코드=${response.code()}")

                // (1) 원본 JSON 통째로 로그 (성공/실패 모두)
                try {
                    val raw = response.raw().peekBody(Long.MAX_VALUE).string()
                    Log.d("UploadRaw", raw)
                } catch (_: Exception) {}

                val bodyObj = response.body()

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && bodyObj?.ok == true) {
                        val imageUrl = bodyObj.payload?.imageUrl
                        Log.d("HomeFragment", "이미지 업로드 성공, parsed imageUrl=$imageUrl")

                        // ★ URL이 비어있으면 이동 금지(여기서 막아야 Register에서 null 안 받음)
                        if (imageUrl.isNullOrBlank()) {
                            Toast.makeText(requireContext(), "이미지 URL을 받지 못했어요.", Toast.LENGTH_SHORT).show()
                            return@withContext
                        }

                        // RegisterFragment로 URL 전달
                        val bundle = Bundle().apply {
                            putString("selectedImagePath", uploadFile.absolutePath) // 미리보기용 파일경로
                            putString("uploadedImageUrl", imageUrl)                  // 서버 URL
                        }
                        // 프래그먼트가 화면에 살아있는지 먼저 확인
                        if (!isAdded || !viewLifecycleOwner.lifecycle.currentState.isAtLeast(
                                Lifecycle.State.STARTED)) {
                            return@withContext
                        }

                        val nav = findNavController()

                        // 액션으로 시도
                        runCatching {
                            nav.navigate(R.id.action_calendarFragment_to_registerFragment, bundle)
                        }.onFailure {
                            // 2차: 액션이 막혔을 때(현재 목적지 변화 등) 대상 ID로 폴백
                            runCatching {
                                nav.navigate(R.id.registerFragment, bundle)
                            }
                        }
                    } else {
                        val errorMsg = response.errorBody()?.string()
                        Log.e("HomeFragment", "업로드 실패: code=${response.code()}, error=$errorMsg, body=$bodyObj")
                        Toast.makeText(requireContext(), bodyObj?.message ?: "업로드 실패", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("UploadDebug", "예외 발생", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "서버 오류", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        rvCalendar.post {
            try {
                val currentMonthIndex = 24
                (rvCalendar.layoutManager as? LinearLayoutManager)?.scrollToPositionWithOffset(currentMonthIndex, 0)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun setupViews(view: View) {
        rvCalendar = view.findViewById(R.id.rvCalendar)
        tvMostUsedStyle = view.findViewById(R.id.tvMostUsedStyle)

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
            registeredDates = outfitRegisteredDates,
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

    /**
     * API로 가장 많이 사용된 태그 조회
     */
    private fun loadMostUsedTag() {
        viewModel.loadMostUsedTag()
    }

    /**
     * ViewModel 상태 관찰
     */
    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                // 기존 코디 데이터 처리
                handleOutfitData(state)

                // 새로 추가: 태그 통계 UI 업데이트
                updateTagUI(state)
            }
        }
    }

    /**
     * 기존 코디 데이터 처리
     */
    private fun handleOutfitData(state: CalendarUiState) {
        when {
            state.isLoading -> {
                // 로딩 중
            }
            state.hasOutfitData -> {
                state.outfitImage?.let { image ->
                    println("Calendar API - 이미지 데이터 수신: ${image.mainImage}")
                }
                state.outfitText?.let { text ->
                    println("Calendar API - 텍스트 데이터 수신: ${text.memo}")
                }
            }
            state.errorMessage != null -> {
                Toast.makeText(context, "코디 데이터: ${state.errorMessage}", Toast.LENGTH_SHORT).show()
                viewModel.clearErrorMessage()
            }
        }
    }

    /**
     * 태그 UI 업데이트
     */
    private fun updateTagUI(state: CalendarUiState) {
        when {
            state.isTagLoading -> {
                // 태그 로딩 중
                tvMostUsedStyle.text = "데이터를 불러오는 중..."
            }
            state.mostUsedTag != null -> {
                // 🔥 실제 API 데이터로 업데이트
                val tag = state.mostUsedTag
                tvMostUsedStyle.text = "#${tag.tag} 스타일이 가장 많았어요! (${tag.count}개)"
            }
            state.tagErrorMessage != null -> {
                // 에러 시 기본값
                tvMostUsedStyle.text = "#포멀 스타일이 가장 많았어요!"

                // 에러 메시지 자동 제거
                viewModel.clearTagError()
            }
            else -> {
                // 초기 상태
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

    private fun handleDateClick(dateString: String, hasOutfit: Boolean) {
        if (hasOutfit) {
            loadOutfitDataInBackground(dateString)
            navigateToOutfitDetail(dateString)
        } else {
            navigateToOutfitRegister(dateString)
        }
    }

    private fun loadOutfitDataInBackground(dateString: String) {
        viewModel.onDateSelected(dateString)  // String 전달 (outfitId 계산 불필요)
    }

    private fun navigateToOutfitDetail(dateString: String) {
        Toast.makeText(context, "코디 상세: $dateString", Toast.LENGTH_SHORT).show()
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
            // 카메라 → 현재는 등록 화면으로 이동만
            dialog.dismiss()
        }
        view.findViewById<LinearLayout>(R.id.gallery_btn).setOnClickListener {
            // 권한 확인 → Pictures 스캔 → 갤러리 열기
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
        val intent = Intent(Intent.ACTION_PICK).apply { type = "image/*" }
        pickImageLauncher.launch(intent)
    }

    /**
     * 외부에서 태그 통계 새로고침
     */
    fun refreshMostUsedTag() {
        loadMostUsedTag()
    }
}

data class MonthData(
    val year: Int,
    val month: Int
)