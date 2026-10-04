package com.esha.fitsync.screens

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.esha.fitsync.R
import com.esha.fitsync.api.RetrofitClient
import com.esha.fitsync.models.AuthResponse
import com.esha.fitsync.models.LoginRequest
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvSignup = findViewById<TextView>(R.id.Signup)
        val tvForgotPassword = findViewById<TextView>(R.id.tvForgotPassword)

        // Go to Signup Screen
        tvSignup.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
            finish()
        }

        // Go to Forgot Password Screen
        tvForgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }

        // Standard Email / Password Login (unchanged)
        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty()) {
                etEmail.error = "Enter Email"
                etEmail.requestFocus()
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etEmail.error = "Invalid Email"
                etEmail.requestFocus()
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                etPassword.error = "Enter Password"
                etPassword.requestFocus()
                return@setOnClickListener
            }

            val request = LoginRequest(
                email = email,
                password = password
            )

            RetrofitClient.api.login(request)
                .enqueue(object : Callback<AuthResponse> {

                    override fun onResponse(
                        call: Call<AuthResponse>,
                        response: Response<AuthResponse>
                    ) {
                        if (response.isSuccessful && response.body() != null) {
                            val authResponse = response.body()!!

                            if (authResponse.success) {
                                Toast.makeText(
                                    this@LoginActivity,
                                    authResponse.message.ifEmpty { "Login Successful!" },
                                    Toast.LENGTH_SHORT
                                ).show()

                                val sharedPref =
                                    getSharedPreferences("FitSync", MODE_PRIVATE)

                                val previousEmail = sharedPref.getString("USER_EMAIL", null)
                                val newEmail = authResponse.user?.email ?: email

                                if (previousEmail != null && !previousEmail.equals(newEmail, ignoreCase = true)) {
                                    sharedPref.edit().clear().apply()
                                    com.esha.fitsync.data.AppCacheManager.clearAll()
                                    com.esha.fitsync.screens.fragments.WorkoutFragment.hasAutoOpenedBottomSheetThisSession = false
                                }

                                val editor = sharedPref.edit()
                                editor.putString("TOKEN", authResponse.token)
                                editor.putString("USER_EMAIL", newEmail)

                                val userId = authResponse.user?.id ?: authResponse.data?.id
                                if (!userId.isNullOrEmpty()) {
                                    editor.putString("USER_ID", userId)
                                }

                                // Roles list extraction
                                val rolesList = authResponse.roles.ifEmpty {
                                    authResponse.user?.roles ?: emptyList()
                                }
                                editor.putString("ROLES_STRING", rolesList.joinToString(","))

                                val needsOnboarding = authResponse.needsOnboarding
                                    ?: (authResponse.user?.needsOnboarding ?: false)
                                editor.putBoolean("NEEDS_ONBOARDING", needsOnboarding)

                                // Set fallback username from email if not already present
                                val currentUserName = sharedPref.getString("USER_NAME", null)
                                if (currentUserName.isNullOrEmpty()) {
                                    val fallbackName = authResponse.user?.name?.ifBlank { null }
                                        ?: email.substringBefore('@').replaceFirstChar { it.uppercase() }
                                    editor.putString("USER_NAME", fallbackName)
                                }
                                editor.apply()

                                // Part 3 Routing Rules:
                                // 1. Admin goes straight to Admin Dashboard
                                if (rolesList.contains("admin")) {
                                    startActivity(
                                        Intent(
                                            this@LoginActivity,
                                            com.esha.fitsync.screens.admin.AdminDashboardActivity::class.java
                                        )
                                    )
                                    finish()
                                    return
                                }

                                // 2. If onboarding is not needed (or partner skipping by default)
                                if (!needsOnboarding) {
                                    startActivity(
                                        Intent(
                                            this@LoginActivity,
                                            MainActivity::class.java
                                        )
                                    )
                                    finish()
                                    return
                                }

                                // 3. Regular new user needing onboarding
                                startActivity(
                                    Intent(
                                        this@LoginActivity,
                                        WelcomeActivity::class.java
                                    )
                                )
                                finish()
                            } else {
                                Toast.makeText(
                                    this@LoginActivity,
                                    authResponse.message.ifEmpty { "Invalid credentials" },
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        } else {
                            val errorMsg = try {
                                val errorStr = response.errorBody()?.string()
                                if (!errorStr.isNullOrEmpty()) {
                                    val json = org.json.JSONObject(errorStr)
                                    json.optString("message", json.optString("error", "Login failed (${response.code()})"))
                                } else {
                                    "Login failed (${response.code()})"
                                }
                            } catch (e: Exception) {
                                "Login failed (${response.code()})"
                            }

                            Toast.makeText(
                                this@LoginActivity,
                                errorMsg,
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }

                    override fun onFailure(
                        call: Call<AuthResponse>,
                        t: Throwable
                    ) {
                        Toast.makeText(
                            this@LoginActivity,
                            "Network Error: ${t.localizedMessage ?: "Unable to reach server"}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                })
        }
    }
}