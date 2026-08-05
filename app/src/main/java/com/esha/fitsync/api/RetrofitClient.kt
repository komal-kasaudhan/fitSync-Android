package com.esha.fitsync.api

import android.content.Context
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    private const val BASE_URL = "http://10.21.198.21:8000/"

    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(logging)
        .addInterceptor { chain ->
            val originalRequest = chain.request()
            val requestBuilder = originalRequest.newBuilder()

            val context = appContext
            if (context != null) {
                val sharedPreferences =
                    context.getSharedPreferences("FitSync", Context.MODE_PRIVATE)
                val token = sharedPreferences.getString("TOKEN", null)

                Log.d("FITSYNC_NETWORK", "Interceptor Token Fetched: $token")

                if (!token.isNullOrEmpty()) {
                    requestBuilder.addHeader("Authorization", "Bearer $token")
                    Log.d("FITSYNC_NETWORK", "Token injected successfully into Request Header!")
                } else {
                    Log.e(
                        "FITSYNC_NETWORK",
                        "TOKEN MISSED! SharedPreferences returned empty/null string."
                    )
                }
            } else {
                Log.e("FITSYNC_NETWORK", "APP CONTEXT IS NULL! Interceptor skipped token pulling.")
            }

            chain.proceed(requestBuilder.build())
        }
        .build()

    val apiService: NutritionApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(NutritionApiService::class.java)
    }
    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)


    }
}