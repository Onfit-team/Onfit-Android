package com.example.onfit.Wardrobe.adapter

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.example.onfit.R
import com.example.onfit.Wardrobe.Network.WardrobeItemDto

class WardrobeAdapter(
    private var itemList: List<Any> = emptyList(),
    private var onItemClick: ((Any) -> Unit)? = null
) : RecyclerView.Adapter<WardrobeAdapter.WardrobeViewHolder>() {

    inner class WardrobeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.imageView)
            ?: throw IllegalStateException("imageView not found in item_wardrobe.xml")
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WardrobeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_wardrobe, parent, false)
        return WardrobeViewHolder(view)
    }

    override fun onBindViewHolder(holder: WardrobeViewHolder, position: Int) {
        val item = itemList[position]

        when (item) {
            is Int -> {
                // 더미 데이터 (drawable 리소스)
                holder.imageView.setImageResource(item)
            }
            is WardrobeItemDto -> {
                // 🔥 API 데이터 처리 개선
                loadWardrobeImage(holder, item)
            }
        }

        // 클릭 리스너 설정
        holder.itemView.setOnClickListener {
            when (item) {
                is WardrobeItemDto -> {
                    Log.d("WardrobeAdapter", "아이템 클릭: ID=${item.id}")

                    // 🔥 더미 아이템 클릭 처리
                    if (item.id < 0) {
                        Log.d("WardrobeAdapter", "더미 아이템 클릭됨: ${item.id}")
                        // 더미 아이템은 특별한 처리 없이 그냥 ID 전달 (WardrobeFragment에서 처리)
                    }

                    onItemClick?.invoke(item.id) // ID만 전달
                }
                is Int -> {
                    Log.d("WardrobeAdapter", "Drawable 아이템 클릭: $item")
                    onItemClick?.invoke(item)
                }
                else -> {
                    Log.e("WardrobeAdapter", "알 수 없는 아이템 타입")
                }
            }
        }
    }

    /**
     * 🔥 NEW: 옷장 아이템 이미지 로딩
     */
    // WardrobeAdapter에서 수정된 코드
    private fun loadWardrobeImage(holder: WardrobeViewHolder, item: WardrobeItemDto) {
        // 🔥 이미지 로딩 전에 초기화
        holder.imageView.setImageDrawable(null)

        when {
            item.image.startsWith("drawable://") -> {
                val imageName = item.image.removePrefix("drawable://")
                val drawableResId = getDrawableResourceId(imageName)
                holder.imageView.setImageResource(drawableResId)
            }
            item.image.startsWith("http") -> {
                Glide.with(holder.imageView.context)
                    .load(item.image)
                    .placeholder(R.drawable.clothes8)
                    .error(R.drawable.clothes8)
                    .into(holder.imageView)
            }
            else -> {
                holder.imageView.setImageResource(R.drawable.clothes8)
            }
        }
    }

    /**
     * 🔥 NEW: drawable 리소스 ID 매핑
     */
    private fun getDrawableResourceId(imageName: String): Int {
        return when (imageName) {
            // 🔥 실제 drawable 파일명과 동일하게 매핑
            "shirts1" -> R.drawable.shirts1
            "pants1" -> R.drawable.pants1
            "shoes1" -> R.drawable.shoes1
            "shirts2" -> R.drawable.shirts2
            "pants2" -> R.drawable.pants2
            "shoes2" -> R.drawable.shoes2
            "shirts3" -> R.drawable.shirts3
            "shoes3" -> R.drawable.shoes3
            "pants3" -> R.drawable.pants3
            "shirts4" -> R.drawable.shirts4
            "shoes4" -> R.drawable.shoes4
            "bag4" -> R.drawable.bag4
            "acc3" -> R.drawable.acc3
            "pants4" -> R.drawable.pants4
            "shirts6" -> R.drawable.shirts6
            "pants6" -> R.drawable.pants6
            "shoes6" -> R.drawable.shoes6
            "acc6" -> R.drawable.acc6
            else -> R.drawable.clothes8 // 기본값 (없는 이미지일 때)
        }
    }

    private fun loadItemImage(imageView: ImageView, imageUrl: String) {
        when {
            // 🔥 NEW: drawable 리소스 처리
            imageUrl.startsWith("drawable://") -> {
                val imageName = imageUrl.removePrefix("drawable://")
                val drawableResId = getDrawableResourceId(imageName)
                imageView.setImageResource(drawableResId)
                Log.d("WardrobeAdapter", "✅ Drawable 이미지 로딩: $imageName")
            }

            // 기존 네트워크 이미지 처리
            imageUrl.startsWith("http") -> {
                Glide.with(imageView.context)
                    .load(imageUrl)
                    .placeholder(R.drawable.clothes8)
                    .error(R.drawable.clothes8)
                    .into(imageView)
            }

            // 기본 이미지
            else -> {
                imageView.setImageResource(R.drawable.clothes8)
            }
        }
    }

    // 🔥 Assets 이미지 직접 로딩 (한글 파일명 지원)
    private fun loadAssetsImageDirect(context: Context, imageUrl: String, imageView: ImageView, itemId: Int) {
        try {
            val fileName = imageUrl.substringAfter("file:///android_asset/dummy_recommend/")
            Log.d("WardrobeAdapter", "Assets 파일 직접 로딩: $fileName")

            val inputStream = context.assets.open("dummy_recommend/$fileName")
            val bitmap = BitmapFactory.decodeStream(inputStream)

            if (bitmap != null) {
                imageView.setImageBitmap(bitmap)
                Log.d("WardrobeAdapter", "Assets 직접 로딩 성공: $fileName")
            } else {
                Log.e("WardrobeAdapter", "Bitmap 디코딩 실패: $fileName")
                loadDummyImage(WardrobeViewHolder(imageView.parent as View), itemId)
            }

            inputStream.close()

        } catch (e: Exception) {
            Log.e("WardrobeAdapter", "Assets 직접 로딩 실패: $imageUrl", e)
            loadDummyImage(WardrobeViewHolder(imageView.parent as View), itemId)
        }
    }

    // 🔥 더미 이미지 로딩 함수
    private fun loadDummyImage(holder: WardrobeViewHolder, itemId: Int) {
        val dummyImages = listOf(
            R.drawable.clothes1, R.drawable.clothes2, R.drawable.clothes3,
            R.drawable.clothes4, R.drawable.clothes5, R.drawable.clothes6,
            R.drawable.clothes7, R.drawable.clothes8
        )

        val imageIndex = if (itemId != 0) {
            kotlin.math.abs(itemId) % dummyImages.size
        } else {
            0
        }

        val selectedImage = dummyImages[imageIndex]
        holder.imageView.setImageResource(selectedImage)
        Log.d("WardrobeAdapter", "더미 이미지 설정: $selectedImage (index: $imageIndex)")
    }

    override fun getItemCount(): Int = itemList.size

    /**
     * 데이터 업데이트 함수
     */
    fun updateData(newItemList: List<Any>) {
        itemList = newItemList
        notifyDataSetChanged()
    }

    /**
     * API 데이터로 업데이트하는 함수
     */
    fun updateWithApiData(newItems: List<WardrobeItemDto>) {
        Log.d("WardrobeAdapter", "🔄 updateWithApiData 호출됨")
        Log.d("WardrobeAdapter", "  - 기존 아이템 수: ${itemList.size}")
        Log.d("WardrobeAdapter", "  - 새로운 아이템 수: ${newItems.size}")

        if (newItems.isNotEmpty()) {
            Log.d("WardrobeAdapter", "  - 새로운 아이템 ID들: ${newItems.map { it.id }}")
        }

        // 🔥 List를 새로 생성해서 교체
        itemList = newItems.toList()

        // 🔥 전체 데이터 갱신
        notifyDataSetChanged()

        Log.d("WardrobeAdapter", "📊 updateWithApiData 완료")
        Log.d("WardrobeAdapter", "  - 최종 아이템 수: ${itemList.size}")
        Log.d("WardrobeAdapter", "  - itemCount: $itemCount")
    }
}