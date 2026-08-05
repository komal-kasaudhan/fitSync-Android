package com.esha.fitsync.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.esha.fitsync.R

class StringWheelAdapter(
    private val stringList: List<String>,
    private val onItemSelected: (String) -> Unit
) : RecyclerView.Adapter<StringWheelAdapter.StringViewHolder>() {

    private var selectedItem: String = if (stringList.isNotEmpty()) stringList[0] else ""

    class StringViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvValue: TextView = view.findViewById(R.id.tvTimelineValue)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StringViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_timeline_wheel, parent, false)
        return StringViewHolder(view)
    }

    override fun onBindViewHolder(holder: StringViewHolder, position: Int) {
        val currentText = stringList[position]
        holder.tvValue.text = currentText

        if (currentText == selectedItem) {
            holder.tvValue.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.brand_cyan))
        } else {
            holder.tvValue.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.text_muted))
        }
    }

    override fun getItemCount(): Int = stringList.size

    fun setSelectedItem(item: String) {
        if (selectedItem != item) {
            val oldIdx = stringList.indexOf(selectedItem)
            val newIdx = stringList.indexOf(item)
            selectedItem = item
            if (oldIdx != -1) notifyItemChanged(oldIdx)
            if (newIdx != -1) notifyItemChanged(newIdx)
            onItemSelected(selectedItem)
        }
    }
}