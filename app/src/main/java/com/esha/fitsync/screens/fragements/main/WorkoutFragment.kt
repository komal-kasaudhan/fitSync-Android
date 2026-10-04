package com.esha.fitsync.screens.fragments

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import com.esha.fitsync.R
import com.esha.fitsync.api.RetrofitClient
import com.esha.fitsync.data.repository.WorkoutRepository
import com.esha.fitsync.databinding.FragmentWorkoutBinding
import com.esha.fitsync.models.*
import com.esha.fitsync.screens.Adapters.WeeklyPlanAdapter
import com.esha.fitsync.screens.bottomsheet.CreateWorkoutPlanBottomSheet
import com.esha.fitsync.screens.bottomsheet.DayRoutineBottomSheet
import com.esha.fitsync.screens.bottomsheet.EquipmentBottomSheetFragment
import com.esha.fitsync.screens.workout.WorkoutDetailActivity
import com.esha.fitsync.viewmodels.WorkoutViewModel
import com.esha.fitsync.viewmodels.WorkoutViewModelFactory
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class WorkoutFragment : Fragment() {

    private var _binding: FragmentWorkoutBinding? = null
    private val binding get() = _binding!!

    private lateinit var workoutViewModel: WorkoutViewModel

    // Active session cache
    private var currentTodaySession: TodayWorkoutSession? = null
    private var snapHelperAttached = false
    private var weeklyPlanAdapter: WeeklyPlanAdapter? = null

    companion object {
        var hasAutoOpenedBottomSheetThisSession = false
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWorkoutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        android.util.Log.d("SheetDebug", "WorkoutFragment.onViewCreated called. hasAutoOpenedBottomSheetThisSession=$hasAutoOpenedBottomSheetThisSession")

        val apiService = RetrofitClient.api
        val repository = WorkoutRepository(apiService)
        val factory = WorkoutViewModelFactory(repository)
        workoutViewModel = ViewModelProvider(requireActivity(), factory)[WorkoutViewModel::class.java]

        setupRecyclerView()
        setupListeners()

        // Instantly display cached targets to prevent blank values
        val cachedTargets = com.esha.fitsync.data.repository.DailyTargetsRepository.getCachedTargets()
        bindDailyTargets(cachedTargets)

        // Decide plan status dynamically from server response instead of local flags
        checkAndLoadWorkoutPlan()

        viewLifecycleOwner.lifecycleScope.launch {
            com.esha.fitsync.data.AppCacheManager.refreshTrigger.collect {
                if (isAdded && _binding != null) {
                    val freshCached = com.esha.fitsync.data.repository.DailyTargetsRepository.getCachedTargets()
                    bindDailyTargets(freshCached)
                    checkAndLoadWorkoutPlan()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (isAdded && _binding != null) {
            val freshCached = com.esha.fitsync.data.repository.DailyTargetsRepository.getCachedTargets()
            bindDailyTargets(freshCached)
            checkAndLoadWorkoutPlan()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupRecyclerView() {
        binding.rvWeeklyPlan.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        if (!snapHelperAttached) {
            val snapHelper = LinearSnapHelper()
            snapHelper.attachToRecyclerView(binding.rvWeeklyPlan)
            snapHelperAttached = true
        }
    }

    private fun setupListeners() {
        binding.srlWorkoutRefresh.setOnRefreshListener {
            checkAndLoadWorkoutPlan()
        }

        binding.btnCreateMyPlan.setOnClickListener {
            openCreatePlanBottomSheet()
        }

        binding.cardLogWeight.setOnClickListener {
            showLogWeightDialog()
        }

        binding.btnEditEquipment.setOnClickListener {
            val sheet = EquipmentBottomSheetFragment(
                onEquipmentSaved = { newEquip ->
                    if (isAdded && _binding != null) {
                        setupAvailableChips(binding.cgAvailableEquipment, newEquip)
                    }
                },
                onPlanRegenerated = {
                    fetchRealWorkoutPlan()
                }
            )
            sheet.show(childFragmentManager, "EquipmentBottomSheet")
        }

        binding.btnGenerateAiPlan.setOnClickListener {
            triggerGenerateAiPlan()
        }

        binding.btnGeneratePlanEmpty.setOnClickListener {
            triggerGenerateAiPlan()
        }

        binding.btnStartWorkout.setOnClickListener {
            handleStartWorkoutClick()
        }

        binding.btnPreviewRoutine.setOnClickListener {
            handleStartWorkoutClick()
        }
    }

    private fun openCreatePlanBottomSheet() {
        if (!isAdded) return
        view?.post {
            if (!isAdded) return@post
            val fm = childFragmentManager
            if (fm.isStateSaved || fm.isDestroyed) return@post
            if (fm.findFragmentByTag("CreateWorkoutPlanBottomSheet") != null) return@post

            android.util.Log.d("SheetDebug", "openCreatePlanBottomSheet: showing CreateWorkoutPlanBottomSheet")
            val bottomSheet = CreateWorkoutPlanBottomSheet(workoutViewModel)
            bottomSheet.isCancelable = true
            bottomSheet.show(fm, "CreateWorkoutPlanBottomSheet")
        }
    }

    private fun checkAndLoadWorkoutPlan() {
        viewLifecycleOwner.lifecycleScope.launch {
            if (!isAdded || _binding == null) return@launch

            val forceDashboard = arguments?.getBoolean("FORCE_LOAD_DASHBOARD", false) == true
            if (forceDashboard) {
                arguments?.remove("FORCE_LOAD_DASHBOARD")
                hasAutoOpenedBottomSheetThisSession = true
                android.util.Log.d("SheetDebug", "Branch taken: DASHBOARD (forced from AI loader)")
                binding.llNoPlanState.visibility = View.GONE
                binding.llWorkoutContent.visibility = View.VISIBLE
                loadWorkoutPlanContent()
                return@launch
            }

            _binding?.srlWorkoutRefresh?.isRefreshing = true
            try {
                val planRes = RetrofitClient.api.getCurrentWorkoutPlan()
                val rawJson = if (planRes.isSuccessful && planRes.body() != null) {
                    planRes.body().toString()
                } else {
                    planRes.errorBody()?.string() ?: "{}"
                }
                android.util.Log.d("FitSyncApi", "GET /api/workout/plan/current response: $rawJson")

                val hasPlan = if (planRes.isSuccessful && planRes.body() != null) {
                    checkIfUserHasPlan(planRes.body())
                } else if (planRes.code() == 404 || planRes.code() == 400) {
                    // Server explicitly confirmed user has no active plan
                    com.esha.fitsync.data.AppCacheManager.saveWorkoutPlanJson("")
                    false
                } else {
                    val cached = com.esha.fitsync.data.AppCacheManager.getWorkoutPlanJson()
                    if (!cached.isNullOrBlank()) {
                        val elem = try { com.google.gson.JsonParser.parseString(cached) } catch (_: Exception) { null }
                        checkIfUserHasPlan(elem)
                    } else false
                }

                val parsedObj = try {
                    if (planRes.isSuccessful && planRes.body() != null) {
                        if (planRes.body()!!.isJsonObject) planRes.body()!!.asJsonObject else null
                    } else {
                        val cached = com.esha.fitsync.data.AppCacheManager.getWorkoutPlanJson()
                        if (!cached.isNullOrBlank()) {
                            val elem = com.google.gson.JsonParser.parseString(cached)
                            if (elem.isJsonObject) elem.asJsonObject else null
                        } else null
                    }
                } catch (e: Exception) { null }

                val daysSize = when {
                    parsedObj == null -> "N/A (null/non-object)"
                    parsedObj.has("days") && parsedObj.get("days").isJsonArray -> parsedObj.getAsJsonArray("days").size().toString()
                    parsedObj.has("plan") && parsedObj.get("plan").isJsonObject && parsedObj.getAsJsonObject("plan").has("days") && parsedObj.getAsJsonObject("plan").get("days").isJsonArray -> parsedObj.getAsJsonObject("plan").getAsJsonArray("days").size().toString()
                    else -> "missing/null"
                }

                android.util.Log.d("SheetDebug", "raw plan/current response: $rawJson")
                android.util.Log.d("SheetDebug", "hasPlan: $hasPlan, days size: $daysSize")

                if (!isAdded || _binding == null) return@launch

                if (!hasPlan) {
                    android.util.Log.d("SheetDebug", "Branch taken: SHEET (No plan yet). hasAutoOpenedBottomSheetThisSession=$hasAutoOpenedBottomSheetThisSession, isAdded=$isAdded")
                    // New user without an active workout plan:
                    // Hide dashboard, show empty state with "Create my plan" button.
                    binding.llWorkoutContent.visibility = View.GONE
                    binding.llNoPlanState.visibility = View.VISIBLE

                    if (!hasAutoOpenedBottomSheetThisSession) {
                        hasAutoOpenedBottomSheetThisSession = true
                        openCreatePlanBottomSheet()
                    }
                } else {
                    android.util.Log.d("SheetDebug", "Branch taken: DASHBOARD (Plan exists)")
                    // User has a plan: load the real fragment dashboard
                    binding.llNoPlanState.visibility = View.GONE
                    binding.llWorkoutContent.visibility = View.VISIBLE

                    if (planRes.isSuccessful && planRes.body() != null) {
                        com.esha.fitsync.data.AppCacheManager.saveWorkoutPlanJson(rawJson)
                    }
                    loadWorkoutPlanContent()
                }
            } catch (e: Exception) {
                android.util.Log.e("FitSyncApi", "Error checking current workout plan", e)
                android.util.Log.e("SheetDebug", "Exception checking current workout plan: ${e.message}", e)
                if (!isAdded || _binding == null) return@launch
                val cached = com.esha.fitsync.data.AppCacheManager.getWorkoutPlanJson()
                if (!cached.isNullOrBlank()) {
                    val elem = try { com.google.gson.JsonParser.parseString(cached) } catch (_: Exception) { null }
                    if (checkIfUserHasPlan(elem)) {
                        android.util.Log.d("SheetDebug", "Branch taken (from catch/cache): DASHBOARD")
                        binding.llNoPlanState.visibility = View.GONE
                        binding.llWorkoutContent.visibility = View.VISIBLE
                        loadWorkoutPlanContent()
                        return@launch
                    }
                }
                android.util.Log.d("SheetDebug", "Branch taken (from catch): SHEET (fallback empty state)")
                binding.llWorkoutContent.visibility = View.GONE
                binding.llNoPlanState.visibility = View.VISIBLE
            } finally {
                _binding?.srlWorkoutRefresh?.isRefreshing = false
            }
        }
    }

    private fun checkIfUserHasPlan(element: JsonElement?): Boolean {
        if (element == null || element.isJsonNull) return false
        if (!element.isJsonObject) return false
        val obj = element.asJsonObject

        // 0. Explicit failure status from backend
        if (obj.has("success") && !obj.get("success").asBoolean) {
            return false
        }

        // 1. Explicit hasPlan boolean if returned by backend
        if (obj.has("hasPlan")) {
            val hasPlan = try { obj.get("hasPlan").asBoolean } catch (_: Exception) { true }
            if (!hasPlan) return false
        }

        // 2. Check if plan or data is null or empty object
        if (obj.has("plan") && (obj.get("plan").isJsonNull || (obj.get("plan").isJsonObject && obj.getAsJsonObject("plan").size() == 0))) {
            return false
        }
        if (obj.has("data") && (obj.get("data").isJsonNull || (obj.get("data").isJsonObject && obj.getAsJsonObject("data").size() == 0))) {
            return false
        }

        // 3. Check days array
        val daysArray = when {
            obj.has("days") && obj.get("days").isJsonArray -> obj.getAsJsonArray("days")
            obj.has("plan") && obj.get("plan").isJsonObject && obj.getAsJsonObject("plan").has("days") && obj.getAsJsonObject("plan").get("days").isJsonArray -> obj.getAsJsonObject("plan").getAsJsonArray("days")
            obj.has("data") && obj.get("data").isJsonObject && obj.getAsJsonObject("data").has("days") && obj.getAsJsonObject("data").get("days").isJsonArray -> obj.getAsJsonObject("data").getAsJsonArray("days")
            else -> null
        }

        if (daysArray != null) {
            if (daysArray.size() == 0) return false
            var totalExercises = 0
            for (i in 0 until daysArray.size()) {
                val dayItem = daysArray.get(i)
                if (dayItem.isJsonObject) {
                    val dayObj = dayItem.asJsonObject
                    if (dayObj.has("exercises") && dayObj.get("exercises").isJsonArray) {
                        totalExercises += dayObj.getAsJsonArray("exercises").size()
                    }
                }
            }
            if (totalExercises == 0) return false
            return true
        }

        // 3. Fallback: check routines parsed from plan
        val routines = parseRoutinesFromJsonElement(element)
        if (routines.isEmpty()) return false
        return routines.any { it.exercises.isNotEmpty() }
    }

    private fun loadWorkoutPlanContent() {
        val sharedPreferences = requireActivity().getSharedPreferences("FitSync", Context.MODE_PRIVATE)
        val userName = sharedPreferences.getString("USER_NAME", "Athlete") ?: "Athlete"
        _binding?.tvWelcomeUser?.text = "Welcome back, $userName."

        // 1. Instant load cached today session
        val cachedToday = com.esha.fitsync.data.AppCacheManager.getTodayWorkout()
        if (cachedToday != null) {
            currentTodaySession = cachedToday
            applyTodaySessionToUI(cachedToday)
        }

        // 2. Instant load cached weekly plan routines
        val cachedPlanJson = com.esha.fitsync.data.AppCacheManager.getWorkoutPlanJson()
        if (!cachedPlanJson.isNullOrBlank() && _binding != null) {
            val jsonElement = try { com.google.gson.JsonParser.parseString(cachedPlanJson) } catch (_: Exception) { null }
            val routines = parseRoutinesFromJsonElement(jsonElement)
            if (routines.isNotEmpty()) {
                binding.llEmptyWeeklyPlan.visibility = View.GONE
                binding.rvWeeklyPlan.visibility = View.VISIBLE
                val todayIndex = calculateTodayIndex()
                if (weeklyPlanAdapter == null) {
                    weeklyPlanAdapter = WeeklyPlanAdapter(routines, todayIndex) { routine ->
                        val intent = Intent(requireContext(), WorkoutDetailActivity::class.java).apply {
                            putExtra("DAY_INDEX", routine.dayIndex)
                            putExtra("dayIndex", routine.dayIndex)
                        }
                        startActivity(intent)
                    }
                    binding.rvWeeklyPlan.adapter = weeklyPlanAdapter
                } else {
                    weeklyPlanAdapter?.submitList(routines, todayIndex)
                }
                binding.rvWeeklyPlan.scrollToPosition(todayIndex)
            }
        }

        fetchRealWorkoutPlan()
    }

    private fun fetchRealWorkoutPlan() {
        viewLifecycleOwner.lifecycleScope.launch {
            if (!isAdded || _binding == null) return@launch
            try {
                // 1. Feature F: Stats
                val statsRes = RetrofitClient.api.getWorkoutStats()
                if (isAdded && _binding != null && statsRes.isSuccessful && statsRes.body() != null) {
                    val stats = statsRes.body()!!
                    binding.tvCurrentWeightValue.text = "${stats.currentWeight} KG"
                    binding.tvTargetWeightValue.text = "${stats.targetWeight} KG"
                    binding.tvStreakValue.text = "${stats.weeklyStreak?.currentStreakDays ?: 0} DAYS"
                    binding.tvCompletionValue.text = "${stats.weekCompletionPercent}%"
                    binding.pbOverallProgress.progress = stats.weightProgressPercent.coerceIn(0, 100)
                    binding.tvOverallProgressText.text = "${stats.weightProgressPercent}%"

                    val sDone = stats.weeklyStreak?.weeklySessionsCompleted ?: 0
                    val sPlan = if ((stats.weeklyStreak?.weeklySessionsPlanned ?: 0) > 0) {
                        stats.weeklyStreak!!.weeklySessionsPlanned
                    } else 5
                    com.esha.fitsync.data.repository.DailyTargetsRepository.updateWeeklyGoal(sDone, sPlan)
                }

                // 2. Feature E: Today's Workout
                val todayRes = RetrofitClient.api.getTodayWorkout()
                if (isAdded && _binding != null && todayRes.isSuccessful && todayRes.body() != null) {
                    val today = todayRes.body()!!
                    currentTodaySession = today
                    com.esha.fitsync.data.AppCacheManager.saveTodayWorkout(today)
                    applyTodaySessionToUI(today)
                }

                // 3. Feature E: 7-Day Current Plan
                val planRes = RetrofitClient.api.getCurrentWorkoutPlan()
                if (isAdded && _binding != null && planRes.isSuccessful && planRes.body() != null) {
                    val rawJson = planRes.body().toString()
                    android.util.Log.d("FitSyncApi", "GET /api/workout/plan/current response: $rawJson")
                    val routines = parseRoutinesFromJsonElement(planRes.body())
                    android.util.Log.d("FitSyncApi", "Parsed routines count: ${routines.size}")

                    if (routines.isNotEmpty()) {
                        com.esha.fitsync.data.AppCacheManager.saveWorkoutPlanJson(rawJson)
                        binding.llEmptyWeeklyPlan.visibility = View.GONE
                        binding.rvWeeklyPlan.visibility = View.VISIBLE
                        val todayIndex = calculateTodayIndex()

                        if (weeklyPlanAdapter == null) {
                            weeklyPlanAdapter = WeeklyPlanAdapter(routines, todayIndex) { routine ->
                                val intent = Intent(requireContext(), WorkoutDetailActivity::class.java).apply {
                                    putExtra("DAY_INDEX", routine.dayIndex)
                                    putExtra("dayIndex", routine.dayIndex)
                                }
                                startActivity(intent)
                            }
                            binding.rvWeeklyPlan.adapter = weeklyPlanAdapter
                        } else {
                            weeklyPlanAdapter?.submitList(routines, todayIndex)
                        }
                        binding.rvWeeklyPlan.scrollToPosition(todayIndex)
                    } else {
                        val cached = com.esha.fitsync.data.AppCacheManager.getWorkoutPlanJson()
                        if (cached.isNullOrBlank()) {
                            binding.rvWeeklyPlan.visibility = View.GONE
                            binding.llEmptyWeeklyPlan.visibility = View.VISIBLE
                        }
                    }
                } else if (isAdded && _binding != null) {
                    val cached = com.esha.fitsync.data.AppCacheManager.getWorkoutPlanJson()
                    if (cached.isNullOrBlank() && (weeklyPlanAdapter == null || weeklyPlanAdapter!!.itemCount == 0)) {
                        binding.rvWeeklyPlan.visibility = View.GONE
                        binding.llEmptyWeeklyPlan.visibility = View.VISIBLE
                    }
                }

                // 4. Feature G: User Equipment
                val equipRes = RetrofitClient.api.getUserEquipment()
                if (isAdded && _binding != null && equipRes.isSuccessful && equipRes.body() != null) {
                    val userEquip = equipRes.body()!!.equipment
                    setupAvailableChips(binding.cgAvailableEquipment, userEquip)
                }

                // 5. Feature H: Daily Target Overview (from unified DailyTargetsRepository using getTodayNutrition)
                val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val targets = com.esha.fitsync.data.repository.DailyTargetsRepository.fetchDailyNutrition(todayStr)
                bindDailyTargets(targets)

                // 6. Dynamic AI Nutrition/Workout Insight
                fetchDynamicInsight(todayStr)

            } catch (e: Exception) {
                android.util.Log.e("FITSYNC_WORKOUT", "Error fetching workout data", e)
                val cached = com.esha.fitsync.data.repository.DailyTargetsRepository.getCachedTargets()
                bindDailyTargets(cached.copy(isFromCache = true))
            } finally {
                _binding?.srlWorkoutRefresh?.isRefreshing = false
            }
        }
    }

    private fun bindDailyTargets(targets: com.esha.fitsync.data.repository.DailyTargetsData) {
        val b = _binding ?: return

        // 1. Weekly Goal: sessions done / planned
        val sDone = targets.weeklySessionsDone
        val sPlan = if (targets.weeklySessionsPlanned > 0) targets.weeklySessionsPlanned else 5
        val weeklyPct = targets.weeklyGoalPercent
        b.tvWeeklyGoalText.text = "$sDone/$sPlan days"
        b.pbWeeklyGoal.max = 100
        b.pbWeeklyGoal.progress = weeklyPct

        // 2. Protein: consumed / target
        val pCurr = targets.proteinConsumed.toInt()
        val pTarg = if (targets.proteinTarget > 0) targets.proteinTarget.toInt() else 120
        val proteinPct = targets.proteinPercent
        b.tvProteinText.text = "${pCurr}/${pTarg}g"
        b.pbProtein.max = 100
        b.pbProtein.progress = proteinPct

        // 3. Water: consumed / target
        val wCurr = targets.waterConsumedLiters
        val wTarg = if (targets.waterTargetLiters > 0) targets.waterTargetLiters else 3.0
        val waterPct = targets.waterPercent
        b.tvWaterText.text = String.format(Locale.US, "%.1f/%.1fL", wCurr, wTarg)
        b.pbWater.max = 100
        b.pbWater.progress = waterPct

        // 4. Offline note
        if (targets.isFromCache) {
            b.tvDailyTargetsSyncStatus.text = "Can't reach server (showing cached data)"
            b.tvDailyTargetsSyncStatus.visibility = View.VISIBLE
        } else {
            b.tvDailyTargetsSyncStatus.visibility = View.GONE
        }

        // 5. Verify by logging with tag FitSyncSync
        android.util.Log.d(
            "FitSyncSync",
            "Workout Screen -> Protein: ${pCurr}/${pTarg}g (${proteinPct}%), Water: ${String.format(Locale.US, "%.1f/%.1fL", wCurr, wTarg)} (${waterPct}%), Weekly Goal: $sDone/$sPlan days (${weeklyPct}%)"
        )
    }

    private fun triggerGenerateAiPlan() {
        val b = _binding ?: return
        b.pbWeeklyPlanLoading.visibility = View.VISIBLE
        b.btnGenerateAiPlan.isEnabled = false
        b.btnGeneratePlanEmpty.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val res = RetrofitClient.api.generateWorkoutPlan()
                if (isAdded && _binding != null) {
                    b.pbWeeklyPlanLoading.visibility = View.GONE
                    b.btnGenerateAiPlan.isEnabled = true
                    b.btnGeneratePlanEmpty.isEnabled = true

                    if (res.isSuccessful) {
                        Toast.makeText(context, "AI Workout Plan generated successfully! ⚡", Toast.LENGTH_SHORT).show()
                        com.esha.fitsync.data.AppCacheManager.notifyDataChanged()
                        fetchRealWorkoutPlan()
                    } else {
                        Toast.makeText(context, "Could not generate plan (${res.code()})", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                if (isAdded && _binding != null) {
                    b.pbWeeklyPlanLoading.visibility = View.GONE
                    b.btnGenerateAiPlan.isEnabled = true
                    b.btnGeneratePlanEmpty.isEnabled = true
                }
                Toast.makeText(context, "Error generating plan: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fetchDynamicInsight(todayStr: String) {
        val b = _binding ?: return
        val cachedInsight = com.esha.fitsync.data.AppCacheManager.getDynamicInsight()
        if (cachedInsight != null && !cachedInsight.message.isNullOrBlank()) {
            b.tvWorkoutAiInsight.text = cachedInsight.message
            b.tvWorkoutAiInsight.visibility = View.VISIBLE
            b.shimmerWorkoutInsight.visibility = View.GONE
            b.vShimmerWorkoutPlaceholder.visibility = View.GONE
        } else {
            b.vShimmerWorkoutPlaceholder.visibility = View.VISIBLE
            b.shimmerWorkoutInsight.visibility = View.VISIBLE
            b.shimmerWorkoutInsight.startShimmer()
            b.tvWorkoutAiInsight.visibility = View.GONE
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val res = RetrofitClient.apiService.getDynamicInsight(todayStr)
                if (isAdded && _binding != null && res.isSuccessful && res.body() != null) {
                    val body = res.body()!!
                    val msg = body.message
                    if (!msg.isNullOrBlank()) {
                        com.esha.fitsync.data.AppCacheManager.saveDynamicInsight(body)
                        binding.tvWorkoutAiInsight.text = msg
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("FITSYNC_WORKOUT", "Failed to fetch dynamic insight", e)
            } finally {
                if (isAdded && _binding != null) {
                    binding.shimmerWorkoutInsight.stopShimmer()
                    binding.shimmerWorkoutInsight.visibility = View.GONE
                    binding.vShimmerWorkoutPlaceholder.visibility = View.GONE
                    binding.tvWorkoutAiInsight.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun applyTodaySessionToUI(today: TodayWorkoutSession) {
        val b = _binding ?: return
        b.tvWorkoutTitle.text = if (today.isRestDay) "Rest & Recovery" else today.focus.ifEmpty { "Daily Workout" }
        b.tvWorkoutMeta.text = "⏱️ ${today.estimatedMinutes} min   🔥 ${today.estimatedCalories} kcal"
        b.tvIntensity.text = if (today.isRestDay) "🧘 Rest Day" else "🏋️ Dynamic Session"

        // Dynamic Hero Image based on focus
        val focusLower = today.focus.lowercase()
        val imageRes = when {
            today.isRestDay -> R.drawable.hologram_body
            focusLower.contains("leg") || focusLower.contains("lower") || focusLower.contains("squat") -> R.drawable.girl_dumbell
            focusLower.contains("chest") || focusLower.contains("push") || focusLower.contains("bench") || focusLower.contains("upper") -> R.drawable.gym
            else -> R.drawable.girl_dumbell
        }
        b.ivHeroWorkoutImage.setImageResource(imageRes)

        if (today.isRestDay) {
            b.tvCurrentEquipment1.text = "Rest"
            b.tvCurrentEquipment2.text = "Recovery"
            b.btnStartWorkout.text = "VIEW RECOVERY 🧘"
            b.btnStartWorkout.isEnabled = true
        } else if (today.completed) {
            b.tvCurrentEquipment1.text = "Completed"
            b.tvCurrentEquipment2.text = "${today.exercises.size} Exercises"
            b.btnStartWorkout.text = "VIEW WORKOUT ✓"
            b.btnStartWorkout.isEnabled = true
        } else {
            val ex1 = today.exercises.getOrNull(0)?.name?.take(14) ?: "Full Body"
            val ex2 = "${today.exercises.size} Exercises"
            b.tvCurrentEquipment1.text = ex1
            b.tvCurrentEquipment2.text = ex2
            b.btnStartWorkout.text = "START WORKOUT ▶"
            b.btnStartWorkout.isEnabled = true
        }
    }

    private fun handleStartWorkoutClick() {
        val today = currentTodaySession
        val dayIdx = today?.dayIndex ?: calculateTodayIndex()
        val intent = Intent(requireContext(), WorkoutDetailActivity::class.java).apply {
            putExtra("DAY_INDEX", dayIdx)
            putExtra("dayIndex", dayIdx)
        }
        startActivity(intent)
    }

    private fun showLogWeightDialog() {
        val builder = AlertDialog.Builder(requireContext(), R.style.Theme_FitSync)
        builder.setTitle("Log Your Weight")

        val input = EditText(requireContext()).apply {
            hint = "e.g. 74.5"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            setHintTextColor(Color.GRAY)
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#1C2638"))
            setPadding(32, 24, 32, 24)
        }
        builder.setView(input)

        builder.setPositiveButton("Log Weight") { dialog, _ ->
            val text = input.text.toString().trim()
            val weight = text.toDoubleOrNull()
            if (weight != null && weight > 20.0 && weight < 300.0) {
                postWeightLog(weight)
            } else {
                Toast.makeText(context, "Please enter a valid weight (e.g. 74.5)", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
        builder.show()
    }

    private fun postWeightLog(weightKg: Double) {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val req = LogWeightRequest(weightKg = weightKg, date = todayStr)
                val res = RetrofitClient.api.logWeight(req)
                if (res.isSuccessful) {
                    Toast.makeText(context, "Weight logged: $weightKg kg! ⚖️", Toast.LENGTH_SHORT).show()
                    fetchRealWorkoutPlan()
                } else {
                    Toast.makeText(context, "Failed to log weight (${res.code()})", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Network error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupAvailableChips(chipGroup: ChipGroup, items: List<String>) {
        chipGroup.removeAllViews()
        val displayItems = if (items.isEmpty()) listOf("Bodyweight", "Dumbbells") else items
        for (item in displayItems) {
            val chip = Chip(requireContext()).apply {
                text = item.replace("_", " ").replaceFirstChar { it.uppercase() }
                isCheckable = false
                isClickable = false
                chipBackgroundColor = ColorStateList.valueOf(Color.parseColor("#1C2638"))
                setTextColor(Color.WHITE)
                chipStrokeWidth = 0f
            }
            chipGroup.addView(chip)
        }
    }

    private fun calculateTodayIndex(): Int {
        val calendar = Calendar.getInstance()
        return when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            Calendar.SATURDAY -> 5
            Calendar.SUNDAY -> 6
            else -> 0
        }
    }

    private fun parseRoutinesFromJsonElement(element: JsonElement?): List<EngineRoutine> {
        if (element == null || element.isJsonNull) return emptyList()
        val gson = Gson()
        return try {
            if (element.isJsonArray) {
                val type = object : TypeToken<List<EngineRoutine>>() {}.type
                gson.fromJson<List<EngineRoutine>>(element, type) ?: emptyList()
            } else if (element.isJsonObject) {
                val obj = element.asJsonObject
                when {
                    obj.has("plan") && obj.get("plan").isJsonObject -> {
                        val planObj = obj.getAsJsonObject("plan")
                        if (planObj.has("routines") && planObj.get("routines").isJsonArray) {
                            val type = object : TypeToken<List<EngineRoutine>>() {}.type
                            gson.fromJson<List<EngineRoutine>>(planObj.get("routines"), type) ?: emptyList()
                        } else emptyList()
                    }
                    obj.has("plan") && obj.get("plan").isJsonArray -> {
                        val type = object : TypeToken<List<EngineRoutine>>() {}.type
                        gson.fromJson<List<EngineRoutine>>(obj.get("plan"), type) ?: emptyList()
                    }
                    obj.has("routines") && obj.get("routines").isJsonArray -> {
                        val type = object : TypeToken<List<EngineRoutine>>() {}.type
                        gson.fromJson<List<EngineRoutine>>(obj.get("routines"), type) ?: emptyList()
                    }
                    obj.has("data") -> {
                        parseRoutinesFromJsonElement(obj.get("data"))
                    }
                    else -> emptyList()
                }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            android.util.Log.e("FITSYNC_PLAN", "Error parsing routines JSON", e)
            emptyList()
        }
    }
}