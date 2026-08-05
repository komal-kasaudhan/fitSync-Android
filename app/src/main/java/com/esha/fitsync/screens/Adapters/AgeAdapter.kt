package com.esha.fitsync.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.esha.fitsync.R

class AgeAdapter(
    private val ageList: List<Int>,
    private val onAgeSelected: (Int) -> Unit
) : RecyclerView.Adapter<AgeAdapter.AgeViewHolder>() {

    private var selectedAge: Int = if (ageList.isNotEmpty()) ageList[0] else -1

    class AgeViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvAge: TextView = view.findViewById(R.id.tvAge)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AgeViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_age, parent, false)
        return AgeViewHolder(view)
    }

    override fun onBindViewHolder(holder: AgeViewHolder, position: Int) {
        val ageValue = ageList[position]
        holder.tvAge.text = ageValue.toString()

        if (ageValue == selectedAge) {
            holder.tvAge.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.brand_cyan))
        } else {
            holder.tvAge.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.text_muted))
        }
    }

    override fun getItemCount(): Int = ageList.size

    fun setSelectedAge(age: Int) {
        if (selectedAge != age) {
            val oldIdx = ageList.indexOf(selectedAge)
            val newIdx = ageList.indexOf(age)
            selectedAge = age
            if (oldIdx != -1) notifyItemChanged(oldIdx)
            if (newIdx != -1) notifyItemChanged(newIdx)
            onAgeSelected(selectedAge)
        }
    }
}