package com.esha.fitsync.screens.gym.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.esha.fitsync.R
import com.esha.fitsync.models.GymBookingDetails

class MyBookingsAdapter(
    private val onCancelClick: (GymBookingDetails) -> Unit,
    private val onReviewClick: (GymBookingDetails) -> Unit
) : ListAdapter<GymBookingDetails, MyBookingsAdapter.BookingViewHolder>(BookingDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_my_booking, parent, false)
        return BookingViewHolder(view, onCancelClick, onReviewClick)
    }

    override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class BookingViewHolder(
        itemView: View,
        private val onCancelClick: (GymBookingDetails) -> Unit,
        private val onReviewClick: (GymBookingDetails) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val tvGymName: TextView = itemView.findViewById(R.id.tvBookingGymName)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvBookingStatus)
        private val tvDate: TextView = itemView.findViewById(R.id.tvBookingDate)
        private val tvSlot: TextView = itemView.findViewById(R.id.tvBookingSlot)
        private val layoutCheckIn: LinearLayout = itemView.findViewById(R.id.layoutCheckInCode)
        private val tvCheckInCode: TextView = itemView.findViewById(R.id.tvCheckInCodeVal)
        private val tvPrice: TextView = itemView.findViewById(R.id.tvPriceVal)
        private val tvRefundNote: TextView = itemView.findViewById(R.id.tvRefundNote)
        private val btnCancel: Button = itemView.findViewById(R.id.btnCancelBooking)
        private val btnReview: Button = itemView.findViewById(R.id.btnWriteReview)

        fun bind(booking: GymBookingDetails) {
            tvGymName.text = booking.gymName.ifEmpty { "Fitness Center" }
            tvDate.text = booking.date
            tvSlot.text = booking.slot
            tvPrice.text = "₹${booking.price.toInt()}"
            tvCheckInCode.text = booking.checkInCode.ifEmpty { "------" }

            when (booking.status.lowercase()) {
                "confirmed" -> {
                    tvStatus.text = "CONFIRMED"
                    tvStatus.setTextColor(ContextCompat.getColor(itemView.context, R.color.gym_green))
                    layoutCheckIn.visibility = View.VISIBLE
                    btnCancel.visibility = View.VISIBLE
                    btnReview.visibility = View.GONE
                    tvRefundNote.visibility = View.GONE
                }
                "attended" -> {
                    tvStatus.text = "ATTENDED"
                    tvStatus.setTextColor(ContextCompat.getColor(itemView.context, R.color.brand_cyan))
                    layoutCheckIn.visibility = View.GONE
                    btnCancel.visibility = View.GONE
                    btnReview.visibility = View.VISIBLE
                    tvRefundNote.visibility = View.VISIBLE
                    tvRefundNote.text = "Session Completed ✓"
                    tvRefundNote.setTextColor(ContextCompat.getColor(itemView.context, R.color.gym_green))
                }
                "cancelled" -> {
                    tvStatus.text = "CANCELLED"
                    tvStatus.setTextColor(ContextCompat.getColor(itemView.context, R.color.status_rejected))
                    layoutCheckIn.visibility = View.GONE
                    btnCancel.visibility = View.GONE
                    btnReview.visibility = View.GONE
                    tvRefundNote.visibility = View.VISIBLE
                    val refundAmt = booking.refundAmount ?: booking.price
                    tvRefundNote.text = "Cancelled • Refund: ₹${refundAmt.toInt()}"
                    tvRefundNote.setTextColor(ContextCompat.getColor(itemView.context, R.color.status_rejected))
                }
                else -> {
                    tvStatus.text = booking.status.uppercase()
                    tvStatus.setTextColor(ContextCompat.getColor(itemView.context, R.color.status_pending))
                    layoutCheckIn.visibility = View.VISIBLE
                    btnCancel.visibility = View.VISIBLE
                    btnReview.visibility = View.GONE
                    tvRefundNote.visibility = View.GONE
                }
            }

            btnCancel.setOnClickListener { onCancelClick(booking) }
            btnReview.setOnClickListener { onReviewClick(booking) }
        }
    }

    object BookingDiffCallback : DiffUtil.ItemCallback<GymBookingDetails>() {
        override fun areItemsTheSame(oldItem: GymBookingDetails, newItem: GymBookingDetails): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: GymBookingDetails, newItem: GymBookingDetails): Boolean =
            oldItem == newItem
    }
}
