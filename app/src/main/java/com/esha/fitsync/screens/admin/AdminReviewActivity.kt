package com.esha.fitsync.screens.admin

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.esha.fitsync.BuildConfig
import com.esha.fitsync.R
import com.esha.fitsync.api.RetrofitClient
import com.esha.fitsync.models.AdminEntityItem
import com.esha.fitsync.models.AdminUpdateStatusRequest
import com.google.android.material.button.MaterialButton
import com.google.gson.Gson
import kotlinx.coroutines.launch
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class AdminReviewActivity : AppCompatActivity() {

    private var entityType: String = "gyms"
    private var entity: AdminEntityItem? = null
    private var mapView: MapView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_review)

        entityType = intent.getStringExtra("ENTITY_TYPE") ?: "gyms"
        val entityJson = intent.getStringExtra("ENTITY_JSON")
        if (entityJson != null) {
            try {
                entity = Gson().fromJson(entityJson, AdminEntityItem::class.java)
            } catch (_: Exception) {}
        }

        if (entity == null) {
            Toast.makeText(this, "Failed to load entity details", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupViews()
    }

    private fun setupViews() {
        val currentEntity = entity ?: return

        findViewById<ImageButton>(R.id.btnReviewBack).setOnClickListener { finish() }

        val tvTitle = findViewById<TextView>(R.id.tvReviewTitle)
        val tvSubtitle = findViewById<TextView>(R.id.tvReviewSubtitle)
        val tvBadge = findViewById<TextView>(R.id.tvReviewStatusBadge)
        val ivImage = findViewById<ImageView>(R.id.ivReviewImage)

        val tvName = findViewById<TextView>(R.id.tvReviewEntityName)
        val tvCategory = findViewById<TextView>(R.id.tvReviewCategoryType)
        val tvPrice = findViewById<TextView>(R.id.tvReviewPrice)
        val tvDescription = findViewById<TextView>(R.id.tvReviewDescription)

        val cardOwner = findViewById<View>(R.id.cardReviewOwnerInfo)
        val tvOwnerName = findViewById<TextView>(R.id.tvReviewOwnerName)
        val tvOwnerEmail = findViewById<TextView>(R.id.tvReviewOwnerEmail)
        val tvOwnerPhone = findViewById<TextView>(R.id.tvReviewOwnerPhone)
        val tvFssai = findViewById<TextView>(R.id.tvReviewFssai)

        val cardLocation = findViewById<View>(R.id.cardReviewLocation)
        val tvAddress = findViewById<TextView>(R.id.tvReviewAddress)
        mapView = findViewById(R.id.mapReviewPreview)

        val layoutMedical = findViewById<LinearLayout>(R.id.layoutMedicalClaimsWarning)
        val tvMedicalReason = findViewById<TextView>(R.id.tvMedicalClaimsReason)

        tvTitle.text = "Review ${entityType.replaceFirstChar { it.uppercase() }.trimEnd('s')}"
        tvSubtitle.text = "FitSync ID: ${currentEntity.id.take(8)}..."

        // Badge
        updateStatusBadge(tvBadge, currentEntity.status)

        // Medical Claims Check
        if (currentEntity.flaggedForAdminReview || !currentEntity.flagReason.isNullOrEmpty()) {
            layoutMedical.visibility = View.VISIBLE
            tvMedicalReason.text = currentEntity.flagReason ?: "Flagged for unverified health or miracle claims"
        } else {
            layoutMedical.visibility = View.GONE
        }

        // Primary Info
        tvName.text = currentEntity.displayName
        val categoryStr = currentEntity.category ?: currentEntity.type ?: entityType
        val cityStr = currentEntity.city ?: ""
        tvCategory.text = listOf(categoryStr, cityStr).filter { it.isNotEmpty() }.joinToString(" • ")

        if (currentEntity.price > 0) {
            tvPrice.visibility = View.VISIBLE
            tvPrice.text = "₹${currentEntity.price.toInt()}"
        } else {
            tvPrice.visibility = View.GONE
        }

        val desc = currentEntity.description ?: currentEntity.bio ?: "No detailed description provided."
        tvDescription.text = desc

        // Image
        val imgUrl = currentEntity.primaryImage
        if (!imgUrl.isNullOrEmpty()) {
            val fullUrl = if (imgUrl.startsWith("http")) imgUrl else {
                "${BuildConfig.BASE_URL.trimEnd('/')}/${imgUrl.trimStart('/')}"
            }
            Glide.with(this).load(fullUrl).placeholder(R.drawable.gym).into(ivImage)
        } else {
            ivImage.setImageResource(R.drawable.gym)
        }

        // Owner Info
        val owner = currentEntity.owner ?: currentEntity.seller
        if (owner != null || !currentEntity.fssaiNumber.isNullOrEmpty() || !currentEntity.phone.isNullOrEmpty()) {
            cardOwner.visibility = View.VISIBLE
            tvOwnerName.text = "Contact Name: ${owner?.name ?: "N/A"}"
            tvOwnerEmail.text = "Email: ${owner?.email ?: "N/A"}"
            tvOwnerPhone.text = "Phone: ${currentEntity.phone ?: "N/A"}"
            if (!currentEntity.fssaiNumber.isNullOrEmpty()) {
                tvFssai.visibility = View.VISIBLE
                tvFssai.text = "FSSAI License: ${currentEntity.fssaiNumber}"
            } else {
                tvFssai.visibility = View.GONE
            }
        } else {
            cardOwner.visibility = View.GONE
        }

        // Location & osmdroid map
        val lat = currentEntity.location?.lat
        val lng = currentEntity.location?.lng
        val addressText = currentEntity.address ?: currentEntity.city
        if (!addressText.isNullOrEmpty() || (lat != null && lng != null)) {
            cardLocation.visibility = View.VISIBLE
            tvAddress.text = addressText ?: "Coordinates: $lat, $lng"

            if (lat != null && lng != null && lat != 0.0 && lng != 0.0) {
                mapView?.let { map ->
                    map.visibility = View.VISIBLE
                    map.setTileSource(TileSourceFactory.MAPNIK)
                    map.setMultiTouchControls(false)
                    val point = GeoPoint(lat, lng)
                    map.controller.setZoom(15.0)
                    map.controller.setCenter(point)

                    val marker = Marker(map)
                    marker.position = point
                    marker.title = currentEntity.displayName
                    map.overlays.add(marker)
                    map.invalidate()
                }
            } else {
                mapView?.visibility = View.GONE
            }
        } else {
            cardLocation.visibility = View.GONE
        }

        // Opening Hours & Membership Plans
        val cardOpeningHours = findViewById<View>(R.id.cardReviewOpeningHours)
        val layoutHoursTable = findViewById<LinearLayout>(R.id.layoutReviewOpeningHoursTable)
        val cardPlans = findViewById<View>(R.id.cardReviewPlans)
        val layoutPlans = findViewById<LinearLayout>(R.id.layoutReviewPlansContainer)

        if (entityType.equals("gyms", ignoreCase = true)) {
            cardOpeningHours.visibility = View.VISIBLE
            cardPlans.visibility = View.VISIBLE
            bindOpeningHoursTable(layoutHoursTable, currentEntity.openingHours)
            bindPlansList(layoutPlans, currentEntity.plans)

            if (currentEntity.openingHours.isEmpty() || currentEntity.plans.isEmpty()) {
                lifecycleScope.launch {
                    try {
                        val res = RetrofitClient.api.getGymDetails(currentEntity.id)
                        if (res.isSuccessful && res.body()?.gym != null) {
                            val gym = res.body()!!.gym!!
                            bindOpeningHoursTable(layoutHoursTable, gym.openingHours)
                            bindPlansList(layoutPlans, gym.plans)
                        }
                    } catch (_: Exception) {}
                }
            }
        } else {
            cardOpeningHours.visibility = View.GONE
            cardPlans.visibility = View.GONE
        }

        // Action Buttons
        val btnApprove = findViewById<MaterialButton>(R.id.btnAdminApprove)
        val btnReject = findViewById<MaterialButton>(R.id.btnAdminReject)
        val btnSuspend = findViewById<MaterialButton>(R.id.btnAdminSuspend)

        btnApprove.setOnClickListener { updateStatus("approved", null) }
        btnSuspend.setOnClickListener { updateStatus("suspended", null) }
        btnReject.setOnClickListener { showRejectDialog() }
    }

    private fun bindOpeningHoursTable(container: LinearLayout, hours: List<com.esha.fitsync.models.GymOpeningHour>) {
        container.removeAllViews()
        val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
        days.forEach { dayName ->
            val match = hours.find { it.day.equals(dayName, ignoreCase = true) }
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 6, 0, 6)
            }
            val tvDay = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                text = dayName
                textSize = 13f
                setTextColor(ContextCompat.getColor(this@AdminReviewActivity, R.color.text_white_crisp))
            }
            val tvShifts = TextView(this).apply {
                textSize = 13f
                if (match == null || match.isClosed) {
                    text = "Closed"
                    setTextColor(ContextCompat.getColor(this@AdminReviewActivity, R.color.status_rejected))
                } else {
                    val shiftList = match.displayShifts
                    text = if (shiftList.isNotEmpty()) {
                        shiftList.joinToString(", ") { "${it.open} - ${it.close}" }
                    } else {
                        "${match.open} - ${match.close}"
                    }
                    setTextColor(ContextCompat.getColor(this@AdminReviewActivity, R.color.gym_accent))
                }
            }
            row.addView(tvDay)
            row.addView(tvShifts)
            container.addView(row)
        }
    }

    private fun bindPlansList(container: LinearLayout, plans: List<com.esha.fitsync.models.GymPlan>) {
        container.removeAllViews()
        if (plans.isEmpty()) {
            val emptyTv = TextView(this).apply {
                text = "No membership plans listed."
                setTextColor(ContextCompat.getColor(this@AdminReviewActivity, R.color.text_gray_blue))
                textSize = 12f
            }
            container.addView(emptyTv)
            return
        }

        plans.forEach { plan ->
            val planCard = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundResource(R.drawable.bg_search_input)
                setPadding(14, 12, 14, 12)
                val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                params.setMargins(0, 0, 0, 10)
                layoutParams = params
            }

            val topRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
            }
            val tvName = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                text = "${plan.name} (${plan.durationDays}d)"
                textSize = 14f
                setTextColor(ContextCompat.getColor(this@AdminReviewActivity, R.color.white))
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            }
            val tvPrice = TextView(this).apply {
                text = "₹${plan.price.toInt()}"
                textSize = 15f
                setTextColor(ContextCompat.getColor(this@AdminReviewActivity, R.color.gym_gold))
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            }
            topRow.addView(tvName)
            topRow.addView(tvPrice)
            planCard.addView(topRow)

            if (plan.mrp != null && plan.mrp > plan.price) {
                val tvMrp = TextView(this).apply {
                    text = "MRP: ₹${plan.mrp.toInt()}"
                    textSize = 11f
                    paintFlags = paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
                    setTextColor(ContextCompat.getColor(this@AdminReviewActivity, R.color.text_muted))
                }
                planCard.addView(tvMrp)
            }

            val detailsText = mutableListOf<String>()
            detailsText.add("Type: ${plan.displayType}")
            if (plan.maxFreezeDays > 0) detailsText.add("Freeze: ${plan.maxFreezeDays}d")
            if (plan.inclusions.isNotEmpty()) detailsText.add("Includes: ${plan.inclusions.joinToString(", ")}")

            val tvMeta = TextView(this).apply {
                text = detailsText.joinToString(" • ")
                textSize = 12f
                setTextColor(ContextCompat.getColor(this@AdminReviewActivity, R.color.text_gray_blue))
                setPadding(0, 4, 0, 0)
            }
            planCard.addView(tvMeta)

            container.addView(planCard)
        }
    }

    private fun updateStatusBadge(tvBadge: TextView, status: String) {
        when (status.lowercase()) {
            "approved" -> {
                tvBadge.text = "APPROVED"
                tvBadge.setTextColor(ContextCompat.getColor(this, R.color.status_approved))
            }
            "rejected" -> {
                tvBadge.text = "REJECTED"
                tvBadge.setTextColor(ContextCompat.getColor(this, R.color.status_rejected))
            }
            "suspended" -> {
                tvBadge.text = "SUSPENDED"
                tvBadge.setTextColor(ContextCompat.getColor(this, R.color.status_rejected))
            }
            else -> {
                tvBadge.text = "PENDING"
                tvBadge.setTextColor(ContextCompat.getColor(this, R.color.status_pending))
            }
        }
    }

    private fun showRejectDialog() {
        val input = EditText(this).apply {
            hint = "Enter reason for rejection (required)"
            setPadding(40, 30, 40, 30)
            setTextColor(ContextCompat.getColor(this@AdminReviewActivity, R.color.white))
            setHintTextColor(ContextCompat.getColor(this@AdminReviewActivity, R.color.text_gray_blue))
        }

        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("Reject Application")
            .setMessage("Please provide an explanation for the partner so they can make corrections:")
            .setView(input)
            .setPositiveButton("Confirm Reject") { _, _ ->
                val reason = input.text.toString().trim()
                if (reason.isEmpty()) {
                    Toast.makeText(this, "Rejection reason is required", Toast.LENGTH_SHORT).show()
                } else {
                    updateStatus("rejected", reason)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateStatus(newStatus: String, reason: String?) {
        val currentEntity = entity ?: return
        val req = AdminUpdateStatusRequest(status = newStatus, rejectionReason = reason)

        lifecycleScope.launch {
            try {
                val response = when (entityType.lowercase()) {
                    "gyms" -> RetrofitClient.api.updateAdminGymStatus(currentEntity.id, req)
                    "trainers" -> RetrofitClient.api.updateAdminTrainerStatus(currentEntity.id, req)
                    "sellers" -> RetrofitClient.api.updateAdminSellerStatus(currentEntity.id, req)
                    "products" -> RetrofitClient.api.updateAdminProductStatus(currentEntity.id, req)
                    else -> RetrofitClient.api.updateAdminGymStatus(currentEntity.id, req)
                }

                if (response.isSuccessful && response.body()?.success == true) {
                    Toast.makeText(this@AdminReviewActivity, "Status updated to $newStatus", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                } else {
                    val err = response.errorBody()?.string() ?: "Failed to update status"
                    Toast.makeText(this@AdminReviewActivity, err, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@AdminReviewActivity, "Network error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        mapView?.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView?.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView?.onDetach()
    }
}
