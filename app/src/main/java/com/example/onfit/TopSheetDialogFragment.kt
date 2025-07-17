package com.example.onfit

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import androidx.fragment.app.DialogFragment

class TopSheetDialogFragment : DialogFragment() {
    interface OnMemoDoneListener {
        fun onMemoDone(memoText: String)
    }

    private var listener: OnMemoDoneListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnMemoDoneListener) {
            listener = context
        } else {
            throw RuntimeException("$context must implement OnMemoDoneListener")
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext(), R.style.TopSheetDialog)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.fragment_top_sheet_dialog)


        // 다이얼로그의 Window 설정
        val window = dialog.window
        window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setGravity(Gravity.TOP)
            setWindowAnimations(R.style.TopSheetAnimation)
        }

        val memoEditText = dialog.findViewById<EditText>(R.id.memo_et)
        val doneBtn = dialog.findViewById<Button>(R.id.memo_done_btn)
        val backBtn = dialog.findViewById<ImageButton>(R.id.memo_back_btn)

        // 완료 버튼 누르면 메모 텍스트 저장
        doneBtn?.setOnClickListener {
            val text = memoEditText?.text.toString()
            listener?.onMemoDone(text) // editText로 전달
            dismiss()
        }

        // 뒤로가기 버튼으로 다이얼로그 닫기
        backBtn?.setOnClickListener {
            dismiss() // 저장 없이 닫기
        }
        return dialog
    }
}