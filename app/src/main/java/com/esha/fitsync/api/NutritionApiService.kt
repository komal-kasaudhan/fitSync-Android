package com.esha.fitsync.api

import retrofit2.Response
import retrofit2.http.*

data class AddWaterRequest(
    val amountMl: Int,
    val date: String? = null
)

data class ScanFoodRequest(
    val imageBase64: String
)

data class ScanFoodResponse(
    val success: Boolean,
    val foodName: String,
    val calories: Int,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val estimatedGram: Double
)

data class AddMealRequest(
    val date: String,
    val mealType: String,
    val foodId: String,
    val quantity: Double
)

data class FoodItem(
    val id: String = "",
    val _id: String = "",
    val name: String = "",
    val category: String = "General",
    val servingType: String = "g",
    val servingWeight: Double = 100.0,
    val calories: Int = 0,
    val protein: Double = 0.0,
    val carbs: Double = 0.0,
    val fat: Double = 0.0,
    val servingUnit: String = "100g"
)

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

    @GET("api/v1/nutrition/foods/search")
    suspend fun searchFoods(
        @Query("q") query: String,
        @Query("category") category: String? = null
    ): Response<FoodSearchResponse>

    @POST("api/v1/nutrition/meals/add")
    suspend fun addMeal(@Body request: AddMealRequest): Response<Any>

    @POST("api/v1/nutrition/water/add")
    suspend fun addWater(@Body request: AddWaterRequest): Response<Any>

    @GET("api/v1/nutrition/ai/insight")
    suspend fun getAiInsight(): Response<AiInsightResponse>

    @POST("api/v1/nutrition/scan")
    suspend fun scanFoodImage(@Body request: ScanFoodRequest): Response<ScanFoodResponse>

    @POST("api/v1/nutrition/ai/ask")
    suspend fun askAiCoach(@Body request: AiAskRequest): Response<AiAskResponse>
}