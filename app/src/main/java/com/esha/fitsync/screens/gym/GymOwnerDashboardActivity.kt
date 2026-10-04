package com.esha.fitsync.screens.gym

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
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
import com.esha.fitsync.models.OwnerMemberCodeCheckInRequest
import com.esha.fitsync.screens.gym.adapters.GymTransactionAdapter
import com.esha.fitsync.screens.gym.adapters.OwnerEnquiriesAdapter
import com.esha.fitsync.screens.gym.adapters.OwnerMembersAdapter
import com.google.android.material.chip.ChipGroup
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

    // Members Section
    private lateinit var rvOwnerMembers: RecyclerView
    private lateinit var tvNoMembers: TextView
    private lateinit var pbMembersLoading: ProgressBar
    private lateinit var chipGroupMemberFilter: ChipGroup
    private val membersAdapter = OwnerMembersAdapter()

    // Enquiries Section
    private lateinit var rvOwnerEnquiries: RecyclerView
    private lateinit var tvNoEnquiries: TextView
    private lateinit var pbEnquiriesLoading: ProgressBar
    private val enquiriesAdapter = OwnerEnquiriesAdapter()

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
            loadMembers(expiringSoon = false)
            loadEnquiries()
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

        // Members
        rvOwnerMembers = findViewById(R.id.rvOwnerMembers)
        tvNoMembers = findViewById(R.id.tvNoMembers)
        pbMembersLoading = findViewById(R.id.pbMembersLoading)
        chipGroupMemberFilter = findViewById(R.id.chipGroupOwnerMemberFilter)
        rvOwnerMembers.layoutManager = LinearLayoutManager(this)
        rvOwnerMembers.adapter = membersAdapter

        // Enquiries
        rvOwnerEnquiries = findViewById(R.id.rvOwnerEnquiries)
        tvNoEnquiries = findViewById(R.id.tvNoEnquiries)
        pbEnquiriesLoading = findViewById(R.id.pbEnquiriesLoading)
        rvOwnerEnquiries.layoutManager = LinearLayoutManager(this)
        rvOwnerEnquiries.adapter = enquiriesAdapter

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

        chipGroupMemberFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            val isExpiringSoon = checkedIds.firstOrNull() == R.id.chipFilterExpiringSoon
            loadMembers(isExpiringSoon)
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
                    loadMembers(expiringSoon = false)
                    loadEnquiries()
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
                val res = RetrofitClient.api.checkInMemberByCode(gymId, OwnerMemberCodeCheckInRequest(code))
                btnVerify.isEnabled = true
                btnVerify.text = "Verify"

                if (res.isSuccessful && res.body() != null) {
                    val body = res.body()!!
                    layoutCheckInResult.visibility = View.VISIBLE
                    tvCheckInResultTitle.text = "Check-In Verified! ✓"
                    tvCheckInResultTitle.setTextColor(ContextCompat.getColor(this@GymOwnerDashboardActivity, R.color.gym_green))

                    if (body.member != null) {
                        val m = body.member
                        tvCheckInMemberInfo.text = "Member: ${m.name} • Plan: ${m.plan} (${m.planType})"
                        val validUntilClean = m.validUntil.substringBefore("T")
                        tvCheckInSessionInfo.text = "Valid Until: $validUntilClean • Check-in Recorded"
                    } else if (body.booking != null) {
                        val booking = body.booking
                        val memberName = booking.userName ?: "Member"
                        val memberEmail = booking.userEmail ?: ""
                        tvCheckInMemberInfo.text = "Member: $memberName ($memberEmail)"
                        tvCheckInSessionInfo.text = "Slot: ${booking.date} • ${booking.slot} (${booking.sessionType})"
                    } else {
                        tvCheckInMemberInfo.text = body.message.ifEmpty { "Check-In Verified" }
                        tvCheckInSessionInfo.text = "Entry verified for member code $code"
                    }

                    etCheckInCode.setText("")
                    Toast.makeText(this@GymOwnerDashboardActivity, "Check-in verified successfully!", Toast.LENGTH_SHORT).show()
                    loadDashboardData()
                    loadMembers(chipGroupMemberFilter.checkedChipId == R.id.chipFilterExpiringSoon)
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

    private fun loadMembers(expiringSoon: Boolean = false) {
        if (gymId.isEmpty()) return
        pbMembersLoading.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val res = if (expiringSoon) {
                    RetrofitClient.api.getExpiringSoonMembers(gymId)
                } else {
                    RetrofitClient.api.getGymMembers(gymId)
                }
                pbMembersLoading.visibility = View.GONE
                if (res.isSuccessful && res.body() != null) {
                    val list = res.body()!!.members
                    membersAdapter.submitList(list)
                    if (list.isEmpty()) {
                        tvNoMembers.visibility = View.VISIBLE
                        rvOwnerMembers.visibility = View.GONE
                        tvNoMembers.text = if (expiringSoon) "No members expiring soon." else "No active members registered yet."
                    } else {
                        tvNoMembers.visibility = View.GONE
                        rvOwnerMembers.visibility = View.VISIBLE
                    }
                } else {
                    tvNoMembers.visibility = View.VISIBLE
                    rvOwnerMembers.visibility = View.GONE
                }
            } catch (e: Exception) {
                pbMembersLoading.visibility = View.GONE
                tvNoMembers.visibility = View.VISIBLE
                rvOwnerMembers.visibility = View.GONE
            }
        }
    }

    private fun loadEnquiries() {
        if (gymId.isEmpty()) return
        pbEnquiriesLoading.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val res = RetrofitClient.api.getGymEnquiries(gymId)
                pbEnquiriesLoading.visibility = View.GONE
                if (res.isSuccessful && res.body() != null) {
                    val list = res.body()!!.enquiries
                    enquiriesAdapter.submitList(list)
                    if (list.isEmpty()) {
                        tvNoEnquiries.visibility = View.VISIBLE
                        rvOwnerEnquiries.visibility = View.GONE
                    } else {
                        tvNoEnquiries.visibility = View.GONE
                        rvOwnerEnquiries.visibility = View.VISIBLE
                    }
                } else {
                    tvNoEnquiries.visibility = View.VISIBLE
                    rvOwnerEnquiries.visibility = View.GONE
                }
            } catch (e: Exception) {
                pbEnquiriesLoading.visibility = View.GONE
                tvNoEnquiries.visibility = View.VISIBLE
                rvOwnerEnquiries.visibility = View.GONE
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
