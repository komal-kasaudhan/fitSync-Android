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

        // Go to Signup Screen
        tvSignup.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
            finish()
        }


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
                                        this@LoginActivity,
                                        WelcomeActivity::class.java
                                    )
                                )

                                finish()

                            } else {

                                Toast.makeText(
                                    this@LoginActivity,
                                    authResponse.message,
                                    Toast.LENGTH_SHORT
                                ).show()

                            }

                        } else {

                            Toast.makeText(
                                this@LoginActivity,
                                "Login Failed",
                                Toast.LENGTH_SHORT
                            ).show()

                        }
                    }

                    override fun onFailure(
                        call: Call<AuthResponse>,
                        t: Throwable
                    ) {

                        Toast.makeText(
                            this@LoginActivity,
                            "Error : ${t.localizedMessage}",
                            Toast.LENGTH_LONG
                        ).show()

                    }

                })

        }
    }
}