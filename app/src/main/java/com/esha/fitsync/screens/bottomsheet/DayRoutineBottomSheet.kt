package com.esha.fitsync.screens.bottomsheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.esha.fitsync.R
import com.esha.fitsync.models.EngineExercise
import com.esha.fitsync.models.EngineRoutine
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton

class DayRoutineBottomSheet(
    private val routine: EngineRoutine
) : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_day_routine, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<TextView>(R.id.tvRoutineDayTitle).text =
            "${routine.dayName}: ${if (routine.isRestDay) "Rest & Recovery" else routine.focus}"

        val coachNoteTv = view.findViewById<TextView>(R.id.tvRoutineCoachNote)
        if (!routine.coachNote.isNullOrEmpty()) {
            coachNoteTv.text = "\"${routine.coachNote}\""
            coachNoteTv.visibility = View.VISIBLE
        } else {
            coachNoteTv.visibility = View.GONE
        }

        val rvExercises = view.findViewById<RecyclerView>(R.id.rvDayExercises)
        rvExercises.layoutManager = LinearLayoutManager(requireContext())
        rvExercises.adapter = RoutineExercisePreviewAdapter(routine.exercises)

        view.findViewById<MaterialButton>(R.id.btnCloseRoutine).setOnClickListener {
            dismiss()
        }
    }

    private class RoutineExercisePreviewAdapter(
        private val exercises: List<EngineExercise>
    ) : RecyclerView.Adapter<RoutineExercisePreviewAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_routine_exercise_preview, parent, false)
            return ViewHolder(v)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val ex = exercises[position]
            holder.tvName.text = ex.name
            holder.tvScheme.text = "${ex.sets} sets • ${ex.reps} reps • ${ex.restSec}s rest"

            if (!ex.imageUrl.isNullOrEmpty()) {
                holder.ivThumb.load(ex.imageUrl) {
                    crossfade(true)
                    placeholder(R.drawable.girl_dumbell)
                    error(R.drawable.girl_dumbell)
                }
            } else {
                holder.ivThumb.setImageResource(R.drawable.girl_dumbell)
            }

            holder.ivCheck.visibility = if (ex.completed) View.VISIBLE else View.GONE
        }

        override fun getItemCount(): Int = exercises.size

        class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvName: TextView = itemView.findViewById(R.id.tvExerciseName)
            val tvScheme: TextView = itemView.findViewById(R.id.tvExerciseScheme)
            val ivThumb: ImageView = itemView.findViewById(R.id.ivExerciseThumb)
            val ivCheck: ImageView = itemView.findViewById(R.id.ivCheckStatus)
        }
    }
}
