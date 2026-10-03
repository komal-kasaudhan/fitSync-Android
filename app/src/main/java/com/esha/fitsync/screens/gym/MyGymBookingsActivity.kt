package com.esha.fitsync.screens.gym

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
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
import com.esha.fitsync.data.AppCacheManager
import com.esha.fitsync.models.GymBookingDetails
import com.esha.fitsync.models.GymBookingsListResponse
import com.esha.fitsync.models.GymReviewRequest
import com.esha.fitsync.screens.MainActivity
import com.esha.fitsync.screens.gym.adapters.MyBookingsAdapter
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.launch

class MyGymBookingsActivity : AppCompatActivity() {

    private var allBookings: List<GymBookingDetails> = emptyList()
    private var currentFilter = "all"

    private lateinit var rvBookings: RecyclerView
    private lateinit var adapter: MyBookingsAdapter
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var layoutNoBookings: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_gym_bookings)

        initViews()
        setupListeners()
        loadBookings()
    }

    private fun initViews() {
        rvBookings = findViewById(R.id.rvBookings)
        swipeRefresh = findViewById(R.id.swipeRefreshBookings)
        layoutNoBookings = findViewById(R.id.layoutNoBookings)

        adapter = MyBookingsAdapter(
            onCancelClick = { booking -> showCancellationDialog(booking) },
            onReviewClick = { booking -> showReviewDialog(booking) }
        )

        rvBookings.layoutManager = LinearLayoutManager(this)
        rvBookings.adapter = adapter
    }

    private fun setupListeners() {
        findViewById<ImageButton>(R.id.btnBookingsBack).setOnClickListener { finish() }

        val chipGroup = findViewById<ChipGroup>(R.id.chipGroupBookingFilter)
        chipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            currentFilter = when (checkedIds.firstOrNull()) {
                R.id.chipFilterUpcoming -> "upcoming"
                R.id.chipFilterAttended -> "attended"
                R.id.chipFilterCancelled -> "cancelled"
                else -> "all"
            }
            filterAndDisplayBookings()
        }

        swipeRefresh.setOnRefreshListener {
            loadBookings()
        }

        findViewById<Button>(R.id.btnExploreGymsCta).setOnClickListener {
            val intent = Intent(this, MainActivity::class.java).apply {
                putExtra("TARGET_FRAGMENT", "EXPLORE_FRAGMENT")
            }
            startActivity(intent)
            finish()
        }
    }

    private fun loadBookings() {
        swipeRefresh.isRefreshing = true
        val cacheKey = "MY_GYM_BOOKINGS"

        lifecycleScope.launch {
            try {
                val res = RetrofitClient.api.getMyBookings()
                swipeRefresh.isRefreshing = false

                if (res.isSuccessful && res.body() != null) {
                    val body = res.body()!!
                    allBookings = body.bookings
                    AppCacheManager.put(cacheKey, body)
                    filterAndDisplayBookings()
                } else {
                    handleBookingsError(cacheKey, "Failed to load bookings (${res.code()})")
                }
            } catch (e: Exception) {
                swipeRefresh.isRefreshing = false
                handleBookingsError(cacheKey, e.localizedMessage ?: "Network error")
            }
        }
    }

    private fun handleBookingsError(cacheKey: String, msg: String) {
        val cached = AppCacheManager.get<GymBookingsListResponse>(cacheKey)
        if (cached != null && cached.bookings.isNotEmpty()) {
            allBookings = cached.bookings
            filterAndDisplayBookings()
            Toast.makeText(this, "Showing saved bookings", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            filterAndDisplayBookings()
        }
    }

    private fun filterAndDisplayBookings() {
        val filtered = when (currentFilter) {
            "upcoming" -> allBookings.filter {
                it.status.equals("confirmed", ignoreCase = true) || it.status.equals("pending_payment", ignoreCase = true)
            }
            "attended" -> allBookings.filter {
                it.status.equals("attended", ignoreCase = true)
            }
            "cancelled" -> allBookings.filter {
                it.status.equals("cancelled", ignoreCase = true)
            }
            else -> allBookings
        }

        if (filtered.isEmpty()) {
            layoutNoBookings.visibility = View.VISIBLE
            rvBookings.visibility = View.GONE
        } else {
            layoutNoBookings.visibility = View.GONE
            rvBookings.visibility = View.VISIBLE
            adapter.submitList(filtered)
        }
    }

    private fun showCancellationDialog(booking: GymBookingDetails) {
        val message = StringBuilder().apply {
            append("Are you sure you want to cancel your booking at ${booking.gymName}?\n\n")
            append("📅 Date: ${booking.date}\n")
            append("⏰ Slot: ${booking.slot}\n")
            append("💰 Paid: ₹${booking.price.toInt()}\n\n")
            append("📋 Refund Policy:\n")
            append("• ≥ 12 hours before slot: 100% full refund\n")
            append("• 2 - 12 hours before slot: 50% partial refund\n")
            append("• < 2 hours before slot: 0% refund\n\n")
            append("The refund will be credited back via Razorpay automatically.")
        }.toString()

        AlertDialog.Builder(this)
            .setTitle("Cancel Gym Booking")
            .setMessage(message)
            .setPositiveButton("Confirm Cancellation") { dialog, _ ->
                dialog.dismiss()
                performCancellation(booking.id)
            }
            .setNegativeButton("Keep Booking") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun performCancellation(bookingId: String) {
        lifecycleScope.launch {
            try {
                val res = RetrofitClient.api.cancelBooking(bookingId)
                if (res.isSuccessful && res.body()?.cancellation != null) {
                    val cancelInfo = res.body()!!.cancellation!!
                    val refundMsg = "Booking cancelled! Refund of ₹${cancelInfo.refundAmount.toInt()} (${cancelInfo.refundStatus} refund) processed."
                    Toast.makeText(this@MyGymBookingsActivity, refundMsg, Toast.LENGTH_LONG).show()
                    loadBookings()
                } else {
                    val errMsg = res.body()?.message ?: "Cancellation failed"
                    Toast.makeText(this@MyGymBookingsActivity, errMsg, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MyGymBookingsActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showReviewDialog(booking: GymBookingDetails) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_write_review, null)
        val etComment = dialogView.findViewById<EditText>(R.id.etReviewComment)
        val stars = listOf<ImageView>(
            dialogView.findViewById(R.id.ivStar1),
            dialogView.findViewById(R.id.ivStar2),
            dialogView.findViewById(R.id.ivStar3),
            dialogView.findViewById(R.id.ivStar4),
            dialogView.findViewById(R.id.ivStar5)
        )

        var selectedRating = 5

        fun updateStarUi(rating: Int) {
            selectedRating = rating
            stars.forEachIndexed { index, star ->
                if (index < rating) {
                    star.setColorFilter(getColor(R.color.gym_gold))
                } else {
                    star.setColorFilter(getColor(R.color.text_gray_blue))
                }
            }
        }

        stars.forEachIndexed { index, star ->
            star.setOnClickListener {
                updateStarUi(index + 1)
            }
        }

        updateStarUi(5)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Review ${booking.gymName}")
            .setView(dialogView)
            .setPositiveButton("Submit Review", null)
            .setNegativeButton("Cancel") { d, _ -> d.dismiss() }
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val comment = etComment.text.toString().trim()
                if (comment.isEmpty()) {
                    etComment.error = "Please write a comment"
                    return@setOnClickListener
                }

                submitReview(booking.gymId, selectedRating, comment, dialog)
            }
        }

        dialog.show()
    }

    private fun submitReview(gymId: String, rating: Int, comment: String, dialog: AlertDialog) {
        lifecycleScope.launch {
            try {
                val req = GymReviewRequest(rating = rating, comment = comment)
                val res = RetrofitClient.api.submitGymReview(gymId, req)

                if (res.isSuccessful) {
                    Toast.makeText(this@MyGymBookingsActivity, "Review submitted successfully! ⭐", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                } else {
                    val errMsg = try {
                        val str = res.errorBody()?.string()
                        if (!str.isNullOrEmpty()) {
                            org.json.JSONObject(str).optString("message", "Review submission failed")
                        } else "Review submission failed (${res.code()})"
                    } catch (_: Exception) {
                        "Review submission failed (${res.code()})"
                    }
                    Toast.makeText(this@MyGymBookingsActivity, errMsg, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MyGymBookingsActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
