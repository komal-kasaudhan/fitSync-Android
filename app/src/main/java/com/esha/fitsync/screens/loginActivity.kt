package com.esha.fitsync.screens

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.lifecycleScope
import com.esha.fitsync.BuildConfig
import com.esha.fitsync.R
import com.esha.fitsync.api.RetrofitClient
import com.esha.fitsync.models.AuthResponse
import com.esha.fitsync.models.GoogleLoginRequest
import com.esha.fitsync.models.LoginRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvSignup = findViewById<TextView>(R.id.Signup)
        val tvForgotPassword = findViewById<TextView>(R.id.tvForgotPassword)
        val btnGoogle = findViewById<android.view.View>(R.id.btnGoogle)

        // Go to Signup Screen
        tvSignup.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
            finish()
        }

        // Go to Forgot Password Screen
        tvForgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }

        // Continue with Google
        btnGoogle.setOnClickListener {
            performGoogleSignIn()
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

                                val editor = sharedPref.edit()
                                editor.putString("TOKEN", authResponse.token)
                                editor.putString("USER_EMAIL", authResponse.user?.email ?: email)

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

    private fun performGoogleSignIn() {
        val webClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID.trim()
        if (webClientId.isEmpty()) {
            Toast.makeText(
                this,
                "Google Client ID is missing. Please configure GOOGLE_WEB_CLIENT_ID in local.properties.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        lifecycleScope.launch {
            try {
                val credentialManager = CredentialManager.create(this@LoginActivity)
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(webClientId)
                    .setAutoSelectEnabled(false)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(
                    request = request,
                    context = this@LoginActivity
                )

                val credential = result.credential
                if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val idToken = googleIdTokenCredential.idToken
                    authenticateWithGoogleBackend(idToken)
                } else {
                    Toast.makeText(this@LoginActivity, "Unable to extract Google credentials", Toast.LENGTH_SHORT).show()
                }
            } catch (e: GetCredentialCancellationException) {
                // User cancelled the prompt - safe to ignore
            } catch (e: NoCredentialException) {
                Toast.makeText(this@LoginActivity, "No Google accounts found on this device", Toast.LENGTH_SHORT).show()
            } catch (e: GetCredentialException) {
                Toast.makeText(this@LoginActivity, "Google Sign-In failed: ${e.message}", Toast.LENGTH_SHORT).show()
            } catch (e: IOException) {
                Toast.makeText(this@LoginActivity, "No internet connection. Please check your network.", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@LoginActivity, "Google Sign-In error: ${e.localizedMessage ?: "Unknown error"}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun authenticateWithGoogleBackend(idToken: String) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.loginWithGoogle(GoogleLoginRequest(idToken))
                if (response.isSuccessful && response.body() != null) {
                    val authResponse = response.body()!!
                    if (authResponse.success) {
                        Toast.makeText(
                            this@LoginActivity,
                            authResponse.message.ifEmpty { "Google Login Successful!" },
                            Toast.LENGTH_SHORT
                        ).show()

                        val userObj = authResponse.user ?: authResponse.data
                        val token = authResponse.token
                        val email = userObj?.email ?: ""
                        val name = userObj?.name ?: ""
                        val rolesList = authResponse.roles.ifEmpty { userObj?.roles ?: emptyList() }
                        val needsOnboarding = authResponse.needsOnboarding ?: (userObj?.needsOnboarding ?: false)

                        val sharedPref = getSharedPreferences("FitSync", MODE_PRIVATE)
                        val editor = sharedPref.edit()
                        editor.putString("TOKEN", token)
                        if (email.isNotBlank()) {
                            editor.putString("USER_EMAIL", email)
                        }
                        editor.putString("ROLES_STRING", rolesList.joinToString(","))
                        editor.putBoolean("NEEDS_ONBOARDING", needsOnboarding)

                        val currentUserName = sharedPref.getString("USER_NAME", null)
                        if (currentUserName.isNullOrEmpty()) {
                            val fallbackName = name.ifBlank { null }
                                ?: email.substringBefore('@').replaceFirstChar { it.uppercase() }
                            editor.putString("USER_NAME", fallbackName)
                        } else if (name.isNotBlank()) {
                            editor.putString("USER_NAME", name)
                        }
                        editor.apply()

                        // Follow exactly same routing as normal login
                        if (rolesList.contains("admin")) {
                            startActivity(
                                Intent(
                                    this@LoginActivity,
                                    com.esha.fitsync.screens.admin.AdminDashboardActivity::class.java
                                )
                            )
                            finish()
                            return@launch
                        }

                        if (!needsOnboarding) {
                            startActivity(
                                Intent(
                                    this@LoginActivity,
                                    MainActivity::class.java
                                )
                            )
                            finish()
                            return@launch
                        }

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
                            authResponse.message.ifEmpty { "Google login failed" },
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    val errorMsg = try {
                        val errorStr = response.errorBody()?.string()
                        if (!errorStr.isNullOrEmpty()) {
                            val json = org.json.JSONObject(errorStr)
                            json.optString("message", json.optString("error", "Google login failed (${response.code()})"))
                        } else {
                            "Google login failed (${response.code()})"
                        }
                    } catch (e: Exception) {
                        "Google login failed (${response.code()})"
                    }
                    Toast.makeText(this@LoginActivity, errorMsg, Toast.LENGTH_LONG).show()
                }
            } catch (e: IOException) {
                Toast.makeText(this@LoginActivity, "No internet connection. Please check your network.", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@LoginActivity, "Network Error: ${e.localizedMessage ?: "Unable to reach server"}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}