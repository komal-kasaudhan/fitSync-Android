package com.esha.fitsync.screens.gym

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.esha.fitsync.R
import com.esha.fitsync.api.RetrofitClient
import com.esha.fitsync.models.CancelMembershipRequest
import com.esha.fitsync.models.FreezeMembershipRequest
import com.esha.fitsync.models.GymMembership
import com.esha.fitsync.screens.MainActivity
import com.esha.fitsync.screens.gym.adapters.MyMembershipsAdapter
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.launch

class MyMembershipsActivity : AppCompatActivity() {

    private var allActive: List<GymMembership> = emptyList()
    private var allUpcoming: List<GymMembership> = emptyList()
    private var allPast: List<GymMembership> = emptyList()
    private var currentTab = "active"

    private lateinit var rvMemberships: RecyclerView
    private lateinit var adapter: MyMembershipsAdapter
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var layoutNoMemberships: LinearLayout
    private lateinit var pbLoading: ProgressBar
    private lateinit var tvEmptyTitle: TextView
    private lateinit var tvEmptySubtitle: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_memberships)

        initViews()
        setupListeners()
        loadMemberships()
    }

    private fun initViews() {
        rvMemberships = findViewById(R.id.rvMemberships)
        swipeRefresh = findViewById(R.id.swipeRefreshMemberships)
        layoutNoMemberships = findViewById(R.id.layoutNoMemberships)
        pbLoading = findViewById(R.id.pbMembershipsLoading)
        tvEmptyTitle = findViewById(R.id.tvEmptyTitle)
        tvEmptySubtitle = findViewById(R.id.tvEmptySubtitle)

        adapter = MyMembershipsAdapter(
            onFreezeClick = { m -> showFreezeDialog(m) },
            onUnfreezeClick = { m -> unfreezeMembership(m) },
            onCancelClick = { m -> showCancelDialog(m) }
        )

        rvMemberships.layoutManager = LinearLayoutManager(this)
        rvMemberships.adapter = adapter
    }

    private fun setupListeners() {
        findViewById<ImageButton>(R.id.btnMembershipsBack).setOnClickListener { finish() }

        val chipGroup = findViewById<ChipGroup>(R.id.chipGroupMembershipFilter)
        chipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            currentTab = when (checkedIds.firstOrNull()) {
                R.id.chipFilterUpcoming -> "upcoming"
                R.id.chipFilterPast -> "past"
                else -> "active"
            }
            displayCurrentTab()
        }

        swipeRefresh.setOnRefreshListener {
            loadMemberships()
        }

        findViewById<Button>(R.id.btnExploreGymsCta).setOnClickListener {
            val intent = Intent(this, MainActivity::class.java).apply {
                putExtra("TARGET_FRAGMENT", "EXPLORE_FRAGMENT")
            }
            startActivity(intent)
            finish()
        }
    }

    private fun loadMemberships() {
        swipeRefresh.isRefreshing = true
        lifecycleScope.launch {
            try {
                val res = RetrofitClient.api.getMyGymMemberships()
                swipeRefresh.isRefreshing = false
                pbLoading.visibility = View.GONE

                if (res.isSuccessful && res.body() != null) {
                    val body = res.body()!!
                    val rawAll = if (body.memberships.isNotEmpty()) body.memberships else (body.active + body.upcoming + body.past)

                    allActive = if (body.active.isNotEmpty()) body.active else rawAll.filter {
                        it.status.equals("active", ignoreCase = true)
                    }
                    allUpcoming = if (body.upcoming.isNotEmpty()) body.upcoming else rawAll.filter {
                        it.status.equals("upcoming", ignoreCase = true)
                    }
                    allPast = if (body.past.isNotEmpty()) body.past else rawAll.filter {
                        !it.status.equals("active", ignoreCase = true) && !it.status.equals("upcoming", ignoreCase = true)
                    }

                    displayCurrentTab()
                } else {
                    displayCurrentTab()
                }
            } catch (e: Exception) {
                swipeRefresh.isRefreshing = false
                pbLoading.visibility = View.GONE
                displayCurrentTab()
            }
        }
    }

    private fun displayCurrentTab() {
        val list = when (currentTab) {
            "upcoming" -> allUpcoming
            "past" -> allPast
            else -> allActive
        }

        if (list.isNotEmpty()) {
            layoutNoMemberships.visibility = View.GONE
            rvMemberships.visibility = View.VISIBLE
            adapter.submitList(list)
        } else {
            rvMemberships.visibility = View.GONE
            layoutNoMemberships.visibility = View.VISIBLE

            when (currentTab) {
                "upcoming" -> {
                    tvEmptyTitle.text = "No Upcoming Memberships"
                    tvEmptySubtitle.text = "You don't have any memberships scheduled to start in the future."
                }
                "past" -> {
                    tvEmptyTitle.text = "No Past Memberships"
                    tvEmptySubtitle.text = "No expired or cancelled memberships on record."
                }
                else -> {
                    tvEmptyTitle.text = "No Active Memberships"
                    tvEmptySubtitle.text = "You don't have any active gym memberships or day passes. Explore partner gyms near you!"
                }
            }
        }
    }

    private fun showFreezeDialog(membership: GymMembership) {
        val maxDays = if (membership.maxFreezeDays > 0) membership.maxFreezeDays else 7

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_holiday, null, false) as LinearLayout
        val tvLabel1 = dialogView.getChildAt(0) as? TextView
        tvLabel1?.text = "Freeze Duration (Max $maxDays days) *"

        val btnPickDate = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnHolidayPickDate)
        btnPickDate.visibility = View.GONE

        val etDays = EditText(this).apply {
            hint = "Number of days (e.g. 5)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText("5")
            setBackgroundResource(R.drawable.bg_search_input)
            setTextColor(getColor(R.color.white))
            setHintTextColor(getColor(R.color.text_muted))
            setPadding(32, 24, 32, 24)
        }
        (dialogView as LinearLayout).addView(etDays, 1)

        val etReason = dialogView.findViewById<EditText>(R.id.etHolidayReason)
        etReason.hint = "Reason (e.g. Traveling, Medical leave)"

        AlertDialog.Builder(this)
            .setTitle("Freeze Membership")
            .setMessage("Your membership expiration will be extended by the frozen days. You will not be able to check in during this period.")
            .setView(dialogView)
            .setPositiveButton("Freeze") { _, _ ->
                val days = etDays.text.toString().toIntOrNull() ?: 5
                val reason = etReason.text.toString().trim()
                if (days <= 0 || days > maxDays) {
                    Toast.makeText(this, "Enter valid days between 1 and $maxDays", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                freezeMembership(membership, days, reason)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun freezeMembership(membership: GymMembership, days: Int, reason: String) {
        pbLoading.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val res = RetrofitClient.api.freezeGymMembership(
                    membership.id,
                    FreezeMembershipRequest(days = days, freezeDays = days, reason = reason)
                )
                pbLoading.visibility = View.GONE
                if (res.isSuccessful) {
                    val msg = res.body()?.message ?: "Membership frozen for $days days"
                    Toast.makeText(this@MyMembershipsActivity, msg, Toast.LENGTH_LONG).show()
                    loadMemberships()
                } else {
                    Toast.makeText(this@MyMembershipsActivity, "Freeze failed (${res.code()})", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                pbLoading.visibility = View.GONE
                Toast.makeText(this@MyMembershipsActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun unfreezeMembership(membership: GymMembership) {
        pbLoading.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val res = RetrofitClient.api.unfreezeGymMembership(membership.id)
                pbLoading.visibility = View.GONE
                if (res.isSuccessful) {
                    Toast.makeText(this@MyMembershipsActivity, "Membership resumed successfully!", Toast.LENGTH_SHORT).show()
                    loadMemberships()
                } else {
                    Toast.makeText(this@MyMembershipsActivity, "Unfreeze failed", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                pbLoading.visibility = View.GONE
                Toast.makeText(this@MyMembershipsActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showCancelDialog(membership: GymMembership) {
        val policyMessage = """
            📋 Refund & Cancellation Rules:
            • Cancellation before commencement date: 100% full refund.
            • Cancellation within 24 hours of start date: Pro-rated refund based on attendance.
            • Cancellation after 24 hours: Non-refundable.

            Any eligible refund will be credited back via original payment method.
        """.trimIndent()

        val input = EditText(this).apply {
            hint = "Reason for cancellation (optional)"
            setBackgroundResource(R.drawable.bg_search_input)
            setTextColor(getColor(R.color.white))
            setHintTextColor(getColor(R.color.text_muted))
            setPadding(32, 24, 32, 24)
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 20, 48, 20)
            addView(input)
        }

        AlertDialog.Builder(this)
            .setTitle("Cancel Membership")
            .setMessage(policyMessage)
            .setView(container)
            .setPositiveButton("Confirm Cancellation") { _, _ ->
                val reason = input.text.toString().trim()
                cancelMembership(membership, reason)
            }
            .setNegativeButton("Keep Membership", null)
            .show()
    }

    private fun cancelMembership(membership: GymMembership, reason: String) {
        pbLoading.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val res = RetrofitClient.api.cancelGymMembership(
                    membership.id,
                    CancelMembershipRequest(reason = reason)
                )
                pbLoading.visibility = View.GONE
                if (res.isSuccessful) {
                    val body = res.body()
                    val refundMsg = if (body?.refundAmount != null && body.refundAmount > 0) {
                        " Refund amount: ₹${body.refundAmount.toInt()}"
                    } else ""
                    Toast.makeText(
                        this@MyMembershipsActivity,
                        "Membership cancelled.$refundMsg",
                        Toast.LENGTH_LONG
                    ).show()
                    loadMemberships()
                } else {
                    Toast.makeText(this@MyMembershipsActivity, "Cancellation failed (${res.code()})", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                pbLoading.visibility = View.GONE
                Toast.makeText(this@MyMembershipsActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
