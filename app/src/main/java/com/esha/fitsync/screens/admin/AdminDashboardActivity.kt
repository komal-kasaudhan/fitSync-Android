package com.esha.fitsync.screens.admin

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.esha.fitsync.R
import com.esha.fitsync.api.RetrofitClient
import com.esha.fitsync.models.AdminEntityItem
import com.google.android.material.chip.ChipGroup
import com.google.gson.Gson
import kotlinx.coroutines.launch

class AdminDashboardActivity : AppCompatActivity() {

    private lateinit var tvEmail: TextView
    private lateinit var tvStatUsers: TextView
    private lateinit var tvStatGyms: TextView
    private lateinit var tvStatTrainers: TextView
    private lateinit var tvStatMarketplace: TextView
    private lateinit var tvStatRevenue: TextView

    private lateinit var rvEntities: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var tvEmptyNotice: TextView

    private lateinit var adapter: AdminEntityAdapter

    private var currentEntity: String = "gyms"
    private var currentStatus: String = "pending"

    private val reviewLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            loadDashboardData()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)

        initViews()
        verifyAdminAccess()
    }

    private fun initViews() {
        findViewById<ImageButton>(R.id.btnAdminBack).setOnClickListener { finish() }
        findViewById<ImageButton>(R.id.btnAdminRefresh).setOnClickListener { loadDashboardData() }

        tvEmail = findViewById(R.id.tvAdminEmail)
        tvStatUsers = findViewById(R.id.tvStatUsers)
        tvStatGyms = findViewById(R.id.tvStatGyms)
        tvStatTrainers = findViewById(R.id.tvStatTrainers)
        tvStatMarketplace = findViewById(R.id.tvStatMarketplace)
        tvStatRevenue = findViewById(R.id.tvStatRevenue)

        rvEntities = findViewById(R.id.rvAdminEntities)
        swipeRefresh = findViewById(R.id.swipeRefreshAdmin)
        layoutEmpty = findViewById(R.id.layoutAdminEmpty)
        tvEmptyNotice = findViewById(R.id.tvAdminEmptyNotice)

        adapter = AdminEntityAdapter(emptyList()) { item ->
            val intent = Intent(this, AdminReviewActivity::class.java).apply {
                putExtra("ENTITY_TYPE", currentEntity)
                putExtra("ENTITY_JSON", Gson().toJson(item))
            }
            reviewLauncher.launch(intent)
        }

        rvEntities.layoutManager = LinearLayoutManager(this)
        rvEntities.adapter = adapter

        swipeRefresh.setOnRefreshListener { loadDashboardData() }

        // Entity tabs
        val chipGroupEntities = findViewById<ChipGroup>(R.id.chipGroupEntities)
        chipGroupEntities.setOnCheckedStateChangeListener { _, checkedIds ->
            when {
                checkedIds.contains(R.id.chipEntityGyms) -> currentEntity = "gyms"
                checkedIds.contains(R.id.chipEntityTrainers) -> currentEntity = "trainers"
                checkedIds.contains(R.id.chipEntitySellers) -> currentEntity = "sellers"
                checkedIds.contains(R.id.chipEntityProducts) -> currentEntity = "products"
            }
            loadEntityList()
        }

        // Status chips
        val chipGroupStatus = findViewById<ChipGroup>(R.id.chipGroupStatus)
        chipGroupStatus.setOnCheckedStateChangeListener { _, checkedIds ->
            when {
                checkedIds.contains(R.id.chipStatusPending) -> currentStatus = "pending"
                checkedIds.contains(R.id.chipStatusApproved) -> currentStatus = "approved"
                checkedIds.contains(R.id.chipStatusRejected) -> currentStatus = "rejected"
                checkedIds.contains(R.id.chipStatusSuspended) -> currentStatus = "suspended"
            }
            loadEntityList()
        }
    }

    private fun verifyAdminAccess() {
        lifecycleScope.launch {
            try {
                val res = RetrofitClient.api.getAdminMe()
                if (res.isSuccessful && res.body()?.success == true) {
                    val user = res.body()?.user
                    val email = user?.email ?: "admin@fitsync.com"
                    val roleStr = user?.roles?.joinToString(", ") ?: "admin"
                    tvEmail.text = "$email • $roleStr"
                    loadDashboardData()
                } else {
                    Toast.makeText(this@AdminDashboardActivity, "You are not an admin (403)", Toast.LENGTH_LONG).show()
                    finish()
                }
            } catch (e: Exception) {
                Toast.makeText(this@AdminDashboardActivity, "Admin verification failed: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun loadDashboardData() {
        swipeRefresh.isRefreshing = true
        lifecycleScope.launch {
            loadStats()
            loadEntityList()
            swipeRefresh.isRefreshing = false
        }
    }

    private suspend fun loadStats() {
        try {
            val response = RetrofitClient.api.getAdminStats()
            if (response.isSuccessful && response.body()?.success == true) {
                val stats = response.body()?.stats
                stats?.let {
                    tvStatUsers.text = "${it.usersCount}"
                    val gymTotal = it.gyms?.total ?: 0
                    val gymPend = it.gyms?.pending ?: 0
                    tvStatGyms.text = "$gymTotal ($gymPend)"

                    val trTotal = it.trainers?.total ?: 0
                    val trPend = it.trainers?.pending ?: 0
                    tvStatTrainers.text = "$trTotal ($trPend)"

                    val prods = it.marketplace?.products ?: 0
                    val sellers = it.marketplace?.sellers ?: 0
                    tvStatMarketplace.text = "$prods Prods ($sellers Sellers)"

                    val rev = it.revenue?.totalRevenue ?: 0.0
                    tvStatRevenue.text = "₹${rev.toInt()}"
                }
            }
        } catch (_: Exception) {}
    }

    private fun loadEntityList() {
        lifecycleScope.launch {
            swipeRefresh.isRefreshing = true
            try {
                val items: List<AdminEntityItem> = when (currentEntity) {
                    "gyms" -> {
                        val res = RetrofitClient.api.getAdminGyms(currentStatus)
                        val gyms = res.body()?.gyms
                        gyms?.ifEmpty {
                            // Backend might return GymItem list or AdminEntityItem list
                            res.body()?.trainers ?: emptyList()
                        } ?: emptyList()
                    }
                    "trainers" -> {
                        val res = RetrofitClient.api.getAdminTrainers(currentStatus)
                        res.body()?.trainers ?: emptyList()
                    }
                    "sellers" -> {
                        val res = RetrofitClient.api.getAdminSellers(currentStatus)
                        res.body()?.sellers ?: emptyList()
                    }
                    "products" -> {
                        val res = RetrofitClient.api.getAdminProducts(currentStatus)
                        res.body()?.products ?: emptyList()
                    }
                    else -> emptyList()
                }

                adapter.submitList(items)
                if (items.isEmpty()) {
                    layoutEmpty.visibility = View.VISIBLE
                    tvEmptyNotice.text = "No $currentStatus $currentEntity found."
                } else {
                    layoutEmpty.visibility = View.GONE
                }
            } catch (e: Exception) {
                Toast.makeText(this@AdminDashboardActivity, "Failed to load: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                swipeRefresh.isRefreshing = false
            }
        }
    }
}
