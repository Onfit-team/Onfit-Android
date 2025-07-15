package com.example.onfit

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.widget.LinearLayout
import androidx.fragment.app.Fragment

class ClothesDetailFragment : Fragment() {

    private var imageResId: Int = 0

    companion object {
        private const val ARG_IMAGE_RES_ID = "image_res_id"

        fun newInstance(imageResId: Int): ClothesDetailFragment {
            val fragment = ClothesDetailFragment()
            val args = Bundle()
            args.putInt(ARG_IMAGE_RES_ID, imageResId)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            imageResId = it.getInt(ARG_IMAGE_RES_ID, 0)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // 이미 존재하는 fragment_clothes_detail 레이아웃 사용
        return inflater.inflate(R.layout.fragment_clothes_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 뒤로가기 버튼 (있다면)
        val backButton = view.findViewById<ImageButton>(R.id.ic_back)
        backButton?.setOnClickListener {
            requireActivity().finish()
        }

        // 편집 버튼 클릭 리스너
        val editButton = view.findViewById<ImageButton>(R.id.ic_edit)
        editButton?.setOnClickListener {
            navigateToAddItem()
        }

        // 삭제 버튼 클릭 리스너
        val deleteButton = view.findViewById<ImageButton>(R.id.ic_delete)
        deleteButton?.setOnClickListener {
            showDeleteConfirmDialog()
        }

        // 이미지 설정 (실제 레이아웃의 ID로 변경 필요)
        val clothesImageView = view.findViewById<ImageView>(R.id.imageView) // 또는 실제 ID
        clothesImageView?.setImageResource(imageResId)

        // 여기에 상세 페이지 로직 추가
        // 옷 정보, 태그, 버튼들 등
    }

    private fun navigateToAddItem() {
        // AddItemActivity로 이동 (편집 모드)
        val intent = Intent(requireContext(), AddItemActivity::class.java)
        intent.putExtra("edit_mode", true) // 편집 모드임을 전달
        intent.putExtra("image_res_id", imageResId) // 현재 이미지 정보 전달
        startActivity(intent)
    }

    private fun showDeleteConfirmDialog() {
        // 커스텀 다이얼로그 생성
        val dialog = android.app.Dialog(requireContext())
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)

        // 메인 레이아웃 (하얀색 배경, 294*132)
        val mainLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(30, 60, 30, 60)
            gravity = android.view.Gravity.CENTER

            // 외부 배경 (하얀색, border radius 8.09dp)
            val outerDrawable = android.graphics.drawable.GradientDrawable().apply {
                setColor(android.graphics.Color.WHITE)
                cornerRadius = 8.09f * resources.displayMetrics.density
            }
            background = outerDrawable

            // 정확한 크기 설정 (294*132 dp)
            val params = LinearLayout.LayoutParams(
                (294 * resources.displayMetrics.density).toInt(),
                (132 * resources.displayMetrics.density).toInt()
            )
            layoutParams = params
        }

        // 메시지 텍스트 (PretendardSemiBold 17sp)
        val messageText = TextView(requireContext()).apply {
            text = "이 아이템을 삭제하겠습니까?"
            textSize = 17f
            setTextColor(android.graphics.Color.BLACK)
            gravity = android.view.Gravity.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD // PretendardSemiBold 대신

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
            layoutParams = params
        }
        mainLayout.addView(messageText)

        // 버튼 컨테이너
        val buttonLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 50, 0, 0)
            }
            layoutParams = params
        }

        // 예 버튼 (127*38 dp)
        val yesButton = Button(requireContext()).apply {
            text = "예"
            setTextColor(android.graphics.Color.WHITE)
            textSize = 16.17f // PretendardSemiBold 16.17sp
            typeface = android.graphics.Typeface.DEFAULT_BOLD

            // 파란색 배경 (border radius 4.04dp)
            val buttonDrawable = android.graphics.drawable.GradientDrawable().apply {
                setColor(android.graphics.Color.parseColor("#007AFF"))
                cornerRadius = 4.04f * resources.displayMetrics.density
            }
            background = buttonDrawable

            // 정확한 크기 설정 (127*38 dp)
            val params = LinearLayout.LayoutParams(
                (127 * resources.displayMetrics.density).toInt(),
                (38 * resources.displayMetrics.density).toInt()
            ).apply {
                setMargins(0, 0, 10, 0) // 버튼 사이 간격
            }
            layoutParams = params

            setOnClickListener {
                deleteItem()
                dialog.dismiss()
            }
        }

        // 아니오 버튼 (127*38 dp)
        val noButton = Button(requireContext()).apply {
            text = "아니오"
            setTextColor(android.graphics.Color.WHITE)
            textSize = 16.17f // PretendardSemiBold 16.17sp
            typeface = android.graphics.Typeface.DEFAULT_BOLD

            // 파란색 배경 (border radius 4.04dp)
            val buttonDrawable = android.graphics.drawable.GradientDrawable().apply {
                setColor(android.graphics.Color.parseColor("#007AFF"))
                cornerRadius = 4.04f * resources.displayMetrics.density
            }
            background = buttonDrawable

            // 정확한 크기 설정 (127*38 dp)
            val params = LinearLayout.LayoutParams(
                (127 * resources.displayMetrics.density).toInt(),
                (38 * resources.displayMetrics.density).toInt()
            ).apply {
                setMargins(10, 0, 0, 0) // 버튼 사이 간격
            }
            layoutParams = params

            setOnClickListener {
                dialog.dismiss()
            }
        }

        buttonLayout.addView(yesButton)
        buttonLayout.addView(noButton)
        mainLayout.addView(buttonLayout)

        dialog.setContentView(mainLayout)

        // 다이얼로그 창 설정
        dialog.window?.apply {
            setLayout(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        }

        dialog.show()
    }

    private fun deleteItem() {
        // 실제 삭제 로직 구현
        // 예: 데이터베이스에서 삭제, 서버에 삭제 요청 등

        Toast.makeText(requireContext(), "아이템이 삭제되었습니다", Toast.LENGTH_SHORT).show()

        // 이전 화면으로 돌아가기
        requireActivity().finish()
    }
}