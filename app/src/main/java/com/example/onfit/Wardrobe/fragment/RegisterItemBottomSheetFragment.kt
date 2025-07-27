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
            // BottomSheet 닫기
            dismiss()

            // 약간의 지연을 주어 dismiss 완료 후 네비게이션
            view?.postDelayed({
                try {
                    // 방법 1: 부모 Fragment의 NavController 사용
                    val navController = parentFragment?.findNavController()

                    if (navController != null) {
                        val bundle = Bundle().apply {
                            putBoolean("edit_mode", false)
                        }
                        navController.navigate(R.id.addItemFragment, bundle)
                        android.util.Log.d("RegisterItemBottomSheet", "Parent navigation successful")
                    } else {
                        // 방법 2: Activity의 NavController 사용
                        navigateWithActivity()
                    }
                } catch (e: Exception) {
                    android.util.Log.e("RegisterItemBottomSheet", "Parent navigation failed", e)
                    navigateWithActivity()
                }
            }, 100)

        } catch (e: Exception) {
            android.util.Log.e("RegisterItemBottomSheet", "Navigation failed", e)
        }
    }

    private fun navigateWithActivity() {
        try {
            android.util.Log.d("RegisterItemBottomSheet", "Using activity NavController")

            // Activity의 NavHostFragment를 통한 네비게이션
            val activity = requireActivity()
            val navHostFragment = activity.supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
            val navController = navHostFragment?.findNavController()

            if (navController != null) {
                val bundle = Bundle().apply {
                    putBoolean("edit_mode", false)
                }
                navController.navigate(R.id.addItemFragment, bundle)
                android.util.Log.d("RegisterItemBottomSheet", "Activity navigation successful")
            } else {
                // 최후의 수단: 직접 Fragment 교체
                navigateWithDirectFragmentTransaction()
            }
        } catch (e: Exception) {
            android.util.Log.e("RegisterItemBottomSheet", "Activity navigation failed", e)
            navigateWithDirectFragmentTransaction()
        }
    }

    private fun navigateWithDirectFragmentTransaction() {
        try {
            android.util.Log.d("RegisterItemBottomSheet", "Using direct fragment transaction")

            // AddItemFragment 직접 생성 및 교체
            val addItemFragment = com.example.onfit.Wardrobe.fragment.AddItemFragment()
            val bundle = Bundle().apply {
                putBoolean("edit_mode", false)
            }
            addItemFragment.arguments = bundle

            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment, addItemFragment)
                .addToBackStack("AddItemFragment")
                .commitAllowingStateLoss()

            android.util.Log.d("RegisterItemBottomSheet", "Direct transaction completed")

        } catch (e: Exception) {
            android.util.Log.e("RegisterItemBottomSheet", "Direct transaction failed", e)
        }
    }

    companion object {
        const val TAG = "RegisterItemBottomSheet"

        fun newInstance(): RegisterItemBottomSheet {
            return RegisterItemBottomSheet()
        }
    }
}