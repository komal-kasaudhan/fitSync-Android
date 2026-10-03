package com.esha.fitsync.payment

import android.app.Activity
import com.esha.fitsync.api.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface PaymentHandler {
    suspend fun isPaymentsEnabled(): Boolean
    suspend fun processPayment(
        activity: Activity,
        amount: Double,
        orderId: String?,
        description: String,
        onSuccess: (paymentId: String, orderId: String, signature: String) -> Unit,
        onFailure: (error: String) -> Unit
    )
}

object FitSyncPaymentHandler : PaymentHandler {

    private var cachedPaymentsEnabled: Boolean? = null

    override suspend fun isPaymentsEnabled(): Boolean {
        if (cachedPaymentsEnabled != null) return cachedPaymentsEnabled!!
        return try {
            val response = RetrofitClient.api.getAppConfig()
            if (response.isSuccessful && response.body() != null) {
                val enabled = response.body()!!.paymentsEnabled
                cachedPaymentsEnabled = enabled
                enabled
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    fun setCachedPaymentsEnabled(enabled: Boolean) {
        cachedPaymentsEnabled = enabled
    }

    override suspend fun processPayment(
        activity: Activity,
        amount: Double,
        orderId: String?,
        description: String,
        onSuccess: (paymentId: String, orderId: String, signature: String) -> Unit,
        onFailure: (error: String) -> Unit
    ) {
        val enabled = isPaymentsEnabled()
        if (!enabled) {
            withContext(Dispatchers.Main) {
                val mockPaymentId = "pay_dev_${System.currentTimeMillis()}"
                val safeOrderId = orderId ?: "order_dev_${System.currentTimeMillis()}"
                val mockSignature = "sig_dev_${System.currentTimeMillis()}"
                onSuccess(mockPaymentId, safeOrderId, mockSignature)
            }
        } else {
            withContext(Dispatchers.Main) {
                onFailure("Razorpay SDK is unplugged in dev mode. Enable Razorpay when live keys are configured.")
            }
        }
    }
}
