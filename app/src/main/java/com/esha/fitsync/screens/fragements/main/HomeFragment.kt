package com.esha.fitsync.screens.fragments

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.esha.fitsync.BuildConfig
import com.esha.fitsync.R
import com.esha.fitsync.api.RetrofitClient
import com.esha.fitsync.data.AppCacheManager
import com.esha.fitsync.models.GymItem
import com.esha.fitsync.models.HomeSummaryResponse
import com.esha.fitsync.screens.Adapters.HomeGymAdapter
import com.esha.fitsync.screens.Nutrition.MealTypeBottomSheetFragment
import com.esha.fitsync.screens.Nutrition.NutritionActivity
import com.esha.fitsync.screens.ProfileActivity
import com.esha.fitsync.screens.gym.GymDetailActivity
import com.esha.fitsync.screens.gym.LocationHelper
import com.esha.fitsync.screens.gym.NearbyGymsFragment
import com.esha.fitsync.screens.workout.WorkoutDetailActivity
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.progressindicator.CircularProgressIndicator
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

class HomeFragment : Fragment() {

    private var srlHomeRefresh: SwipeRefreshLayout? = null
    private var llHomeErrorBanner: View? = null
    private var tvHomeErrorMessage: TextView? = null
    private var btnRetryHome: TextView? = null

    // 1. Header & Greeting
    private var btnProfile: ShapeableImageView? = null
    private var tvWelcomeUser: TextView? = null

    // 2. Nutrition
    private var tvHomeCalorieSummary: TextView? = null
    private var pbHomeProtein: CircularProgressIndicator? = null
    private var tvHomeProteinPercent: TextView? = null
    private var pbHomeCarbs: CircularProgressIndicator? = null
    private var tvHomeCarbsPercent: TextView? = null
    private var pbHomeWater: CircularProgressIndicator? = null
    private var tvHomeWaterPercent: TextView? = null
    private var ivCheckBreakfast: ImageView? = null
    private var ivCheckLunch: ImageView? = null
    private var ivCheckDinner: ImageView? = null

    // 3. Today's Workout
    private var tvHomeWorkoutTitle: TextView? = null
    private var tvHomeWorkoutMeta: TextView? = null
    private var tvHomeWorkoutProgressText: TextView? = null
    private var pbHomeWorkout: ProgressBar? = null
    private var btnContinueWorkoutHome: MaterialButton? = null

    // 4. Gyms Near You
    private var rvHomeNearbyGyms: RecyclerView? = null
    private var shimmerHomeGyms: ShimmerFrameLayout? = null
    private var llHomeGymEmptyState: LinearLayout? = null
    private var llHomeGymLocationDenied: LinearLayout? = null
    private var homeGymAdapter: HomeGymAdapter? = null

    private var currentDayIndex = 0

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fine = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarse = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fine || coarse) {
            llHomeGymLocationDenied?.visibility = View.GONE
            fetchNearbyGymsForHome()
        } else {
            checkLocationPermissionState()
        }
    }

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

        // 1. Instantly display cached data
        val cached = AppCacheManager.getHomeSummary()
        if (cached != null) {
            bindHomeSummary(cached)
        }
        updateUserHeader()

        val cachedGyms = AppCacheManager.get<List<GymItem>>("HOME_NEARBY_GYMS")
        if (!cachedGyms.isNullOrEmpty()) {
            homeGymAdapter?.submitList(cachedGyms)
            rvHomeNearbyGyms?.visibility = View.VISIBLE
            shimmerHomeGyms?.visibility = View.GONE
        }

        // 2. Fetch fresh data from backend
        fetchHomeSummaryData()
        fetchNearbyGymsForHome()
    }

    override fun onResume() {
        super.onResume()
        // Refresh greeting and avatar immediately if modified in ProfileActivity
        updateUserHeader()
        fetchHomeSummaryData()
        fetchNearbyGymsForHome()
    }

    private fun initViews(view: View) {
        srlHomeRefresh = view.findViewById(R.id.srlHomeRefresh)
        llHomeErrorBanner = view.findViewById(R.id.llHomeErrorBanner)
        tvHomeErrorMessage = view.findViewById(R.id.tvHomeErrorMessage)
        btnRetryHome = view.findViewById(R.id.btnRetryHome)

        // Header & Greeting
        btnProfile = view.findViewById(R.id.btnProfile)
        tvWelcomeUser = view.findViewById(R.id.tvWelcomeUser)

        // Nutrition
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

        // Today's Workout
        tvHomeWorkoutTitle = view.findViewById(R.id.tvHomeWorkoutTitle)
        tvHomeWorkoutMeta = view.findViewById(R.id.tvHomeWorkoutMeta)
        tvHomeWorkoutProgressText = view.findViewById(R.id.tvHomeWorkoutProgressText)
        pbHomeWorkout = view.findViewById(R.id.pbHomeWorkout)
        btnContinueWorkoutHome = view.findViewById(R.id.btnContinueWorkoutHome)

        // Gyms Near You
        rvHomeNearbyGyms = view.findViewById(R.id.rvHomeNearbyGyms)
        shimmerHomeGyms = view.findViewById(R.id.shimmerHomeGyms)
        llHomeGymEmptyState = view.findViewById(R.id.llHomeGymEmptyState)
        llHomeGymLocationDenied = view.findViewById(R.id.llHomeGymLocationDenied)

        rvHomeNearbyGyms?.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        homeGymAdapter = HomeGymAdapter { gym ->
            val intent = Intent(requireContext(), GymDetailActivity::class.java).apply {
                putExtra("GYM_ID", gym.id)
            }
            startActivity(intent)
        }
        rvHomeNearbyGyms?.adapter = homeGymAdapter
    }

    private fun setupListeners(view: View) {
        srlHomeRefresh?.setOnRefreshListener {
            fetchHomeSummaryData()
            fetchNearbyGymsForHome()
        }

        btnRetryHome?.setOnClickListener {
            llHomeErrorBanner?.visibility = View.GONE
            fetchHomeSummaryData()
            fetchNearbyGymsForHome()
        }

        btnProfile?.setOnClickListener {
            startActivity(Intent(requireContext(), ProfileActivity::class.java))
        }

        view.findViewById<Button>(R.id.btnManageNutrition)?.setOnClickListener {
            startActivity(Intent(requireContext(), NutritionActivity::class.java))
        }

        view.findViewById<Button>(R.id.btnAddMealHome)?.setOnClickListener {
            val modal = MealTypeBottomSheetFragment()
            modal.show(childFragmentManager, "MealTypeBottomSheet")
        }

        view.findViewById<Button>(R.id.btnScanFoodHome)?.setOnClickListener {
            startActivity(Intent(requireContext(), NutritionActivity::class.java))
        }

        btnContinueWorkoutHome?.setOnClickListener {
            val intent = Intent(requireContext(), WorkoutDetailActivity::class.java).apply {
                putExtra("DAY_INDEX", currentDayIndex)
                putExtra("dayIndex", currentDayIndex)
            }
            startActivity(intent)
        }

        view.findViewById<View>(R.id.tvExploreAllGyms)?.setOnClickListener {
            navigateToNearbyGyms()
        }

        view.findViewById<View>(R.id.btnEmptySeeAllGyms)?.setOnClickListener {
            navigateToNearbyGyms()
        }

        view.findViewById<View>(R.id.btnEnableLocationHome)?.setOnClickListener {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun navigateToNearbyGyms() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, NearbyGymsFragment())
            .addToBackStack("HOME_TO_NEARBY")
            .commit()
    }

    private fun updateUserHeader() {
        val prefs = requireActivity().getSharedPreferences("FitSync", Context.MODE_PRIVATE)
        val name = prefs.getString("USER_NAME", null)?.ifBlank { null } ?: "Athlete"
        val avatar = prefs.getString("USER_AVATAR", null)

        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val timeGreeting = when {
            hour < 12 -> "Good Morning"
            hour < 17 -> "Good Afternoon"
            else -> "Good Evening"
        }
        tvWelcomeUser?.text = "$timeGreeting, $name"

        btnProfile?.let { iv ->
            val initialsBitmap = createInitialsBitmap(name, 40)
            val initialsDrawable = android.graphics.drawable.BitmapDrawable(resources, initialsBitmap)
            if (!avatar.isNullOrBlank()) {
                val fullUrl = if (avatar.startsWith("http")) {
                    avatar
                } else {
                    "${BuildConfig.BASE_URL.trimEnd('/')}/${avatar.trimStart('/')}"
                }
                Glide.with(this)
                    .load(fullUrl)
                    .transform(CircleCrop())
                    .placeholder(initialsDrawable)
                    .error(initialsDrawable)
                    .into(iv)
            } else {
                iv.setImageDrawable(initialsDrawable)
            }
        }
    }

    private fun checkLocationPermissionState() {
        val hasPermission = LocationHelper.hasLocationPermission(requireContext())
        if (!hasPermission) {
            val cachedGyms = AppCacheManager.get<List<GymItem>>("HOME_NEARBY_GYMS")
            if (cachedGyms.isNullOrEmpty()) {
                llHomeGymLocationDenied?.visibility = View.VISIBLE
                rvHomeNearbyGyms?.visibility = View.GONE
                llHomeGymEmptyState?.visibility = View.GONE
                shimmerHomeGyms?.visibility = View.GONE
            }
        } else {
            llHomeGymLocationDenied?.visibility = View.GONE
        }
    }

    private fun fetchNearbyGymsForHome() {
        viewLifecycleOwner.lifecycleScope.launch {
            if (!isAdded || view == null) return@launch

            val hasPermission = LocationHelper.hasLocationPermission(requireContext())
            if (!hasPermission) {
                checkLocationPermissionState()
                return@launch
            }

            llHomeGymLocationDenied?.visibility = View.GONE

            val cachedGyms = AppCacheManager.get<List<GymItem>>("HOME_NEARBY_GYMS")
            if (cachedGyms.isNullOrEmpty()) {
                shimmerHomeGyms?.visibility = View.VISIBLE
                shimmerHomeGyms?.startShimmer()
                rvHomeNearbyGyms?.visibility = View.GONE
                llHomeGymEmptyState?.visibility = View.GONE
            }

            try {
                // Get real location or default coordinate with 5 km radius
                val loc = LocationHelper.getDeviceLocation(requireContext())
                val lat = loc?.latitude ?: LocationHelper.DEFAULT_LAT
                val lng = loc?.longitude ?: LocationHelper.DEFAULT_LNG

                val response = RetrofitClient.api.getNearbyGyms(
                    lat = lat,
                    lng = lng,
                    radiusKm = 5.0
                )

                if (response.isSuccessful && response.body() != null) {
                    val gyms = response.body()!!.gyms
                    AppCacheManager.put("HOME_NEARBY_GYMS", gyms)

                    if (gyms.isEmpty()) {
                        rvHomeNearbyGyms?.visibility = View.GONE
                        llHomeGymEmptyState?.visibility = View.VISIBLE
                    } else {
                        homeGymAdapter?.submitList(gyms)
                        rvHomeNearbyGyms?.visibility = View.VISIBLE
                        llHomeGymEmptyState?.visibility = View.GONE
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("FitSyncApi", "Home nearby gyms error", e)
            } finally {
                shimmerHomeGyms?.stopShimmer()
                shimmerHomeGyms?.visibility = View.GONE
            }
        }
    }

    private fun observeAppEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            AppCacheManager.refreshTrigger.collect {
                if (isAdded && view != null) {
                    updateUserHeader()
                    fetchHomeSummaryData()
                    fetchNearbyGymsForHome()
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
                    val code = response.code()
                    showErrorBanner("Server response: $code. Using cached data.")
                }
            } catch (e: Exception) {
                if (!isAdded || view == null) return@launch
                showErrorBanner("Cannot reach server. Using cached data.")
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
        tvWelcomeUser?.text = "$greeting, $name"

        // 2. Nutrition Card: Set final progress directly (no looping animation)
        val nut = summary.todayNutrition
        if (nut != null) {
            val eatenCal = nut.calories?.consumed ?: 0
            val targetCalVal = if ((nut.calories?.target ?: 0) > 0) nut.calories!!.target else 2000
            tvHomeCalorieSummary?.text = "${String.format("%,d", eatenCal)} / ${String.format("%,d", targetCalVal)} kcal"

            // Protein
            val pConsumed = nut.protein?.consumed ?: 0.0
            val pTarget = if ((nut.protein?.target ?: 0.0) > 0) nut.protein!!.target else 120.0
            val pPct = ((pConsumed / pTarget) * 100).toInt().coerceIn(0, 100)
            pbHomeProtein?.setProgressCompat(pPct, false)
            tvHomeProteinPercent?.text = "$pPct%"

            // Carbs
            val cConsumed = nut.carbs?.consumed ?: 0.0
            val cTarget = if ((nut.carbs?.target ?: 0.0) > 0) nut.carbs!!.target else 250.0
            val cPct = ((cConsumed / cTarget) * 100).toInt().coerceIn(0, 100)
            pbHomeCarbs?.setProgressCompat(cPct, false)
            tvHomeCarbsPercent?.text = "$cPct%"

            // Water
            val wConsumed = nut.water?.consumedLiters ?: 0.0
            val wTarget = if ((nut.water?.targetLiters ?: 0.0) > 0) nut.water!!.targetLiters else 3.0
            val wPct = ((wConsumed / wTarget) * 100).toInt().coerceIn(0, 100)
            pbHomeWater?.setProgressCompat(wPct, false)
            tvHomeWaterPercent?.text = "$wPct%"

            // Meal checkboxes with theme colors
            updateMealCheck(ivCheckBreakfast, eatenCal > 0)
            updateMealCheck(ivCheckLunch, eatenCal > 600)
            updateMealCheck(ivCheckDinner, eatenCal > 1200)

            // Synchronize with DailyTargetsRepository so all screens use one data source
            com.esha.fitsync.data.repository.DailyTargetsRepository.updateNutrition(
                proteinConsumed = pConsumed,
                proteinTarget = pTarget,
                waterConsumedLiters = wConsumed,
                waterTargetLiters = wTarget,
                caloriesConsumed = eatenCal,
                caloriesTarget = targetCalVal
            )

            android.util.Log.d(
                "FitSyncSync",
                "Home Screen -> Protein: ${pConsumed.toInt()}/${pTarget.toInt()}g (${pPct}%), Water: ${String.format(Locale.US, "%.1f/%.1fL", wConsumed, wTarget)} (${wPct}%)"
            )
        } else {
            val cached = com.esha.fitsync.data.repository.DailyTargetsRepository.getCachedTargets()
            val pConsumed = cached.proteinConsumed
            val pTarget = if (cached.proteinTarget > 0) cached.proteinTarget else 120.0
            val pPct = cached.proteinPercent
            pbHomeProtein?.setProgressCompat(pPct, false)
            tvHomeProteinPercent?.text = "$pPct%"

            val wConsumed = cached.waterConsumedLiters
            val wTarget = if (cached.waterTargetLiters > 0) cached.waterTargetLiters else 3.0
            val wPct = cached.waterPercent
            pbHomeWater?.setProgressCompat(wPct, false)
            tvHomeWaterPercent?.text = "$wPct%"

            android.util.Log.d(
                "FitSyncSync",
                "Home Screen (Cached) -> Protein: ${pConsumed.toInt()}/${pTarget.toInt()}g (${pPct}%), Water: ${String.format(Locale.US, "%.1f/%.1fL", wConsumed, wTarget)} (${wPct}%)"
            )
        }

        val weekly = summary.weeklyWorkout
        if (weekly != null) {
            val sDone = weekly.sessionsCompleted
            val sPlan = if (weekly.sessionsPlanned > 0) weekly.sessionsPlanned else 5
            com.esha.fitsync.data.repository.DailyTargetsRepository.updateWeeklyGoal(sDone, sPlan)
        }

        // 3. Today's Workout Card
        val todayWorkout = summary.todayWorkout
        if (todayWorkout != null) {
            currentDayIndex = todayWorkout.dayIndex
            val title = if (todayWorkout.isRestDay) "Rest & Recovery" else todayWorkout.focus.ifBlank { "Daily Routine" }
            tvHomeWorkoutTitle?.text = title

            val dur = if (todayWorkout.durationMin > 0) todayWorkout.durationMin else 45
            val cals = if (todayWorkout.calories > 0) todayWorkout.calories else 300
            tvHomeWorkoutMeta?.text = "${dur}m • ${cals} kcal"

            if (todayWorkout.completed) {
                tvHomeWorkoutProgressText?.text = "Completed"
                pbHomeWorkout?.progress = 100
                btnContinueWorkoutHome?.text = "Completed today"
                btnContinueWorkoutHome?.isEnabled = true
            } else if (todayWorkout.isRestDay) {
                tvHomeWorkoutProgressText?.text = "Active recovery"
                pbHomeWorkout?.progress = 100
                btnContinueWorkoutHome?.text = "View recovery guide"
                btnContinueWorkoutHome?.isEnabled = true
            } else {
                tvHomeWorkoutProgressText?.text = "Ready to start"
                pbHomeWorkout?.progress = 0
                btnContinueWorkoutHome?.text = "Start workout"
                btnContinueWorkoutHome?.isEnabled = true
            }
        } else {
            tvHomeWorkoutTitle?.text = "Fitness Profile Setup"
            tvHomeWorkoutMeta?.text = "Complete your profile to generate personalized routines"
            tvHomeWorkoutProgressText?.text = "Pending"
            pbHomeWorkout?.progress = 0
            btnContinueWorkoutHome?.text = "Complete profile"
            btnContinueWorkoutHome?.isEnabled = true
        }
    }

    private fun updateMealCheck(iv: ImageView?, isChecked: Boolean) {
        if (iv == null) return
        val context = context ?: return
        if (isChecked) {
            iv.setImageResource(android.R.drawable.checkbox_on_background)
            iv.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.primary))
        } else {
            iv.setImageResource(android.R.drawable.checkbox_off_background)
            iv.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.text_secondary))
        }
    }

    private fun createInitialsBitmap(name: String, sizeDp: Int = 40): Bitmap {
        val px = (sizeDp * Resources.getSystem().displayMetrics.density).toInt()
        val bitmap = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val paintBg = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#334155")
            style = Paint.Style.FILL
        }
        canvas.drawCircle(px / 2f, px / 2f, px / 2f, paintBg)

        val initials = name.trim().split("\\s+".toRegex())
            .filter { it.isNotEmpty() }
            .map { it.first().uppercaseChar() }
            .take(2)
            .joinToString("")
            .ifEmpty { "FS" }

        val paintText = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#F8FAFC")
            textSize = px * 0.40f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        val yPos = (canvas.height / 2f) - ((paintText.descent() + paintText.ascent()) / 2f)
        canvas.drawText(initials, canvas.width / 2f, yPos, paintText)
        return bitmap
    }
}