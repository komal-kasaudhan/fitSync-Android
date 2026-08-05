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


    private lateinit var pbCalorieRing: ProgressBar
    private lateinit var tvKcalEaten: TextView
    private lateinit var tvKcalLeft: TextView
    private lateinit var tvKcalTarget: TextView
    private lateinit var tvAiInsightText: TextView


    private lateinit var tvProteinVal: TextView
    private lateinit var tvCarbsVal: TextView
    private lateinit var tvFatVal: TextView
    private lateinit var tvWaterVal: TextView
    private lateinit var tvHydrationProgress: TextView


    private lateinit var cardAddMeal: MaterialCardView
    private lateinit var cardScan: MaterialCardView
    private lateinit var cardWater: MaterialCardView
    private lateinit var cardAskAi: MaterialCardView


    private lateinit var btnAddLunch: MaterialButton
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
        tvHydrationProgress = findViewById(R.id.tvHydrationProgress)

        cardAddMeal = findViewById(R.id.cardAddMeal)
        cardScan = findViewById(R.id.cardScan)
        cardWater = findViewById(R.id.cardWater)
        cardAskAi = findViewById(R.id.cardAskAi)

        btnAddLunch = findViewById(R.id.btnAddLunch)
        btnAddRecommendedLunch = findViewById(R.id.btnAddRecommendedLunch)
        btnQuickWater = findViewById(R.id.btnQuickAddWater)
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

        btnAddLunch.setOnClickListener { openFoodSearch("LUNCH") }
        btnAddRecommendedLunch.setOnClickListener { openFoodSearch("LUNCH") }
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


                        tvProteinVal.text = "${it.protein.current.toInt()}/${it.protein.target.toInt()}g"
                        tvCarbsVal.text = "${it.carbs.current.toInt()}/${it.carbs.target.toInt()}g"
                        tvFatVal.text = "${it.fat.current.toInt()}/${it.fat.target.toInt()}g"

                        val formattedWater = String.format(Locale.US, "%.1f/%.1fL", it.water.current, it.water.target)
                        tvWaterVal.text = formattedWater
                        tvHydrationProgress.text = formattedWater
                    }
                } else {
                    Toast.makeText(this@NutritionActivity, "Failed to sync today's nutrition", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@NutritionActivity, "Check server connectivity", Toast.LENGTH_SHORT).show()
            }
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

    // 📸 Camera Runtime Permission Handler
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