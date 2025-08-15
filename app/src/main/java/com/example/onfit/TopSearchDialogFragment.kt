// app/src/main/java/com/example/onfit/TopSearchDialogFragment.kt
package com.example.onfit

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.view.Window
import androidx.core.view.children
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.findNavController
import com.example.onfit.databinding.FragmentTopSearchDialogBinding
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

class TopSearchDialogFragment : DialogFragment() {

    private var _binding: FragmentTopSearchDialogBinding? = null
    private val binding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext(), R.style.TopSheetDialog)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        _binding = FragmentTopSearchDialogBinding.inflate(layoutInflater)
        dialog.setContentView(binding.root)

        dialog.window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setGravity(Gravity.TOP)
            setWindowAnimations(R.style.TopSheetAnimation)
        }

        // 뒤로가기
        binding.backBtn.setOnClickListener { dismiss() }

        // ===== 전달받은 선택 서버태그ID들(=chip.tag 값)로 프리체크 =====
        val preSelected = arguments?.getIntArray("preSelectedTagIds")?.toSet() ?: emptySet()

        fun precheck(group: ChipGroup) {
            group.children
                .filterIsInstance<Chip>()
                .forEach { chip ->
                    val tagId = chip.tag?.toString()?.toIntOrNull()
                    chip.isChecked = tagId != null && preSelected.contains(tagId)
                }
        }
        precheck(binding.vibeChips)
        precheck(binding.useChips)

        // ===== 적용 버튼 =====
        binding.doneBtn.setOnClickListener {
            // chip.tag 값(서버 태그 ID) 수집
            val selectedServerTagIds = buildList {
                fun collect(group: ChipGroup) {
                    group.children
                        .filterIsInstance<Chip>()
                        .filter { it.isChecked }
                        .mapNotNull { it.tag?.toString()?.toIntOrNull() }
                        .forEach { add(it) }
                }
                collect(binding.vibeChips)
                collect(binding.useChips)
            }

            // CommunityFragment로 결과 전달
            Log.d("TopSearch", "set selectedTagIds=$selectedServerTagIds")
            val result = android.os.Bundle().apply {
                putIntArray("selectedTagIds", selectedServerTagIds.toIntArray())
            }
            parentFragmentManager.setFragmentResult("selectedTags", result)

            dismiss()

        }

        return dialog
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
