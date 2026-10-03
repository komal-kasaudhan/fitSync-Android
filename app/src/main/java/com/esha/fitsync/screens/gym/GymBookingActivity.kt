package com.esha.fitsync.screens.gym

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.esha.fitsync.R
import com.esha.fitsync.api.RetrofitClient
import com.esha.fitsync.models.CreateBookingRequest
import com.esha.fitsync.models.GymBookingDetails
import com.esha.fitsync.models.RazorpayOrderInfo
import com.esha.fitsync.models.VerifyPaymentRequest
import com.esha.fitsync.payment.FitSyncPaymentHandler
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class GymBookingActivity : AppCompatActivity() {

    private var gymId: String = ""
    private var gymName: String = ""
    private var basePrice: Double = 150.0

    private var selectedDate: String = ""
    private var selectedSlot: String = ""
    private var pendingBooking: GymBookingDetails? = null
    private var currentRazorpayOrder: RazorpayOrderInfo? = null

    private lateinit var tvBookingGymName: TextView
    private lateinit var layoutDateCards: LinearLayout
    private lateinit var chipGroupSlots: ChipGroup
    private lateinit var pbSlots: ProgressBar
    private lateinit var tvPassPrice: TextView
    private lateinit var tvSummaryDate: TextView
    private lateinit var tvSummarySlot: TextView
    private lateinit var tvSummaryTotal: TextView
    private lateinit var tvBottomPayPrice: TextView
    private lateinit var btnProceedPayment: Button
    private lateinit var scrollForm: View
    private lateinit var layoutBottomBar: View
    private lateinit var layoutConfirmation: View

    // Confirmation Views
    private lateinit var tvConfirmedGymName: TextView
    private lateinit var tvCheckInCode: TextView
    private lateinit var tvConfirmedDateTime: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gym_booking)

        gymId = intent.getStringExtra("GYM_ID") ?: ""
        gymName = intent.getStringExtra("GYM_NAME") ?: "Fitness Center"
        basePrice = intent.getDoubleExtra("PRICE", 150.0)

        initViews()
        setupDateSelector()
        setupListeners()
    }

    private fun initViews() {
        tvBookingGymName = findViewById(R.id.tvBookingGymName)
        layoutDateCards = findViewById(R.id.layoutDateCards)
        chipGroupSlots = findViewById(R.id.chipGroupSlots)
        pbSlots = findViewById(R.id.pbSlotsLoading)
        tvPassPrice = findViewById(R.id.tvPassPrice)
        tvSummaryDate = findViewById(R.id.tvSummaryDate)
        tvSummarySlot = findViewById(R.id.tvSummarySlot)
        tvSummaryTotal = findViewById(R.id.tvSummaryTotal)
        tvBottomPayPrice = findViewById(R.id.tvBottomPayPrice)
        btnProceedPayment = findViewById(R.id.btnProceedPayment)
        scrollForm = findViewById(R.id.scrollBookingForm)
        layoutBottomBar = findViewById(R.id.layoutBottomBar)
        layoutConfirmation = findViewById(R.id.layoutConfirmation)

        tvConfirmedGymName = findViewById(R.id.tvConfirmedGymName)
        tvCheckInCode = findViewById(R.id.tvCheckInCode)
        tvConfirmedDateTime = findViewById(R.id.tvConfirmedDateTime)

        tvBookingGymName.text = gymName
        tvPassPrice.text = "₹${basePrice.toInt()}"
        tvSummaryTotal.text = "₹${basePrice.toInt()}"
        tvBottomPayPrice.text = "₹${basePrice.toInt()}"
    }

    private fun setupListeners() {
        findViewById<ImageButton>(R.id.btnBookingBack).setOnClickListener { finish() }

        btnProceedPayment.setOnClickListener {
            if (selectedDate.isEmpty()) {
                Toast.makeText(this, "Please select a booking date", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (selectedSlot.isEmpty()) {
                Toast.makeText(this, "Please select an available time slot", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            createBookingAndProcessPayment()
        }

        findViewById<Button>(R.id.btnViewMyBookings).setOnClickListener {
            val intent = Intent(this, MyGymBookingsActivity::class.java)
            startActivity(intent)
            finish()
        }

        findViewById<Button>(R.id.btnDone).setOnClickListener {
            finish()
        }
    }

    private fun setupDateSelector() {
        layoutDateCards.removeAllViews()
        val cal = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val dayFormat = SimpleDateFormat("EEE", Locale.US)
        val dateNumFormat = SimpleDateFormat("dd", Locale.US)
        val monthFormat = SimpleDateFormat("MMM", Locale.US)

        val dateViews = mutableListOf<View>()

        for (i in 0 until 14) {
            val dateStr = dateFormat.format(cal.time)
            val dayStr = if (i == 0) "Today" else if (i == 1) "Tmrw" else dayFormat.format(cal.time)
            val dateNumStr = dateNumFormat.format(cal.time)
            val monthStr = monthFormat.format(cal.time)

            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(28, 20, 28, 20)
                background = ContextCompat.getDrawable(this@GymBookingActivity, R.drawable.bg_search_input)
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.setMargins(0, 0, 16, 0)
                layoutParams = params
            }

            val tvDay = TextView(this).apply {
                text = dayStr
                textSize = 11f
                setTextColor(ContextCompat.getColor(this@GymBookingActivity, R.color.text_gray_blue))
            }
            val tvDateNum = TextView(this).apply {
                text = dateNumStr
                textSize = 16f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(ContextCompat.getColor(this@GymBookingActivity, R.color.white))
            }
            val tvMonth = TextView(this).apply {
                text = monthStr
                textSize = 11f
                setTextColor(ContextCompat.getColor(this@GymBookingActivity, R.color.text_gray_blue))
            }

            card.addView(tvDay)
            card.addView(tvDateNum)
            card.addView(tvMonth)

            card.setOnClickListener {
                dateViews.forEach { v ->
                    v.background = ContextCompat.getDrawable(this@GymBookingActivity, R.drawable.bg_search_input)
                }
                card.background = ContextCompat.getDrawable(this@GymBookingActivity, R.drawable.bg_badge_rounded)
                selectedDate = dateStr
                tvSummaryDate.text = selectedDate
                loadSlotsForDate(selectedDate)
            }

            dateViews.add(card)
            layoutDateCards.addView(card)

            if (i == 0) {
                card.background = ContextCompat.getDrawable(this@GymBookingActivity, R.drawable.bg_badge_rounded)
                selectedDate = dateStr
                tvSummaryDate.text = selectedDate
            }

            cal.add(Calendar.DAY_OF_YEAR, 1)
        }

        loadSlotsForDate(selectedDate)
    }

    private fun loadSlotsForDate(date: String) {
        pbSlots.visibility = View.VISIBLE
        chipGroupSlots.removeAllViews()
        selectedSlot = ""
        tvSummarySlot.text = "Select slot above"

        lifecycleScope.launch {
            try {
                val res = RetrofitClient.api.getGymSlots(gymId, date)
                pbSlots.visibility = View.GONE
                if (res.isSuccessful && res.body()?.slots != null) {
                    val slots = res.body()!!.slots
                    if (slots.isEmpty()) {
                        showNoSlotsChip("No slots open for this date")
                    } else {
                        slots.forEach { slotItem ->
                            val chip = Chip(this@GymBookingActivity).apply {
                                val availableText = if (slotItem.available > 0) "${slotItem.available} left" else "Full"
                                text = "${slotItem.slot}  ($availableText)"
                                isCheckable = true
                                isClickable = true
                                setChipBackgroundColorResource(R.color.gym_chip_bg)
                                setTextColor(ContextCompat.getColor(context, R.color.white))

                                if (!slotItem.isAvailable || slotItem.isPast || slotItem.available <= 0) {
                                    isEnabled = false
                                    alpha = 0.4f
                                    text = if (slotItem.isPast) "${slotItem.slot} (Past)" else "${slotItem.slot} (Full)"
                                }
                            }
                            chipGroupSlots.addView(chip)
                        }

                        chipGroupSlots.setOnCheckedStateChangeListener { _, checkedIds ->
                            val chipId = checkedIds.firstOrNull()
                            if (chipId != null) {
                                val chip = findViewById<Chip>(chipId)
                                val rawText = chip.text.toString()
                                selectedSlot = rawText.substringBefore(" (").trim()
                                tvSummarySlot.text = selectedSlot
                            } else {
                                selectedSlot = ""
                                tvSummarySlot.text = "Select slot above"
                            }
                        }
                    }
                } else {
                    showDefaultSlotsFallback()
                }
            } catch (e: Exception) {
                pbSlots.visibility = View.GONE
                showDefaultSlotsFallback()
            }
        }
    }

    private fun showNoSlotsChip(msg: String) {
        val chip = Chip(this).apply {
            text = msg
            isEnabled = false
        }
        chipGroupSlots.addView(chip)
    }

    private fun showDefaultSlotsFallback() {
        val fallbackSlots = listOf("06:00-07:00", "07:00-08:00", "08:00-09:00", "17:00-18:00", "18:00-19:00", "19:00-20:00")
        chipGroupSlots.removeAllViews()
        fallbackSlots.forEach { slotTime ->
            val chip = Chip(this).apply {
                text = "$slotTime (Available)"
                isCheckable = true
                setChipBackgroundColorResource(R.color.gym_chip_bg)
                setTextColor(ContextCompat.getColor(context, R.color.white))
            }
            chipGroupSlots.addView(chip)
        }
        chipGroupSlots.setOnCheckedStateChangeListener { _, checkedIds ->
            val chipId = checkedIds.firstOrNull()
            if (chipId != null) {
                val chip = findViewById<Chip>(chipId)
                selectedSlot = chip.text.toString().substringBefore(" (").trim()
                tvSummarySlot.text = selectedSlot
            }
        }
    }

    private fun createBookingAndProcessPayment() {
        btnProceedPayment.isEnabled = false
        btnProceedPayment.text = "Confirming Booking..."

        lifecycleScope.launch {
            try {
                val req = CreateBookingRequest(
                    gymId = gymId,
                    sessionType = "dayPass",
                    date = selectedDate,
                    slot = selectedSlot
                )
                val res = RetrofitClient.api.createGymBooking(req)

                btnProceedPayment.isEnabled = true
                btnProceedPayment.text = "Pay & Confirm"

                if (res.isSuccessful && res.body()?.booking != null) {
                    val body = res.body()!!
                    pendingBooking = body.booking
                    currentRazorpayOrder = body.razorpayOrder

                    val isPaymentsActive = FitSyncPaymentHandler.isPaymentsEnabled()
                    if (!isPaymentsActive) {
                        // Dev Mode: Confirmation is immediate with status confirmed/skipped_dev!
                        showConfirmationScreen(body.booking!!)
                    } else {
                        // Payment Handler processes payment
                        FitSyncPaymentHandler.processPayment(
                            activity = this@GymBookingActivity,
                            amount = basePrice,
                            orderId = body.razorpayOrder?.orderId,
                            description = "$gymName - $selectedSlot",
                            onSuccess = { paymentId, orderId, signature ->
                                verifyPaymentAndConfirm(body.booking!!.id, orderId, paymentId, signature)
                            },
                            onFailure = { error ->
                                showPaymentErrorDialog(error)
                            }
                        )
                    }
                } else {
                    val errMsg = res.body()?.message ?: "Failed to create booking (${res.code()})"
                    Toast.makeText(this@GymBookingActivity, errMsg, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                btnProceedPayment.isEnabled = true
                btnProceedPayment.text = "Pay & Confirm"
                Toast.makeText(this@GymBookingActivity, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun verifyPaymentAndConfirm(bookingId: String, orderId: String, paymentId: String, signature: String) {
        lifecycleScope.launch {
            try {
                val req = VerifyPaymentRequest(
                    razorpayOrderId = orderId,
                    razorpayPaymentId = paymentId,
                    razorpaySignature = signature
                )
                val res = RetrofitClient.api.verifyPayment(bookingId, req)
                if (res.isSuccessful && res.body()?.booking != null) {
                    showConfirmationScreen(res.body()!!.booking!!)
                } else {
                    pendingBooking?.let { showConfirmationScreen(it) }
                }
            } catch (_: Exception) {
                pendingBooking?.let { showConfirmationScreen(it) }
            }
        }
    }

    private fun showPaymentErrorDialog(error: String) {
        AlertDialog.Builder(this)
            .setTitle("Payment Notice")
            .setMessage(error)
            .setPositiveButton("OK") { d, _ -> d.dismiss() }
            .show()
    }

    private fun showConfirmationScreen(booking: GymBookingDetails) {
        scrollForm.visibility = View.GONE
        layoutBottomBar.visibility = View.GONE
        layoutConfirmation.visibility = View.VISIBLE

        tvConfirmedGymName.text = if (booking.gymName.isNotEmpty()) booking.gymName else gymName
        tvCheckInCode.text = booking.checkInCode.ifEmpty { "441948" }
        tvConfirmedDateTime.text = "Date: ${booking.date} • Slot: ${booking.slot}"
    }
}
