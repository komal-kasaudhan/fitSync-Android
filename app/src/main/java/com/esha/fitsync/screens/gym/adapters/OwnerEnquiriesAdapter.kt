package com.esha.fitsync.screens.gym.adapters

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.esha.fitsync.R
import com.esha.fitsync.models.GymEnquiryItem

class OwnerEnquiriesAdapter : RecyclerView.Adapter<OwnerEnquiriesAdapter.EnquiryViewHolder>() {

    private var items: List<GymEnquiryItem> = emptyList()

    fun submitList(newItems: List<GymEnquiryItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EnquiryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_owner_enquiry, parent, false)
        return EnquiryViewHolder(view)
    }

    override fun onBindViewHolder(holder: EnquiryViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class EnquiryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tvEnquiryName)
        private val tvContact: TextView = itemView.findViewById(R.id.tvEnquiryContact)
        private val tvPrefTime: TextView = itemView.findViewById(R.id.tvEnquiryPrefTime)
        private val tvTrialBadge: TextView = itemView.findViewById(R.id.tvEnquiryTrialBadge)
        private val tvMessage: TextView = itemView.findViewById(R.id.tvEnquiryMessage)
        private val btnCall: Button = itemView.findViewById(R.id.btnCallEnquiry)

        fun bind(enquiry: GymEnquiryItem) {
            tvName.text = enquiry.name
            val emailText = if (!enquiry.email.isNullOrEmpty()) " • ${enquiry.email}" else ""
            tvContact.text = "${enquiry.phone}$emailText"

            val pref = enquiry.preferredTime ?: "morning"
            tvPrefTime.text = "PREF: ${pref.uppercase()}"

            if (enquiry.wantsTrial) {
                tvTrialBadge.visibility = View.VISIBLE
            } else {
                tvTrialBadge.visibility = View.GONE
            }

            if (enquiry.message.isNotEmpty()) {
                tvMessage.visibility = View.VISIBLE
                tvMessage.text = enquiry.message
            } else {
                tvMessage.visibility = View.GONE
            }

            btnCall.setOnClickListener {
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${enquiry.phone}"))
                itemView.context.startActivity(intent)
            }
        }
    }
}
