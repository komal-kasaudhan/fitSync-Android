package com.esha.fitsync.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.esha.fitsync.R

class RulerAdapter(
    private val values: List<Int>,
    private val onValueSelected: (Int) -> Unit
) : RecyclerView.Adapter<RulerAdapter.RulerViewHolder>() {

    private var selectedValue: Int = if (values.isNotEmpty()) values[0] else -1

    class RulerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvValue: TextView = itemView.findViewById(R.id.tvValue)
        val vRulerLine: View = itemView.findViewById(R.id.vRulerLine)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RulerViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_ruler, parent, false)
        return RulerViewHolder(view)
    }

    override fun onBindViewHolder(holder: RulerViewHolder, position: Int) {
        val currentValue = values[position]
        holder.tvValue.text = currentValue.toString()

        val layoutParams = holder.vRulerLine.layoutParams as LinearLayout.LayoutParams


        if (currentValue % 5 == 0) {
            layoutParams.height = 62
            holder.tvValue.visibility = View.VISIBLE
        } else {
            layoutParams.height = 30
            holder.tvValue.visibility = View.INVISIBLE
        }
        holder.vRulerLine.layoutParams = layoutParams

        if (currentValue == selectedValue) {
            holder.vRulerLine.setBackgroundColor(ContextCompat.getColor(holder.itemView.context, android.R.color.white))
            holder.tvValue.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.brand_cyan))
        } else {
            holder.vRulerLine.setBackgroundColor(android.graphics.Color.parseColor("#3300D4FF"))
            holder.tvValue.setTextColor(android.graphics.Color.parseColor("#8A99AD"))
        }
    }

    override fun getItemCount(): Int = values.size


    fun setSelectedValue(value: Int) {
        if (selectedValue != value) {
            val oldIndex = values.indexOf(selectedValue)
            val newIndex = values.indexOf(value)

            selectedValue = value

            if (oldIndex != -1) notifyItemChanged(oldIndex)
            if (newIndex != -1) notifyItemChanged(newIndex)

            onValueSelected(selectedValue)
        }
    }
}