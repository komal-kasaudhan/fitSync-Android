package com.esha.fitsync.data

import android.content.Context
import android.content.SharedPreferences
import com.esha.fitsync.models.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object AppCacheManager {

    @PublishedApi internal val gson = Gson()
    @PublishedApi internal var prefs: SharedPreferences? = null

    // Shared Flow to signal across fragments/activities that a mutation occurred
    private val _refreshTrigger = MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 1)
    val refreshTrigger = _refreshTrigger.asSharedFlow()

    fun notifyDataChanged() {
        _refreshTrigger.tryEmit(Unit)
    }

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences("FitSyncDataCache", Context.MODE_PRIVATE)
        }
    }

    // 1. Home Summary Cache
    @Volatile
    private var cachedHomeSummary: HomeSummaryResponse? = null

    fun getHomeSummary(): HomeSummaryResponse? {
        if (cachedHomeSummary != null) return cachedHomeSummary
        val json = prefs?.getString("CACHE_HOME_SUMMARY", null) ?: return null
        return try {
            cachedHomeSummary = gson.fromJson(json, HomeSummaryResponse::class.java)
            cachedHomeSummary
        } catch (_: Exception) {
            null
        }
    }

    fun saveHomeSummary(summary: HomeSummaryResponse) {
        cachedHomeSummary = summary
        prefs?.edit()?.putString("CACHE_HOME_SUMMARY", gson.toJson(summary))?.apply()
    }

    // 2. Weekly Nutrition Cache
    @Volatile
    private var cachedWeeklyNutrition: List<WeeklyNutritionDay>? = null

    fun getWeeklyNutrition(): List<WeeklyNutritionDay>? {
        if (!cachedWeeklyNutrition.isNullOrEmpty()) return cachedWeeklyNutrition
        val json = prefs?.getString("CACHE_WEEKLY_NUTRITION", null) ?: return null
        return try {
            val type = object : TypeToken<List<WeeklyNutritionDay>>() {}.type
            cachedWeeklyNutrition = gson.fromJson<List<WeeklyNutritionDay>>(json, type)
            cachedWeeklyNutrition
        } catch (_: Exception) {
            null
        }
    }

    fun saveWeeklyNutrition(days: List<WeeklyNutritionDay>) {
        if (days.isNotEmpty()) {
            cachedWeeklyNutrition = days
            prefs?.edit()?.putString("CACHE_WEEKLY_NUTRITION", gson.toJson(days))?.apply()
        }
    }

    // 3. Dynamic Insight Cache
    @Volatile
    private var cachedInsight: DynamicInsightResponse? = null

    fun getDynamicInsight(): DynamicInsightResponse? {
        if (cachedInsight != null) return cachedInsight
        val json = prefs?.getString("CACHE_DYNAMIC_INSIGHT", null) ?: return null
        return try {
            cachedInsight = gson.fromJson(json, DynamicInsightResponse::class.java)
            cachedInsight
        } catch (_: Exception) {
            null
        }
    }

    fun saveDynamicInsight(insight: DynamicInsightResponse) {
        cachedInsight = insight
        prefs?.edit()?.putString("CACHE_DYNAMIC_INSIGHT", gson.toJson(insight))?.apply()
    }

    // 4. Workout Plan Cache
    @Volatile
    private var cachedPlanJson: String? = null

    fun getWorkoutPlanJson(): String? {
        if (!cachedPlanJson.isNullOrEmpty()) return cachedPlanJson
        cachedPlanJson = prefs?.getString("CACHE_WORKOUT_PLAN", null)
        return cachedPlanJson
    }

    fun saveWorkoutPlanJson(rawJson: String) {
        if (rawJson.isNotBlank()) {
            cachedPlanJson = rawJson
            prefs?.edit()?.putString("CACHE_WORKOUT_PLAN", rawJson)?.apply()
        }
    }

    // 5. Today's Workout Cache
    @Volatile
    private var cachedTodayWorkout: TodayWorkoutSession? = null

    fun getTodayWorkout(): TodayWorkoutSession? {
        if (cachedTodayWorkout != null) return cachedTodayWorkout
        val json = prefs?.getString("CACHE_TODAY_WORKOUT", null) ?: return null
        return try {
            cachedTodayWorkout = gson.fromJson(json, TodayWorkoutSession::class.java)
            cachedTodayWorkout
        } catch (_: Exception) {
            null
        }
    }

    fun saveTodayWorkout(today: TodayWorkoutSession) {
        cachedTodayWorkout = today
        prefs?.edit()?.putString("CACHE_TODAY_WORKOUT", gson.toJson(today))?.apply()
    }

    // 6. Recommendations Cache
    @Volatile
    private var cachedRecommendations: RecommendationResponse? = null

    fun getRecommendations(): RecommendationResponse? {
        if (cachedRecommendations != null) return cachedRecommendations
        val json = prefs?.getString("CACHE_RECOMMENDATIONS", null) ?: return null
        return try {
            cachedRecommendations = gson.fromJson(json, RecommendationResponse::class.java)
            cachedRecommendations
        } catch (_: Exception) {
            null
        }
    }

    fun saveRecommendations(recs: RecommendationResponse) {
        cachedRecommendations = recs
        prefs?.edit()?.putString("CACHE_RECOMMENDATIONS", gson.toJson(recs))?.apply()
    }

    // 7. Generic Object Cache
    fun <T> put(key: String, value: T) {
        try {
            val json = gson.toJson(value)
            prefs?.edit()?.putString("GENERIC_$key", json)?.apply()
        } catch (_: Exception) {}
    }

    inline fun <reified T> get(key: String): T? {
        val json = prefs?.getString("GENERIC_$key", null) ?: return null
        return try {
            gson.fromJson(json, object : TypeToken<T>() {}.type)
        } catch (_: Exception) {
            null
        }
    }
}
