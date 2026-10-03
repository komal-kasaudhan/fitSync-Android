package com.esha.fitsync.screens.fragments

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.esha.fitsync.R
import com.esha.fitsync.api.RetrofitClient
import com.esha.fitsync.data.AppCacheManager
import com.esha.fitsync.models.HomeRecommendedMeal
import com.esha.fitsync.models.HomeSummaryResponse
import com.esha.fitsync.screens.Adapters.HomeRecommendedMealAdapter
import com.esha.fitsync.screens.LoginActivity
import com.esha.fitsync.screens.Nutrition.MealTypeBottomSheetFragment
import com.esha.fitsync.screens.Nutrition.NutritionActivity
import com.esha.fitsync.screens.workout.WorkoutDetailActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class HomeFragment : Fragment() {

    private var srlHomeRefresh: SwipeRefreshLayout? = null
    private var llHomeErrorBanner: View? = null
    private var tvHomeErrorMessage: TextView? = null
    private var btnRetryHome: TextView? = null

    private var tvWelcomeUser: TextView? = null

    // Module 1: Wellness / Recovery
    private var tvRecoveryPercent: TextView? = null
    private var tvRecoveryLabel: TextView? = null
    private var tvHomeWeightStat: TextView? = null
    private var tvHomeWorkoutFocusStat: TextView? = null
    private var tvHomeStreakStat: TextView? = null
    private var tvHomeCalorieGoalStat: TextView? = null
    private var tvHomeAiQuote: TextView? = null

    // Module 2: Today's Workout
    private var tvHomeWorkoutTitle: TextView? = null
    private var tvHomeWorkoutMeta: TextView? = null
    private var tvHomeWorkoutProgressText: TextView? = null
    private var pbHomeWorkout: ProgressBar? = null
    private var btnContinueWorkoutHome: MaterialButton? = null

    // Module 3: Nutrition
    private var tvHomeCalorieSummary: TextView? = null
    private var pbHomeProtein: ProgressBar? = null
    private var tvHomeProteinPercent: TextView? = null
    private var pbHomeCarbs: ProgressBar? = null
    private var tvHomeCarbsPercent: TextView? = null
    private var pbHomeWater: ProgressBar? = null
    private var tvHomeWaterPercent: TextView? = null
    private var ivCheckBreakfast: ImageView? = null
    private var ivCheckLunch: ImageView? = null
    private var ivCheckDinner: ImageView? = null

    // Module 5: Recommended Meals
    private var rvRecommendedProducts: RecyclerView? = null
    private var recommendedAdapter: HomeRecommendedMealAdapter? = null

    // Module 6: Gyms Near You
    private var homeGymAdapter: com.esha.fitsync.screens.Adapters.HomeGymAdapter? = null

    // Module 7: Streaks & Stats
    private var tvHomeStreakDays: TextView? = null
    private var tvHomeSessionsCompleted: TextView? = null
    private var tvHomeWeightGoalPercent: TextView? = null
    private var tvHomeConsistencyPercent: TextView? = null

    private var currentDayIndex = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupListeners(view)
        observeAppEvents()

        // 1. Instantly display cached data (0ms latency, prevents screen clearing)
        val cached = AppCacheManager.getHomeSummary()
        if (cached != null) {
            bindHomeSummary(cached)
        }

        // 2. Fetch fresh data from backend
        fetchHomeSummaryData()
        fetchNearbyGymsForHome()
    }

    override fun onResume() {
        super.onResume()
        // Refresh silently in background, keeping current data visible
        fetchHomeSummaryData()
        fetchNearbyGymsForHome()
    }

    private fun initViews(view: View) {
        srlHomeRefresh = view.findViewById(R.id.srlHomeRefresh)
        llHomeErrorBanner = view.findViewById(R.id.llHomeErrorBanner)
        tvHomeErrorMessage = view.findViewById(R.id.tvHomeErrorMessage)
        btnRetryHome = view.findViewById(R.id.btnRetryHome)

        tvWelcomeUser = view.findViewById(R.id.tvWelcomeUser)

        // Module 1
        tvRecoveryPercent = view.findViewById(R.id.tvRecoveryPercent)
        tvRecoveryLabel = view.findViewById(R.id.tvRecoveryLabel)
        tvHomeWeightStat = view.findViewById(R.id.tvHomeWeightStat)
        tvHomeWorkoutFocusStat = view.findViewById(R.id.tvHomeWorkoutFocusStat)
        tvHomeStreakStat = view.findViewById(R.id.tvHomeStreakStat)
        tvHomeCalorieGoalStat = view.findViewById(R.id.tvHomeCalorieGoalStat)
        tvHomeAiQuote = view.findViewById(R.id.tvHomeAiQuote)

        // Module 2
        tvHomeWorkoutTitle = view.findViewById(R.id.tvHomeWorkoutTitle)
        tvHomeWorkoutMeta = view.findViewById(R.id.tvHomeWorkoutMeta)
        tvHomeWorkoutProgressText = view.findViewById(R.id.tvHomeWorkoutProgressText)
        pbHomeWorkout = view.findViewById(R.id.pbHomeWorkout)
        btnContinueWorkoutHome = view.findViewById(R.id.btnContinueWorkoutHome)

        // Module 3
        tvHomeCalorieSummary = view.findViewById(R.id.tvHomeCalorieSummary)
        pbHomeProtein = view.findViewById(R.id.pbHomeProtein)
        tvHomeProteinPercent = view.findViewById(R.id.tvHomeProteinPercent)
        pbHomeCarbs = view.findViewById(R.id.pbHomeCarbs)
        tvHomeCarbsPercent = view.findViewById(R.id.tvHomeCarbsPercent)
        pbHomeWater = view.findViewById(R.id.pbHomeWater)
        tvHomeWaterPercent = view.findViewById(R.id.tvHomeWaterPercent)
        ivCheckBreakfast = view.findViewById(R.id.ivCheckBreakfast)
        ivCheckLunch = view.findViewById(R.id.ivCheckLunch)
        ivCheckDinner = view.findViewById(R.id.ivCheckDinner)

        // Module 5: Recommended Meals
        rvRecommendedProducts = view.findViewById(R.id.rvRecommendedProducts)
        rvRecommendedProducts?.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        recommendedAdapter = HomeRecommendedMealAdapter(emptyList()) { meal ->
            logQuickRecommendedMeal(meal)
        }
        rvRecommendedProducts?.adapter = recommendedAdapter

        // Module 6: Real Nearby Gyms
        val rvHomeNearbyGyms = view.findViewById<RecyclerView>(R.id.rvHomeNearbyGyms)
        rvHomeNearbyGyms?.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        homeGymAdapter = com.esha.fitsync.screens.Adapters.HomeGymAdapter { gym ->
            val intent = Intent(requireContext(), com.esha.fitsync.screens.gym.GymDetailActivity::class.java).apply {
                putExtra("GYM_ID", gym.id)
            }
            startActivity(intent)
        }
        rvHomeNearbyGyms?.adapter = homeGymAdapter

        // Module 7: Streaks & Stats
        tvHomeStreakDays = view.findViewById(R.id.tvHomeStreakDays)
        tvHomeSessionsCompleted = view.findViewById(R.id.tvHomeSessionsCompleted)
        tvHomeWeightGoalPercent = view.findViewById(R.id.tvHomeWeightGoalPercent)
        tvHomeConsistencyPercent = view.findViewById(R.id.tvHomeConsistencyPercent)
    }

    private fun setupListeners(view: View) {
        srlHomeRefresh?.setOnRefreshListener {
            fetchHomeSummaryData()
        }

        btnRetryHome?.setOnClickListener {
            llHomeErrorBanner?.visibility = View.GONE
            fetchHomeSummaryData()
        }

        val btnManageNutrition = view.findViewById<Button>(R.id.btnManageNutrition)
        btnManageNutrition?.setOnClickListener {
            val intent = Intent(requireContext(), NutritionActivity::class.java)
            startActivity(intent)
        }

        view.findViewById<Button>(R.id.btnAddMealHome)?.setOnClickListener {
            val modal = MealTypeBottomSheetFragment()
            modal.show(childFragmentManager, "MealTypeBottomSheet")
        }

        view.findViewById<Button>(R.id.btnScanFoodHome)?.setOnClickListener {
            val intent = Intent(requireContext(), NutritionActivity::class.java)
            startActivity(intent)
        }

        btnContinueWorkoutHome?.setOnClickListener {
            val intent = Intent(requireContext(), WorkoutDetailActivity::class.java).apply {
                putExtra("DAY_INDEX", currentDayIndex)
                putExtra("dayIndex", currentDayIndex)
            }
            startActivity(intent)
        }

        view.findViewById<View>(R.id.btnProfile)?.setOnClickListener {
            showSignOutConfirmationDialog()
        }

        view.findViewById<Button>(R.id.btnSignOut)?.setOnClickListener {
            showSignOutConfirmationDialog()
        }

        view.findViewById<View>(R.id.tvExploreAllGyms)?.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, com.esha.fitsync.screens.gym.NearbyGymsFragment())
                .addToBackStack("HOME_TO_NEARBY")
                .commit()
        }
    }

    private fun fetchNearbyGymsForHome() {
        viewLifecycleOwner.lifecycleScope.launch {
            if (!isAdded || view == null) return@launch
            try {
                // Query Bokaro fallback or nearby gyms (lat: 23.6693, lng: 86.1511, radiusKm: 10.0)
                val response = RetrofitClient.api.getNearbyGyms(lat = 23.6693, lng = 86.1511, radiusKm = 10.0)
                if (response.isSuccessful && response.body() != null) {
                    val gyms = response.body()!!.gyms
                    homeGymAdapter?.submitList(gyms)
                }
            } catch (e: Exception) {
                android.util.Log.e("FitSyncApi", "Home nearby gyms error", e)
            }
        }
    }

    private fun observeAppEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            AppCacheManager.refreshTrigger.collect {
                if (isAdded && view != null) {
                    fetchHomeSummaryData()
                }
            }
        }
    }

    private fun fetchHomeSummaryData() {
        viewLifecycleOwner.lifecycleScope.launch {
            if (!isAdded || view == null) return@launch
            try {
                val response = RetrofitClient.api.getHomeSummary()
                if (!isAdded || view == null) return@launch

                if (response.isSuccessful && response.body() != null) {
                    val summary = response.body()!!
                    AppCacheManager.saveHomeSummary(summary)
                    bindHomeSummary(summary)
                    llHomeErrorBanner?.visibility = View.GONE
                } else {
                    // On failure, keep previous data visible!
                    val code = response.code()
                    android.util.Log.w("FitSyncApi", "Home summary returned status $code")
                    showErrorBanner("Server response: $code. Using cached data.")
                }
            } catch (e: Exception) {
                if (!isAdded || view == null) return@launch
                android.util.Log.e("FitSyncApi", "Home summary network error", e)
                val isConnectionError = e is java.net.ConnectException || e is java.net.SocketTimeoutException
                val friendlyMessage = if (isConnectionError) {
                    "Cannot reach server. Check USB cable / adb reverse (Tap to retry)"
                } else {
                    "Failed to refresh: ${e.localizedMessage ?: "Unknown error"}"
                }
                showErrorBanner(friendlyMessage)
            } finally {
                srlHomeRefresh?.isRefreshing = false
            }
        }
    }

    private fun showErrorBanner(message: String) {
        tvHomeErrorMessage?.text = message
        llHomeErrorBanner?.visibility = View.VISIBLE
    }

    private fun bindHomeSummary(summary: HomeSummaryResponse) {
        // 1. User Greeting
        val savedName = requireActivity().getSharedPreferences("FitSync", Context.MODE_PRIVATE)
            .getString("USER_NAME", null)
        val name = summary.user?.name?.ifBlank { null } ?: savedName ?: "Athlete"
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val greeting = when {
            hour < 12 -> "Good Morning"
            hour < 17 -> "Good Afternoon"
            else -> "Good Evening"
        }
        tvWelcomeUser?.text = "$greeting, $name 👋"

        // 2. Module 1: Wellness / Recovery
        val progressPercent = if (summary.weight != null && summary.weight.weightProgressPercent > 0) {
            summary.weight.weightProgressPercent
        } else {
            summary.weeklyWorkout?.completionPercent ?: 0
        }
        tvRecoveryPercent?.text = "$progressPercent%"

        val currWeight = summary.weight?.currentWeightKg ?: 0.0
        val targetWeight = summary.weight?.targetWeightKg ?: 0.0
        tvHomeWeightStat?.text = if (currWeight > 0) String.format(Locale.US, "%.1f kg", currWeight) else "-- kg"

        val todayWorkout = summary.todayWorkout
        if (todayWorkout != null) {
            currentDayIndex = todayWorkout.dayIndex
            val focusTitle = if (todayWorkout.isRestDay) "Rest Day" else todayWorkout.focus.ifBlank { "Workout" }
            tvHomeWorkoutFocusStat?.text = focusTitle
        }

        val streak = summary.weeklyWorkout?.streakDays ?: 0
        tvHomeStreakStat?.text = "$streak Days"

        val calConsumed = summary.todayNutrition?.calories?.consumed ?: 0
        val calTarget = summary.todayNutrition?.calories?.target ?: 2000
        val calHit = calConsumed >= calTarget && calTarget > 0
        tvHomeCalorieGoalStat?.text = if (calHit) "Hit ✓" else "$calConsumed kcal"

        val insightMsg = summary.insight?.message
        if (!insightMsg.isNullOrBlank()) {
            tvHomeAiQuote?.text = "\"$insightMsg\""
        } else {
            tvHomeAiQuote?.text = "\"Stay consistent today. Every workout counts.\""
        }

        // 3. Module 2: Today's Workout Card
        if (todayWorkout != null) {
            val title = if (todayWorkout.isRestDay) "Rest & Recovery" else todayWorkout.focus.ifBlank { "Daily Routine" }
            tvHomeWorkoutTitle?.text = title

            val dur = if (todayWorkout.durationMin > 0) todayWorkout.durationMin else 45
            val cals = if (todayWorkout.calories > 0) todayWorkout.calories else 300
            tvHomeWorkoutMeta?.text = "${dur}m • ${cals} kcal • ${todayWorkout.dayName.ifBlank { "Today" }}"

            if (todayWorkout.completed) {
                tvHomeWorkoutProgressText?.text = "Completed ✓"
                pbHomeWorkout?.progress = 100
                btnContinueWorkoutHome?.text = "✓ Completed Today"
                btnContinueWorkoutHome?.isEnabled = true
            } else if (todayWorkout.isRestDay) {
                tvHomeWorkoutProgressText?.text = "Active Recovery"
                pbHomeWorkout?.progress = 100
                btnContinueWorkoutHome?.text = "View Recovery Guide"
                btnContinueWorkoutHome?.isEnabled = true
            } else {
                tvHomeWorkoutProgressText?.text = "Ready to start"
                pbHomeWorkout?.progress = 0
                btnContinueWorkoutHome?.text = "▶ Start $title"
                btnContinueWorkoutHome?.isEnabled = true
            }
        } else {
            tvHomeWorkoutTitle?.text = "Fitness Profile Setup"
            tvHomeWorkoutMeta?.text = "Complete your profile to generate personalized routines"
            tvHomeWorkoutProgressText?.text = "Pending"
            pbHomeWorkout?.progress = 0
            btnContinueWorkoutHome?.text = "Complete Your Profile"
            btnContinueWorkoutHome?.isEnabled = true
            btnContinueWorkoutHome?.setOnClickListener {
                startActivity(Intent(requireContext(), com.esha.fitsync.screens.OnboardingActivity::class.java))
            }
        }

        // 4. Module 3: Nutrition Ring & Macros
        val nut = summary.todayNutrition
        if (nut != null) {
            val eatenCal = nut.calories?.consumed ?: 0
            val targetCalVal = if ((nut.calories?.target ?: 0) > 0) nut.calories!!.target else 2000
            tvHomeCalorieSummary?.text = "${String.format("%,d", eatenCal)} / ${String.format("%,d", targetCalVal)} kcal"

            // Protein
            val pConsumed = nut.protein?.consumed ?: 0.0
            val pTarget = if ((nut.protein?.target ?: 0.0) > 0) nut.protein!!.target else 120.0
            val pPct = ((pConsumed / pTarget) * 100).toInt().coerceIn(0, 100)
            pbHomeProtein?.progress = pPct
            tvHomeProteinPercent?.text = "$pPct%"

            // Carbs
            val cConsumed = nut.carbs?.consumed ?: 0.0
            val cTarget = if ((nut.carbs?.target ?: 0.0) > 0) nut.carbs!!.target else 250.0
            val cPct = ((cConsumed / cTarget) * 100).toInt().coerceIn(0, 100)
            pbHomeCarbs?.progress = cPct
            tvHomeCarbsPercent?.text = "$cPct%"

            // Water
            val wConsumed = nut.water?.consumedLiters ?: 0.0
            val wTarget = if ((nut.water?.targetLiters ?: 0.0) > 0) nut.water!!.targetLiters else 3.0
            val wPct = ((wConsumed / wTarget) * 100).toInt().coerceIn(0, 100)
            pbHomeWater?.progress = wPct
            tvHomeWaterPercent?.text = "$wPct%"

            // Checkboxes
            updateMealCheck(ivCheckBreakfast, eatenCal > 0)
            updateMealCheck(ivCheckLunch, eatenCal > 600)
            updateMealCheck(ivCheckDinner, eatenCal > 1200)
        }

        // 5. Module 5: Recommended Meals
        val meals = summary.recommendedMeals
        if (meals.isNotEmpty()) {
            recommendedAdapter?.submitList(meals)
        }

        // 6. Module 7: Streaks & Stats
        val weekly = summary.weeklyWorkout
        if (weekly != null) {
            tvHomeStreakDays?.text = "${weekly.streakDays} Days"
            tvHomeSessionsCompleted?.text = "${weekly.sessionsCompleted}/${weekly.sessionsPlanned} Done"
            tvHomeConsistencyPercent?.text = "${weekly.completionPercent}%"
        }
        val weightGoalPct = summary.weight?.weightProgressPercent ?: 0
        tvHomeWeightGoalPercent?.text = "$weightGoalPct%"
    }

    private fun updateMealCheck(iv: ImageView?, isChecked: Boolean) {
        if (iv == null) return
        if (isChecked) {
            iv.setImageResource(android.R.drawable.checkbox_on_background)
            iv.imageTintList = ColorStateList.valueOf(Color.parseColor("#10B981"))
        } else {
            iv.setImageResource(android.R.drawable.checkbox_off_background)
            iv.imageTintList = ColorStateList.valueOf(Color.parseColor("#9CA3AF"))
        }
    }

    private fun logQuickRecommendedMeal(meal: HomeRecommendedMeal) {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val searchRes = RetrofitClient.apiService.searchFoods(meal.name)
                val foodId = searchRes.body()?.data?.firstOrNull()?.effectiveId ?: meal.id
                val req = com.esha.fitsync.api.AddMealRequest(
                    date = todayStr,
                    mealType = meal.mealType.ifBlank { "Lunch" },
                    foodId = foodId,
                    quantity = 1.0
                )
                val res = RetrofitClient.apiService.addMeal(req)
                if (res.isSuccessful) {
                    Toast.makeText(requireContext(), "Logged ${meal.name}! 🍲", Toast.LENGTH_SHORT).show()
                    AppCacheManager.notifyDataChanged()
                } else {
                    Toast.makeText(requireContext(), "Logged meal to daily intake!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Unable to log meal: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showSignOutConfirmationDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Sign Out")
            .setMessage("Are you sure you want to sign out of FitSync?")
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .setPositiveButton("Sign Out") { dialog, _ ->
                dialog.dismiss()
                performSignOut()
            }
            .show()
    }

    private fun performSignOut() {
        val sharedPreferences = requireActivity().getSharedPreferences("FitSync", Context.MODE_PRIVATE)
        sharedPreferences.edit().clear().apply()
        Toast.makeText(requireContext(), "Signed out successfully", Toast.LENGTH_SHORT).show()

        val intent = Intent(requireContext(), LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        requireActivity().finish()
    }
}