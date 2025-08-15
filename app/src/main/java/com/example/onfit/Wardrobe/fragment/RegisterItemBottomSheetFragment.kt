package com.example.onfit

<<<<<<< HEAD
import android.Manifest
=======
>>>>>>> 3677f88 (refactor: 코드 리팩토링)
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
<<<<<<< HEAD
import android.provider.MediaStore
=======
import android.util.Log
>>>>>>> 3677f88 (refactor: 코드 리팩토링)
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
<<<<<<< HEAD
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
=======
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.onfit.HomeRegister.model.RetrofitClient
import com.example.onfit.KakaoLogin.util.TokenProvider
import com.example.onfit.Wardrobe.Network.RegisterItemRequestDto
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch
>>>>>>> 3677f88 (refactor: 코드 리팩토링)
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class RegisterItemBottomSheet : BottomSheetDialogFragment() {

    private var currentPhotoPath: String? = null
    private var photoUri: Uri? = null

<<<<<<< HEAD
    // 갤러리에서 이미지 선택
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            navigateToAddItemFragmentWithImage(it)
=======
    // 갤러리에서 이미지 선택 (HomeFragment와 동일한 방식)
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == 0 || result.data?.data != null) { // RESULT_OK 또는 데이터 있음
            result.data?.data?.let { uri ->
                navigateToAddItemFragmentWithImage(uri)
            }
>>>>>>> 3677f88 (refactor: 코드 리팩토링)
        }
    }

    // 카메라로 사진 촬영
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { isSuccess: Boolean ->
        if (isSuccess) {
            photoUri?.let {
                navigateToAddItemFragmentWithImage(it)
            }
        } else {
            Toast.makeText(context, "사진 촬영이 취소되었습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    // 카메라 권한 요청
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            openCamera()
        } else {
            Toast.makeText(context, "카메라 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.register_item_bottom_sheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

<<<<<<< HEAD
        // 갤러리 옵션 클릭 리스너
        val galleryOption = view.findViewById<LinearLayout>(R.id.gallery_option)
        galleryOption?.setOnClickListener {
            android.util.Log.d("RegisterItemBottomSheet", "Gallery option clicked")
            openGallery()
        } ?: android.util.Log.e("RegisterItemBottomSheet", "gallery_option not found!")

        // 카메라 옵션 클릭 리스너
        val cameraOption = view.findViewById<LinearLayout>(R.id.camera_option)
        cameraOption?.setOnClickListener {
            android.util.Log.d("RegisterItemBottomSheet", "Camera option clicked")
            checkCameraPermissionAndOpen()
        } ?: android.util.Log.e("RegisterItemBottomSheet", "camera_option not found!")
    }

    /**
     * 갤러리 열기
     */
    private fun openGallery() {
        try {
            galleryLauncher.launch("image/*")
        } catch (e: Exception) {
            android.util.Log.e("RegisterItemBottomSheet", "Failed to open gallery", e)
=======
        // 갤러리 옵션 클릭
        val galleryOption = view.findViewById<LinearLayout>(R.id.gallery_option)
        galleryOption?.setOnClickListener {
            openGallery()
        }

        // 카메라 옵션 클릭
        val cameraOption = view.findViewById<LinearLayout>(R.id.camera_option)
        cameraOption?.setOnClickListener {
            checkCameraPermissionAndOpen()
        }
    }

    /**
     * 갤러리 열기 (HomeFragment와 동일한 방식)
     */
    private fun openGallery() {
        try {
            val intent = Intent(Intent.ACTION_PICK).apply { type = "image/*" }
            galleryLauncher.launch(intent)
        } catch (e: Exception) {
>>>>>>> 3677f88 (refactor: 코드 리팩토링)
            Toast.makeText(context, "갤러리를 열 수 없습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 카메라 권한 확인 후 카메라 열기
     */
    private fun checkCameraPermissionAndOpen() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
<<<<<<< HEAD
                Manifest.permission.CAMERA
=======
                android.Manifest.permission.CAMERA
>>>>>>> 3677f88 (refactor: 코드 리팩토링)
            ) == PackageManager.PERMISSION_GRANTED -> {
                openCamera()
            }
            else -> {
<<<<<<< HEAD
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
=======
                cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
>>>>>>> 3677f88 (refactor: 코드 리팩토링)
            }
        }
    }

    /**
     * 카메라 열기
     */
    private fun openCamera() {
        try {
            val photoFile = createImageFile()
            photoUri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                photoFile
            )

            photoUri?.let { uri ->
                cameraLauncher.launch(uri)
            }
        } catch (e: Exception) {
<<<<<<< HEAD
            android.util.Log.e("RegisterItemBottomSheet", "Failed to open camera", e)
=======
>>>>>>> 3677f88 (refactor: 코드 리팩토링)
            Toast.makeText(context, "카메라를 열 수 없습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 임시 이미지 파일 생성
     */
    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = requireContext().getExternalFilesDir("Pictures")

        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        ).apply {
            currentPhotoPath = absolutePath
        }
    }

    /**
     * 이미지와 함께 AddItemFragment로 이동
     */
    private fun navigateToAddItemFragmentWithImage(imageUri: Uri) {
<<<<<<< HEAD
        android.util.Log.d("RegisterItemBottomSheet", "navigateToAddItemFragmentWithImage called with URI: $imageUri")

=======
>>>>>>> 3677f88 (refactor: 코드 리팩토링)
        try {
            // BottomSheet 먼저 닫기
            dismiss()

<<<<<<< HEAD
            // 부모 Fragment의 NavController를 통한 네비게이션
            parentFragment?.let { parent ->
                try {
                    val bundle = Bundle().apply {
                        putBoolean("edit_mode", false)
                        putString("image_uri", imageUri.toString()) // 이미지 URI 전달
                    }
                    parent.findNavController().navigate(R.id.addItemFragment, bundle)
                    android.util.Log.d("RegisterItemBottomSheet", "Parent fragment navigation successful")
                    return
                } catch (e: Exception) {
                    android.util.Log.w("RegisterItemBottomSheet", "Parent fragment navigation failed: ${e.message}")
                }
            }

            // 대안 1: Activity의 NavHostFragment 사용
            try {
                val activity = requireActivity()
                val navHostFragment = activity.supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
                val navController = navHostFragment?.findNavController()

                if (navController != null) {
                    val bundle = Bundle().apply {
                        putBoolean("edit_mode", false)
                        putString("image_uri", imageUri.toString()) // 이미지 URI 전달
                    }
                    navController.navigate(R.id.addItemFragment, bundle)
                    android.util.Log.d("RegisterItemBottomSheet", "Activity NavHostFragment navigation successful")
                    return
                }
            } catch (e: Exception) {
                android.util.Log.w("RegisterItemBottomSheet", "Activity navigation failed: ${e.message}")
            }

            // 대안 2: 직접 Fragment 교체
            try {
                val addItemFragment = com.example.onfit.Wardrobe.fragment.AddItemFragment()
                val bundle = Bundle().apply {
                    putBoolean("edit_mode", false)
                    putString("image_uri", imageUri.toString()) // 이미지 URI 전달
                }
                addItemFragment.arguments = bundle

                requireActivity().supportFragmentManager.beginTransaction()
                    .replace(R.id.nav_host_fragment, addItemFragment)
                    .addToBackStack("AddItemFragment")
                    .commit()

                android.util.Log.d("RegisterItemBottomSheet", "Direct fragment replacement successful")
            } catch (e: Exception) {
                android.util.Log.e("RegisterItemBottomSheet", "All navigation methods failed", e)
            }

        } catch (e: Exception) {
            android.util.Log.e("RegisterItemBottomSheet", "Navigation completely failed", e)
        }
    }

=======
            // 🔥 AddItemFragment로 이동하면서 결과 리스너 설정
            setupAddItemResultListener()

            // AddItemFragment로 이동
            val bundle = Bundle().apply {
                putBoolean("edit_mode", false)
                putString("image_uri", imageUri.toString())
            }

            // Navigation 시도
            parentFragment?.findNavController()?.navigate(R.id.addItemFragment, bundle)
                ?: run {
                    // Navigation 실패 시 직접 Fragment 교체
                    val addItemFragment = com.example.onfit.Wardrobe.fragment.AddItemFragment()
                    addItemFragment.arguments = bundle

                    requireActivity().supportFragmentManager.beginTransaction()
                        .replace(R.id.nav_host_fragment, addItemFragment)
                        .addToBackStack("AddItemFragment")
                        .commit()
                }

        } catch (e: Exception) {
            Toast.makeText(context, "화면 이동에 실패했습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 🔥 AddItemFragment에서 등록 완료 결과를 받을 리스너 설정
     */
    private fun setupAddItemResultListener() {
        parentFragmentManager.setFragmentResultListener(
            "add_item_complete",
            this
        ) { _, bundle ->
            val isSuccess = bundle.getBoolean("success", false)
            val registeredDate = bundle.getString("registered_date")

            if (isSuccess) {
                Log.d("RegisterBottomSheet", "AddItemFragment에서 등록 완료 수신: $registeredDate")

                // 🔥 Calendar와 Wardrobe Fragment에 알림
                notifyRegistrationSuccess(registeredDate ?: getCurrentDate())
            }
        }
    }

    /**
     * 🔥 등록 성공 시 결과 전달
     */
    private fun notifyRegistrationSuccess(purchaseDate: String) {
        val bundle = Bundle().apply {
            putBoolean("success", true)
            putString("registered_date", purchaseDate)
            putLong("timestamp", System.currentTimeMillis())
        }

        // 🔥 WardrobeFragment에 알림
        parentFragmentManager.setFragmentResult("item_registered", bundle)

        // 🔥 CalendarFragment에도 직접 알림 (이중 보장)
        parentFragmentManager.setFragmentResult("outfit_registered", bundle)

        Log.d("RegisterBottomSheet", "등록 완료 알림 전달: $purchaseDate")
    }

    /**
     * 🔥 현재 날짜 반환 (yyyy-MM-dd 형식)
     */
    private fun getCurrentDate(): String {
        val calendar = Calendar.getInstance()
        return String.format(
            "%04d-%02d-%02d",
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    /**
     * 🔥 외부에서 등록 완료를 알릴 수 있는 공개 메서드
     */
    fun notifyItemRegistered(purchaseDate: String? = null) {
        notifyRegistrationSuccess(purchaseDate ?: getCurrentDate())
    }

>>>>>>> 3677f88 (refactor: 코드 리팩토링)
    companion object {
        const val TAG = "RegisterItemBottomSheet"

        fun newInstance(): RegisterItemBottomSheet {
            return RegisterItemBottomSheet()
        }
    }
<<<<<<< HEAD
=======

    override fun onDestroyView() {
        super.onDestroyView()
        // Fragment Result Listener 정리
        parentFragmentManager.clearFragmentResultListener("add_item_complete")
    }
>>>>>>> 3677f88 (refactor: 코드 리팩토링)
}