package com.example.onfit

import android.app.DatePickerDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.onfit.databinding.FragmentCalendarRewriteBinding

class CalendarRewriteFragment : Fragment() {
    private var _binding: FragmentCalendarRewriteBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: CalendarRewriteAdapter


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

        // 다이얼로그 띄우기
        binding.calendarRewriteMemoTv.setOnClickListener {
            TopSheetDialogFragment().show(parentFragmentManager, "TopSheet")
        }

        // 뒤로가기 (프래그먼트 백스택에서 pop)
        binding.calendarRewriteBackBtn.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

//        binding.calendarRewriteSaveBtn.setOnClickListener {
//            parentFragmentManager.beginTransaction()
//                .replace(R.id.register_container, CalendarSelectFragment())
//                .addToBackStack(null)
//                .commit()
//        }

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

    override fun onResume() {
        super.onResume()
        // 실행 중 bottom navigation view 보이지 않게
        activity?.findViewById<View>(R.id.bottomNavigationView)?.visibility = View.GONE
    }

    override fun onPause() {
        super.onPause()
        // 실행 안 할 때 bottom navigation view 다시 보이게
        activity?.findViewById<View>(R.id.bottomNavigationView)?.visibility = View.VISIBLE
    }
}