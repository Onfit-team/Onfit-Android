package com.example.onfit.HomeRegister.fragment

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.canhub.cropper.CropImage
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import com.example.onfit.databinding.FragmentOutfitCropBinding
import java.io.File
import java.io.FileOutputStream


class OutfitCropFragment : Fragment() {
    private var _binding: FragmentOutfitCropBinding? = null
    private val binding get() = _binding!!

    private var croppedImageUri: Uri? = null // 크롭된 이미지 URI를 저장할 변수 추가

    private val cropImageLauncher = registerForActivityResult(CropImageContract()) { result ->
        if (result.isSuccessful) {
            val uriContent = result.uriContent
            if (uriContent != null) {
                binding.cropOutfitIv.setImageURI(uriContent)
                croppedImageUri = uriContent
            }
        } else {
            val exception = result.error
            Toast.makeText(requireContext(), "크롭 실패: ${exception?.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentOutfitCropBinding.inflate(inflater, container, false)

        binding.cropOutfitIv.setOnClickListener {
            val imageUri = getImageUriFromImageView()

            if (imageUri != null) {
                val cropOptions = CropImageContractOptions(
                    uri = imageUri, // 기존 ImageView의 이미지 URI 사용
                    cropImageOptions = CropImageOptions().apply {
                        guidelines = CropImageView.Guidelines.ON
                        aspectRatioX = 1
                        aspectRatioY = 1
                    }
                )
                cropImageLauncher.launch(cropOptions)
            } else {
                Toast.makeText(requireContext(), "이미지가 없습니다.", Toast.LENGTH_SHORT).show()
            }
        }

        binding.cropSaveBtn.setOnClickListener {
            croppedImageUri?.let { uri ->
                // URI를 파일로 변환 (선택사항: Bitmap 변환 후 저장해도 됨)
                val result = Bundle().apply {
                    putString("cropped_image_uri", uri.toString())
                }
                parentFragmentManager.setFragmentResult("crop_result", result)
                findNavController().popBackStack()
            } ?: run {
                Toast.makeText(requireContext(), "이미지를 먼저 크롭하세요", Toast.LENGTH_SHORT).show()
            }
        }

        return binding.root
    }

    // 이미지뷰 캐시 파일에 저장
    private fun getImageUriFromImageView(): Uri? {
        val drawable = binding.cropOutfitIv.drawable ?: return null
        val bitmap = (drawable as BitmapDrawable).bitmap

        // 캐시 디렉토리에 임시 파일로 저장
        val file = File(requireContext().cacheDir, "temp_crop_image.png")
        FileOutputStream(file).use { fos ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
        }

        return Uri.fromFile(file)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}