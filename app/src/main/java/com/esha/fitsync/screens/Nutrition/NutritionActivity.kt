package com.esha.fitsync.screens.Nutrition

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import coil.load
import com.esha.fitsync.R
import com.esha.fitsync.api.AddWaterRequest
import com.esha.fitsync.api.LoggedMeal
import com.esha.fitsync.api.RetrofitClient
import com.esha.fitsync.api.ScanFoodRequest
import com.esha.fitsync.models.AddRecommendationRequest
import com.esha.fitsync.models.RecipeRecommendation
import com.esha.fitsync.models.WeeklyNutritionDay
import com.esha.fitsync.screens.bottomsheet.AiChatBottomSheet
import com.esha.fitsync.screens.bottomsheet.RecipeDetailBottomSheet
import com.esha.fitsync.ui.WeeklyBarChartView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*

class NutritionActivity : AppCompatActivity() {

    private lateinit var srlNutritionRefresh: SwipeRefreshLayout
    private lateinit var btnBack: ImageButton
    private lateinit var btnHeaderAiChat: ImageView

    // Circular ring & top texts
    private lateinit var pbCalorieRing: ProgressBar
    private lateinit var tvKcalEaten: TextView
    private lateinit var tvKcalLeft: TextView
    private lateinit var tvKcalTarget: TextView
    private lateinit var tvAiInsightText: TextView
    private var shimmerNutritionInsight: com.facebook.shimmer.ShimmerFrameLayout? = null
    private var vShimmerNutritionPlaceholder: View? = null

    // Macro tiles
    private lateinit var tvProteinVal: TextView
    private lateinit var tvCarbsVal: TextView
    private lateinit var tvFatVal: TextView
    private lateinit var tvWaterVal: TextView
    private lateinit var pbProtein: ProgressBar
    private lateinit var pbCarbs: ProgressBar
    private lateinit var pbFat: ProgressBar
    private lateinit var pbWater: ProgressBar
    private lateinit var tvHydrationProgress: TextView

    // Action cards
    private lateinit var cardAddMeal: MaterialCardView
    private lateinit var cardScan: MaterialCardView
    private lateinit var cardWater: MaterialCardView
    private lateinit var cardAskAi: MaterialCardView

    // Timeline elements
    private lateinit var tvTimelineDate: TextView
    private lateinit var ivCheckBreakfast: ImageView
    private lateinit var tvBreakfastSubtitle: TextView
    private lateinit var tvBreakfastKcal: TextView
    private lateinit var btnAddBreakfast: MaterialButton

    private lateinit var ivCheckLunch: ImageView
    private lateinit var tvLunchSubtitle: TextView
    private lateinit var tvLunchKcal: TextView
    private lateinit var btnAddLunch: MaterialButton

    private lateinit var ivCheckDinner: ImageView
    private lateinit var tvDinnerSubtitle: TextView
    private lateinit var tvDinnerKcal: TextView
    private lateinit var btnAddDinner: MaterialButton

    private lateinit var ivCheckSnack: ImageView
    private lateinit var tvSnackSubtitle: TextView
    private lateinit var tvSnackKcal: TextView
    private lateinit var btnAddSnack: MaterialButton

    // Feature 1: Dynamic Recommendations
    private lateinit var tvRemainingProteinBadge: TextView
    private lateinit var llRecommendationsLoading: View
    private lateinit var llRecommendationsEmpty: View
    private lateinit var llRecommendationsError: View
    private lateinit var btnRetryRecommendations: MaterialButton
    private lateinit var rvRecommendations: RecyclerView
    private lateinit var recommendationAdapter: com.esha.fitsync.screens.Adapters.RecommendationAdapter

    // Feature 2: Weekly Bar Chart & Day Detail
    private lateinit var chartWeeklyNutrition: WeeklyBarChartView
    private lateinit var cardSelectedDayDetail: MaterialCardView
    private lateinit var tvSelectedDayLabel: TextView
    private lateinit var tvSelectedDayCalories: TextView
    private lateinit var tvSelectedDayProtein: TextView
    private lateinit var tvSelectedDayCarbs: TextView
    private lateinit var tvSelectedDayFat: TextView
    private lateinit var tvSelectedDayWater: TextView

    private lateinit var btnQuickWater: MaterialButton

    private val CAMERA_PERMISSION_CODE = 101

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val photoBitmap = result.data?.extras?.get("data") as? Bitmap
            photoBitmap?.let {
                sendImageToGeminiAi(it)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nutrition)

        initViews()
        setupListeners()
        loadDashboardData()

        lifecycleScope.launch {
            com.esha.fitsync.data.AppCacheManager.refreshTrigger.collect {
                loadDashboardData()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadDashboardData()
    }

    private fun initViews() {
        srlNutritionRefresh = findViewById(R.id.srlNutritionRefresh)
        btnBack = findViewById(R.id.btnBack)
        btnHeaderAiChat = findViewById(R.id.btnHeaderAiChat)

        tvKcalEaten = findViewById(R.id.tvKcalEaten)
        tvKcalLeft = findViewById(R.id.tvKcalLeft)
        tvKcalTarget = findViewById(R.id.tvKcalTarget)
        tvAiInsightText = findViewById(R.id.tvAiInsightText)
        shimmerNutritionInsight = findViewById(R.id.shimmerNutritionInsight)
        vShimmerNutritionPlaceholder = findViewById(R.id.vShimmerNutritionPlaceholder)
        pbCalorieRing = findViewById(R.id.pbCalorieRing)
        pbCalorieRing.isIndeterminate = false

        tvProteinVal = findViewById(R.id.tvProteinVal)
        tvCarbsVal = findViewById(R.id.tvCarbsVal)
        tvFatVal = findViewById(R.id.tvFatVal)
        tvWaterVal = findViewById(R.id.tvWaterVal)
        pbProtein = findViewById(R.id.pbProtein)
        pbCarbs = findViewById(R.id.pbCarbs)
        pbFat = findViewById(R.id.pbFat)
        pbWater = findViewById(R.id.pbWater)
        tvHydrationProgress = findViewById(R.id.tvHydrationProgress)

        cardAddMeal = findViewById(R.id.cardAddMeal)
        cardScan = findViewById(R.id.cardScan)
        cardWater = findViewById(R.id.cardWater)
        cardAskAi = findViewById(R.id.cardAskAi)

        tvTimelineDate = findViewById(R.id.tvTimelineDate)
        ivCheckBreakfast = findViewById(R.id.checkB)
        tvBreakfastSubtitle = findViewById(R.id.tvBreakfastSubtitle)
        tvBreakfastKcal = findViewById(R.id.tvBreakfastKcal)
        btnAddBreakfast = findViewById(R.id.btnAddBreakfast)

        ivCheckLunch = findViewById(R.id.checkL)
        tvLunchSubtitle = findViewById(R.id.tvLunchSubtitle)
        tvLunchKcal = findViewById(R.id.tvLunchKcal)
        btnAddLunch = findViewById(R.id.btnAddLunch)

        ivCheckDinner = findViewById(R.id.checkD)
        tvDinnerSubtitle = findViewById(R.id.tvDinnerSubtitle)
        tvDinnerKcal = findViewById(R.id.tvDinnerKcal)
        btnAddDinner = findViewById(R.id.btnAddDinner)

        ivCheckSnack = findViewById(R.id.checkS)
        tvSnackSubtitle = findViewById(R.id.tvSnackSubtitle)
        tvSnackKcal = findViewById(R.id.tvSnackKcal)
        btnAddSnack = findViewById(R.id.btnAddSnack)

        // Recommendations (Dynamic RecyclerView)
        tvRemainingProteinBadge = findViewById(R.id.tvRemainingProteinBadge)
        llRecommendationsLoading = findViewById(R.id.llRecommendationsLoading)
        llRecommendationsEmpty = findViewById(R.id.llRecommendationsEmpty)
        llRecommendationsError = findViewById(R.id.llRecommendationsError)
        btnRetryRecommendations = findViewById(R.id.btnRetryRecommendations)
        rvRecommendations = findViewById(R.id.rvRecommendations)
        rvRecommendations.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this, androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL, false)

        recommendationAdapter = com.esha.fitsync.screens.Adapters.RecommendationAdapter(
            items = emptyList(),
            onViewRecipe = { rec ->
                val sheet = RecipeDetailBottomSheet(rec) { logRecommendationMeal(it) }
                sheet.show(supportFragmentManager, "RecipeDetailBottomSheet")
            },
            onAddMeal = { rec ->
                logRecommendationMeal(rec)
            }
        )
        rvRecommendations.adapter = recommendationAdapter

        // Weekly Bar Chart
        chartWeeklyNutrition = findViewById(R.id.chartWeeklyNutrition)
        cardSelectedDayDetail = findViewById(R.id.cardSelectedDayDetail)
        tvSelectedDayLabel = findViewById(R.id.tvSelectedDayLabel)
        tvSelectedDayCalories = findViewById(R.id.tvSelectedDayCalories)
        tvSelectedDayProtein = findViewById(R.id.tvSelectedDayProtein)
        tvSelectedDayCarbs = findViewById(R.id.tvSelectedDayCarbs)
        tvSelectedDayFat = findViewById(R.id.tvSelectedDayFat)
        tvSelectedDayWater = findViewById(R.id.tvSelectedDayWater)

        btnQuickWater = findViewById(R.id.btnQuickAddWater)

        // Set dynamic date in header
        val displayDate = SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(Date())
        tvTimelineDate.text = displayDate
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }

        srlNutritionRefresh.setOnRefreshListener {
            loadDashboardData()
        }

        btnRetryRecommendations.setOnClickListener {
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            fetchRecommendations(todayStr)
        }

        cardAddMeal.setOnClickListener {
            highlightQuickActionCard(cardAddMeal)
            val modal = MealTypeBottomSheetFragment()
            modal.show(supportFragmentManager, "MealTypeBottomSheet")
        }

        cardScan.setOnClickListener {
            highlightQuickActionCard(cardScan)
            checkCameraPermissionAndOpen()
        }

        cardWater.setOnClickListener {
            highlightQuickActionCard(cardWater)
            logWaterToBackend(250)
        }
        btnQuickWater.setOnClickListener { logWaterToBackend(250) }

        cardAskAi.setOnClickListener {
            highlightQuickActionCard(cardAskAi)
            openAiCoachActivity()
        }
        btnHeaderAiChat.setOnClickListener { openAiCoachActivity() }

        btnAddBreakfast.setOnClickListener { openFoodSearch("Breakfast") }
        btnAddLunch.setOnClickListener { openFoodSearch("Lunch") }
        btnAddDinner.setOnClickListener { openFoodSearch("Dinner") }
        btnAddSnack.setOnClickListener { openFoodSearch("Snack") }

        chartWeeklyNutrition.setOnDaySelectedListener { day ->
            bindSelectedDayDetail(day)
        }
    }

    private fun highlightQuickActionCard(selectedCard: MaterialCardView) {
        val defaultStroke = ColorStateList.valueOf(Color.parseColor("#1A2E44"))
        val activeStroke = ColorStateList.valueOf(Color.parseColor("#00D4FF"))

        cardAddMeal.setStrokeColor(defaultStroke)
        cardScan.setStrokeColor(defaultStroke)
        cardWater.setStrokeColor(defaultStroke)
        cardAskAi.setStrokeColor(defaultStroke)

        selectedCard.setStrokeColor(activeStroke)
    }

    private fun loadDashboardData() {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        lifecycleScope.launch {
            try {
                // 1. Fetch Today's Nutrition Summary
                val response = RetrofitClient.apiService.getTodayNutrition(todayStr)
                if (response.isSuccessful && response.body()?.success == true) {
                    val data = response.body()?.data
                    data?.let {
                        val eaten = it.eatenCalories
                        val targetCal = if (it.targetCalories > 0) it.targetCalories else 2200
                        val remaining = if (it.remainingCalories > 0) it.remainingCalories else Math.max(0, targetCal - eaten)
                        val calPercent = if (targetCal > 0) ((eaten.toDouble() / targetCal) * 100).toInt() else 0

                        pbCalorieRing.isIndeterminate = false
                        pbCalorieRing.max = 100
                        pbCalorieRing.progress = calPercent.coerceIn(0, 100)

                        tvKcalEaten.text = String.format("%,d", eaten)
                        tvKcalLeft.text = "$remaining kcal Left"
                        tvKcalTarget.text = "Target: ${String.format("%,d", targetCal)} kcal"

                        val pCurr = it.protein.current.toInt()
                        val pTarg = if (it.protein.target > 0) it.protein.target.toInt() else 120
                        tvProteinVal.text = "${pCurr}/${pTarg}g"
                        pbProtein.max = 100
                        pbProtein.progress = ((pCurr.toDouble() / pTarg) * 100).toInt().coerceIn(0, 100)

                        val cCurr = it.carbs.current.toInt()
                        val cTarg = if (it.carbs.target > 0) it.carbs.target.toInt() else 260
                        tvCarbsVal.text = "${cCurr}/${cTarg}g"
                        pbCarbs.max = 100
                        pbCarbs.progress = ((cCurr.toDouble() / cTarg) * 100).toInt().coerceIn(0, 100)

                        val fCurr = it.fat.current.toInt()
                        val fTarg = if (it.fat.target > 0) it.fat.target.toInt() else 70
                        tvFatVal.text = "${fCurr}/${fTarg}g"
                        pbFat.max = 100
                        pbFat.progress = ((fCurr.toDouble() / fTarg) * 100).toInt().coerceIn(0, 100)

                        val formattedWater = String.format(Locale.US, "%.1f/%.1fL", it.water.current, it.water.target)
                        tvWaterVal.text = formattedWater
                        tvHydrationProgress.text = formattedWater
                        val wTarg = if (it.water.target > 0) it.water.target else 3.0
                        pbWater.max = 100
                        pbWater.progress = ((it.water.current / wTarg) * 100).toInt().coerceIn(0, 100)

                        updateTimelineMeals(it.mealsLogged)

                        // Synchronize to unified DailyTargetsRepository
                        com.esha.fitsync.data.repository.DailyTargetsRepository.updateNutrition(
                            proteinConsumed = it.protein.current,
                            proteinTarget = if (it.protein.target > 0) it.protein.target else 120.0,
                            waterConsumedLiters = it.water.current,
                            waterTargetLiters = if (it.water.target > 0) it.water.target else 3.0,
                            caloriesConsumed = eaten,
                            caloriesTarget = targetCal
                        )

                        android.util.Log.d(
                            "FitSyncSync",
                            "Nutrition Screen -> Protein: ${pCurr}/${pTarg}g, Water: $formattedWater"
                        )
                    }
                }

                // 2. Feature 1: Fetch Daily Protein Recommendations (Dynamic RecyclerView)
                fetchRecommendations(todayStr)

                // 3. Feature 2: Fetch Weekly Nutrition for Bar Chart
                fetchWeeklyNutrition(todayStr)

                // 4. Feature 3: Fetch Dynamic AI Nutrition Insight
                fetchDynamicInsight(todayStr)

            } catch (e: Exception) {
                android.util.Log.e("FITSYNC_NETWORK", "Exception in loadDashboardData", e)
                val cached = com.esha.fitsync.data.repository.DailyTargetsRepository.getCachedTargets()
                val pCurr = cached.proteinConsumed.toInt()
                val pTarg = if (cached.proteinTarget > 0) cached.proteinTarget.toInt() else 120
                tvProteinVal.text = "${pCurr}/${pTarg}g"
                pbProtein.max = 100
                pbProtein.progress = cached.proteinPercent

                val formattedWater = String.format(Locale.US, "%.1f/%.1fL", cached.waterConsumedLiters, cached.waterTargetLiters)
                tvWaterVal.text = formattedWater
                tvHydrationProgress.text = formattedWater
                pbWater.max = 100
                pbWater.progress = cached.waterPercent

                android.util.Log.d(
                    "FitSyncSync",
                    "Nutrition Screen (Cached) -> Protein: ${pCurr}/${pTarg}g, Water: $formattedWater"
                )
            } finally {
                srlNutritionRefresh.isRefreshing = false
            }
        }
    }

    private fun fetchRecommendations(todayStr: String) {
        // Show cached recommendations immediately to avoid disappearing data
        val cachedRecs = com.esha.fitsync.data.AppCacheManager.getRecommendations()
        if (cachedRecs != null && cachedRecs.recommendations.isNotEmpty()) {
            tvRemainingProteinBadge.text = "${cachedRecs.remainingProtein}g protein left"
            rvRecommendations.visibility = View.VISIBLE
            llRecommendationsEmpty.visibility = View.GONE
            llRecommendationsError.visibility = View.GONE
            recommendationAdapter.submitList(cachedRecs.recommendations)
        } else {
            llRecommendationsLoading.visibility = View.VISIBLE
            llRecommendationsEmpty.visibility = View.GONE
            llRecommendationsError.visibility = View.GONE
            rvRecommendations.visibility = View.GONE
        }

        lifecycleScope.launch {
            try {
                val recRes = RetrofitClient.apiService.getRecommendations(todayStr)
                android.util.Log.d("FITSYNC_REC", "Recommendations: code=${recRes.code()}, body=${recRes.body()?.recommendations?.size}")
                llRecommendationsLoading.visibility = View.GONE

                if (recRes.isSuccessful && recRes.body() != null) {
                    val recBody = recRes.body()!!
                    com.esha.fitsync.data.AppCacheManager.saveRecommendations(recBody)
                    val list = recBody.recommendations
                    tvRemainingProteinBadge.text = "${recBody.remainingProtein}g protein left"

                    if (list.isEmpty()) {
                        llRecommendationsEmpty.visibility = View.VISIBLE
                        rvRecommendations.visibility = View.GONE
                    } else {
                        llRecommendationsEmpty.visibility = View.GONE
                        llRecommendationsError.visibility = View.GONE
                        rvRecommendations.visibility = View.VISIBLE
                        recommendationAdapter.submitList(list)
                    }
                } else {
                    val existing = com.esha.fitsync.data.AppCacheManager.getRecommendations()
                    if (existing == null || existing.recommendations.isEmpty()) {
                        llRecommendationsError.visibility = View.VISIBLE
                        rvRecommendations.visibility = View.GONE
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("FITSYNC_REC", "Failed to fetch recommendations", e)
                llRecommendationsLoading.visibility = View.GONE
                val existing = com.esha.fitsync.data.AppCacheManager.getRecommendations()
                if (existing == null || existing.recommendations.isEmpty()) {
                    llRecommendationsError.visibility = View.VISIBLE
                    rvRecommendations.visibility = View.GONE
                }
            }
        }
    }

    private fun fetchWeeklyNutrition(todayStr: String) {
        // Show cached bars immediately to prevent chart blanking
        val cachedDays = com.esha.fitsync.data.AppCacheManager.getWeeklyNutrition()
        if (!cachedDays.isNullOrEmpty()) {
            chartWeeklyNutrition.setData(cachedDays, defaultSelectToday = true)
        }

        lifecycleScope.launch {
            try {
                android.util.Log.d("FITSYNC_WEEKLY", "Fetching weekly nutrition: /api/nutrition/weekly?endDate=$todayStr")
                val weeklyRes = RetrofitClient.apiService.getWeeklyNutrition(todayStr)
                android.util.Log.d("FITSYNC_WEEKLY", "Weekly response: code=${weeklyRes.code()}, count=${weeklyRes.body()?.size}")

                if (weeklyRes.isSuccessful && !weeklyRes.body().isNullOrEmpty()) {
                    val days = weeklyRes.body()!!
                    com.esha.fitsync.data.AppCacheManager.saveWeeklyNutrition(days)
                    chartWeeklyNutrition.setData(days, defaultSelectToday = true)
                } else {
                    val existing = com.esha.fitsync.data.AppCacheManager.getWeeklyNutrition()
                    if (existing.isNullOrEmpty()) {
                        android.util.Log.w("FITSYNC_WEEKLY", "Weekly API empty or failed (${weeklyRes.code()}), displaying default 7-day bars")
                        chartWeeklyNutrition.setData(emptyList(), defaultSelectToday = true)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("FITSYNC_WEEKLY", "Failed to fetch weekly nutrition", e)
                val existing = com.esha.fitsync.data.AppCacheManager.getWeeklyNutrition()
                if (existing.isNullOrEmpty()) {
                    chartWeeklyNutrition.setData(emptyList(), defaultSelectToday = true)
                }
            }
        }
    }

    private fun fetchDynamicInsight(todayStr: String) {
        val cachedInsight = com.esha.fitsync.data.AppCacheManager.getDynamicInsight()
        if (cachedInsight != null && !cachedInsight.message.isNullOrBlank()) {
            tvAiInsightText.text = cachedInsight.message
            tvAiInsightText.visibility = View.VISIBLE
            shimmerNutritionInsight?.visibility = View.GONE
            vShimmerNutritionPlaceholder?.visibility = View.GONE
        } else {
            vShimmerNutritionPlaceholder?.visibility = View.VISIBLE
            shimmerNutritionInsight?.visibility = View.VISIBLE
            shimmerNutritionInsight?.startShimmer()
            tvAiInsightText.visibility = View.GONE
        }

        lifecycleScope.launch {
            try {
                val insightRes = RetrofitClient.apiService.getDynamicInsight(todayStr)
                if (insightRes.isSuccessful && insightRes.body() != null) {
                    val body = insightRes.body()!!
                    val msg = body.message
                    if (!msg.isNullOrBlank()) {
                        com.esha.fitsync.data.AppCacheManager.saveDynamicInsight(body)
                        tvAiInsightText.text = msg
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("FITSYNC_NETWORK", "Failed to fetch dynamic insight", e)
            } finally {
                shimmerNutritionInsight?.stopShimmer()
                shimmerNutritionInsight?.visibility = View.GONE
                vShimmerNutritionPlaceholder?.visibility = View.GONE
                tvAiInsightText.visibility = View.VISIBLE
            }
        }
    }

    private fun bindSelectedDayDetail(day: WeeklyNutritionDay) {
        cardSelectedDayDetail.visibility = View.VISIBLE
        tvSelectedDayLabel.text = "${day.dayLabel} (${day.date})"
        tvSelectedDayCalories.text = "${day.caloriesConsumed} / ${day.calorieTarget} kcal"
        tvSelectedDayProtein.text = "P: ${day.protein}g"
        tvSelectedDayCarbs.text = "C: ${day.carbs}g"
        tvSelectedDayFat.text = "F: ${day.fat}g"
        tvSelectedDayWater.text = "W: ${day.water}ml"
    }

    private fun logRecommendationMeal(recipe: RecipeRecommendation) {
        val mealType = recipe.mealType ?: "Lunch"
        lifecycleScope.launch {
            try {
                val res = RetrofitClient.apiService.addRecommendation(
                    recipeId = recipe.id,
                    request = AddRecommendationRequest(mealType = mealType)
                )
                if (res.isSuccessful) {
                    Toast.makeText(this@NutritionActivity, "Logged '${recipe.name}' to $mealType! 🍲", Toast.LENGTH_SHORT).show()
                    com.esha.fitsync.data.AppCacheManager.notifyDataChanged()
                    loadDashboardData()
                } else {
                    Toast.makeText(this@NutritionActivity, "Failed to log recommendation (${res.code()})", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@NutritionActivity, "Network error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateTimelineMeals(meals: List<LoggedMeal>) {
        val mealMap = meals.associateBy { it.mealType?.lowercase() }

        // 1. Breakfast
        bindMealSection(
            meal = mealMap["breakfast"],
            mealTypeName = "Breakfast",
            ivCheck = ivCheckBreakfast,
            tvSubtitle = tvBreakfastSubtitle,
            tvKcal = tvBreakfastKcal,
            btnAdd = btnAddBreakfast
        )

        // 2. Lunch
        bindMealSection(
            meal = mealMap["lunch"],
            mealTypeName = "Lunch",
            ivCheck = ivCheckLunch,
            tvSubtitle = tvLunchSubtitle,
            tvKcal = tvLunchKcal,
            btnAdd = btnAddLunch
        )

        // 3. Dinner
        bindMealSection(
            meal = mealMap["dinner"],
            mealTypeName = "Dinner",
            ivCheck = ivCheckDinner,
            tvSubtitle = tvDinnerSubtitle,
            tvKcal = tvDinnerKcal,
            btnAdd = btnAddDinner
        )

        // 4. Snack
        bindMealSection(
            meal = mealMap["snack"],
            mealTypeName = "Snack",
            ivCheck = ivCheckSnack,
            tvSubtitle = tvSnackSubtitle,
            tvKcal = tvSnackKcal,
            btnAdd = btnAddSnack
        )
    }

    private fun bindMealSection(
        meal: LoggedMeal?,
        mealTypeName: String,
        ivCheck: ImageView,
        tvSubtitle: TextView,
        tvKcal: TextView,
        btnAdd: MaterialButton
    ) {
        if (meal != null && meal.items.isNotEmpty()) {
            val totalCal = meal.totalMealNutrition?.calories ?: meal.items.sumOf { it.calculatedNutrition?.calories ?: 0 }
            val totalProt = meal.totalMealNutrition?.protein ?: meal.items.sumOf { it.calculatedNutrition?.protein ?: 0.0 }
            val itemNames = meal.items.take(2).mapNotNull { it.foodName }.joinToString(", ")
            val countStr = if (meal.items.size > 2) " +${meal.items.size - 2} more" else ""

            ivCheck.setImageResource(android.R.drawable.checkbox_on_background)
            ivCheck.imageTintList = ColorStateList.valueOf(Color.parseColor("#10B981"))
            tvSubtitle.text = "$itemNames$countStr • ${totalProt.toInt()}g Prot"
            tvKcal.text = "$totalCal kcal"
            tvKcal.visibility = View.VISIBLE
            btnAdd.visibility = View.GONE
        } else {
            ivCheck.setImageResource(android.R.drawable.presence_invisible)
            ivCheck.imageTintList = ColorStateList.valueOf(Color.parseColor("#1A2E44"))
            tvSubtitle.text = "Not logged yet"
            tvKcal.visibility = View.GONE
            btnAdd.visibility = View.VISIBLE
        }
    }

    private fun logWaterToBackend(amountMl: Int) {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.addWater(AddWaterRequest(amountMl = amountMl, date = todayStr))

                if (response.isSuccessful) {
                    Toast.makeText(this@NutritionActivity, "+${amountMl}ml Water Logged! 💧", Toast.LENGTH_SHORT).show()
                    com.esha.fitsync.data.AppCacheManager.notifyDataChanged()
                    loadDashboardData()
                }
            } catch (e: Exception) {
                Toast.makeText(this@NutritionActivity, "Server connection failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkCameraPermissionAndOpen() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            openCamera()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera()
            } else {
                Toast.makeText(this, "Camera permission is required to scan food!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openCamera() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraLauncher.launch(cameraIntent)
    }

    private fun sendImageToGeminiAi(bitmap: Bitmap) {
        val progressDialog = com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("Scanning Food")
            .setMessage("Gemini AI is analyzing your food photo... 🧠📸")
            .setCancelable(false)
            .create()
        progressDialog.show()

        lifecycleScope.launch {
            try {
                // 1. Scale down bitmap to reasonable max dimensions
                val scaledBitmap = if (bitmap.width > 1200 || bitmap.height > 1200) {
                    val scale = 1200f / Math.max(bitmap.width, bitmap.height)
                    Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
                } else {
                    bitmap
                }

                // 2. Compress to JPEG
                val outputStream = ByteArrayOutputStream()
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
                val imageBytes = outputStream.toByteArray()

                val mediaType = "image/jpeg".toMediaTypeOrNull()
                val requestBody = imageBytes.toRequestBody(mediaType, 0, imageBytes.size)
                val part = MultipartBody.Part.createFormData("image", "food_${System.currentTimeMillis()}.jpg", requestBody)

                val response = RetrofitClient.apiService.scanFoodImageMultipart(part)
                progressDialog.dismiss()

                if (response.isSuccessful && response.body() != null) {
                    val scanData = response.body()!!
                    if (scanData.isDetected) {
                        showFoodDetectedDialog(scanData)
                    } else {
                        val backendMessage = scanData.message ?: "No food detected in the photo. Would you like to search manually?"
                        showNoFoodDetectedDialog(backendMessage)
                    }
                } else {
                    val errorStr = response.errorBody()?.string()
                    val errorMsg = try {
                        if (!errorStr.isNullOrEmpty()) {
                            org.json.JSONObject(errorStr).optString("message", "Scan failed (${response.code()})")
                        } else "Scan failed (${response.code()})"
                    } catch (e: Exception) {
                        "Scan failed (${response.code()})"
                    }
                    showScanErrorDialog(errorMsg)
                }
            } catch (e: Exception) {
                progressDialog.dismiss()
                android.util.Log.e("FITSYNC_NETWORK", "AI food scan error", e)
                showScanErrorDialog(e.localizedMessage ?: "Failed to reach server")
            }
        }
    }

    private fun showFoodDetectedDialog(data: com.esha.fitsync.api.ScanFoodResponse) {
        val foodName = data.effectiveFoodName ?: "Identified Dish"
        val message = "Item: $foodName\nCalories: ${data.effectiveCalories} kcal\nProtein: ${data.effectiveProtein}g • Carbs: ${data.effectiveCarbs}g • Fat: ${data.effectiveFat}g\nEstimated Serving: ${data.effectiveWeightGrams.toInt()}g\n\nSelect meal type to log this food:"

        val mealTypes = arrayOf("Breakfast", "Lunch", "Dinner", "Snack")
        var selectedTypeIndex = 1 // Default to Lunch

        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("Food Detected! 🥗")
            .setMessage(message)
            .setSingleChoiceItems(mealTypes, selectedTypeIndex) { _, which ->
                selectedTypeIndex = which
            }
            .setPositiveButton("Log Meal") { dialog, _ ->
                dialog.dismiss()
                val chosenMeal = mealTypes[selectedTypeIndex]
                logScannedFoodDirectly(data, chosenMeal)
            }
            .setNeutralButton("Search Manually") { dialog, _ ->
                dialog.dismiss()
                openFoodSearch("Lunch")
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showNoFoodDetectedDialog(message: String) {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("No Food Detected 📷")
            .setMessage(message)
            .setPositiveButton("Search Manually") { dialog, _ ->
                dialog.dismiss()
                openFoodSearch("Lunch")
            }
            .setNegativeButton("Close") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showScanErrorDialog(errorMessage: String) {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("AI Scan Failed")
            .setMessage(errorMessage)
            .setPositiveButton("Search Manually") { dialog, _ ->
                dialog.dismiss()
                openFoodSearch("Lunch")
            }
            .setNegativeButton("Close") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun logScannedFoodDirectly(data: com.esha.fitsync.api.ScanFoodResponse, mealType: String) {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        lifecycleScope.launch {
            try {
                val searchRes = RetrofitClient.apiService.searchFoods(data.effectiveFoodName)
                val matchingFood = searchRes.body()?.data?.firstOrNull()
                val foodId = matchingFood?.effectiveId

                if (!foodId.isNullOrEmpty()) {
                    val addReq = com.esha.fitsync.api.AddMealRequest(
                        date = todayStr,
                        mealType = mealType,
                        foodId = foodId,
                        quantity = 1.0
                    )
                    val logRes = RetrofitClient.apiService.addMeal(addReq)
                    if (logRes.isSuccessful) {
                        Toast.makeText(this@NutritionActivity, "Logged $mealType successfully! 🍲", Toast.LENGTH_SHORT).show()
                        com.esha.fitsync.data.AppCacheManager.notifyDataChanged()
                        loadDashboardData()
                    } else {
                        Toast.makeText(this@NutritionActivity, "Failed to log meal to server", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val intent = Intent(this@NutritionActivity, FoodSearchActivity::class.java).apply {
                        putExtra("MEAL_TYPE", mealType)
                    }
                    startActivity(intent)
                    Toast.makeText(this@NutritionActivity, "Select verified '${data.foodName}' item to confirm portion", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@NutritionActivity, "Failed to log scanned food: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openFoodSearch(mealType: String) {
        val intent = Intent(this, FoodSearchActivity::class.java)
        intent.putExtra("MEAL_TYPE", mealType)
        startActivity(intent)
    }

    private fun openAiCoachActivity() {
        val sheet = AiChatBottomSheet()
        sheet.show(supportFragmentManager, "AiChatBottomSheet")
    }
}