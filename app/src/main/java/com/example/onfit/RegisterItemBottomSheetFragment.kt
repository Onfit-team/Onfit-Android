package com.example.onfit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.content.Intent
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class RegisterItemBottomSheet : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.register_item_bottom_sheet, container, false)
    }

    // 대안 방법 - Activity를 통한 Fragment 전환
    private fun navigateWithActivitySupport() {
        try {
            android.util.Log.d("RegisterItemBottomSheet", "Using activity support method")

            dismiss()

            // Activity의 supportFragmentManager 사용
            val addItemFragment = AddItemFragment()
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, addItemFragment)
                .addToBackStack("AddItemFragment")
                .commitAllowingStateLoss()

            android.util.Log.d("RegisterItemBottomSheet", "Activity method completed")

        } catch (e: Exception) {
            android.util.Log.e("RegisterItemBottomSheet", "Activity method failed", e)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 갤러리 옵션 클릭 리스너
        val galleryOption = view.findViewById<LinearLayout>(R.id.gallery_option)
        galleryOption?.setOnClickListener {
            android.util.Log.d("RegisterItemBottomSheet", "Gallery option clicked")

            // 방법 1: 직접 Fragment 전환
            navigateToAddItemFragment()

            // 방법 2: 만약 위 방법이 안 되면 주석 해제
            // navigateWithActivitySupport()
        } ?: android.util.Log.e("RegisterItemBottomSheet", "gallery_option not found!")

        // 카메라 옵션 클릭 리스너 (필요하다면)
        val cameraOption = view.findViewById<LinearLayout>(R.id.camera_option)
        cameraOption?.setOnClickListener {
            android.util.Log.d("RegisterItemBottomSheet", "Camera option clicked")
            // 카메라 기능 구현
            dismiss()
        }
    }

    private fun navigateToAddItemFragment() {
        android.util.Log.d("RegisterItemBottomSheet", "navigateToAddItemFragment called")

        // 간단하게 새로운 Activity로 이동
        val intent = Intent(requireContext(), AddItemActivity::class.java)
        startActivity(intent)

        // BottomSheet 닫기
        dismiss()
    }

    companion object {
        const val TAG = "RegisterItemBottomSheet"

        fun newInstance(): RegisterItemBottomSheet {
            return RegisterItemBottomSheet()
        }
    }
}