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
import com.esha.fitsync.api.AddMealRequest
import com.esha.fitsync.api.RetrofitClient
import com.esha.fitsync.databinding.ActivityFoodSearchBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class FoodSearchActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFoodSearchBinding
    private lateinit var foodAdapter: FoodAdapter
    private var selectedMealType = "LUNCH"
    private var searchJob: Job? = null

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

        binding.btnClearSearch.setOnClickListener {
            binding.etSearchQuery.text?.clear()
            binding.chipAll.isChecked = true
            fetchFoods("")
        }

        binding.btnRetrySearch.setOnClickListener {
            val query = binding.etSearchQuery.text.toString().trim()
            val category = getSelectedCategory()
            fetchFoods(query, category)
        }

        binding.etSearchQuery.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString()?.trim() ?: ""
                val category = getSelectedCategory()
                searchJob?.cancel()
                searchJob = lifecycleScope.launch {
                    delay(300) // Debounce typing
                    fetchFoods(query, category)
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.chipGroupCategories.setOnCheckedStateChangeListener { _, _ ->
            val query = binding.etSearchQuery.text.toString().trim()
            val category = getSelectedCategory()
            fetchFoods(query, category)
        }
    }

    private fun getSelectedCategory(): String? {
        return when (binding.chipGroupCategories.checkedChipId) {
            R.id.chipGrains -> "Grains"
            R.id.chipDairy -> "Dairy"
            R.id.chipFruits -> "Fruits"
            else -> null // All
        }
    }

    private fun fetchFoods(query: String, category: String? = null) {
        lifecycleScope.launch {
            try {
                binding.pbSearchLoading.visibility = View.VISIBLE
                binding.layoutEmptyState.visibility = View.GONE
                binding.layoutErrorState.visibility = View.GONE

                val qParam = if (query.trim().isNotEmpty()) query.trim() else null
                val response = RetrofitClient.apiService.searchFoods(
                    query = qParam,
                    category = category
                )
                binding.pbSearchLoading.visibility = View.GONE

                if (response.isSuccessful && response.body()?.success == true) {
                    val apiFoods = response.body()?.data ?: emptyList()

                    val mappedFoods = apiFoods.map { apiItem ->
                        FoodItem(
                            id = apiItem.effectiveId,
                            name = apiItem.effectiveName,
                            category = apiItem.effectiveCategory,
                            servingType = apiItem.servingType?.ifEmpty { "g" } ?: "g",
                            servingWeight = if (apiItem.servingWeight > 0) apiItem.servingWeight else 100.0,
                            calories = apiItem.calories,
                            protein = apiItem.protein,
                            carbs = apiItem.carbs,
                            fat = apiItem.fat
                        )
                    }

                    if (mappedFoods.isEmpty()) {
                        binding.rvSearchResults.visibility = View.GONE
                        binding.layoutEmptyState.visibility = View.VISIBLE
                        binding.layoutErrorState.visibility = View.GONE
                        foodAdapter.submitList(emptyList())
                    } else {
                        binding.rvSearchResults.visibility = View.VISIBLE
                        binding.layoutEmptyState.visibility = View.GONE
                        binding.layoutErrorState.visibility = View.GONE
                        foodAdapter.submitList(mappedFoods)
                    }
                } else {
                    val errorMsg = try {
                        val errStr = response.errorBody()?.string()
                        if (!errStr.isNullOrEmpty()) {
                            org.json.JSONObject(errStr).optString("message", "Error ${response.code()}")
                        } else "Failed to load foods (${response.code()})"
                    } catch (e: Exception) {
                        "Failed to load foods (${response.code()})"
                    }
                    showErrorState(errorMsg)
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) return@launch
                android.util.Log.e("FITSYNC_NETWORK", "Error loading foods", e)
                binding.pbSearchLoading.visibility = View.GONE
                showErrorState(e.localizedMessage ?: "Unable to connect to server")
            }
        }
    }

    private fun showErrorState(message: String) {
        binding.rvSearchResults.visibility = View.GONE
        binding.layoutEmptyState.visibility = View.GONE
        binding.layoutErrorState.visibility = View.VISIBLE
        binding.tvErrorSubtitle.text = message
        foodAdapter.submitList(emptyList())
    }

    private fun logMealToBackend(food: FoodItem, quantity: Double) {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val formattedMealType = when (selectedMealType.uppercase()) {
            "BREAKFAST" -> "Breakfast"
            "LUNCH" -> "Lunch"
            "DINNER" -> "Dinner"
            "SNACK", "SNACKS" -> "Snack"
            else -> selectedMealType.replaceFirstChar { it.uppercase() }
        }

        val request = AddMealRequest(
            date = todayStr,
            mealType = formattedMealType,
            foodId = food.id,
            quantity = quantity
        )

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.addMeal(request)
                if (response.isSuccessful) {
                    Toast.makeText(this@FoodSearchActivity, "Logged to $formattedMealType! 🍲", Toast.LENGTH_SHORT).show()
                    com.esha.fitsync.data.AppCacheManager.notifyDataChanged()
                    finish()
                } else {
                    val err = response.errorBody()?.string() ?: ""
                    android.util.Log.e("FITSYNC_NETWORK", "AddMeal error: $err")
                    val errorMsg = try {
                        org.json.JSONObject(err).optString("message", "Failed to log meal")
                    } catch (e: Exception) {
                        "Failed to log meal"
                    }
                    Toast.makeText(this@FoodSearchActivity, errorMsg, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                android.util.Log.e("FITSYNC_NETWORK", "AddMeal exception", e)
                Toast.makeText(this@FoodSearchActivity, "Server Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}