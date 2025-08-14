package com.example.onfit

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class RegisterItemBottomSheet : BottomSheetDialogFragment() {

    private var currentPhotoPath: String? = null
    private var photoUri: Uri? = null

    // 갤러리에서 이미지 선택
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            navigateToAddItemFragmentWithImage(it)
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
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                openCamera()
            }
            else -> {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
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
            android.util.Log.e("RegisterItemBottomSheet", "Failed to open camera", e)
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
        android.util.Log.d("RegisterItemBottomSheet", "navigateToAddItemFragmentWithImage called with URI: $imageUri")

        try {
            // BottomSheet 먼저 닫기
            dismiss()

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

    companion object {
        const val TAG = "RegisterItemBottomSheet"

        fun newInstance(): RegisterItemBottomSheet {
            return RegisterItemBottomSheet()
        }
    }
}