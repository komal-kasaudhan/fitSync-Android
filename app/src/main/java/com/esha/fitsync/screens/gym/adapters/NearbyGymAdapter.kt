package com.esha.fitsync.screens.gym.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.esha.fitsync.BuildConfig
import com.esha.fitsync.R
import com.esha.fitsync.models.GymItem
import java.util.Calendar
import java.util.Locale

class NearbyGymAdapter(
    private val onGymClick: (GymItem) -> Unit
) : ListAdapter<GymItem, NearbyGymAdapter.GymViewHolder>(GymDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GymViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_gym_card, parent, false)
        return GymViewHolder(view, onGymClick)
    }

    override fun onBindViewHolder(holder: GymViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class GymViewHolder(
        itemView: View,
        private val onGymClick: (GymItem) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val ivPhoto: ImageView = itemView.findViewById(R.id.ivGymPhoto)
        private val tvRating: TextView = itemView.findViewById(R.id.tvRating)
        private val tvDistance: TextView = itemView.findViewById(R.id.tvDistance)
        private val tvOpenStatus: TextView = itemView.findViewById(R.id.tvOpenStatus)
        private val tvGymName: TextView = itemView.findViewById(R.id.tvGymName)
        private val tvGymAddress: TextView = itemView.findViewById(R.id.tvGymAddress)
        private val tvAmenities: TextView = itemView.findViewById(R.id.tvAmenities)
        private val tvPrice: TextView = itemView.findViewById(R.id.tvPrice)

        fun bind(gym: GymItem) {
            tvGymName.text = gym.name
            val fullAddress = if (gym.city.isNotEmpty() && !gym.address.contains(gym.city, ignoreCase = true)) {
                "${gym.address}, ${gym.city}"
            } else {
                gym.address
            }
            tvGymAddress.text = fullAddress

            tvRating.text = String.format(Locale.US, "%.1f (%d)", gym.ratingAvg, gym.ratingCount)
            tvDistance.text = String.format(Locale.US, "%.2f km", gym.distanceKm)

            val price = gym.startingPrice
            val planType = gym.startingPlanType?.replace("_", " ")?.replaceFirstChar { it.uppercase() } ?: "day"
            if (price > 0) {
                tvPrice.visibility = View.VISIBLE
                tvPrice.text = "From ₹${price.toInt()}/$planType"
            } else {
                tvPrice.visibility = View.GONE
            }

            if (gym.amenities.isNotEmpty()) {
                val formattedAmenities = gym.amenities.take(4).joinToString("  •  ") {
                    it.replaceFirstChar(Char::uppercase)
                }
                tvAmenities.visibility = View.VISIBLE
                tvAmenities.text = "• $formattedAmenities"
            } else {
                tvAmenities.visibility = View.GONE
            }

            // Open/closed status check
            val isOpen = isGymOpenNow(gym)
            if (isOpen) {
                tvOpenStatus.text = "OPEN NOW"
                tvOpenStatus.setTextColor(ContextCompat.getColor(itemView.context, R.color.gym_green))
            } else {
                tvOpenStatus.text = "CLOSED"
                tvOpenStatus.setTextColor(ContextCompat.getColor(itemView.context, R.color.text_gray_blue))
            }

            // Image loading
            val photoUrl = gym.primaryPhoto
            val fullUrl = when {
                photoUrl.isNullOrEmpty() -> null
                photoUrl.startsWith("http://") || photoUrl.startsWith("https://") -> photoUrl
                else -> {
                    val base = BuildConfig.BASE_URL.trimEnd('/')
                    val path = if (photoUrl.startsWith("/")) photoUrl else "/$photoUrl"
                    "$base$path"
                }
            }

            if (!fullUrl.isNullOrEmpty()) {
                Glide.with(itemView.context)
                    .load(fullUrl)
                    .transform(CenterCrop(), RoundedCorners(24))
                    .placeholder(R.drawable.gym)
                    .error(R.drawable.gym)
                    .into(ivPhoto)
            } else {
                ivPhoto.setImageResource(R.drawable.gym)
            }

            itemView.setOnClickListener { onGymClick(gym) }
        }

        private fun isGymOpenNow(gym: GymItem): Boolean {
            val cal = Calendar.getInstance()
            val todayDateStr = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
            if (gym.holidays.any { it.equals(todayDateStr, ignoreCase = true) }) {
                return false
            }

            if (gym.openingHours.isEmpty()) return true
            val dayOfWeek = when (cal.get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY -> "Monday"
                Calendar.TUESDAY -> "Tuesday"
                Calendar.WEDNESDAY -> "Wednesday"
                Calendar.THURSDAY -> "Thursday"
                Calendar.FRIDAY -> "Friday"
                Calendar.SATURDAY -> "Saturday"
                Calendar.SUNDAY -> "Sunday"
                else -> "Monday"
            }
            val todaySchedule = gym.openingHours.find { it.day.equals(dayOfWeek, ignoreCase = true) }
                ?: return true

            if (todaySchedule.isClosed) return false

            val currentTotalMin = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
            val shifts = todaySchedule.displayShifts
            if (shifts.isEmpty()) return false

            return shifts.any { shift ->
                val openParts = shift.open.split(":")
                val closeParts = shift.close.split(":")
                val openMin = (openParts.getOrNull(0)?.toIntOrNull() ?: 0) * 60 + (openParts.getOrNull(1)?.toIntOrNull() ?: 0)
                val closeMin = (closeParts.getOrNull(0)?.toIntOrNull() ?: 0) * 60 + (closeParts.getOrNull(1)?.toIntOrNull() ?: 0)
                currentTotalMin in openMin..closeMin
            }
        }
    }

    object GymDiffCallback : DiffUtil.ItemCallback<GymItem>() {
        override fun areItemsTheSame(oldItem: GymItem, newItem: GymItem): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: GymItem, newItem: GymItem): Boolean =
            oldItem == newItem
    }
}
