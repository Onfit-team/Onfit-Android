package com.example.onfit.Community.fragment

import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.onfit.Community.adapter.CommunityDetailClothAdapter
import com.example.onfit.R
import com.example.onfit.databinding.FragmentCommunityDetailBinding
import com.google.android.material.button.MaterialButton
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class CommunityDetailFragment : Fragment() {

    private var _binding: FragmentCommunityDetailBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCommunityDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 날짜 표시
        val today = LocalDate.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")
        binding.dateTv.text = today.format(formatter)

        // 더미 데이터
        binding.mainIv.setImageResource(R.drawable.communitycloth1)
        binding.likesTv.text = "128"
        binding.tempTv.text = "18.5°C"
        binding.descTv.setText("오늘의 스타일은 편한 느낌이에요.")

        val dummyClothList = listOf(
            Pair(R.drawable.cody_image4, "후드티"),
            Pair(R.drawable.clothes1, "이상한 옷"),
            Pair(R.drawable.clothes8, "후드티")
        )

        binding.clothRecyclerview.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.clothRecyclerview.adapter = CommunityDetailClothAdapter(dummyClothList)

        binding.deleteIv.setOnClickListener {
            showDeleteOutfitDialog()
        }

        binding.backIv.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun showDeleteOutfitDialog() {
        val dialogView = layoutInflater.inflate(R.layout.outfit_delete_dialog, null)
        val dialog = AlertDialog.Builder(requireContext()).create()
        dialog.setView(dialogView)
        dialog.setCancelable(false)
        dialog.window?.setBackgroundDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.rounded_white_bg))

        val yesButton = dialogView.findViewById<MaterialButton>(R.id.delete_dialog_yes_btn)
        val noButton = dialogView.findViewById<MaterialButton>(R.id.delete_dialog_no_btn)

        yesButton.setOnClickListener { dialog.dismiss() }
        noButton.setOnClickListener { dialog.dismiss() }

        val width = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 294f, resources.displayMetrics
        ).toInt()
        dialog.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)

        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
