package com.esha.fitsync.screens

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Patterns
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.esha.fitsync.R
import com.esha.fitsync.api.RetrofitClient
import com.esha.fitsync.models.ForgotPasswordRequest
import com.esha.fitsync.models.ResetPasswordRequest
import com.esha.fitsync.models.VerifyOtpRequest
import com.google.android.material.button.MaterialButton
import com.google.gson.JsonParser
import kotlinx.coroutines.launch
import java.io.IOException

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageView

    // Step 1: Email
    private lateinit var layoutStepEmail: LinearLayout
    private lateinit var etForgotEmail: EditText
    private lateinit var tvStep1Error: TextView
    private lateinit var btnSendCode: MaterialButton
    private lateinit var pbStep1Loading: ProgressBar

    // Step 2: OTP
    private lateinit var layoutStepOtp: LinearLayout
    private lateinit var tvStep2Subtitle: TextView
    private lateinit var etForgotOtp: EditText
    private lateinit var tvResendTimer: TextView
    private lateinit var btnResendOtp: TextView
    private lateinit var tvStep2Error: TextView
    private lateinit var btnVerifyOtp: MaterialButton
    private lateinit var pbStep2Loading: ProgressBar

    // Step 3: Reset Password
    private lateinit var layoutStepReset: LinearLayout
    private lateinit var etNewPassword: EditText
    private lateinit var etConfirmNewPassword: EditText
    private lateinit var tvStep3Error: TextView
    private lateinit var btnResetPassword: MaterialButton
    private lateinit var pbStep3Loading: ProgressBar

    // Step 4: Success
    private lateinit var layoutStepSuccess: LinearLayout
    private lateinit var btnReturnToLogin: MaterialButton

    private var currentStep = 1
    private var enteredEmail: String = ""
    private var receivedResetToken: String = ""
    private var resendTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        initViews()
        setupListeners()
        setupBackNavigation()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)

        // Step 1
        layoutStepEmail = findViewById(R.id.layoutStepEmail)
        etForgotEmail = findViewById(R.id.etForgotEmail)
        tvStep1Error = findViewById(R.id.tvStep1Error)
        btnSendCode = findViewById(R.id.btnSendCode)
        pbStep1Loading = findViewById(R.id.pbStep1Loading)

        // Step 2
        layoutStepOtp = findViewById(R.id.layoutStepOtp)
        tvStep2Subtitle = findViewById(R.id.tvStep2Subtitle)
        etForgotOtp = findViewById(R.id.etForgotOtp)
        tvResendTimer = findViewById(R.id.tvResendTimer)
        btnResendOtp = findViewById(R.id.btnResendOtp)
        tvStep2Error = findViewById(R.id.tvStep2Error)
        btnVerifyOtp = findViewById(R.id.btnVerifyOtp)
        pbStep2Loading = findViewById(R.id.pbStep2Loading)

        // Step 3
        layoutStepReset = findViewById(R.id.layoutStepReset)
        etNewPassword = findViewById(R.id.etNewPassword)
        etConfirmNewPassword = findViewById(R.id.etConfirmNewPassword)
        tvStep3Error = findViewById(R.id.tvStep3Error)
        btnResetPassword = findViewById(R.id.btnResetPassword)
        pbStep3Loading = findViewById(R.id.pbStep3Loading)

        // Step 4
        layoutStepSuccess = findViewById(R.id.layoutStepSuccess)
        btnReturnToLogin = findViewById(R.id.btnReturnToLogin)
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            handleBackAction()
        }

        // Step 1: Send OTP
        btnSendCode.setOnClickListener {
            val email = etForgotEmail.text.toString().trim()
            if (email.isEmpty()) {
                showError(tvStep1Error, "Please enter your email address")
                return@setOnClickListener
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                showError(tvStep1Error, "Please enter a valid email address")
                return@setOnClickListener
            }

            hideError(tvStep1Error)
            enteredEmail = email
            requestOtp(isResend = false)
        }

        // Step 2: Verify OTP
        btnVerifyOtp.setOnClickListener {
            val otp = etForgotOtp.text.toString().trim()
            if (otp.length != 6) {
                showError(tvStep2Error, "Please enter the complete 6-digit verification code")
                return@setOnClickListener
            }

            hideError(tvStep2Error)
            verifyOtpCode(otp)
        }

        btnResendOtp.setOnClickListener {
            hideError(tvStep2Error)
            requestOtp(isResend = true)
        }

        // Step 3: Reset Password
        btnResetPassword.setOnClickListener {
            val newPass = etNewPassword.text.toString().trim()
            val confirmPass = etConfirmNewPassword.text.toString().trim()

            if (newPass.length < 6) {
                showError(tvStep3Error, "Password must be at least 6 characters")
                return@setOnClickListener
            }
            if (newPass != confirmPass) {
                showError(tvStep3Error, "Passwords do not match")
                return@setOnClickListener
            }

            hideError(tvStep3Error)
            resetPasswordWithToken(newPass)
        }

        // Step 4: Return to Login
        btnReturnToLogin.setOnClickListener {
            navigateToLogin()
        }
    }

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleBackAction()
            }
        })
    }

    private fun handleBackAction() {
        when (currentStep) {
            2 -> {
                resendTimer?.cancel()
                showStep(1)
            }
            3 -> {
                showStep(2)
            }
            4 -> {
                navigateToLogin()
            }
            else -> {
                finish()
            }
        }
    }

    private fun showStep(step: Int) {
        currentStep = step
        layoutStepEmail.visibility = if (step == 1) View.VISIBLE else View.GONE
        layoutStepOtp.visibility = if (step == 2) View.VISIBLE else View.GONE
        layoutStepReset.visibility = if (step == 3) View.VISIBLE else View.GONE
        layoutStepSuccess.visibility = if (step == 4) View.VISIBLE else View.GONE
    }

    private fun requestOtp(isResend: Boolean) {
        val targetButton = if (isResend) btnResendOtp else btnSendCode
        val targetLoading = if (isResend) pbStep2Loading else pbStep1Loading
        val targetError = if (isResend) tvStep2Error else tvStep1Error

        targetButton.isEnabled = false
        targetLoading.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.forgotPassword(ForgotPasswordRequest(enteredEmail))
                if (response.isSuccessful && response.body()?.success == true) {
                    tvStep2Subtitle.text = "Enter the 6-digit code sent to $enteredEmail."
                    startResendCountdown()
                    showStep(2)
                } else {
                    val msg = parseErrorMessage(response.errorBody()?.string())
                        ?: response.body()?.message
                        ?: "Failed to send code. Please try again."
                    showError(targetError, msg)
                }
            } catch (e: IOException) {
                showError(targetError, "No internet connection. Please check your network and try again.")
            } catch (e: Exception) {
                showError(targetError, "An unexpected error occurred: ${e.message}")
            } finally {
                targetButton.isEnabled = true
                targetLoading.visibility = View.GONE
            }
        }
    }

    private fun startResendCountdown() {
        resendTimer?.cancel()
        btnResendOtp.visibility = View.GONE
        tvResendTimer.visibility = View.VISIBLE

        resendTimer = object : CountDownTimer(30000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsRemaining = (millisUntilFinished / 1000) + 1
                tvResendTimer.text = "Resend code in ${secondsRemaining}s"
            }

            override fun onFinish() {
                tvResendTimer.visibility = View.GONE
                btnResendOtp.visibility = View.VISIBLE
                btnResendOtp.isEnabled = true
            }
        }.start()
    }

    private fun verifyOtpCode(otp: String) {
        btnVerifyOtp.isEnabled = false
        pbStep2Loading.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.verifyOtp(VerifyOtpRequest(enteredEmail, otp))
                if (response.isSuccessful && response.body()?.success == true) {
                    receivedResetToken = response.body()?.resetToken ?: ""
                    resendTimer?.cancel()
                    showStep(3)
                } else {
                    val msg = parseErrorMessage(response.errorBody()?.string())
                        ?: response.body()?.message
                        ?: "Invalid or expired OTP. Please try again."
                    showError(tvStep2Error, msg)
                }
            } catch (e: IOException) {
                showError(tvStep2Error, "No internet connection. Please check your network and try again.")
            } catch (e: Exception) {
                showError(tvStep2Error, "An unexpected error occurred: ${e.message}")
            } finally {
                btnVerifyOtp.isEnabled = true
                pbStep2Loading.visibility = View.GONE
            }
        }
    }

    private fun resetPasswordWithToken(newPassword: String) {
        btnResetPassword.isEnabled = false
        pbStep3Loading.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.resetPassword(
                    ResetPasswordRequest(
                        email = enteredEmail,
                        resetToken = receivedResetToken,
                        newPassword = newPassword
                    )
                )

                if (response.isSuccessful && response.body()?.success == true) {
                    showStep(4)
                } else {
                    val msg = parseErrorMessage(response.errorBody()?.string())
                        ?: response.body()?.message
                        ?: "Failed to reset password. Please try again."
                    showError(tvStep3Error, msg)
                }
            } catch (e: IOException) {
                showError(tvStep3Error, "No internet connection. Please check your network and try again.")
            } catch (e: Exception) {
                showError(tvStep3Error, "An unexpected error occurred: ${e.message}")
            } finally {
                btnResetPassword.isEnabled = true
                pbStep3Loading.visibility = View.GONE
            }
        }
    }

    private fun parseErrorMessage(errorBody: String?): String? {
        if (errorBody.isNullOrBlank()) return null
        return try {
            val json = JsonParser.parseString(errorBody).asJsonObject
            json.get("message")?.asString
        } catch (_: Exception) {
            null
        }
    }

    private fun showError(tv: TextView, message: String) {
        tv.text = message
        tv.visibility = View.VISIBLE
    }

    private fun hideError(tv: TextView) {
        tv.text = ""
        tv.visibility = View.GONE
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        resendTimer?.cancel()
    }
}
