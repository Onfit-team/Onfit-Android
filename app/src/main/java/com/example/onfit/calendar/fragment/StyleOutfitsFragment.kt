package com.example.onfit.calendar.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.Gravity
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.util.Log
import android.widget.Toast
import com.example.onfit.R

class StyleOutfitsFragment : Fragment() {

    private lateinit var rvOutfitItems: RecyclerView
    private lateinit var styleFilterLayout: LinearLayout
    private lateinit var styleFilterScrollView: HorizontalScrollView

    // ✅ 스타일 필터 목록 (캐주얼, 스트릿 맨 앞으로)
    private val styleFilters = listOf("캐주얼", "스트릿", "빈티지", "미니멀", "포멀")

    // 🔥 더미 코디 데이터
    private val dummyOutfits = listOf(
        DummyOutfitItem(
            id = 1,
            imageResName = "ccody1",
            style = "스트릿",
            date = "2025-08-01",
            description = "캐주얼 스타일 코디 1"
        ),
        DummyOutfitItem(
            id = 2,
            imageResName = "ccody2",
            style = "스트릿",
            date = "2025-08-02",
            description = "캐주얼 스타일 코디 2"
        ),
        DummyOutfitItem(
            id = 3,
            imageResName = "ccody3",
            style = "캐주얼",
            date = "2025-08-03",
            description = "캐주얼 스타일 코디 3"
        ),
        DummyOutfitItem(
            id = 4,
            imageResName = "ccody4",
            style = "캐주얼",
            date = "2025-08-04",
            description = "캐주얼 스타일 코디 4"
        ),
        DummyOutfitItem(
            id = 4,
            imageResName = "cody6",
            style = "캐주얼",
            date = "2025-08-14",
            description = "캐주얼 스타일 코디 4"
        ),
        DummyOutfitItem(
            id = 5,
            imageResName = "cody5",
            style = "캐주얼",
            date = "2025-08-05",
            description = "캐주얼 스타일 코디 5"
        )
    )

    // 스타일별 개수 계산
    private val styleTagCounts by lazy {
        val counts = mutableMapOf<String, Int>()
        styleFilters.forEach { style ->
            counts[style] = dummyOutfits.count { it.style == style }
        }
        counts
    }

    private var selectedStyleIndex = 0
    private var currentOutfits = mutableListOf<DummyOutfitItem>()
    private lateinit var outfitGridAdapter: OutfitGridAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_style_outfits, container, false)

        initializeViews(view)
        setupRecyclerView()
        setupStyleFilters()
        setupBackButton(view)

        // ✅ 캐주얼 먼저 표시
        loadAllOutfits()
        filterOutfitsByStyle("캐주얼")

        return view
    }

    private fun initializeViews(view: View) {
        rvOutfitItems = view.findViewById(R.id.rvOutfitItems)
        styleFilterLayout = view.findViewById(R.id.styleFilterLayout)
        styleFilterScrollView = view.findViewById(R.id.styleFilterScrollView)
    }

    private fun setupRecyclerView() {
        outfitGridAdapter = OutfitGridAdapter(emptyList()) { outfit ->
            navigateToCalendarSave(outfit)
        }

        rvOutfitItems.layoutManager = GridLayoutManager(requireContext(), 2)
        rvOutfitItems.adapter = outfitGridAdapter
    }

    private fun updateRecyclerView() {
        if (::outfitGridAdapter.isInitialized) {
            outfitGridAdapter.updateItems(currentOutfits)
        }
    }

    private fun navigateToCalendarSave(outfit: DummyOutfitItem) {
        try {
            val bundle = Bundle().apply {
                putString("selected_date", outfit.date)
                putInt("outfit_id", outfit.id + 1100)
                putInt("outfit_number", outfit.id)
                putString("outfit_image_res", outfit.imageResName)
                putString("outfit_style", outfit.style)
                putString("memo", outfit.description)
                putBoolean("is_dummy_outfit", true)
                putBoolean("from_style_outfits", true)
                putBoolean("from_outfit_record", true)
            }

            findNavController().navigate(R.id.calendarSaveFragment, bundle)
        } catch (e: Exception) {
            Toast.makeText(
                requireContext(),
                "${outfit.description} (${outfit.date})",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun setupStyleFilters() {
        styleFilters.forEachIndexed { index, styleName ->
            val button = createStyleFilterButton(styleName, index)
            styleFilterLayout.addView(button)
        }

        styleFilterScrollView.post {
            styleFilterScrollView.scrollTo(0, 0)
        }

        val casualIndex = styleFilters.indexOf("캐주얼")
        if (casualIndex != -1) {
            updateStyleButtonSelection(casualIndex)
        }
    }

    private fun loadAllOutfits() {
        currentOutfits.clear()
        currentOutfits.addAll(dummyOutfits)
        updateRecyclerView()
    }

    private fun createStyleFilterButton(styleName: String, index: Int): Button {
        val count = styleTagCounts[styleName] ?: 0
        val buttonText = "$styleName $count"

        val button = Button(requireContext()).apply {
            text = buttonText
            setTextColor(ContextCompat.getColor(context, android.R.color.black))
            background = try {
                ContextCompat.getDrawable(context, R.drawable.style_tag_background)
            } catch (e: Exception) {
                null
            }
            setPadding(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8))
            textSize = 14f
            isAllCaps = false
            minWidth = dpToPx(80)
            minHeight = dpToPx(36)
            setSingleLine(true)
            gravity = Gravity.CENTER
        }

        val buttonParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            if (index > 0) leftMargin = dpToPx(8)
        }
        button.layoutParams = buttonParams

        button.setOnClickListener {
            updateStyleButtonSelection(index)
            filterOutfitsByStyle(styleName)
        }

        return button
    }

    private fun updateStyleButtonSelection(newSelectedIndex: Int) {
        if (!isAdded || context == null) return
        try {
            for (i in 0 until styleFilterLayout.childCount) {
                val button = styleFilterLayout.getChildAt(i) as? Button
                button?.isSelected = false
                button?.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))
            }

            if (newSelectedIndex < styleFilterLayout.childCount) {
                val selectedButton = styleFilterLayout.getChildAt(newSelectedIndex) as? Button
                selectedButton?.isSelected = true
                selectedButton?.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
            }

            selectedStyleIndex = newSelectedIndex
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun filterOutfitsByStyle(styleName: String) {
        currentOutfits.clear()
        val filteredOutfits = dummyOutfits.filter { it.style == styleName }
        currentOutfits.addAll(filteredOutfits)
        updateRecyclerView()
    }

    private fun setupBackButton(view: View) {
        view.findViewById<View>(R.id.ic_back)?.setOnClickListener {
            try {
                findNavController().navigateUp()
            } catch (e: Exception) {
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        try {
            if (::rvOutfitItems.isInitialized) {
                rvOutfitItems.adapter = null
            }
            if (::styleFilterLayout.isInitialized) {
                styleFilterLayout.removeAllViews()
            }
        } catch (_: Exception) {}
    }

    override fun onStart() {
        super.onStart()
        try {
            val activity = requireActivity()
            val bottomNav = activity.findViewById<View>(R.id.bottomNavigationView)
            bottomNav?.visibility = View.GONE
        } catch (_: Exception) {}
    }

    override fun onStop() {
        super.onStop()
        try {
            val activity = requireActivity()
            val bottomNav = activity.findViewById<View>(R.id.bottomNavigationView)
            bottomNav?.visibility = View.VISIBLE
        } catch (_: Exception) {}
    }
}

// 🔥 더미 코디 데이터 클래스
data class DummyOutfitItem(
    val id: Int,
    val imageResName: String,
    val style: String,
    val date: String,
    val description: String
)

// 🔥 어댑터
class OutfitGridAdapter(
    private var items: List<DummyOutfitItem>,
    private val onItemClick: (DummyOutfitItem) -> Unit
) : RecyclerView.Adapter<OutfitGridAdapter.OutfitViewHolder>() {

    fun updateItems(newItems: List<DummyOutfitItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OutfitViewHolder {
        return OutfitViewHolder.create(parent)
    }

    override fun onBindViewHolder(holder: OutfitViewHolder, position: Int) {
        holder.bind(items[position], onItemClick)
    }

    override fun getItemCount(): Int = items.size

    class OutfitViewHolder private constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView

        init {
            imageView = (itemView as LinearLayout).getChildAt(0) as ImageView
        }

        companion object {
            fun create(parent: ViewGroup): OutfitViewHolder {
                val context = parent.context

                val container = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = ViewGroup.MarginLayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(12, 8, 12, 8)
                    }
                    // ✅ 그림자 제거 / 흰 배경 제거
                    setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent))
                    elevation = 0f
                }

                // ✅ 정사각형 이미지뷰
                val size = (180 * context.resources.displayMetrics.density).toInt()
                val imageView = ImageView(context).apply {
                    layoutParams = LinearLayout.LayoutParams(size, size).apply {
                        gravity = Gravity.CENTER
                    }
                    scaleType = ImageView.ScaleType.CENTER_CROP

                    background = ContextCompat.getDrawable(context, R.drawable.rounded_gray_bg)
                    clipToOutline = true
                }

                container.addView(imageView)

                return OutfitViewHolder(container)
            }
        }

        fun bind(outfit: DummyOutfitItem, onItemClick: (DummyOutfitItem) -> Unit) {
            try {
                val resourceId = itemView.context.resources.getIdentifier(
                    outfit.imageResName,
                    "drawable",
                    itemView.context.packageName
                )
                if (resourceId != 0) {
                    imageView.setImageResource(resourceId)
                } else {
                    imageView.setImageResource(android.R.drawable.ic_menu_gallery)
                }
            } catch (e: Exception) {
                imageView.setImageResource(android.R.drawable.ic_menu_gallery)
            }

            itemView.setOnClickListener { onItemClick(outfit) }
        }
    }
}
