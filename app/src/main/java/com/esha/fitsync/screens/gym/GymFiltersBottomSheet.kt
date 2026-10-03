package com.esha.fitsync.screens.gym

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.esha.fitsync.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.switchmaterial.SwitchMaterial

class GymFiltersBottomSheet(
    private var currentSort: String = "distance",
    private var currentOpenNow: Boolean = false,
    private var currentMaxPrice: Double? = null,
    private var currentAmenities: List<String> = emptyList(),
    private val onApply: (sort: String, openNow: Boolean, maxPrice: Double?, amenities: List<String>) -> Unit
) : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_gym_filters, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val chipGroupSort = view.findViewById<ChipGroup>(R.id.chipGroupSort)
        val switchOpenNow = view.findViewById<SwitchMaterial>(R.id.switchOpenNow)
        val chipGroupPrice = view.findViewById<ChipGroup>(R.id.chipGroupPrice)
        val chipGroupAmenities = view.findViewById<ChipGroup>(R.id.chipGroupAmenities)
        val btnApply = view.findViewById<Button>(R.id.btnApplyFilters)
        val btnReset = view.findViewById<Button>(R.id.btnResetFilters)

        // Pre-select sort
        when (currentSort) {
            "rating" -> view.findViewById<Chip>(R.id.chipSortRating)?.isChecked = true
            "price" -> view.findViewById<Chip>(R.id.chipSortPrice)?.isChecked = true
            else -> view.findViewById<Chip>(R.id.chipSortDistance)?.isChecked = true
        }

        // Pre-select open now
        switchOpenNow.isChecked = currentOpenNow

        // Pre-select max price
        when (currentMaxPrice?.toInt()) {
            200 -> view.findViewById<Chip>(R.id.chipPrice200)?.isChecked = true
            500 -> view.findViewById<Chip>(R.id.chipPrice500)?.isChecked = true
            else -> view.findViewById<Chip>(R.id.chipPriceAny)?.isChecked = true
        }

        // Pre-select amenities
        val amenitiesMap = mapOf(
            "ac" to R.id.chipAc,
            "cardio" to R.id.chipCardio,
            "weights" to R.id.chipWeights,
            "lockers" to R.id.chipLockers,
            "parking" to R.id.chipParking,
            "wifi" to R.id.chipWifi,
            "shower" to R.id.chipShower
        )
        currentAmenities.forEach { key ->
            amenitiesMap[key.lowercase()]?.let { id ->
                view.findViewById<Chip>(id)?.isChecked = true
            }
        }

        btnReset.setOnClickListener {
            view.findViewById<Chip>(R.id.chipSortDistance)?.isChecked = true
            switchOpenNow.isChecked = false
            view.findViewById<Chip>(R.id.chipPriceAny)?.isChecked = true
            chipGroupAmenities.clearCheck()
        }

        btnApply.setOnClickListener {
            val selectedSort = when {
                view.findViewById<Chip>(R.id.chipSortRating)?.isChecked == true -> "rating"
                view.findViewById<Chip>(R.id.chipSortPrice)?.isChecked == true -> "price"
                else -> "distance"
            }

            val isOpenNow = switchOpenNow.isChecked

            val maxPrice = when {
                view.findViewById<Chip>(R.id.chipPrice200)?.isChecked == true -> 200.0
                view.findViewById<Chip>(R.id.chipPrice500)?.isChecked == true -> 500.0
                else -> null
            }

            val selectedAmenities = mutableListOf<String>()
            amenitiesMap.forEach { (key, id) ->
                if (view.findViewById<Chip>(id)?.isChecked == true) {
                    selectedAmenities.add(key)
                }
            }

            onApply(selectedSort, isOpenNow, maxPrice, selectedAmenities)
            dismiss()
        }
    }
}
