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
import com.esha.fitsync.models.CreateMembershipRequest
import com.esha.fitsync.models.GymBookingDetails
import com.esha.fitsync.models.GymMembership
import com.esha.fitsync.models.MembershipPaymentDetails
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

    // Membership Plan Extras
    private var planId: String = ""
    private var planName: String = ""
    private var planType: String = ""
    private var planDuration: Int = 1
    private var maxFreezeDays: Int = 0
    private var isMembershipFlow: Boolean = false

    private var selectedDate: String = ""
    private var selectedSlot: String = ""
    private var pendingBooking: GymBookingDetails? = null
    private var currentRazorpayOrder: RazorpayOrderInfo? = null

    private lateinit var tvBookingGymName: TextView
    private lateinit var layoutDateCards: LinearLayout
    private lateinit var layoutSlotContainer: View
    private lateinit var chipGroupSlots: ChipGroup
    private lateinit var pbSlots: ProgressBar
    private lateinit var tvSelectedPlanTitle: TextView
    private lateinit var tvSelectedPlanSubtitle: TextView
    private lateinit var tvPassPrice: TextView
    private lateinit var tvSummaryPlan: TextView
    private lateinit var tvSummaryDate: TextView
    private lateinit var tvSummaryDateLabel: TextView
    private lateinit var rowSummaryEndDate: View
    private lateinit var tvSummaryEndDate: TextView
    private lateinit var rowSummarySlot: View
    private lateinit var tvSummarySlot: TextView
    private lateinit var tvSummaryTotal: TextView
    private lateinit var tvBottomPayPrice: TextView
    private lateinit var btnProceedPayment: Button
    private lateinit var scrollForm: View
    private lateinit var layoutBottomBar: View
    private lateinit var layoutConfirmation: View

    // Confirmation Views
    private lateinit var tvConfirmationTitle: TextView
    private lateinit var tvConfirmedGymName: TextView
    private lateinit var tvConfirmedPlanName: TextView
    private lateinit var tvCheckInCodeLabel: TextView
    private lateinit var tvCheckInCode: TextView
    private lateinit var tvCheckInCodeDescription: TextView
    private lateinit var tvConfirmedDateTime: TextView
    private lateinit var tvConfirmedDaysLeft: TextView
    private lateinit var btnViewMyBookings: Button
    private lateinit var btnDone: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gym_booking)

        gymId = intent.getStringExtra("GYM_ID") ?: ""
        gymName = intent.getStringExtra("GYM_NAME") ?: "Fitness Center"
        basePrice = intent.getDoubleExtra("PRICE", 150.0)

        planId = intent.getStringExtra("PLAN_ID") ?: ""
        planName = intent.getStringExtra("PLAN_NAME") ?: "Day Pass"
        planType = intent.getStringExtra("PLAN_TYPE") ?: "day_pass"
        planDuration = intent.getIntExtra("PLAN_DURATION", 1)
        maxFreezeDays = intent.getIntExtra("MAX_FREEZE_DAYS", 0)
        isMembershipFlow = planId.isNotEmpty()

        initViews()
        setupDateSelector()
        setupListeners()
    }

    private fun initViews() {
        tvBookingGymName = findViewById(R.id.tvBookingGymName)
        layoutDateCards = findViewById(R.id.layoutDateCards)
        layoutSlotContainer = findViewById(R.id.layoutSlotContainer)
        chipGroupSlots = findViewById(R.id.chipGroupSlots)
        pbSlots = findViewById(R.id.pbSlotsLoading)
        tvSelectedPlanTitle = findViewById(R.id.tvSelectedPlanTitle)
        tvSelectedPlanSubtitle = findViewById(R.id.tvSelectedPlanSubtitle)
        tvPassPrice = findViewById(R.id.tvPassPrice)
        tvSummaryPlan = findViewById(R.id.tvSummaryPlan)
        tvSummaryDate = findViewById(R.id.tvSummaryDate)
        tvSummaryDateLabel = findViewById(R.id.tvSummaryDateLabel)
        rowSummaryEndDate = findViewById(R.id.rowSummaryEndDate)
        tvSummaryEndDate = findViewById(R.id.tvSummaryEndDate)
        rowSummarySlot = findViewById(R.id.rowSummarySlot)
        tvSummarySlot = findViewById(R.id.tvSummarySlot)
        tvSummaryTotal = findViewById(R.id.tvSummaryTotal)
        tvBottomPayPrice = findViewById(R.id.tvBottomPayPrice)
        btnProceedPayment = findViewById(R.id.btnProceedPayment)
        scrollForm = findViewById(R.id.scrollBookingForm)
        layoutBottomBar = findViewById(R.id.layoutBottomBar)
        layoutConfirmation = findViewById(R.id.layoutConfirmation)

        tvConfirmationTitle = findViewById(R.id.tvConfirmationTitle)
        tvConfirmedGymName = findViewById(R.id.tvConfirmedGymName)
        tvConfirmedPlanName = findViewById(R.id.tvConfirmedPlanName)
        tvCheckInCodeLabel = findViewById(R.id.tvCheckInCodeLabel)
        tvCheckInCode = findViewById(R.id.tvCheckInCode)
        tvCheckInCodeDescription = findViewById(R.id.tvCheckInCodeDescription)
        tvConfirmedDateTime = findViewById(R.id.tvConfirmedDateTime)
        tvConfirmedDaysLeft = findViewById(R.id.tvConfirmedDaysLeft)
        btnViewMyBookings = findViewById(R.id.btnViewMyBookings)
        btnDone = findViewById(R.id.btnDone)

        tvBookingGymName.text = gymName
        tvPassPrice.text = "₹${basePrice.toInt()}"
        tvSummaryTotal.text = "₹${basePrice.toInt()}"
        tvBottomPayPrice.text = "₹${basePrice.toInt()}"

        if (isMembershipFlow) {
            tvSelectedPlanTitle.text = planName
            tvSelectedPlanSubtitle.text = "$planDuration days full gym access • ${planType.replace('_', ' ').replaceFirstChar(Char::uppercase)}"
            tvSummaryPlan.text = planName
            tvSummaryDateLabel.text = "Start Date"

            if (planDuration > 1) {
                layoutSlotContainer.visibility = View.GONE
                rowSummarySlot.visibility = View.GONE
                selectedSlot = "Continuous Access"
            } else {
                layoutSlotContainer.visibility = View.VISIBLE
                rowSummarySlot.visibility = View.VISIBLE
            }
            rowSummaryEndDate.visibility = View.VISIBLE
        } else {
            tvSelectedPlanTitle.text = "Day Pass"
            tvSelectedPlanSubtitle.text = "Full access to strength, cardio & lockers for 1 slot"
            tvSummaryPlan.text = "Day Pass"
            rowSummaryEndDate.visibility = View.GONE
        }
    }

    private fun setupListeners() {
        findViewById<ImageButton>(R.id.btnBookingBack).setOnClickListener { finish() }

        btnProceedPayment.setOnClickListener {
            if (selectedDate.isEmpty()) {
                Toast.makeText(this, "Please select a start date", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!isMembershipFlow && selectedSlot.isEmpty()) {
                Toast.makeText(this, "Please select an available time slot", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (isMembershipFlow) {
                createMembershipAndProcessPayment()
            } else {
                createBookingAndProcessPayment()
            }
        }

        btnViewMyBookings.setOnClickListener {
            if (isMembershipFlow) {
                startActivity(Intent(this, MyMembershipsActivity::class.java))
            } else {
                startActivity(Intent(this, MyGymBookingsActivity::class.java))
            }
            finish()
        }

        btnDone.setOnClickListener {
            finish()
        }
    }

    private fun updateCalculatedEndDate(startDateStr: String) {
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val cal = Calendar.getInstance()
            cal.time = sdf.parse(startDateStr) ?: return
            cal.add(Calendar.DAY_OF_YEAR, planDuration - 1)
            tvSummaryEndDate.text = sdf.format(cal.time)
        } catch (_: Exception) {}
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
                if (isMembershipFlow) {
                    updateCalculatedEndDate(selectedDate)
                }
                if (!isMembershipFlow || planDuration == 1) {
                    loadSlotsForDate(selectedDate)
                }
            }

            dateViews.add(card)
            layoutDateCards.addView(card)

            if (i == 0) {
                card.background = ContextCompat.getDrawable(this@GymBookingActivity, R.drawable.bg_badge_rounded)
                selectedDate = dateStr
                tvSummaryDate.text = selectedDate
                if (isMembershipFlow) {
                    updateCalculatedEndDate(selectedDate)
                }
            }

            cal.add(Calendar.DAY_OF_YEAR, 1)
        }

        if (!isMembershipFlow || planDuration == 1) {
            loadSlotsForDate(selectedDate)
        }
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

    private fun createMembershipAndProcessPayment() {
        btnProceedPayment.isEnabled = false
        btnProceedPayment.text = "Confirming Membership..."

        lifecycleScope.launch {
            try {
                val configRes = try {
                    RetrofitClient.api.getAppConfig()
                } catch (_: Exception) { null }

                val paymentsEnabled = configRes?.body()?.paymentsEnabled ?: false

                if (!paymentsEnabled) {
                    val req = CreateMembershipRequest(
                        gymId = gymId,
                        planId = planId,
                        startDate = selectedDate,
                        paymentDetails = null
                    )
                    val res = RetrofitClient.api.createGymMembership(req)
                    btnProceedPayment.isEnabled = true
                    btnProceedPayment.text = "Pay & Confirm"

                    if (res.isSuccessful && res.body()?.membership != null) {
                        showMembershipConfirmationScreen(res.body()!!.membership!!)
                    } else {
                        val errMsg = res.body()?.message ?: "Failed to confirm membership (${res.code()})"
                        Toast.makeText(this@GymBookingActivity, errMsg, Toast.LENGTH_LONG).show()
                    }
                } else {
                    FitSyncPaymentHandler.processPayment(
                        activity = this@GymBookingActivity,
                        amount = basePrice,
                        orderId = null,
                        description = "$gymName - $planName",
                        onSuccess = { paymentId, _, _ ->
                            confirmPaidMembership(paymentId)
                        },
                        onFailure = { error ->
                            btnProceedPayment.isEnabled = true
                            btnProceedPayment.text = "Pay & Confirm"
                            showPaymentErrorDialog(error)
                        }
                    )
                }
            } catch (e: Exception) {
                btnProceedPayment.isEnabled = true
                btnProceedPayment.text = "Pay & Confirm"
                Toast.makeText(this@GymBookingActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun confirmPaidMembership(paymentId: String) {
        lifecycleScope.launch {
            try {
                val req = CreateMembershipRequest(
                    gymId = gymId,
                    planId = planId,
                    startDate = selectedDate,
                    paymentDetails = MembershipPaymentDetails("razorpay", paymentId)
                )
                val res = RetrofitClient.api.createGymMembership(req)
                btnProceedPayment.isEnabled = true
                btnProceedPayment.text = "Pay & Confirm"

                if (res.isSuccessful && res.body()?.membership != null) {
                    showMembershipConfirmationScreen(res.body()!!.membership!!)
                } else {
                    val errMsg = res.body()?.message ?: "Membership verification pending"
                    Toast.makeText(this@GymBookingActivity, errMsg, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                btnProceedPayment.isEnabled = true
                btnProceedPayment.text = "Pay & Confirm"
                Toast.makeText(this@GymBookingActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showMembershipConfirmationScreen(membership: GymMembership) {
        scrollForm.visibility = View.GONE
        layoutBottomBar.visibility = View.GONE
        layoutConfirmation.visibility = View.VISIBLE

        tvConfirmationTitle.text = "Membership Confirmed!"
        tvConfirmedGymName.text = membership.displayGymName
        tvConfirmedPlanName.visibility = View.VISIBLE
        tvConfirmedPlanName.text = "${membership.planName} (${membership.durationDays} Days Access)"

        tvCheckInCodeLabel.text = "YOUR 6-DIGIT MEMBER CODE"
        tvCheckInCode.text = membership.memberCode.ifEmpty { "441948" }
        tvCheckInCodeDescription.text = "Show your 6-digit member code at the gym reception anytime during gym opening hours for entry."

        val startDateClean = membership.startDate.substringBefore("T")
        val endDateClean = membership.endDate.substringBefore("T")
        tvConfirmedDateTime.text = "Valid: $startDateClean to $endDateClean"

        tvConfirmedDaysLeft.visibility = View.VISIBLE
        tvConfirmedDaysLeft.text = "STATUS: ${membership.status.uppercase(Locale.US)} • ${membership.durationDays} DAYS"

        btnViewMyBookings.text = "View My Memberships"
        btnViewMyBookings.setOnClickListener {
            val intent = Intent(this, MyMembershipsActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun showConfirmationScreen(booking: GymBookingDetails) {
        scrollForm.visibility = View.GONE
        layoutBottomBar.visibility = View.GONE
        layoutConfirmation.visibility = View.VISIBLE

        tvConfirmationTitle.text = "Booking Confirmed!"
        tvConfirmedGymName.text = if (booking.gymName.isNotEmpty()) booking.gymName else gymName
        tvConfirmedPlanName.visibility = View.GONE
        tvCheckInCodeLabel.text = "YOUR 6-DIGIT CHECK-IN CODE"
        tvCheckInCode.text = booking.checkInCode.ifEmpty { "441948" }
        tvCheckInCodeDescription.text = "Show this code to the gym receptionist when you arrive to verify attendance."
        tvConfirmedDateTime.text = "Date: ${booking.date} • Slot: ${booking.slot}"
        tvConfirmedDaysLeft.visibility = View.GONE

        btnViewMyBookings.text = "View in My Bookings"
        btnViewMyBookings.setOnClickListener {
            val intent = Intent(this, MyGymBookingsActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}
