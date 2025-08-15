package com.example.onfit

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.onfit.HomeRegister.model.RetrofitClient
import com.example.onfit.KakaoLogin.util.TokenProvider
import com.example.onfit.Wardrobe.Network.RegisterItemRequestDto
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class RegisterItemBottomSheet : BottomSheetDialogFragment() {

    private var currentPhotoPath: String? = null
    private var photoUri: Uri? = null

    // 갤러리에서 이미지 선택 (HomeFragment와 동일한 방식)
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == 0 || result.data?.data != null) { // RESULT_OK 또는 데이터 있음
            result.data?.data?.let { uri ->
                navigateToAddItemFragmentWithImage(uri)
            }
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
                android.Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                openCamera()
            }
            else -> {
                cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
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
        try {
            // BottomSheet 먼저 닫기
            dismiss()

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

    companion object {
        const val TAG = "RegisterItemBottomSheet"

        fun newInstance(): RegisterItemBottomSheet {
            return RegisterItemBottomSheet()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Fragment Result Listener 정리
        parentFragmentManager.clearFragmentResultListener("add_item_complete")
    }
}