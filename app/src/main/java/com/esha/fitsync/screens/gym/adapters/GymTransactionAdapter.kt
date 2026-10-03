package com.esha.fitsync.screens.gym.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.esha.fitsync.R
import com.esha.fitsync.models.GymTransactionItem

class GymTransactionAdapter(
    private var transactions: List<GymTransactionItem> = emptyList()
) : RecyclerView.Adapter<GymTransactionAdapter.TransactionViewHolder>() {

    fun submitList(list: List<GymTransactionItem>) {
        transactions = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_gym_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        holder.bind(transactions[position])
    }

    override fun getItemCount(): Int = transactions.size

    class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tvTxMemberName)
        private val tvOrderId: TextView = itemView.findViewById(R.id.tvTxOrderId)
        private val tvDate: TextView = itemView.findViewById(R.id.tvTxDate)
        private val tvPartnerShare: TextView = itemView.findViewById(R.id.tvTxPartnerShare)
        private val tvFee: TextView = itemView.findViewById(R.id.tvTxTotalFee)

        fun bind(item: GymTransactionItem) {
            tvName.text = item.user?.name ?: "Member"
            tvOrderId.text = if (item.orderId.length > 18) item.orderId.substring(0, 18) + "..." else item.orderId
            val dateStr = if (item.createdAt.length >= 10) item.createdAt.substring(0, 10) else item.createdAt
            tvDate.text = dateStr
            tvPartnerShare.text = "+₹${item.partnerAmount.toInt()}"
            tvFee.text = "Fee: ₹${item.platformFee.toInt()}"
        }
    }
}
