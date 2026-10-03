package com.esha.fitsync.api

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

data class AddWaterRequest(
    val amountMl: Int,
    val date: String? = null
)

data class ScanFoodRequest(
    val imageBase64: String
)

data class ScannedFoodData(
    val foodName: String? = null,
    val servingSize: String? = null,
    val servingWeightGrams: Double = 0.0,
    val calories: Int = 0,
    val protein: Double = 0.0,
    val carbs: Double = 0.0,
    val fat: Double = 0.0,
    val fiber: Double = 0.0,
    val confidenceScore: Double = 0.0,
    val dietType: String? = null,
    val ingredientsDetected: List<String> = emptyList()
)

data class ScanFoodResponse(
    val success: Boolean = false,
    val detected: Boolean = false,
    val isFood: Boolean? = true,
    val message: String? = null,
    val data: ScannedFoodData? = null,
    val foodName: String? = null,
    val calories: Int = 0,
    val protein: Double = 0.0,
    val carbs: Double = 0.0,
    val fat: Double = 0.0,
    val estimatedGram: Double = 100.0
) {
    val effectiveFoodName: String? get() = data?.foodName ?: foodName
    val effectiveCalories: Int get() = data?.calories ?: calories
    val effectiveProtein: Double get() = data?.protein ?: protein
    val effectiveCarbs: Double get() = data?.carbs ?: carbs
    val effectiveFat: Double get() = data?.fat ?: fat
    val effectiveWeightGrams: Double get() = if (data != null && data.servingWeightGrams > 0) data.servingWeightGrams else estimatedGram
    val isDetected: Boolean get() = detected || (isFood == true && (!effectiveFoodName.isNullOrBlank() || data != null))
}

data class AddMealRequest(
    val date: String,
    val mealType: String,
    val foodId: String,
    val quantity: Double,
    val unit: String? = null
)

data class FoodItem(
    val id: String? = null,
    val _id: String? = null,
    val name: String? = null,
    val category: String? = null,
    val subCategory: String? = null,
    val servingType: String? = null,
    val servingSize: String? = null,
    val servingWeight: Double = 100.0,
    val calories: Int = 0,
    val protein: Double = 0.0,
    val carbs: Double = 0.0,
    val fat: Double = 0.0,
    val fiber: Double = 0.0,
    val dietType: String? = null,
    val mealTypes: List<String> = emptyList(),
    val servingUnit: String? = null,
    val verified: Boolean = true
) {
    val effectiveId: String
        get() = if (!id.isNullOrEmpty()) id else (_id ?: "")
    val effectiveName: String
        get() = name ?: "Food Item"
    val effectiveCategory: String
        get() = category ?: "General"
}

data class FoodSearchResponse(
    val success: Boolean = false,
    val count: Int = 0,
    val data: List<FoodItem> = emptyList()
)

data class MealItemNutrition(
    val calories: Int = 0,
    val protein: Double = 0.0,
    val carbs: Double = 0.0,
    val fat: Double = 0.0,
    val fiber: Double = 0.0
)

data class LoggedFoodItem(
    val foodId: String? = null,
    val foodName: String? = null,
    val quantity: Double = 0.0,
    val unit: String? = null,
    val calculatedNutrition: MealItemNutrition? = null
)

data class LoggedMeal(
    val _id: String? = null,
    val date: String? = null,
    val mealType: String? = null,
    val items: List<LoggedFoodItem> = emptyList(),
    val totalMealNutrition: MealItemNutrition? = null
)

data class TodayNutritionResponse(
    val success: Boolean = false,
    val data: TodayNutritionData? = null
)

data class TodayNutritionData(
    val eatenCalories: Int = 0,
    val remainingCalories: Int = 2000,
    val targetCalories: Int = 2000,
    val protein: MacroDetail = MacroDetail(0.0, 100.0),
    val carbs: MacroDetail = MacroDetail(0.0, 250.0),
    val fat: MacroDetail = MacroDetail(0.0, 65.0),
    val water: MacroDetail = MacroDetail(0.0, 3.0),
    val mealsLogged: List<LoggedMeal> = emptyList()
)

data class MacroDetail(
    val current: Double = 0.0,
    val target: Double = 100.0
)

data class NutritionTargetResponse(
    val success: Boolean = false,
    val data: NutritionTargetData? = null
)

data class NutritionTargetData(
    val targetCalories: Int = 2000,
    val targetProtein: Int = 120,
    val targetCarbs: Int = 250,
    val targetFat: Int = 60,
    val targetWaterMl: Int = 3000,
    val targetWaterLiters: Double = 3.0,
    val goalType: String = "MAINTAIN"
)

data class AiInsightResponse(
    val success: Boolean,
    val insight: String
)

data class AiAskRequest(
    val query: String
)

data class AiAskResponse(
    val success: Boolean,
    val answer: String
)

interface NutritionApiService {

    @GET("api/v1/nutrition/today")
    suspend fun getTodayNutrition(
        @Query("date") date: String? = null
    ): Response<TodayNutritionResponse>

    @GET("api/v1/nutrition/targets")
    suspend fun getTargets(): Response<NutritionTargetResponse>

    @GET("api/v1/nutrition/foods/search")
    suspend fun searchFoods(
        @Query("q") query: String? = null,
        @Query("category") category: String? = null,
        @Query("mealType") mealType: String? = null
    ): Response<FoodSearchResponse>

    @POST("api/v1/nutrition/meals/add")
    suspend fun addMeal(@Body request: AddMealRequest): Response<Any>

    @POST("api/v1/nutrition/water/add")
    suspend fun addWater(@Body request: AddWaterRequest): Response<Any>

    @GET("api/v1/nutrition/ai/insight")
    suspend fun getAiInsight(): Response<AiInsightResponse>

    @POST("api/v1/nutrition/scan")
    suspend fun scanFoodImage(@Body request: ScanFoodRequest): Response<ScanFoodResponse>

    @Multipart
    @POST("api/nutrition/scan")
    suspend fun scanFoodImageMultipart(
        @Part image: MultipartBody.Part
    ): Response<ScanFoodResponse>

    @POST("api/v1/nutrition/ai/ask")
    suspend fun askAiCoach(@Body request: AiAskRequest): Response<AiAskResponse>

    // 🥗 Feature A: Protein Recommendations
    @GET("api/nutrition/recommendations")
    suspend fun getRecommendations(
        @Query("date") date: String? = null
    ): Response<com.esha.fitsync.models.RecommendationResponse>

    @POST("api/nutrition/recommendations/{id}/add")
    suspend fun addRecommendation(
        @Path("id") recipeId: String,
        @Body request: com.esha.fitsync.models.AddRecommendationRequest
    ): Response<com.esha.fitsync.models.AddRecommendationResponse>

    // 📊 Feature B: Weekly Nutrition Bar Graph
    @GET("api/nutrition/weekly")
    suspend fun getWeeklyNutrition(
        @Query("endDate") endDate: String? = null
    ): Response<List<com.esha.fitsync.models.WeeklyNutritionDay>>

    // 💡 Feature C: Dynamic AI Nutrition Insight
    @GET("api/nutrition/insight")
    suspend fun getDynamicInsight(
        @Query("date") date: String? = null
    ): Response<com.esha.fitsync.models.DynamicInsightResponse>
}