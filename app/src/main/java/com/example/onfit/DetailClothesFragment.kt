package com.example.onfit

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Toast
import androidx.fragment.app.Fragment

class DetailClothesFragment : Fragment() {

    private var isEditMode = false  // 수정 모드인지 여부

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_clothes_detail, container, false)

        val deleteButton = view.findViewById<ImageButton>(R.id.ic_delete)
        deleteButton.setOnClickListener {
            showDeleteConfirmationDialog()
        }

        val editButton = view.findViewById<ImageButton>(R.id.ic_edit)
        editButton.setOnClickListener {
            enterEditMode()
        }

        return view
    }

    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setMessage("이 아이템을 삭제하겠습니까?")
            .setPositiveButton("예") { dialog, _ ->
                // TODO: 삭제 처리 구현
                Toast.makeText(requireContext(), "삭제되었습니다", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton("아니요") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun enterEditMode() {
        isEditMode = true

        // TODO: 수정 모드 UI로 변경하는 코드 추가
        // 예: 텍스트 수정 가능하게, 저장 버튼 보이게 등
    }
}
