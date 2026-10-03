package com.esha.fitsync.screens.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.esha.fitsync.BuildConfig
import com.esha.fitsync.R
import com.esha.fitsync.models.GymItem
import java.util.Locale

class HomeGymAdapter(
    private var gyms: List<GymItem> = emptyList(),
    private val onGymClick: (GymItem) -> Unit
) : RecyclerView.Adapter<HomeGymAdapter.HomeGymViewHolder>() {

    fun submitList(newGyms: List<GymItem>) {
        gyms = newGyms
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HomeGymViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_home_gym_card, parent, false)
        return HomeGymViewHolder(view, onGymClick)
    }

    override fun onBindViewHolder(holder: HomeGymViewHolder, position: Int) {
        holder.bind(gyms[position])
    }

    override fun getItemCount(): Int = gyms.size

    class HomeGymViewHolder(
        itemView: View,
        private val onGymClick: (GymItem) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val ivPhoto: ImageView = itemView.findViewById(R.id.ivHomeGymPhoto)
        private val tvRating: TextView = itemView.findViewById(R.id.tvHomeGymRating)
        private val tvDistance: TextView = itemView.findViewById(R.id.tvHomeGymDistance)
        private val tvName: TextView = itemView.findViewById(R.id.tvHomeGymName)
        private val tvCity: TextView = itemView.findViewById(R.id.tvHomeGymCity)
        private val tvPrice: TextView = itemView.findViewById(R.id.tvHomeGymPrice)

        fun bind(gym: GymItem) {
            tvName.text = gym.name
            tvCity.text = if (gym.city.isNotEmpty()) "${gym.address}, ${gym.city}" else gym.address
            tvRating.text = String.format(Locale.US, "%.1f", gym.ratingAvg)
            tvDistance.text = String.format(Locale.US, "%.1f km", gym.distanceKm)

            val price = gym.startingPrice
            val planType = gym.startingPlanType?.replace("_", " ")?.replaceFirstChar { it.uppercase() } ?: "day"
            if (price > 0) {
                tvPrice.visibility = View.VISIBLE
                tvPrice.text = "From ₹${price.toInt()}/$planType"
            } else {
                tvPrice.visibility = View.GONE
            }

            val photoUrl = gym.primaryPhoto
            if (!photoUrl.isNullOrEmpty()) {
                val fullUrl = if (photoUrl.startsWith("http")) photoUrl else {
                    val base = BuildConfig.BASE_URL.trimEnd('/')
                    "$base/${photoUrl.trimStart('/')}"
                }
                Glide.with(itemView.context)
                    .load(fullUrl)
                    .transform(CenterCrop(), RoundedCorners(14))
                    .placeholder(R.drawable.gym)
                    .error(R.drawable.gym)
                    .into(ivPhoto)
            } else {
                ivPhoto.setImageResource(R.drawable.gym)
            }

            itemView.setOnClickListener { onGymClick(gym) }
        }
    }
}
