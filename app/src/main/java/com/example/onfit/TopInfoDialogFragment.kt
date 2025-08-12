package com.example.onfit

import android.app.Dialog
import android.os.Bundle
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatButton
import androidx.fragment.app.DialogFragment

class TopInfoDialogFragment : DialogFragment() {

    interface OnTopInfoSavedListener {
        fun onTopInfoSaved(brand: String, price: String, size: String, site: String)
    }

    private var listener: OnTopInfoSavedListener? = null

    fun setOnTopInfoSavedListener(listener: OnTopInfoSavedListener) {
        this.listener = listener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext(), R.style.TopSheetDialog)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.fragment_top_info_dialog)

        // 다이얼로그 window 설정
        val window = dialog.window
        window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setGravity(Gravity.TOP)
            setWindowAnimations(R.style.TopSheetAnimation)
        }


        val backBtn = dialog.findViewById<ImageButton>(R.id.top_info_back_btn)
        val doneBtn = dialog.findViewById<AppCompatButton>(R.id.top_info_done_btn)
        val et1 = dialog.findViewById<EditText>(R.id.top_info_et1)
        val et2 = dialog.findViewById<EditText>(R.id.top_info_et2)
        val et3 = dialog.findViewById<EditText>(R.id.top_info_et3)
        val et4 = dialog.findViewById<EditText>(R.id.top_info_et4)

        // 뒤로가기 버튼으로 다이얼로그 저장 없이 닫기
        backBtn?.setOnClickListener {
            dismiss()
        }

        // 완료 버튼으로 내용 저장하고 닫기
        doneBtn?.setOnClickListener {
            val brand = et1?.text.toString()
            val price = et2?.text.toString()
            val size = et3?.text.toString()
            val site = et4?.text.toString()
            listener?.onTopInfoSaved(brand, price, size, site)
            dismiss()
        }

        return dialog
    }
}