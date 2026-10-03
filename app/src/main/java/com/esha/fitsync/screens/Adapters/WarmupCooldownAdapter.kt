package com.esha.fitsync.screens.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.esha.fitsync.R
import com.esha.fitsync.models.EngineExercise

class WarmupCooldownAdapter(
    private var items: List<EngineExercise>
) : RecyclerView.Adapter<WarmupCooldownAdapter.ViewHolder>() {

    fun updateList(newItems: List<EngineExercise>) {
        this.items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_warmup_cooldown, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvItemName.text = item.name

        val equipStr = if (item.equipment.isNotEmpty()) item.equipment.joinToString(", ") else "Bodyweight"
        val muscleStr = item.targetMuscles ?: item.primaryTarget ?: "Mobility"
        holder.tvItemSpecs.text = "$equipStr • $muscleStr"
        holder.tvItemReps.text = item.reps.ifEmpty { "30s" }
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvItemName: TextView = itemView.findViewById(R.id.tvItemName)
        val tvItemSpecs: TextView = itemView.findViewById(R.id.tvItemSpecs)
        val tvItemReps: TextView = itemView.findViewById(R.id.tvItemReps)
    }
}
