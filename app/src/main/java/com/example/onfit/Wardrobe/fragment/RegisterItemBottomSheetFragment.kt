package com.example.onfit.Wardrobe.fragment

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.navigation.fragment.findNavController
import com.example.onfit.R
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class RegisterItemBottomSheet : BottomSheetDialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // BottomSheetDialog 스타일 강제 적용
        return BottomSheetDialog(requireContext(), theme)
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

        // 갤러리 버튼 클릭 리스너
        val galleryBtn = view.findViewById<LinearLayout>(R.id.gallery_btn)
        galleryBtn?.setOnClickListener {
            android.util.Log.d("RegisterItemBottomSheet", "Gallery button clicked")
            navigateToAddItemFragment()
        }

        // 카메라 버튼 클릭 리스너
        val cameraBtn = view.findViewById<LinearLayout>(R.id.camera_btn)
        cameraBtn?.setOnClickListener {
            android.util.Log.d("RegisterItemBottomSheet", "Camera button clicked")
            navigateToAddItemFragment()
        }
    }

    override fun onStart() {
        super.onStart()

        // Dialog 스타일 추가 설정
        val dialog = dialog as? BottomSheetDialog
        dialog?.let {
            val bottomSheet = it.findViewById<ViewGroup>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let { sheet ->
                sheet.background = null // 기본 배경 제거해서 커스텀 배경 적용
            }
        }
    }

    private fun navigateToAddItemFragment() {
        android.util.Log.d("RegisterItemBottomSheet", "navigateToAddItemFragment called")

        // AddItemFragment로 이동 (Navigation 사용)
        findNavController().navigate(R.id.addItemFragment)

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