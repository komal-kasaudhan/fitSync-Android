package com.esha.fitsync.api

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.edit
import com.esha.fitsync.BuildConfig
import com.esha.fitsync.screens.LoginActivity
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    val BASE_URL: String
        get() = BuildConfig.BASE_URL.ifEmpty { "http://10.0.2.2:8000/" }

    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    private val logging = HttpLoggingInterceptor { message ->
        Log.d("FitSyncApi", message)
    }.apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(90, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(90, TimeUnit.SECONDS)
        // 1. Idempotent GET Retry Interceptor
        .addInterceptor { chain ->
            val request = chain.request()
            var response: okhttp3.Response? = null
            var exception: java.io.IOException? = null
            val isGet = request.method.equals("GET", ignoreCase = true)
            val maxRetries = if (isGet) 2 else 0
            var tryCount = 0

            while (tryCount <= maxRetries) {
                try {
                    response = chain.proceed(request)
                    if (response.isSuccessful || tryCount >= maxRetries) {
                        return@addInterceptor response
                    }
                    // If 503 or 504 on GET, allow quick retry
                    if (isGet && (response.code == 503 || response.code == 504)) {
                        response.close()
                        tryCount++
                        Thread.sleep(500L * tryCount)
                        continue
                    }
                    return@addInterceptor response
                } catch (e: java.io.IOException) {
                    exception = e
                    tryCount++
                    if (tryCount <= maxRetries && isGet) {
                        Log.w("FitSyncApi", "GET ${request.url} failed (${e.message}). Retrying attempt $tryCount of $maxRetries...")
                        try { Thread.sleep(500L * tryCount) } catch (_: InterruptedException) {}
                    } else {
                        Log.e("FitSyncApi", "Network failure on ${request.method} ${request.url}: ${e.message}. (Physical phone: check 'adb reverse tcp:8000 tcp:8000')")
                        throw e
                    }
                }
            }
            response ?: throw (exception ?: java.io.IOException("Request failed"))
        }
        // 2. Metrics & Status Interceptor
        .addInterceptor { chain ->
            val request = chain.request()
            val startNs = System.nanoTime()
            val response: okhttp3.Response
            try {
                response = chain.proceed(request)
            } catch (e: Exception) {
                val tookMs = (System.nanoTime() - startNs) / 1e6
                Log.e("FitSyncApi", "FAILED: ${request.method} ${request.url} took ${String.format(java.util.Locale.US, "%.1f", tookMs)}ms -> ${e.localizedMessage}")
                throw e
            }
            val tookMs = (System.nanoTime() - startNs) / 1e6
            Log.d("FitSyncApi", "STATUS ${response.code} [${request.method}] ${request.url} took ${String.format(java.util.Locale.US, "%.1f", tookMs)}ms")
            response
        }
        // 3. Auth Token & 401 Handler
        .addInterceptor { chain ->
            val originalRequest = chain.request()
            val requestBuilder = originalRequest.newBuilder()

            val context = appContext
            if (context != null) {
                val sharedPreferences =
                    context.getSharedPreferences("FitSync", Context.MODE_PRIVATE)
                val token = sharedPreferences.getString("TOKEN", null)

                if (!token.isNullOrEmpty()) {
                    requestBuilder.addHeader("Authorization", "Bearer $token")
                } else {
                    Log.w("FitSyncApi", "No token found in SharedPreferences for request: ${originalRequest.url}")
                }
            }

            val response = chain.proceed(requestBuilder.build())

            if (response.code == 401) {
                Log.w("FitSyncApi", "401 Unauthorized received! Clearing token and redirecting to Login.")
                context?.let { ctx ->
                    val sharedPreferences = ctx.getSharedPreferences("FitSync", Context.MODE_PRIVATE)
                    sharedPreferences.edit {
                        remove("TOKEN")
                    }

                    val intent = Intent(ctx, LoginActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    ctx.startActivity(intent)
                }
            }

            response
        }
        .apply {
            if (BuildConfig.DEBUG) {
                addInterceptor(logging)
            }
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