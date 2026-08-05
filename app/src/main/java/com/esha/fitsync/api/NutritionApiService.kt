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

data class AddMealFoodItem(
    val foodId: String,
    val quantity: Double
)

data class AddMealRequest(
    val date: String,
    val mealType: String,
    val foods: List<AddMealFoodItem>
)

data class FoodItem(
    val id: String = "",
    val name: String = "",
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
data class TodayNutritionResponse(
    val success: Boolean,
    val data: TodayNutritionData
)

data class TodayNutritionData(
    val eatenCalories: Int,
    val remainingCalories: Int,
    val targetCalories: Int = 2000,
    val protein: MacroDetail,
    val carbs: MacroDetail,
    val fat: MacroDetail,
    val water: MacroDetail
)

data class MacroDetail(
    val current: Double,
    val target: Double
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