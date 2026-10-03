package com.esha.fitsync.screens.admin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.esha.fitsync.BuildConfig
import com.esha.fitsync.R
import com.esha.fitsync.models.AdminEntityItem

class AdminEntityAdapter(
    private var items: List<AdminEntityItem> = emptyList(),
    private val onItemClick: (AdminEntityItem) -> Unit
) : RecyclerView.Adapter<AdminEntityAdapter.EntityViewHolder>() {

    fun submitList(newItems: List<AdminEntityItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EntityViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_admin_entity, parent, false)
        return EntityViewHolder(view, onItemClick)
    }

    override fun onBindViewHolder(holder: EntityViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class EntityViewHolder(
        itemView: View,
        private val onItemClick: (AdminEntityItem) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val tvName: TextView = itemView.findViewById(R.id.tvEntityName)
        private val tvSubtitle: TextView = itemView.findViewById(R.id.tvEntitySubtitle)
        private val tvBadge: TextView = itemView.findViewById(R.id.tvEntityStatusBadge)
        private val tvFlagged: TextView = itemView.findViewById(R.id.tvFlaggedAlert)
        private val ivThumb: ImageView = itemView.findViewById(R.id.ivEntityThumbnail)

        fun bind(item: AdminEntityItem) {
            tvName.text = item.displayName
            val ownerName = item.owner?.name ?: item.seller?.name ?: item.type ?: ""
            val cityStr = item.city ?: ""
            tvSubtitle.text = listOf(cityStr, ownerName).filter { it.isNotEmpty() }.joinToString(" • ")

            // Status Badge
            when (item.status.lowercase()) {
                "approved" -> {
                    tvBadge.text = "APPROVED"
                    tvBadge.setTextColor(ContextCompat.getColor(itemView.context, R.color.status_approved))
                }
                "rejected" -> {
                    tvBadge.text = "REJECTED"
                    tvBadge.setTextColor(ContextCompat.getColor(itemView.context, R.color.status_rejected))
                }
                "suspended" -> {
                    tvBadge.text = "SUSPENDED"
                    tvBadge.setTextColor(ContextCompat.getColor(itemView.context, R.color.status_rejected))
                }
                else -> {
                    tvBadge.text = "PENDING"
                    tvBadge.setTextColor(ContextCompat.getColor(itemView.context, R.color.status_pending))
                }
            }

            // Flagged for medical claims
            if (item.flaggedForAdminReview || !item.flagReason.isNullOrEmpty()) {
                tvFlagged.visibility = View.VISIBLE
            } else {
                tvFlagged.visibility = View.GONE
            }

            // Thumbnail
            val img = item.primaryImage
            if (!img.isNullOrEmpty()) {
                val fullUrl = if (img.startsWith("http")) img else {
                    val base = BuildConfig.BASE_URL.trimEnd('/')
                    "$base/${img.trimStart('/')}"
                }
                Glide.with(itemView.context)
                    .load(fullUrl)
                    .transform(CenterCrop(), RoundedCorners(14))
                    .placeholder(R.drawable.gym)
                    .error(R.drawable.gym)
                    .into(ivThumb)
            } else {
                ivThumb.setImageResource(R.drawable.gym)
            }

            itemView.setOnClickListener { onItemClick(item) }
        }
    }
}
