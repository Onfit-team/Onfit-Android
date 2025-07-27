package com.example.onfit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class RegisterItemBottomSheet : BottomSheetDialogFragment() {

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
            navigateToAddItemFragment()
        } ?: android.util.Log.e("RegisterItemBottomSheet", "gallery_option not found!")

        // 카메라 옵션 클릭 리스너
        val cameraOption = view.findViewById<LinearLayout>(R.id.camera_option)
        cameraOption?.setOnClickListener {
            android.util.Log.d("RegisterItemBottomSheet", "Camera option clicked")
            navigateToAddItemFragment()
        } ?: android.util.Log.e("RegisterItemBottomSheet", "camera_option not found!")
    }

    private fun navigateToAddItemFragment() {
        android.util.Log.d("RegisterItemBottomSheet", "navigateToAddItemFragment called")

        try {
            // BottomSheet 먼저 닫기
            dismiss()

            // 부모 Fragment의 NavController를 통한 네비게이션
            parentFragment?.let { parent ->
                try {
                    val bundle = Bundle().apply {
                        putBoolean("edit_mode", false)
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
