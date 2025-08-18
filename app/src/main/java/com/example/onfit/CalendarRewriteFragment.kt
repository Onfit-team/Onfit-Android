package com.example.onfit

import android.app.DatePickerDialog
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.onfit.databinding.FragmentCalendarRewriteBinding

class CalendarRewriteFragment : Fragment() {
    private var _binding: FragmentCalendarRewriteBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: CalendarRewriteAdapter
    private lateinit var pickImageLauncher: ActivityResultLauncher<String>
    private var selectedImageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        pickImageLauncher = registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            selectedImageUri = uri
            // 화면이 살아있을 때만 이미지 세팅
            if (_binding != null && uri != null) {
                binding.calendarRewriteOutfitIv.setImageURI(uri)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentCalendarRewriteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // RecyclerView 설정
        val recyclerView = binding.calendarRewriteRv
        recyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        val dummyItems = mutableListOf(
            CalendarRewriteItem(R.drawable.calendar_save_image2),
            CalendarRewriteItem(R.drawable.calendar_save_image3)
        )

        adapter = CalendarRewriteAdapter(dummyItems)
        recyclerView.adapter = adapter

        findNavController().currentBackStackEntry
            ?.savedStateHandle
            ?.getLiveData<List<Int>>("calendar_select_result")
            ?.observe(viewLifecycleOwner) { resIds ->
                if (resIds.isNullOrEmpty()) return@observe

                // 하나씩 어댑터에 추가
                resIds.forEach { resId ->
                    adapter.addItem(CalendarRewriteItem(resId))
                }
                // 맨 끝으로 스크롤 (선택사항)
                binding.calendarRewriteRv.post {
                    binding.calendarRewriteRv.smoothScrollToPosition(adapter.itemCount - 1)
                }

                // 재관찰로 인한 중복 추가 방지
                findNavController().currentBackStackEntry
                    ?.savedStateHandle
                    ?.remove<List<Int>>("calendar_select_result")
            }

        // 앨범 버튼 → 갤러리 열기
        binding.calendarRewriteAlbumIv.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        // 화면 재생성 시 선택된 이미지 복원
        selectedImageUri?.let { binding.calendarRewriteOutfitIv.setImageURI(it) }

        // 다이얼로그 띄우기
        binding.calendarRewriteMemoTv.setOnClickListener {
            TopSheetDialogFragment().show(parentFragmentManager, "TopSheet")
        }

        // 뒤로가기 (프래그먼트 백스택에서 pop)
        binding.calendarRewriteBackBtn.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // 옷장 선택 화면으로
        binding.calendarRewriteFl.setOnClickListener {
            findNavController().navigate(R.id.action_calendarRewriteFragment_to_calendarSelectFragment)
        }

        // 저장하면 수정한 정보 담아서 뒤로가기
        binding.calendarRewriteSaveBtn.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // 날짜 선택 드롭다운
        binding.calendarRewriteDropdownBtn.setOnClickListener {
            val currentDateText = binding.calendarRewriteDateTv.text.toString()
            val parts = currentDateText.split(".")
            val year = parts[0].toInt()
            val month = parts[1].toInt() - 1
            val day = parts[2].toInt()

            val datePickerDialog = DatePickerDialog(requireContext(), { _, selectedYear, selectedMonth, selectedDay ->
                val formatted = String.format("%04d.%02d.%02d", selectedYear, selectedMonth + 1, selectedDay)
                binding.calendarRewriteDateTv.text = formatted
            }, year, month, day)

            datePickerDialog.show()
        }
    }

    fun onMemoDone(memoText: String) {
        binding.calendarRewriteMemoTv.setText(memoText)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}