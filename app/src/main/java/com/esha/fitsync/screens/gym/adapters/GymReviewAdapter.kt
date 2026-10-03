package com.esha.fitsync.screens.gym.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.esha.fitsync.R
import com.esha.fitsync.models.ReviewItem
import java.util.Locale

class GymReviewAdapter(
    private var reviews: List<ReviewItem> = emptyList()
) : RecyclerView.Adapter<GymReviewAdapter.ReviewViewHolder>() {

    fun submitList(newList: List<ReviewItem>) {
        reviews = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_gym_review, parent, false)
        return ReviewViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        holder.bind(reviews[position])
    }

    override fun getItemCount(): Int = reviews.size

    class ReviewViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tvReviewerName)
        private val tvRating: TextView = itemView.findViewById(R.id.tvReviewRating)
        private val tvDate: TextView = itemView.findViewById(R.id.tvReviewDate)
        private val tvComment: TextView = itemView.findViewById(R.id.tvReviewComment)

        fun bind(review: ReviewItem) {
            tvName.text = review.userName ?: "Verified Member"
            tvRating.text = String.format(Locale.US, "%d.0", review.rating)
            val dateStr = if (review.createdAt.length >= 10) review.createdAt.substring(0, 10) else review.createdAt
            tvDate.text = if (dateStr.isNotEmpty()) "Attended Session • $dateStr" else "Verified Attendance"
            tvComment.text = review.comment
        }
    }
}
