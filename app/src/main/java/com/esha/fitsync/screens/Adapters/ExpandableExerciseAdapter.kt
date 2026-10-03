package com.esha.fitsync.screens.Adapters

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.esha.fitsync.R
import com.esha.fitsync.models.EngineExercise
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.imageview.ShapeableImageView

class ExpandableExerciseAdapter(
    private var exercises: List<EngineExercise>,
    private val onExerciseCompleteChanged: (EngineExercise, Boolean) -> Unit
) : RecyclerView.Adapter<ExpandableExerciseAdapter.ExerciseViewHolder>() {

    private val expandedStates = mutableMapOf<Int, Boolean>()
    private val activeTimers = mutableMapOf<Int, CountDownTimer>()

    fun updateList(newExercises: List<EngineExercise>) {
        this.exercises = newExercises
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExerciseViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_workout_exercise_expandable, parent, false)
        return ExerciseViewHolder(view)
    }

    override fun onBindViewHolder(holder: ExerciseViewHolder, position: Int) {
        val exercise = exercises[position]

        // 1. Basic Metadata
        holder.tvExerciseName.text = exercise.name
        holder.tvSetsReps.text = "${exercise.sets} sets × ${exercise.reps} • ${exercise.restDuration}"
        holder.tvTargetMuscles.text = exercise.targetMuscles ?: exercise.primaryTarget ?: "Core & Stabilization"

        // 2. Glide Image Loading with Fallback Placeholder
        if (!exercise.imageUrl.isNullOrBlank()) {
            Glide.with(holder.itemView.context)
                .load(exercise.imageUrl)
                .placeholder(R.drawable.girl_dumbell)
                .error(R.drawable.girl_dumbell)
                .into(holder.ivExerciseThumb)
        } else {
            holder.ivExerciseThumb.setImageResource(R.drawable.girl_dumbell)
        }

        // 3. Set Counter & Completion
        holder.btnSetCounter.text = "Set ${exercise.currentCompletedSets} / ${exercise.sets}"
        holder.cbExerciseCompleted.setOnCheckedChangeListener(null)
        holder.cbExerciseCompleted.isChecked = exercise.completed

        holder.btnSetCounter.setOnClickListener {
            val next = (exercise.currentCompletedSets + 1)
            if (next <= exercise.sets) {
                exercise.currentCompletedSets = next
            } else {
                exercise.currentCompletedSets = 0
            }
            exercise.completed = (exercise.currentCompletedSets == exercise.sets)
            holder.btnSetCounter.text = "Set ${exercise.currentCompletedSets} / ${exercise.sets}"
            holder.cbExerciseCompleted.isChecked = exercise.completed
            onExerciseCompleteChanged(exercise, exercise.completed)
        }

        holder.cbExerciseCompleted.setOnCheckedChangeListener { _, isChecked ->
            exercise.completed = isChecked
            if (isChecked) {
                exercise.currentCompletedSets = exercise.sets
            }
            holder.btnSetCounter.text = "Set ${exercise.currentCompletedSets} / ${exercise.sets}"
            onExerciseCompleteChanged(exercise, isChecked)
        }

        // 4. Rest Timer logic
        holder.btnRestTimer.setOnClickListener {
            val restSec = if (exercise.restSec > 0) exercise.restSec else 60
            startRestTimer(holder, position, restSec)
        }

        holder.btnCancelTimer.setOnClickListener {
            cancelRestTimer(position)
            holder.llTimerBanner.visibility = View.GONE
        }

        // 5. Expand / Collapse Handling
        val isExpanded = expandedStates[position] == true
        holder.llExpandableBody.visibility = if (isExpanded) View.VISIBLE else View.GONE
        holder.ivExpandChevron.rotation = if (isExpanded) 180f else 0f

        holder.llExerciseHeader.setOnClickListener {
            val willExpand = !(expandedStates[position] == true)
            expandedStates[position] = willExpand
            holder.llExpandableBody.visibility = if (willExpand) View.VISIBLE else View.GONE
            holder.ivExpandChevron.animate().rotation(if (willExpand) 180f else 0f).setDuration(200).start()
        }

        // 6. Required Equipment Chips
        holder.cgExerciseEquipment.removeAllViews()
        val equipList = if (exercise.equipment.isNotEmpty()) exercise.equipment else listOf("bodyweight")
        for (eq in equipList) {
            val chip = Chip(holder.itemView.context).apply {
                text = eq.replace("_", " ").capitalizeWords()
                setTextColor(Color.WHITE)
                chipBackgroundColor = ColorStateList.valueOf(Color.parseColor("#162536"))
                chipStrokeWidth = 0f
                textSize = 10f
            }
            holder.cgExerciseEquipment.addView(chip)
        }

        // 7. Instructions / Steps
        val instructionsText = if (exercise.instructions.isNotEmpty()) {
            exercise.instructions.mapIndexed { idx, step -> "${idx + 1}. $step" }.joinToString("\n")
        } else {
            "Perform exercise with steady control, proper spine alignment, and full range of motion."
        }
        holder.tvInstructions.text = instructionsText

        // 8. AI Coach Tips
        val tipsText = if (exercise.coachTips.isNotEmpty()) {
            exercise.coachTips.joinToString("\n• ", prefix = "• ")
        } else {
            "Control the eccentric phase for 2-3 seconds to maximize muscular hypertrophy."
        }
        holder.tvCoachTips.text = tipsText

        // 9. Common Mistakes
        val mistakesText = if (exercise.commonMistakes.isNotEmpty()) {
            exercise.commonMistakes.joinToString("\n• ", prefix = "• ")
        } else {
            "Avoid momentum or arching lower back. Keep core engaged throughout."
        }
        holder.tvCommonMistakes.text = mistakesText
    }

    private fun startRestTimer(holder: ExerciseViewHolder, position: Int, durationSec: Int) {
        cancelRestTimer(position)
        holder.llTimerBanner.visibility = View.VISIBLE

        val timer = object : CountDownTimer((durationSec * 1000).toLong(), 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secLeft = millisUntilFinished / 1000
                holder.tvTimerCountdown.text = "⏱ Rest Timer: ${secLeft}s remaining"
            }

            override fun onFinish() {
                holder.tvTimerCountdown.text = "Rest Complete! Ready for next set 💪"
                holder.llTimerBanner.postDelayed({
                    holder.llTimerBanner.visibility = View.GONE
                }, 2500)
            }
        }.start()

        activeTimers[position] = timer
    }

    private fun cancelRestTimer(position: Int) {
        activeTimers[position]?.cancel()
        activeTimers.remove(position)
    }

    override fun getItemCount(): Int = exercises.size

    class ExerciseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardExercise: MaterialCardView = itemView.findViewById(R.id.cardExercise)
        val llExerciseHeader: LinearLayout = itemView.findViewById(R.id.llExerciseHeader)
        val ivExerciseThumb: ShapeableImageView = itemView.findViewById(R.id.ivExerciseThumb)
        val tvExerciseName: TextView = itemView.findViewById(R.id.tvExerciseName)
        val tvSetsReps: TextView = itemView.findViewById(R.id.tvSetsReps)
        val tvTargetMuscles: TextView = itemView.findViewById(R.id.tvTargetMuscles)
        val ivExpandChevron: ImageView = itemView.findViewById(R.id.ivExpandChevron)

        val btnSetCounter: MaterialButton = itemView.findViewById(R.id.btnSetCounter)
        val btnRestTimer: MaterialButton = itemView.findViewById(R.id.btnRestTimer)
        val cbExerciseCompleted: CheckBox = itemView.findViewById(R.id.cbExerciseCompleted)

        val llTimerBanner: LinearLayout = itemView.findViewById(R.id.llTimerBanner)
        val tvTimerCountdown: TextView = itemView.findViewById(R.id.tvTimerCountdown)
        val btnCancelTimer: TextView = itemView.findViewById(R.id.btnCancelTimer)

        val llExpandableBody: LinearLayout = itemView.findViewById(R.id.llExpandableBody)
        val cgExerciseEquipment: ChipGroup = itemView.findViewById(R.id.cgExerciseEquipment)
        val tvInstructions: TextView = itemView.findViewById(R.id.tvInstructions)
        val tvCoachTips: TextView = itemView.findViewById(R.id.tvCoachTips)
        val tvCommonMistakes: TextView = itemView.findViewById(R.id.tvCommonMistakes)
    }

    private fun String.capitalizeWords(): String =
        split(" ").joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
}
