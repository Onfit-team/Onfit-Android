package com.example.onfit.HomeRegister.fragment

import android.app.DatePickerDialog
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.onfit.TopSheetDialogFragment
import com.example.onfit.databinding.FragmentHomeRegisterBinding
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import com.example.onfit.R


class HomeRegisterFragment : Fragment(), TopSheetDialogFragment.OnMemoDoneListener {

    private var _binding: FragmentHomeRegisterBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 날짜 기본값
        val today = Calendar.getInstance().time
        val dateFormat = SimpleDateFormat("yyyy.MM.dd", Locale.KOREA)
        binding.registerDateTv.text = dateFormat.format(today)

        // 드롭다운으로 날짜 변경
        binding.registerDropdownBtn.setOnClickListener {
            val parts = binding.registerDateTv.text.toString().split(".")
            val year = parts[0].toInt()
            val month = parts[1].toInt() - 1
            val day = parts[2].toInt()

            val datePickerDialog = DatePickerDialog(requireContext(), { _, y, m, d ->
                val formatted = String.format("%04d.%02d.%02d", y, m + 1, d)
                binding.registerDateTv.text = formatted
            }, year, month, day)

            datePickerDialog.show()
        }

        // 메모 다이얼로그
        binding.registerMemoEt.setOnClickListener {
            TopSheetDialogFragment().show(parentFragmentManager, "TopSheet")
        }

        // 기록하기 버튼
        binding.registerSaveBtn.setOnClickListener {
            val dateParts = binding.registerDateTv.text.toString().split(".")
            val formattedDate = "${dateParts[1].toInt()}월 ${dateParts[2].toInt()}일"

            val drawable = binding.registerOutfitIv.drawable
            val bitmap = (drawable as BitmapDrawable).bitmap
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            val byteArray = stream.toByteArray()

            // 다음 프래그먼트로 전환
            // 기록하기 버튼
            binding.registerSaveBtn.setOnClickListener {
                val dateParts = binding.registerDateTv.text.toString().split(".")
                val formattedDate = "${dateParts[1].toInt()}월 ${dateParts[2].toInt()}일"

                val drawable = binding.registerOutfitIv.drawable
                val bitmap = (drawable as BitmapDrawable).bitmap
                val stream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                val byteArray = stream.toByteArray()

                // 데이터 전달할 Bundle
                val bundle = Bundle().apply {
                    putString("save_date", formattedDate)
                    putByteArray("outfit_image", byteArray)
                }

                // Navigation을 통한 전환
                findNavController().navigate(R.id.homeSaveFragment, bundle)
            }

        }
    }

    override fun onMemoDone(memoText: String) {
        binding.registerMemoEt.setText(memoText)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
