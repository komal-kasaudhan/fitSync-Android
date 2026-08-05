package com.esha.fitsync.screens.Nutrition

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.esha.fitsync.R
import com.esha.fitsync.api.AddMealFoodItem
import com.esha.fitsync.api.AddMealRequest
import com.esha.fitsync.api.FoodItem
import com.esha.fitsync.api.RetrofitClient
import com.esha.fitsync.databinding.ActivityFoodSearchBinding
import com.google.android.material.chip.Chip
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class FoodSearchActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFoodSearchBinding
    private lateinit var foodAdapter: FoodAdapter
    private var selectedMealType = "LUNCH"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFoodSearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        selectedMealType = intent.getStringExtra("MEAL_TYPE") ?: "LUNCH"
        binding.tvHeaderTitle.text = "Add to $selectedMealType"

        setupRecyclerView()
        setupListeners()
        fetchFoods("")
    }

    private fun setupRecyclerView() {
        foodAdapter = FoodAdapter { foodItem ->

            val bottomSheet = QuantityBottomSheetFragment(
                selectedFood = foodItem,
                selectedMealType = selectedMealType,
                onMealLogged = { food, actualServings, totalCal ->
                    logMealToBackend(food, actualServings)
                }
            )
            bottomSheet.show(supportFragmentManager, "QuantityBottomSheet")
        }

        binding.rvSearchResults.apply {
            layoutManager = LinearLayoutManager(this@FoodSearchActivity)
            adapter = foodAdapter
        }
    }

    private fun setupListeners() {

        binding.btnBack.setOnClickListener { finish() }


        binding.etSearchQuery.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val category = getSelectedCategory()
                fetchFoods(s.toString().trim(), category)
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })


        binding.chipGroupCategories.setOnCheckedStateChangeListener { _, checkedIds ->
            val query = binding.etSearchQuery.text.toString().trim()
            val category = getSelectedCategory()
            fetchFoods(query, category)
        }
    }

    private fun getSelectedCategory(): String? {
        val checkedChipId = binding.chipGroupCategories.checkedChipId
        if (checkedChipId != View.NO_ID) {
            val chip = binding.chipGroupCategories.findViewById<Chip>(checkedChipId)
            val categoryText = chip?.text?.toString()?.uppercase()
            if (categoryText != "ALL") {
                return categoryText
            }
        }
        return null
    }

    private fun fetchFoods(query: String, category: String? = null) {
        lifecycleScope.launch {
            try {
                binding.pbSearchLoading.visibility = View.VISIBLE

                val response = RetrofitClient.apiService.searchFoods(query, category)
                binding.pbSearchLoading.visibility = View.GONE

                if (response.isSuccessful && response.body()?.success == true) {
                    val apiFoods = response.body()?.data ?: emptyList()

                    val mappedFoods = apiFoods.map { apiItem ->
                        com.esha.fitsync.screens.Nutrition.FoodItem(
                            id = apiItem.id ?: "",
                            name = apiItem.name ?: "",
                            category = "General",
                            servingType = "g",
                            servingWeight = 100.0,
                            calories = apiItem.calories ?: 0,
                            protein = apiItem.protein ?: 0.0,
                            carbs = apiItem.carbs ?: 0.0,
                            fat = apiItem.fat ?: 0.0

                        )
                    }
                    foodAdapter.submitList(mappedFoods)
                } else {
                    foodAdapter.submitList(emptyList())
                }
            } catch (e: Exception) {
                binding.pbSearchLoading.visibility = View.GONE
                Toast.makeText(this@FoodSearchActivity, "Error loading foods", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun logMealToBackend(food: com.esha.fitsync.screens.Nutrition.FoodItem, quantity: Double) {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val request = AddMealRequest(
            date = todayStr,
            mealType = selectedMealType,
            foods = listOf(AddMealFoodItem(foodId = food.id, quantity = quantity))
        )

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.addMeal(request)
                if (response.isSuccessful) {
                    Toast.makeText(this@FoodSearchActivity, "Logged to $selectedMealType! 🍲", Toast.LENGTH_SHORT).show()
                    finish() // Close activity and auto-refresh Nutrition Dashboard
                } else {
                    Toast.makeText(this@FoodSearchActivity, "Failed to log meal", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@FoodSearchActivity, "Server Error", Toast.LENGTH_SHORT).show()
            }
        }
    }
}