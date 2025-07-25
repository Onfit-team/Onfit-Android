package com.example.onfit

import android.app.DatePickerDialog
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.onfit.databinding.FragmentRegisterBinding
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class RegisterFragment : Fragment(), TopSheetDialogFragment.OnMemoDoneListener {
    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 날짜 오늘 날짜로 기본 설정
        val today = Calendar.getInstance().time
        val dateFormat = SimpleDateFormat("yyyy.MM.dd", Locale.KOREA)
        binding.registerDateTv.text = dateFormat.format(today)

        // 위에서 메모 TopSheetDialog 내려옴
        binding.registerMemoEt.setOnClickListener {
            val dialog = TopSheetDialogFragment()
            dialog.setOnMemoDoneListener(object : TopSheetDialogFragment.OnMemoDoneListener {
                override fun onMemoDone(memoText: String) {
                    binding.registerMemoEt.setText(memoText)
                }
            })
            dialog.show(parentFragmentManager, "TopSheet")
        }

        // 날짜 수정
        binding.registerDropdownBtn.setOnClickListener {
            val parts = binding.registerDateTv.text.toString().split(".")
            val year = parts[0].toInt()
            val month = parts[1].toInt() - 1
            val day = parts[2].toInt()

            DatePickerDialog(requireContext(), { _, y, m, d ->
                binding.registerDateTv.text = String.format("%04d.%02d.%02d", y, m + 1, d)
            }, year, month, day).show()
        }

        // 저장 버튼 누르면 날짜, 이미지 Bundle로 전달
        binding.registerSaveBtn.setOnClickListener {
            val parts = binding.registerDateTv.text.toString().split(".")
            val formattedDate = "${parts[1].toInt()}월 ${parts[2].toInt()}일"

            val bitmap = (binding.registerOutfitIv.drawable as BitmapDrawable).bitmap
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            val byteArray = stream.toByteArray()

            val bundle = Bundle().apply {
                putString("save_date", formattedDate)
                putByteArray("outfit_image", byteArray)
            }

            val saveFragment = SaveFragment().apply { arguments = bundle }

            parentFragmentManager.beginTransaction()
                .replace(R.id.register_container, saveFragment)
                .addToBackStack(null)
                .commit()
        }

        // 뒤로가기 버튼
        binding.registerBackBtn.setOnClickListener {
            activity?.finish()
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