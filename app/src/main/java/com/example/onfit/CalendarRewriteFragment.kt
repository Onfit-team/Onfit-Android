package com.example.onfit

import android.app.DatePickerDialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.onfit.databinding.FragmentCalendarRewriteBinding
import java.io.File
import java.io.FileOutputStream

class CalendarRewriteFragment : Fragment() {
    private var _binding: FragmentCalendarRewriteBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: CalendarRewriteAdapter
    private lateinit var pickImageLauncher: ActivityResultLauncher<String>
    private var selectedImageUri: Uri? = null

    // ⭐ Safe Args 받기
    private val args by navArgs<CalendarRewriteFragmentArgs>()
    private val rewriteItems = mutableListOf<CalendarRewriteItem>() // ← 저장/전달용 리스트


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

        // ⭐ 1) Save → Rewrite 전달값을 그대로 UI에 반영
        binding.calendarRewriteDateTv.text    = args.selectedDate
        binding.calendarRewriteWeatherTv.text = args.weatherText
        binding.calendarRewriteMemoTv.setText(args.memoText)

        // 메인 이미지: URL/URI 모두 Glide로 처리 가능
        args.mainImageUrl?.let { url ->
            Glide.with(this)
                .load(url) // "http://", "content://", "file://" 모두 OK
                .placeholder(R.drawable.ic_launcher_background)
                .error(R.drawable.ic_launcher_background)
                .into(binding.calendarRewriteOutfitIv)
        }

        // RecyclerView 설정
        val recyclerView = binding.calendarRewriteRv
        recyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        val passedUrls: List<String> = args.itemImageUrls?.toList().orEmpty()

        val items = if (passedUrls.isNotEmpty()) {
            passedUrls.map { CalendarRewriteItem(imageUrl = it) }
        } else {
            // 넘어온 게 없으면 기존 더미 사용
            listOf(
                CalendarRewriteItem(imageResId = R.drawable.calendar_save_image2),
                CalendarRewriteItem(imageResId = R.drawable.calendar_save_image3)
            )
        }
        rewriteItems.clear()
        rewriteItems.addAll(items)

        adapter = CalendarRewriteAdapter(rewriteItems)
        recyclerView.adapter = adapter

        // CalendarSelect에서 돌아온 선택값 수신
        findNavController().currentBackStackEntry
            ?.savedStateHandle
            ?.getLiveData<List<Int>>("calendar_select_result")
            ?.observe(viewLifecycleOwner) { resIds ->
                if (resIds.isNullOrEmpty()) return@observe

                // 하나씩 어댑터에 추가
                resIds.forEach { resId ->
                    adapter.addItem(CalendarRewriteItem(resId))
                }

                // 재호출로 인한 중복 추가 방지
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
            val ctx = requireContext()
            val source: String? =
                selectedImageUri?.toString()
                    ?: drawableToCacheUri(ctx, binding.calendarRewriteOutfitIv.drawable)?.toString()

            val bundle = bundleOf("imageSource" to source)
            findNavController().navigate(
                R.id.action_calendarRewriteFragment_to_calendarSelectFragment,
                bundle
            )
        }

        // 저장하면 수정한 정보 담아서 뒤로가기
        binding.calendarRewriteSaveBtn.setOnClickListener {
            val newDate    = binding.calendarRewriteDateTv.text?.toString()
            val newWeather = binding.calendarRewriteWeatherTv.text?.toString()
            val newMemo    = binding.calendarRewriteMemoTv.text?.toString()
            val newMainUrl = currentMainImageString() // 아래 3) 함수
            val newItemUrls = collectItemUrls() // 아래 3) 함수

            findNavController().previousBackStackEntry?.savedStateHandle?.apply {
                set("rewrite_date", newDate)
                set("rewrite_weather", newWeather)
                set("rewrite_memo", newMemo)
                set("rewrite_main_image_url", newMainUrl)
                set("rewrite_item_image_urls", newItemUrls) // ArrayList<String>
            }
            findNavController().popBackStack()
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

    private fun currentMainImageString(): String? {
        // 1) 갤러리에서 고른 경우 우선
        selectedImageUri?.let { return it.toString() }
        // 2) 처음 넘어온 Safe Args
        if (!args.mainImageUrl.isNullOrBlank()) return args.mainImageUrl
        // 3) 현재 ImageView의 Drawable을 캐시에 저장해서 file:// 로
        return drawableToCacheUri(requireContext(), binding.calendarRewriteOutfitIv.drawable)?.toString()
    }

    private fun collectItemUrls(): ArrayList<String> {
        val list = arrayListOf<String>()
        for (item in rewriteItems) {
            when {
                item.imageUrl != null -> list.add(item.imageUrl)
                item.imageResId != null -> {
                    val d = ContextCompat.getDrawable(requireContext(), item.imageResId)
                    val uri = drawableToCacheUri(requireContext(), d)
                    if (uri != null) list.add(uri.toString())
                }
            }
        }
        return list
    }

    // drawable -> 캐시 파일 -> file://Uri
    private fun drawableToCacheUri(context: Context, d: Drawable?): Uri? {
        d ?: return null
        val bmp = when (d) {
            is BitmapDrawable -> d.bitmap
            else -> {
                val w = maxOf(1, d.intrinsicWidth)
                val h = maxOf(1, d.intrinsicHeight)
                Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).also { b ->
                    val c = Canvas(b); d.setBounds(0,0,w,h); d.draw(c)
                }
            }
        }
        val out = File(context.cacheDir, "rewrite_${System.currentTimeMillis()}.jpg")
        FileOutputStream(out).use { bmp.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        return Uri.fromFile(out)
    }

    fun onMemoDone(memoText: String) {
        binding.calendarRewriteMemoTv.setText(memoText)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}