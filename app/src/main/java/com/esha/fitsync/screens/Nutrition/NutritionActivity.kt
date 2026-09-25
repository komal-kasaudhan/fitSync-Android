package com.esha.fitsync.screens.Nutrition

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
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
import com.esha.fitsync.R
import com.esha.fitsync.api.AddWaterRequest
import com.esha.fitsync.api.LoggedMeal
import com.esha.fitsync.api.RetrofitClient
import com.esha.fitsync.api.ScanFoodRequest
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*

class NutritionActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageButton
    private lateinit var btnHeaderAiChat: ImageView

    // Circular ring & top texts
    private lateinit var pbCalorieRing: ProgressBar
    private lateinit var tvKcalEaten: TextView
    private lateinit var tvKcalLeft: TextView
    private lateinit var tvKcalTarget: TextView
    private lateinit var tvAiInsightText: TextView

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

    private lateinit var btnAddRecommendedLunch: MaterialButton
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
        loadGeminiAiInsight()
    }

    override fun onResume() {
        super.onResume()
        loadDashboardData()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        btnHeaderAiChat = findViewById(R.id.btnHeaderAiChat)

        tvKcalEaten = findViewById(R.id.tvKcalEaten)
        tvKcalLeft = findViewById(R.id.tvKcalLeft)
        tvKcalTarget = findViewById(R.id.tvKcalTarget)
        tvAiInsightText = findViewById(R.id.tvAiInsightText)
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

        btnAddRecommendedLunch = findViewById(R.id.btnAddRecommendedLunch)
        btnQuickWater = findViewById(R.id.btnQuickAddWater)

        // Set dynamic date in header
        val displayDate = SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(Date())
        tvTimelineDate.text = displayDate
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }

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
        btnAddRecommendedLunch.setOnClickListener { openFoodSearch("Lunch") }
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
                val response = RetrofitClient.apiService.getTodayNutrition(todayStr)

                if (response.isSuccessful && response.body()?.success == true) {
                    val data = response.body()?.data

                    data?.let {
                        val eaten = it.eatenCalories
                        val targetCal = if (it.targetCalories > 0) it.targetCalories else 2200
                        val remaining = if (it.remainingCalories > 0) it.remainingCalories else Math.max(0, targetCal - eaten)

                        val calPercent = if (targetCal > 0) ((eaten.toDouble() / targetCal) * 100).toInt() else 0

                        pbCalorieRing.isIndeterminate = false
                        pbCalorieRing.progress = calPercent.coerceIn(0, 100)

                        tvKcalEaten.text = String.format("%,d", eaten)
                        tvKcalLeft.text = "$remaining kcal Left"
                        tvKcalTarget.text = "Target: ${String.format("%,d", targetCal)} kcal"

                        // Macro values
                        val pCurr = it.protein.current.toInt()
                        val pTarg = if (it.protein.target > 0) it.protein.target.toInt() else 120
                        tvProteinVal.text = "${pCurr}/${pTarg}g"
                        pbProtein.progress = ((pCurr.toDouble() / pTarg) * 100).toInt().coerceIn(0, 100)

                        val cCurr = it.carbs.current.toInt()
                        val cTarg = if (it.carbs.target > 0) it.carbs.target.toInt() else 260
                        tvCarbsVal.text = "${cCurr}/${cTarg}g"
                        pbCarbs.progress = ((cCurr.toDouble() / cTarg) * 100).toInt().coerceIn(0, 100)

                        val fCurr = it.fat.current.toInt()
                        val fTarg = if (it.fat.target > 0) it.fat.target.toInt() else 70
                        tvFatVal.text = "${fCurr}/${fTarg}g"
                        pbFat.progress = ((fCurr.toDouble() / fTarg) * 100).toInt().coerceIn(0, 100)

                        val formattedWater = String.format(Locale.US, "%.1f/%.1fL", it.water.current, it.water.target)
                        tvWaterVal.text = formattedWater
                        tvHydrationProgress.text = formattedWater
                        val wTarg = if (it.water.target > 0) it.water.target else 3.0
                        pbWater.progress = ((it.water.current / wTarg) * 100).toInt().coerceIn(0, 100)

                        // Update timeline meals
                        updateTimelineMeals(it.mealsLogged)
                    }
                } else {
                    android.util.Log.e("FITSYNC_NETWORK", "Failed to sync nutrition: ${response.code()}")
                }
            } catch (e: Exception) {
                android.util.Log.e("FITSYNC_NETWORK", "Exception in loadDashboardData", e)
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

    private fun loadGeminiAiInsight() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getAiInsight()
                if (response.isSuccessful && response.body()?.success == true) {
                    val insight = response.body()?.insight
                    tvAiInsightText.text = insight ?: "Keep pushing towards your daily fitness goals!"
                }
            } catch (e: Exception) {
                // Keep default banner text
            }
        }
    }

    private fun logWaterToBackend(amountMl: Int) {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.addWater(AddWaterRequest(amountMl = amountMl, date = todayStr))

                if (response.isSuccessful) {
                    Toast.makeText(this@NutritionActivity, "+${amountMl}ml Water Logged! 💧", Toast.LENGTH_SHORT).show()
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
        Toast.makeText(this, "Scanning food with Gemini AI... 🧠📸", Toast.LENGTH_SHORT).show()

        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        val imageBytes = outputStream.toByteArray()
        val base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP)

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.scanFoodImage(ScanFoodRequest(imageBase64 = base64Image))

                if (response.isSuccessful && response.body()?.success == true) {
                    val data = response.body()!!
                    Toast.makeText(
                        this@NutritionActivity,
                        "Detected: ${data.foodName} (${data.calories} kcal, ${data.protein}g Protein)",
                        Toast.LENGTH_LONG
                    ).show()

                    loadDashboardData()
                } else {
                    Toast.makeText(this@NutritionActivity, "AI couldn't detect food clearly", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@NutritionActivity, "Failed to analyze photo", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openFoodSearch(mealType: String) {
        val intent = Intent(this, FoodSearchActivity::class.java)
        intent.putExtra("MEAL_TYPE", mealType)
        startActivity(intent)
    }

    private fun openAiCoachActivity() {
        val intent = Intent(this, AiCoachActivity::class.java)
        startActivity(intent)
    }
}