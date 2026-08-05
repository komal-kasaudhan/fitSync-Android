package com.esha.fitsync.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.esha.fitsync.R
import com.esha.fitsync.models.DetailedExercise

class RoutineAdapter(
    private var exerciseList: List<DetailedExercise>,
    private val onItemClick: (DetailedExercise) -> Unit,
    private val onMarkCompleteClick: (DetailedExercise, position: Int) -> Unit
) : RecyclerView.Adapter<RoutineAdapter.RoutineViewHolder>() {

    class RoutineViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvExerciseName: TextView = view.findViewById(R.id.tvExerciseRoutineName)
        val tvExerciseSpecs: TextView = view.findViewById(R.id.tvExerciseRoutineSpecs)
        val imgThumb: ImageView = view.findViewById(R.id.imgExerciseThumb)
        val imgAction: ImageView = view.findViewById(R.id.imgRoutineActionIndicator)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoutineViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_routine_exercise, parent, false)
        return RoutineViewHolder(view)
    }

    override fun onBindViewHolder(holder: RoutineViewHolder, position: Int) {
        val item = exerciseList[position]

        holder.tvExerciseName.text = item.name
        holder.tvExerciseSpecs.text = "${item.targetSetsReps}  •  ${item.restDuration}"

        if (item.isCompleted) {
            holder.imgAction.setImageResource(android.R.drawable.checkbox_on_background)
            holder.itemView.alpha = 0.5f
        } else {
            holder.imgAction.setImageResource(android.R.drawable.checkbox_off_background)
            holder.itemView.alpha = 1.0f
        }

        holder.itemView.setOnClickListener { onItemClick(item) }


        holder.imgAction.setOnClickListener {
            if (!item.isCompleted) {
                onMarkCompleteClick(item, position)
            }
        }
    }

    override fun getItemCount() = exerciseList.size

    fun updateList(newList: List<DetailedExercise>) {
        this.exerciseList = newList
        notifyDataSetChanged()
    }
}