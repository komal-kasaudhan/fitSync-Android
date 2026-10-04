package com.esha.fitsync.screens.gym.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.esha.fitsync.R
import com.esha.fitsync.models.GymMembership
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class MyMembershipsAdapter(
    private val onFreezeClick: (GymMembership) -> Unit,
    private val onUnfreezeClick: (GymMembership) -> Unit,
    private val onCancelClick: (GymMembership) -> Unit
) : RecyclerView.Adapter<MyMembershipsAdapter.MembershipViewHolder>() {

    private var items: List<GymMembership> = emptyList()

    fun submitList(newItems: List<GymMembership>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MembershipViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_my_membership, parent, false)
        return MembershipViewHolder(view)
    }

    override fun onBindViewHolder(holder: MembershipViewHolder, position: Int) {
        holder.bind(items[position], onFreezeClick, onUnfreezeClick, onCancelClick)
    }

    override fun getItemCount(): Int = items.size

    class MembershipViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvGymName: TextView = itemView.findViewById(R.id.tvMembershipGymName)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvMembershipStatus)
        private val tvPlan: TextView = itemView.findViewById(R.id.tvMembershipPlan)
        private val tvMemberCode: TextView = itemView.findViewById(R.id.tvMemberCode)
        private val tvDaysLeft: TextView = itemView.findViewById(R.id.tvDaysLeftBadge)
        private val tvDates: TextView = itemView.findViewById(R.id.tvMembershipDates)
        private val tvPrice: TextView = itemView.findViewById(R.id.tvMembershipPrice)
        private val tvFrozenNotice: TextView = itemView.findViewById(R.id.tvFrozenNotice)
        private val layoutActions: View = itemView.findViewById(R.id.layoutMembershipActions)
        private val btnFreeze: MaterialButton = itemView.findViewById(R.id.btnFreezeMembership)
        private val btnUnfreeze: MaterialButton = itemView.findViewById(R.id.btnUnfreezeMembership)
        private val btnCancel: MaterialButton = itemView.findViewById(R.id.btnCancelMembership)

        fun bind(
            membership: GymMembership,
            onFreeze: (GymMembership) -> Unit,
            onUnfreeze: (GymMembership) -> Unit,
            onCancel: (GymMembership) -> Unit
        ) {
            val context = itemView.context
            tvGymName.text = membership.displayGymName
            tvPlan.text = "${membership.planName} • ${membership.durationDays} days"
            tvMemberCode.text = membership.memberCode.ifEmpty { "------" }
            tvPrice.text = "₹${membership.price.toInt()}"

            val startFormatted = formatDate(membership.startDate)
            val endFormatted = formatDate(membership.endDate)
            tvDates.text = "$startFormatted — $endFormatted"

            val statusStr = membership.status.lowercase()
            when (statusStr) {
                "active" -> {
                    if (membership.isFrozen) {
                        tvStatus.text = "FROZEN"
                        tvStatus.setTextColor(ContextCompat.getColor(context, R.color.gym_accent))
                        tvFrozenNotice.visibility = View.VISIBLE
                        btnFreeze.visibility = View.GONE
                        btnUnfreeze.visibility = View.VISIBLE
                        btnCancel.visibility = View.GONE
                        layoutActions.visibility = View.VISIBLE
                    } else {
                        tvStatus.text = "ACTIVE"
                        tvStatus.setTextColor(ContextCompat.getColor(context, R.color.status_approved))
                        tvFrozenNotice.visibility = View.GONE
                        btnUnfreeze.visibility = View.GONE

                        if (membership.maxFreezeDays > 0) {
                            btnFreeze.visibility = View.VISIBLE
                        } else {
                            btnFreeze.visibility = View.GONE
                        }
                        btnCancel.visibility = View.VISIBLE
                        layoutActions.visibility = View.VISIBLE
                    }
                }
                "upcoming" -> {
                    tvStatus.text = "UPCOMING"
                    tvStatus.setTextColor(ContextCompat.getColor(context, R.color.status_pending))
                    tvFrozenNotice.visibility = View.GONE
                    btnFreeze.visibility = View.GONE
                    btnUnfreeze.visibility = View.GONE
                    btnCancel.visibility = View.VISIBLE
                    layoutActions.visibility = View.VISIBLE
                }
                "cancelled" -> {
                    tvStatus.text = "CANCELLED"
                    tvStatus.setTextColor(ContextCompat.getColor(context, R.color.status_rejected))
                    tvFrozenNotice.visibility = View.GONE
                    layoutActions.visibility = View.GONE
                }
                else -> {
                    tvStatus.text = "EXPIRED"
                    tvStatus.setTextColor(ContextCompat.getColor(context, R.color.text_muted))
                    tvFrozenNotice.visibility = View.GONE
                    layoutActions.visibility = View.GONE
                }
            }

            // Calculate days left
            val daysLeft = calculateDaysRemaining(membership.endDate)
            when {
                statusStr == "cancelled" -> {
                    tvDaysLeft.text = "Cancelled"
                    tvDaysLeft.setTextColor(ContextCompat.getColor(context, R.color.status_rejected))
                }
                statusStr == "upcoming" -> {
                    val daysUntilStart = calculateDaysRemaining(membership.startDate)
                    tvDaysLeft.text = "Starts in $daysUntilStart d"
                    tvDaysLeft.setTextColor(ContextCompat.getColor(context, R.color.status_pending))
                }
                daysLeft > 0 -> {
                    tvDaysLeft.text = "$daysLeft days left"
                    tvDaysLeft.setTextColor(ContextCompat.getColor(context, R.color.gym_green))
                }
                else -> {
                    tvDaysLeft.text = "Expired"
                    tvDaysLeft.setTextColor(ContextCompat.getColor(context, R.color.text_muted))
                }
            }

            btnFreeze.setOnClickListener { onFreeze(membership) }
            btnUnfreeze.setOnClickListener { onUnfreeze(membership) }
            btnCancel.setOnClickListener { onCancel(membership) }
        }

        private fun formatDate(isoDate: String): String {
            if (isoDate.isEmpty()) return "N/A"
            return try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val date = if (isoDate.contains("T")) {
                    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).parse(isoDate.substringBefore("."))
                } else {
                    inputFormat.parse(isoDate)
                }
                if (date != null) {
                    SimpleDateFormat("dd MMM yyyy", Locale.US).format(date)
                } else isoDate.take(10)
            } catch (_: Exception) {
                isoDate.take(10)
            }
        }

        private fun calculateDaysRemaining(isoDate: String): Long {
            if (isoDate.isEmpty()) return 0
            return try {
                val date = if (isoDate.contains("T")) {
                    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).parse(isoDate.substringBefore("."))
                } else {
                    SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(isoDate)
                }
                if (date != null) {
                    val diff = date.time - Date().time
                    if (diff > 0) TimeUnit.MILLISECONDS.toDays(diff) + 1 else 0
                } else 0
            } catch (_: Exception) {
                0
            }
        }
    }
}
