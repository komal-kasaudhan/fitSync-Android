package com.esha.fitsync.screens.gym.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.esha.fitsync.R
import com.esha.fitsync.models.GymMemberInfo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class OwnerMembersAdapter : RecyclerView.Adapter<OwnerMembersAdapter.MemberViewHolder>() {

    private var items: List<GymMemberInfo> = emptyList()

    fun submitList(newItems: List<GymMemberInfo>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_owner_member, parent, false)
        return MemberViewHolder(view)
    }

    override fun onBindViewHolder(holder: MemberViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class MemberViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tvOwnerMemberName)
        private val tvEmail: TextView = itemView.findViewById(R.id.tvOwnerMemberEmail)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvOwnerMemberStatus)
        private val tvPlan: TextView = itemView.findViewById(R.id.tvOwnerMemberPlan)
        private val tvCode: TextView = itemView.findViewById(R.id.tvOwnerMemberCode)
        private val tvDates: TextView = itemView.findViewById(R.id.tvOwnerMemberDates)
        private val tvDaysLeft: TextView = itemView.findViewById(R.id.tvOwnerMemberDaysLeft)

        fun bind(member: GymMemberInfo) {
            val context = itemView.context
            tvName.text = member.displayName
            tvEmail.text = member.displayEmail.ifEmpty { "No email registered" }

            val planTypeClean = member.planType.replace('_', ' ')
            tvPlan.text = "${member.planName} • $planTypeClean"
            tvCode.text = "CODE: ${member.memberCode.ifEmpty { "------" }}"

            val startFormatted = formatDate(member.startDate)
            val endFormatted = formatDate(member.endDate)
            tvDates.text = "Valid: $startFormatted to $endFormatted"

            val daysRemaining = calculateDaysRemaining(member.endDate)
            if (member.isFrozen) {
                tvStatus.text = "FROZEN"
                tvStatus.setTextColor(ContextCompat.getColor(context, R.color.gym_accent))
                tvDaysLeft.text = "Membership on pause"
                tvDaysLeft.setTextColor(ContextCompat.getColor(context, R.color.gym_accent))
            } else if (member.status.equals("active", ignoreCase = true)) {
                if (daysRemaining <= 0) {
                    tvStatus.text = "EXPIRED"
                    tvStatus.setTextColor(ContextCompat.getColor(context, R.color.status_rejected))
                    tvDaysLeft.text = "Expired"
                    tvDaysLeft.setTextColor(ContextCompat.getColor(context, R.color.status_rejected))
                } else if (daysRemaining <= 7) {
                    tvStatus.text = "EXPIRING SOON"
                    tvStatus.setTextColor(ContextCompat.getColor(context, R.color.gym_accent))
                    tvDaysLeft.text = "⚠️ $daysRemaining days left"
                    tvDaysLeft.setTextColor(ContextCompat.getColor(context, R.color.gym_accent))
                } else {
                    tvStatus.text = "ACTIVE"
                    tvStatus.setTextColor(ContextCompat.getColor(context, R.color.status_approved))
                    tvDaysLeft.text = "$daysRemaining days left"
                    tvDaysLeft.setTextColor(ContextCompat.getColor(context, R.color.status_approved))
                }
            } else {
                tvStatus.text = member.status.uppercase(Locale.US)
                tvStatus.setTextColor(ContextCompat.getColor(context, R.color.text_gray_blue))
                tvDaysLeft.text = member.status.replaceFirstChar(Char::uppercase)
                tvDaysLeft.setTextColor(ContextCompat.getColor(context, R.color.text_gray_blue))
            }
        }

        private fun formatDate(raw: String): String {
            if (raw.isEmpty()) return ""
            return try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
                val date = inputFormat.parse(raw)
                val outputFormat = SimpleDateFormat("dd MMM yyyy", Locale.US)
                date?.let { outputFormat.format(it) } ?: raw.substringBefore("T")
            } catch (_: Exception) {
                raw.substringBefore("T")
            }
        }

        private fun calculateDaysRemaining(endStr: String): Long {
            if (endStr.isEmpty()) return 0
            return try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
                val endDate = inputFormat.parse(endStr) ?: SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(endStr.substringBefore("T"))
                if (endDate != null) {
                    val diffMs = endDate.time - Date().time
                    TimeUnit.MILLISECONDS.toDays(diffMs).coerceAtLeast(0)
                } else 0
            } catch (_: Exception) {
                0
            }
        }
    }
}
