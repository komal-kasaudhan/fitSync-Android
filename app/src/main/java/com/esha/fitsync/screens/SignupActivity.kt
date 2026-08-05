package com.esha.fitsync.screens

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.esha.fitsync.R
import com.esha.fitsync.api.RetrofitClient
import com.esha.fitsync.models.AuthResponse
import com.esha.fitsync.models.SignupRequest
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SignupActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        val etName = findViewById<EditText>(R.id.etName)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val etConfirmPassword = findViewById<EditText>(R.id.etconfirmPassword)
        val btnSignup = findViewById<Button>(R.id.btnCreateAccount)

        btnSignup.setOnClickListener {

            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val confirmPassword = etConfirmPassword.text.toString().trim()

            if (name.isEmpty()) {
                etName.error = "Enter your name"
                return@setOnClickListener
            }

            if (email.isEmpty()) {
                etEmail.error = "Enter email"
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etEmail.error = "Invalid email"
                return@setOnClickListener
            }

            if (password.length < 6) {
                etPassword.error = "Password must be at least 6 characters"
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                etConfirmPassword.error = "Passwords do not match"
                return@setOnClickListener
            }

            val request = SignupRequest(
                name = name,
                email = email,
                password = password
            )

            RetrofitClient.api.signup(request)
                .enqueue(object : Callback<AuthResponse> {

                    override fun onResponse(
                        call: Call<AuthResponse>,
                        response: Response<AuthResponse>
                    ) {

                        if (response.isSuccessful && response.body() != null) {

                            val authResponse = response.body()!!

                            if (authResponse.success) {

                                Toast.makeText(
                                    this@SignupActivity,
                                    authResponse.message,
                                    Toast.LENGTH_SHORT
                                ).show()

                                val sharedPref =
                                    getSharedPreferences("FitSync", MODE_PRIVATE)

                                sharedPref.edit()
                                    .putString("TOKEN", authResponse.token)
                                    .apply()

                                startActivity(
                                    Intent(
                                        this@SignupActivity,
                                        WelcomeActivity::class.java
                                    )
                                )

                                finish()

                            } else {

                                Toast.makeText(
                                    this@SignupActivity,
                                    authResponse.message,
                                    Toast.LENGTH_SHORT
                                ).show()

                            }

                        } else {

                            Toast.makeText(
                                this@SignupActivity,
                                "Signup Failed",
                                Toast.LENGTH_SHORT
                            ).show()

                        }
                    }

                    override fun onFailure(
                        call: Call<AuthResponse>,
                    t: Throwable
                    ) {

                        Toast.makeText(
                            this@SignupActivity,
                            t.localizedMessage ?: "Network Error",
                            Toast.LENGTH_LONG
                        ).show()

                    }

                })

        }
    }
}