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

        // Action Buttons
        val btnApprove = findViewById<MaterialButton>(R.id.btnAdminApprove)
        val btnReject = findViewById<MaterialButton>(R.id.btnAdminReject)
        val btnSuspend = findViewById<MaterialButton>(R.id.btnAdminSuspend)

        btnApprove.setOnClickListener { updateStatus("approved", null) }
        btnSuspend.setOnClickListener { updateStatus("suspended", null) }
        btnReject.setOnClickListener { showRejectDialog() }
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
