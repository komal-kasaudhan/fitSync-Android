package com.esha.fitsync.data.repository

import android.util.Log
import com.esha.fitsync.api.RetrofitClient
import com.esha.fitsync.data.AppCacheManager
import com.google.gson.annotations.SerializedName
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DailyTargetsData(
    @SerializedName("proteinConsumed") val proteinConsumed: Double = 0.0,
    @SerializedName("proteinTarget") val proteinTarget: Double = 120.0,
    @SerializedName("waterConsumedLiters") val waterConsumedLiters: Double = 0.0,
    @SerializedName("waterTargetLiters") val waterTargetLiters: Double = 3.0,
    @SerializedName("weeklySessionsDone") val weeklySessionsDone: Int = 0,
    @SerializedName("weeklySessionsPlanned") val weeklySessionsPlanned: Int = 5,
    @SerializedName("caloriesConsumed") val caloriesConsumed: Int = 0,
    @SerializedName("caloriesTarget") val caloriesTarget: Int = 2000,
    @SerializedName("isFromCache") val isFromCache: Boolean = false,
    @SerializedName("lastUpdated") val lastUpdated: Long = System.currentTimeMillis()
) {
    val weeklyGoalPercent: Int
        get() = if (weeklySessionsPlanned > 0) {
            ((weeklySessionsDone.toDouble() / weeklySessionsPlanned) * 100).toInt().coerceIn(0, 100)
        } else 0

    val proteinPercent: Int
        get() = if (proteinTarget > 0) {
            ((proteinConsumed / proteinTarget) * 100).toInt().coerceIn(0, 100)
        } else 0

    val waterPercent: Int
        get() = if (waterTargetLiters > 0) {
            ((waterConsumedLiters / waterTargetLiters) * 100).toInt().coerceIn(0, 100)
        } else 0
}

object DailyTargetsRepository {

    private const val TAG = "FitSyncSync"
    private const val CACHE_KEY = "SHARED_DAILY_TARGETS"

    @Volatile
    private var inMemoryTargets: DailyTargetsData? = null

    fun getCachedTargets(): DailyTargetsData {
        if (inMemoryTargets != null) return inMemoryTargets!!
        val cached = AppCacheManager.get<DailyTargetsData>(CACHE_KEY)
        if (cached != null) {
            inMemoryTargets = cached
            return cached
        }
        val defaultTargets = DailyTargetsData()
        inMemoryTargets = defaultTargets
        return defaultTargets
    }

    fun saveTargets(data: DailyTargetsData) {
        inMemoryTargets = data
        AppCacheManager.put(CACHE_KEY, data)
    }

    fun clearCache() {
        inMemoryTargets = null
    }

    fun updateNutrition(
        proteinConsumed: Double,
        proteinTarget: Double,
        waterConsumedLiters: Double,
        waterTargetLiters: Double,
        caloriesConsumed: Int = 0,
        caloriesTarget: Int = 2000
    ): DailyTargetsData {
        val current = getCachedTargets()
        val updated = current.copy(
            proteinConsumed = proteinConsumed,
            proteinTarget = if (proteinTarget > 0) proteinTarget else current.proteinTarget,
            waterConsumedLiters = waterConsumedLiters,
            waterTargetLiters = if (waterTargetLiters > 0) waterTargetLiters else current.waterTargetLiters,
            caloriesConsumed = if (caloriesConsumed > 0) caloriesConsumed else current.caloriesConsumed,
            caloriesTarget = if (caloriesTarget > 0) caloriesTarget else current.caloriesTarget,
            isFromCache = false,
            lastUpdated = System.currentTimeMillis()
        )
        saveTargets(updated)
        return updated
    }

    fun updateWeeklyGoal(
        sessionsDone: Int,
        sessionsPlanned: Int
    ): DailyTargetsData {
        val current = getCachedTargets()
        val updated = current.copy(
            weeklySessionsDone = sessionsDone,
            weeklySessionsPlanned = if (sessionsPlanned > 0) sessionsPlanned else current.weeklySessionsPlanned,
            isFromCache = false,
            lastUpdated = System.currentTimeMillis()
        )
        saveTargets(updated)
        return updated
    }

    suspend fun fetchDailyNutrition(dateStr: String? = null): DailyTargetsData {
        val todayStr = dateStr ?: SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        try {
            val response = RetrofitClient.apiService.getTodayNutrition(todayStr)
            if (response.isSuccessful && response.body()?.success == true && response.body()?.data != null) {
                val data = response.body()!!.data!!
                val pConsumed = data.protein.current
                val pTarget = if (data.protein.target > 0) data.protein.target else 120.0
                val wConsumed = data.water.current
                val wTarget = if (data.water.target > 0) data.water.target else 3.0
                val calConsumed = data.eatenCalories
                val calTarget = if (data.targetCalories > 0) data.targetCalories else 2000

                val current = getCachedTargets()
                val updated = current.copy(
                    proteinConsumed = pConsumed,
                    proteinTarget = pTarget,
                    waterConsumedLiters = wConsumed,
                    waterTargetLiters = wTarget,
                    caloriesConsumed = calConsumed,
                    caloriesTarget = calTarget,
                    isFromCache = false,
                    lastUpdated = System.currentTimeMillis()
                )
                saveTargets(updated)
                return updated
            } else {
                Log.w(TAG, "Nutrition API returned ${response.code()}, falling back to cached targets")
                return getCachedTargets().copy(isFromCache = true)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network error fetching nutrition targets, using cache", e)
            return getCachedTargets().copy(isFromCache = true)
        }
    }

    suspend fun fetchWeeklyWorkoutGoal(): DailyTargetsData {
        try {
            val statsRes = RetrofitClient.api.getWorkoutStats()
            if (statsRes.isSuccessful && statsRes.body() != null) {
                val stats = statsRes.body()!!
                val done = stats.weeklyStreak?.weeklySessionsCompleted ?: 0
                val planned = if ((stats.weeklyStreak?.weeklySessionsPlanned ?: 0) > 0) {
                    stats.weeklyStreak!!.weeklySessionsPlanned
                } else 5

                val current = getCachedTargets()
                val updated = current.copy(
                    weeklySessionsDone = done,
                    weeklySessionsPlanned = planned,
                    isFromCache = false,
                    lastUpdated = System.currentTimeMillis()
                )
                saveTargets(updated)
                return updated
            } else {
                Log.w(TAG, "Workout stats API returned ${statsRes.code()}, falling back to cached weekly goal")
                return getCachedTargets().copy(isFromCache = true)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network error fetching workout stats, using cache", e)
            return getCachedTargets().copy(isFromCache = true)
        }
    }

    suspend fun syncAll(dateStr: String? = null): DailyTargetsData {
        // 1. Fetch weekly goal from /workout/stats
        fetchWeeklyWorkoutGoal()
        // 2. Fetch protein and water from /api/v1/nutrition/today (same source as Nutrition screen)
        return fetchDailyNutrition(dateStr)
    }
}
