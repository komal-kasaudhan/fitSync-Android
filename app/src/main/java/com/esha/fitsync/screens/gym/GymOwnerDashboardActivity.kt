package com.esha.fitsync.screens.gym

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.esha.fitsync.R
import com.esha.fitsync.api.RetrofitClient
import com.esha.fitsync.models.CheckInRequest
import com.esha.fitsync.screens.gym.adapters.GymTransactionAdapter
import kotlinx.coroutines.launch

class GymOwnerDashboardActivity : AppCompatActivity() {

    private var gymId: String = ""
    private var gymName: String = ""

    private lateinit var tvGymName: TextView
    private lateinit var etCheckInCode: EditText
    private lateinit var btnVerify: Button
    private lateinit var layoutCheckInResult: LinearLayout
    private lateinit var tvCheckInResultTitle: TextView
    private lateinit var tvCheckInMemberInfo: TextView
    private lateinit var tvCheckInSessionInfo: TextView

    private lateinit var tvPartnerEarnings: TextView
    private lateinit var tvTotalRevenue: TextView
    private lateinit var tvPlatformFee: TextView
    private lateinit var tvCountTotal: TextView
    private lateinit var tvCountAttended: TextView
    private lateinit var tvCountConfirmed: TextView
    private lateinit var tvCountCancelled: TextView

    private lateinit var rvTransactions: RecyclerView
    private lateinit var tvNoTransactions: TextView
    private val txAdapter = GymTransactionAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gym_owner_dashboard)

        gymId = intent.getStringExtra("GYM_ID") ?: ""
        gymName = intent.getStringExtra("GYM_NAME") ?: "Gym Partner Dashboard"

        initViews()
        setupListeners()

        if (gymId.isEmpty()) {
            fetchMyGymId()
        } else {
            loadDashboardData()
        }
    }

    private fun initViews() {
        tvGymName = findViewById(R.id.tvOwnerGymName)
        etCheckInCode = findViewById(R.id.etCheckInCode)
        btnVerify = findViewById(R.id.btnVerifyCheckIn)
        layoutCheckInResult = findViewById(R.id.layoutCheckInResult)
        tvCheckInResultTitle = findViewById(R.id.tvCheckInResultTitle)
        tvCheckInMemberInfo = findViewById(R.id.tvCheckInMemberInfo)
        tvCheckInSessionInfo = findViewById(R.id.tvCheckInSessionInfo)

        tvPartnerEarnings = findViewById(R.id.tvTotalPartnerEarnings)
        tvTotalRevenue = findViewById(R.id.tvTotalRevenue)
        tvPlatformFee = findViewById(R.id.tvTotalPlatformFee)
        tvCountTotal = findViewById(R.id.tvCountTotal)
        tvCountAttended = findViewById(R.id.tvCountAttended)
        tvCountConfirmed = findViewById(R.id.tvCountConfirmed)
        tvCountCancelled = findViewById(R.id.tvCountCancelled)

        rvTransactions = findViewById(R.id.rvTransactions)
        tvNoTransactions = findViewById(R.id.tvNoTransactions)

        tvGymName.text = gymName
        rvTransactions.layoutManager = LinearLayoutManager(this)
        rvTransactions.adapter = txAdapter
    }

    private fun setupListeners() {
        findViewById<ImageButton>(R.id.btnOwnerBack).setOnClickListener { finish() }

        findViewById<Button>(R.id.btnEditGym).setOnClickListener {
            val intent = Intent(this, RegisterGymActivity::class.java).apply {
                putExtra("GYM_ID", gymId)
            }
            startActivity(intent)
        }

        btnVerify.setOnClickListener {
            val code = etCheckInCode.text.toString().trim()
            if (code.length != 6) {
                etCheckInCode.error = "Enter 6-digit code"
                return@setOnClickListener
            }
            verifyCheckInCode(code)
        }
    }

    private fun fetchMyGymId() {
        lifecycleScope.launch {
            try {
                val res = RetrofitClient.api.getMyGym()
                if (res.isSuccessful && res.body()?.gym != null) {
                    val gym = res.body()!!.gym!!
                    gymId = gym.id
                    gymName = gym.name
                    tvGymName.text = gymName
                    loadDashboardData()
                } else {
                    Toast.makeText(this@GymOwnerDashboardActivity, "Unable to load partner gym", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@GymOwnerDashboardActivity, "Network error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun verifyCheckInCode(code: String) {
        btnVerify.isEnabled = false
        btnVerify.text = "Checking..."

        lifecycleScope.launch {
            try {
                val res = RetrofitClient.api.checkInMember(gymId, CheckInRequest(code))
                btnVerify.isEnabled = true
                btnVerify.text = "Verify"

                if (res.isSuccessful && res.body()?.booking != null) {
                    val booking = res.body()!!.booking!!
                    layoutCheckInResult.visibility = View.VISIBLE
                    tvCheckInResultTitle.text = "Check-In Verified! Session Marked Attended ✓"
                    tvCheckInResultTitle.setTextColor(ContextCompat.getColor(this@GymOwnerDashboardActivity, R.color.gym_green))

                    val memberName = booking.userName ?: "Member"
                    val memberEmail = booking.userEmail ?: ""
                    tvCheckInMemberInfo.text = "Member: $memberName ($memberEmail)"
                    tvCheckInSessionInfo.text = "Slot: ${booking.date} • ${booking.slot} (${booking.sessionType})"

                    etCheckInCode.setText("")
                    Toast.makeText(this@GymOwnerDashboardActivity, "Attendance verified successfully!", Toast.LENGTH_SHORT).show()
                    loadDashboardData()
                } else {
                    val errMsg = try {
                        val str = res.errorBody()?.string()
                        if (!str.isNullOrEmpty()) {
                            org.json.JSONObject(str).optString("message", "Invalid check-in code")
                        } else "Invalid or expired check-in code"
                    } catch (_: Exception) {
                        "Invalid or expired check-in code"
                    }
                    layoutCheckInResult.visibility = View.VISIBLE
                    tvCheckInResultTitle.text = "Verification Failed ❌"
                    tvCheckInResultTitle.setTextColor(ContextCompat.getColor(this@GymOwnerDashboardActivity, R.color.status_rejected))
                    tvCheckInMemberInfo.text = errMsg
                    tvCheckInSessionInfo.text = "Check code with member and try again"
                }
            } catch (e: Exception) {
                btnVerify.isEnabled = true
                btnVerify.text = "Verify"
                Toast.makeText(this@GymOwnerDashboardActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadDashboardData() {
        if (gymId.isEmpty()) return
        lifecycleScope.launch {
            try {
                val res = RetrofitClient.api.getGymEarnings(gymId)
                if (res.isSuccessful && res.body() != null) {
                    val body = res.body()!!
                    val summary = body.summary
                    if (summary != null) {
                        tvPartnerEarnings.text = "₹${summary.totalPartnerEarnings.toInt()}"
                        tvTotalRevenue.text = "₹${summary.totalRevenue.toInt()}"
                        tvPlatformFee.text = "₹${summary.totalPlatformFee.toInt()}"

                        tvCountTotal.text = summary.totalBookings.toString()
                        tvCountAttended.text = summary.attendedBookings.toString()
                        tvCountConfirmed.text = summary.confirmedBookings.toString()
                        tvCountCancelled.text = summary.cancelledBookings.toString()
                    }

                    val txList = body.recentTransactions
                    if (txList.isNotEmpty()) {
                        tvNoTransactions.visibility = View.GONE
                        rvTransactions.visibility = View.VISIBLE
                        txAdapter.submitList(txList)
                    } else {
                        tvNoTransactions.visibility = View.VISIBLE
                        rvTransactions.visibility = View.GONE
                    }
                }
            } catch (_: Exception) {}
        }
    }
}
