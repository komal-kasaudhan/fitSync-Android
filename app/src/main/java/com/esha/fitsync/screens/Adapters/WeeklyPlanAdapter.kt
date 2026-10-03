package com.esha.fitsync.screens.Adapters

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.esha.fitsync.R
import com.esha.fitsync.models.EngineRoutine
import com.google.android.material.card.MaterialCardView

class WeeklyPlanAdapter(
    private var routines: List<EngineRoutine>,
    private var todayIndex: Int,
    private val onDayClick: (EngineRoutine) -> Unit
) : RecyclerView.Adapter<WeeklyPlanAdapter.DayViewHolder>() {

    fun submitList(newRoutines: List<EngineRoutine>, newTodayIndex: Int = todayIndex) {
        this.routines = newRoutines
        this.todayIndex = newTodayIndex
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_weekly_plan_day, parent, false)
        return DayViewHolder(view)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        val routine = routines[position]
        val isToday = routine.dayIndex == todayIndex

        holder.tvDayLabel.text = routine.displayDayName.take(3).uppercase()
        holder.tvDayFocus.text = routine.displayFocus

        if (routine.isRestDay) {
            holder.ivDayIcon.setImageResource(android.R.drawable.ic_lock_idle_charging)
            holder.ivDayIcon.imageTintList = ColorStateList.valueOf(Color.parseColor("#9CA3AF"))
            holder.tvDayMeta.text = "Rest Day"
        } else {
            holder.ivDayIcon.setImageResource(android.R.drawable.ic_menu_agenda)
            holder.ivDayIcon.imageTintList = ColorStateList.valueOf(if (isToday) Color.parseColor("#071018") else Color.parseColor("#00D4FF"))
            val exCount = routine.exerciseCount
            val exText = if (exCount == 1) "1 exercise" else "$exCount exercises"
            holder.tvDayMeta.text = "${routine.duration}m • $exText"
        }

        if (routine.completed) {
            holder.ivCompletedBadge.visibility = View.VISIBLE
        } else {
            holder.ivCompletedBadge.visibility = View.GONE
        }

        // Highlight today's card
        if (isToday) {
            holder.card.setCardBackgroundColor(Color.WHITE)
            holder.card.strokeColor = Color.parseColor("#00D4FF")
            holder.card.strokeWidth = 4
            holder.tvDayLabel.setTextColor(Color.parseColor("#071018"))
            holder.tvDayFocus.setTextColor(Color.parseColor("#071018"))
            holder.tvDayMeta.setTextColor(Color.parseColor("#4B5563"))
        } else {
            holder.card.setCardBackgroundColor(Color.parseColor("#1C2638"))
            holder.card.strokeColor = Color.parseColor("#1F2E3D")
            holder.card.strokeWidth = 1
            holder.tvDayLabel.setTextColor(Color.parseColor("#9CA3AF"))
            holder.tvDayFocus.setTextColor(Color.WHITE)
            holder.tvDayMeta.setTextColor(Color.parseColor("#6B7280"))
        }

        holder.card.setOnClickListener {
            onDayClick(routine)
        }
    }

    override fun getItemCount(): Int = routines.size

    class DayViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val card: MaterialCardView = itemView.findViewById(R.id.cardWeeklyDay)
        val tvDayLabel: TextView = itemView.findViewById(R.id.tvDayLabel)
        val ivDayIcon: ImageView = itemView.findViewById(R.id.ivDayIcon)
        val tvDayFocus: TextView = itemView.findViewById(R.id.tvDayFocus)
        val tvDayMeta: TextView = itemView.findViewById(R.id.tvDayMeta)
        val ivCompletedBadge: ImageView = itemView.findViewById(R.id.ivDayCompletedBadge)
    }
}

