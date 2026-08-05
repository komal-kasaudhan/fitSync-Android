package com.esha.fitsync.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.esha.fitsync.R

class DecimalWheelAdapter(
    private val weightList: List<Float>,
    private val onWeightSelected: (Float) -> Unit
) : RecyclerView.Adapter<DecimalWheelAdapter.DecimalViewHolder>() {

    private var selectedWeight: Float = if (weightList.isNotEmpty()) weightList[0] else -1f

    class DecimalViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvValue: TextView = view.findViewById(R.id.tvDecimalValue) // Layout item_decimal_wheel ki TextView ID
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DecimalViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_decimalwheel, parent, false)
        return DecimalViewHolder(view)
    }

    override fun onBindViewHolder(holder: DecimalViewHolder, position: Int) {
        val currentValue = weightList[position]


        holder.tvValue.text = String.format("%.1f", currentValue)


        if (currentValue == selectedWeight) {
            holder.tvValue.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.brand_cyan))
        } else {
            holder.tvValue.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.text_muted))
        }
    }

    override fun getItemCount(): Int = weightList.size

    fun setSelectedWeight(weight: Float) {
        if (selectedWeight != weight) {
            val oldIdx = weightList.indexOf(selectedWeight)
            val newIdx = weightList.indexOf(weight)
            selectedWeight = weight

            if (oldIdx != -1) notifyItemChanged(oldIdx)
            if (newIdx != -1) notifyItemChanged(newIdx)

            onWeightSelected(selectedWeight)
        }
    }
}