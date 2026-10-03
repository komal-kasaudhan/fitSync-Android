package com.esha.fitsync.screens.bottomsheet

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.esha.fitsync.R
import com.esha.fitsync.api.RetrofitClient
import com.esha.fitsync.models.UpdateEquipmentRequest
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.launch

class EquipmentBottomSheetFragment(
    private val onEquipmentSaved: (List<String>) -> Unit,
    private val onPlanRegenerated: () -> Unit
) : BottomSheetDialogFragment() {

    private lateinit var cgEquipmentList: ChipGroup
    private lateinit var pbLoading: ProgressBar
    private lateinit var btnSave: MaterialButton

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_equipment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cgEquipmentList = view.findViewById(R.id.cgEquipmentList)
        pbLoading = view.findViewById(R.id.pbEquipmentLoading)
        btnSave = view.findViewById(R.id.btnSaveEquipment)

        loadEquipmentData()

        btnSave.setOnClickListener {
            saveUserEquipment()
        }
    }

    private fun loadEquipmentData() {
        pbLoading.visibility = View.VISIBLE
        btnSave.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val masterRes = RetrofitClient.api.getMasterEquipment()
                val userRes = RetrofitClient.api.getUserEquipment()

                pbLoading.visibility = View.GONE
                btnSave.isEnabled = true

                if (masterRes.isSuccessful && userRes.isSuccessful) {
                    val masterList = masterRes.body()?.equipment ?: emptyList()
                    val userEquip = userRes.body()?.equipment ?: emptyList()

                    cgEquipmentList.removeAllViews()

                    val bgStates = ColorStateList(
                        arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
                        intArrayOf(Color.parseColor("#00D4FF"), Color.parseColor("#162536"))
                    )
                    val textStates = ColorStateList(
                        arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
                        intArrayOf(Color.parseColor("#071018"), Color.parseColor("#9CA3AF"))
                    )

                    for (item in masterList) {
                        val chip = Chip(requireContext()).apply {
                            text = item.label
                            tag = item.id
                            isCheckable = true
                            isCheckedIconVisible = false
                            chipBackgroundColor = bgStates
                            setTextColor(textStates)
                            chipStrokeWidth = 0f
                            isChecked = userEquip.contains(item.id)
                        }
                        cgEquipmentList.addView(chip)
                    }
                } else {
                    Toast.makeText(context, "Could not retrieve equipment data. Please try again.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                pbLoading.visibility = View.GONE
                btnSave.isEnabled = true
                val friendlyMsg = when (e) {
                    is com.google.gson.JsonSyntaxException -> "Unable to parse equipment catalog format."
                    is java.io.IOException -> "Network issue: Unable to reach server. Please check connection."
                    else -> "Error: ${e.localizedMessage ?: "Unknown error"}"
                }
                Toast.makeText(context, friendlyMsg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveUserEquipment() {
        val selectedIds = mutableListOf<String>()
        for (i in 0 until cgEquipmentList.childCount) {
            val chip = cgEquipmentList.getChildAt(i) as? Chip
            if (chip != null && chip.isChecked) {
                selectedIds.add(chip.tag.toString())
            }
        }

        btnSave.isEnabled = false
        pbLoading.visibility = View.VISIBLE

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.updateUserEquipment(UpdateEquipmentRequest(selectedIds))
                pbLoading.visibility = View.GONE
                btnSave.isEnabled = true

                if (response.isSuccessful) {
                    val isPlanStale = response.body()?.planStale == true
                    onEquipmentSaved(selectedIds)

                    if (isPlanStale) {
                        showRegenerateDialog()
                    } else {
                        Toast.makeText(context, "Equipment updated successfully! 💪", Toast.LENGTH_SHORT).show()
                        dismiss()
                    }
                } else {
                    Toast.makeText(context, "Failed to update equipment (${response.code()})", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                pbLoading.visibility = View.GONE
                btnSave.isEnabled = true
                val friendlyMsg = when (e) {
                    is com.google.gson.JsonSyntaxException -> "Unable to process equipment update response."
                    is java.io.IOException -> "Network issue: Unable to reach server. Please check connection."
                    else -> "Network error: ${e.localizedMessage ?: "Unknown error"}"
                }
                Toast.makeText(context, friendlyMsg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showRegenerateDialog() {
        AlertDialog.Builder(requireContext(), R.style.Theme_FitSync)
            .setTitle("Regenerate Plan?")
            .setMessage("Your available equipment has changed. Would you like to regenerate your workout plan to match your new equipment?")
            .setPositiveButton("Regenerate Plan") { dialog, _ ->
                dialog.dismiss()
                triggerPlanRegeneration()
            }
            .setNegativeButton("Keep Current Plan") { dialog, _ ->
                dialog.dismiss()
                dismiss()
            }
            .show()
    }

    private fun triggerPlanRegeneration() {
        pbLoading.visibility = View.VISIBLE
        btnSave.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val regenRes = RetrofitClient.api.regenerateWorkoutPlan()
                pbLoading.visibility = View.GONE
                btnSave.isEnabled = true

                if (regenRes.isSuccessful) {
                    Toast.makeText(context, "Workout plan updated with your new equipment! 🚀", Toast.LENGTH_LONG).show()
                    onPlanRegenerated()
                    dismiss()
                } else {
                    Toast.makeText(context, "Could not regenerate plan (${regenRes.code()})", Toast.LENGTH_SHORT).show()
                    dismiss()
                }
            } catch (e: Exception) {
                pbLoading.visibility = View.GONE
                btnSave.isEnabled = true
                val friendlyMsg = when (e) {
                    is com.google.gson.JsonSyntaxException -> "Unable to process regenerated plan format."
                    is java.io.IOException -> "Network issue: Unable to reach server. Please check connection."
                    else -> "Regeneration error: ${e.localizedMessage ?: "Unknown error"}"
                }
                Toast.makeText(context, friendlyMsg, Toast.LENGTH_SHORT).show()
                dismiss()
            }
        }
    }
}
