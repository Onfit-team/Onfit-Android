package com.example.onfit

import android.app.Dialog
import android.os.Bundle
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageButton
import androidx.fragment.app.DialogFragment


class TopSearchDialogFragment : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext(), R.style.TopSheetDialog)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.fragment_top_search_dialog)

        // 다이얼로그의 Window 설정
        val window = dialog.window
        window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setGravity(Gravity.TOP)
            setWindowAnimations(R.style.TopSheetAnimation)
        }

        // 뒤로가기 버튼으로 다이얼로그 닫기
        val backBtn = dialog.findViewById<ImageButton>(R.id.back_btn)
        backBtn?.setOnClickListener {
            dismiss()
        }
        return dialog
    }
}