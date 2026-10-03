package com.esha.fitsync.screens.gym.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.esha.fitsync.BuildConfig
import com.esha.fitsync.R

class GymPhotoPagerAdapter(
    private var photoUrls: List<String> = emptyList()
) : RecyclerView.Adapter<GymPhotoPagerAdapter.PhotoViewHolder>() {

    fun submitList(photos: List<String>) {
        photoUrls = if (photos.isEmpty()) listOf("") else photos
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_gym_photo_page, parent, false)
        return PhotoViewHolder(view)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        holder.bind(photoUrls[position])
    }

    override fun getItemCount(): Int = photoUrls.size

    class PhotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivPhoto: ImageView = itemView.findViewById(R.id.ivPhotoPage)

        fun bind(url: String) {
            if (url.isEmpty()) {
                ivPhoto.setImageResource(R.drawable.gym)
                return
            }

            val fullUrl = if (url.startsWith("http")) url else {
                val base = BuildConfig.BASE_URL.trimEnd('/')
                "$base/${url.trimStart('/')}"
            }

            Glide.with(itemView.context)
                .load(fullUrl)
                .transform(CenterCrop())
                .placeholder(R.drawable.gym)
                .error(R.drawable.gym)
                .into(ivPhoto)
        }
    }
}
