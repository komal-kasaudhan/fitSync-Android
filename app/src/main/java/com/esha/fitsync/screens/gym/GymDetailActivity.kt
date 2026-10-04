package com.esha.fitsync.screens.gym

import android.content.Intent
import android.graphics.Paint
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.CheckBox
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
import androidx.viewpager2.widget.ViewPager2
import com.esha.fitsync.BuildConfig
import com.esha.fitsync.R
import com.esha.fitsync.api.RetrofitClient
import com.esha.fitsync.maps.OsmMapProvider
import com.esha.fitsync.models.GymEnquiryRequest
import com.esha.fitsync.models.GymItem
import com.esha.fitsync.models.GymPlan
import com.esha.fitsync.screens.gym.adapters.GymPhotoPagerAdapter
import com.esha.fitsync.screens.gym.adapters.GymReviewAdapter
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.launch
import org.osmdroid.views.MapView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class GymDetailActivity : AppCompatActivity() {

    private var gymId: String = ""
    private var gymNameExtra: String = ""
    private var loadedGym: GymItem? = null

    private lateinit var vpPhotos: ViewPager2
    private lateinit var tvPhotoCount: TextView
    private lateinit var tvName: TextView
    private lateinit var tvRating: TextView
    private lateinit var tvAddress: TextView
    private lateinit var tvDistance: TextView
    private lateinit var tvDescription: TextView
    private lateinit var tvTodayOpenStatus: TextView
    private lateinit var tvBottomPrice: TextView
    private lateinit var tvReviewsSummary: TextView
    private lateinit var tvNoReviews: TextView
    private lateinit var chipGroupAmenities: ChipGroup
    private lateinit var layoutHours: LinearLayout
    private lateinit var layoutDetailPlans: LinearLayout
    private lateinit var rvReviews: RecyclerView
    private lateinit var mapDetailOsm: MapView

    // Enquiry views
    private lateinit var etEnquiryName: EditText
    private lateinit var etEnquiryPhone: EditText
    private lateinit var etEnquiryEmail: EditText
    private lateinit var chipGroupPreferredTime: ChipGroup
    private lateinit var cbEnquiryTrial: CheckBox
    private lateinit var etEnquiryMessage: EditText
    private lateinit var btnSubmitEnquiry: MaterialButton

    private val photoAdapter = GymPhotoPagerAdapter()
    private val reviewAdapter = GymReviewAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gym_detail)

        gymId = intent.getStringExtra("GYM_ID") ?: ""
        gymNameExtra = intent.getStringExtra("GYM_NAME") ?: "Gym Details"

        initViews()
        setupListeners()
        loadGymData()
        loadReviews()
    }

    private fun initViews() {
        vpPhotos = findViewById(R.id.vpGymPhotos)
        tvPhotoCount = findViewById(R.id.tvPhotoCount)
        tvName = findViewById(R.id.tvDetailGymName)
        tvRating = findViewById(R.id.tvDetailRating)
        tvAddress = findViewById(R.id.tvDetailAddress)
        tvDistance = findViewById(R.id.tvDetailDistance)
        tvDescription = findViewById(R.id.tvDetailDescription)
        tvTodayOpenStatus = findViewById(R.id.tvDetailTodayOpenStatus)
        tvBottomPrice = findViewById(R.id.tvBottomPrice)
        tvReviewsSummary = findViewById(R.id.tvReviewsSummary)
        tvNoReviews = findViewById(R.id.tvNoReviews)
        chipGroupAmenities = findViewById(R.id.chipGroupDetailAmenities)
        layoutHours = findViewById(R.id.layoutHoursSchedule)
        layoutDetailPlans = findViewById(R.id.layoutDetailPlans)
        rvReviews = findViewById(R.id.rvReviews)
        mapDetailOsm = findViewById(R.id.mapDetailOsm)

        etEnquiryName = findViewById(R.id.etEnquiryName)
        etEnquiryPhone = findViewById(R.id.etEnquiryPhone)
        etEnquiryEmail = findViewById(R.id.etEnquiryEmail)
        chipGroupPreferredTime = findViewById(R.id.chipGroupPreferredTime)
        cbEnquiryTrial = findViewById(R.id.cbEnquiryTrial)
        etEnquiryMessage = findViewById(R.id.etEnquiryMessage)
        btnSubmitEnquiry = findViewById(R.id.btnSubmitEnquiry)

        tvName.text = gymNameExtra

        vpPhotos.adapter = photoAdapter
        vpPhotos.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                val total = photoAdapter.itemCount
                tvPhotoCount.text = "${position + 1} / $total"
            }
        })

        rvReviews.layoutManager = LinearLayoutManager(this)
        rvReviews.adapter = reviewAdapter

        OsmMapProvider.setupMap(mapDetailOsm, LocationHelper.DEFAULT_LAT, LocationHelper.DEFAULT_LNG, 15.0, false)
        mapDetailOsm.setOnClickListener { openDirectionsIntent() }
    }

    private fun setupListeners() {
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        findViewById<MaterialButton>(R.id.btnCallGym).setOnClickListener {
            val phone = loadedGym?.phone ?: "+91 98351 11002"
            val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
            startActivity(dialIntent)
        }

        findViewById<MaterialButton>(R.id.btnDirections).setOnClickListener {
            openDirectionsIntent()
        }

        findViewById<Button>(R.id.btnBookSession).setOnClickListener {
            val gym = loadedGym
            val firstPlan = gym?.activePlans?.firstOrNull()
            launchBookingOrMembershipFlow(firstPlan)
        }

        btnSubmitEnquiry.setOnClickListener {
            submitEnquiry()
        }
    }

    private fun openDirectionsIntent() {
        val lat = loadedGym?.location?.lat ?: LocationHelper.DEFAULT_LAT
        val lng = loadedGym?.location?.lng ?: LocationHelper.DEFAULT_LNG
        val label = Uri.encode(loadedGym?.name ?: gymNameExtra)

        val geoUri = Uri.parse("geo:$lat,$lng?q=$lat,$lng($label)")
        val mapIntent = Intent(Intent.ACTION_VIEW, geoUri)

        try {
            startActivity(mapIntent)
        } catch (_: Exception) {
            val browserUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$lat,$lng")
            startActivity(Intent(Intent.ACTION_VIEW, browserUri))
        }
    }

    private fun loadGymData() {
        lifecycleScope.launch {
            try {
                val res = RetrofitClient.api.getGymDetails(gymId)
                if (res.isSuccessful && res.body()?.gym != null) {
                    val gym = res.body()!!.gym!!
                    loadedGym = gym
                    bindGymDetails(gym)
                } else {
                    fetchNearbyFallback()
                }
            } catch (e: Exception) {
                fetchNearbyFallback()
            }
        }
    }

    private suspend fun fetchNearbyFallback() {
        try {
            val res = RetrofitClient.api.getNearbyGyms(
                lat = LocationHelper.DEFAULT_LAT,
                lng = LocationHelper.DEFAULT_LNG,
                radiusKm = 25.0
            )
            if (res.isSuccessful) {
                val match = res.body()?.gyms?.find { it.id == gymId }
                if (match != null) {
                    loadedGym = match
                    bindGymDetails(match)
                }
            }
        } catch (_: Exception) {}
    }

    private fun bindGymDetails(gym: GymItem) {
        tvName.text = gym.name
        tvRating.text = String.format(Locale.US, "%.1f (%d reviews)", gym.ratingAvg, gym.ratingCount)
        tvReviewsSummary.text = String.format(Locale.US, "%.1f ★ (%d)", gym.ratingAvg, gym.ratingCount)

        val fullAddress = if (gym.city.isNotEmpty() && !gym.address.contains(gym.city, ignoreCase = true)) {
            "${gym.address}, ${gym.city}"
        } else {
            gym.address
        }
        tvAddress.text = fullAddress
        tvDistance.text = String.format(Locale.US, "📍 %.2f km away", gym.distanceKm)

        if (!gym.description.isNullOrEmpty()) {
            tvDescription.text = gym.description
        }

        val startPrice = gym.startingPriceRaw ?: gym.plans.filter { it.isActive }.minOfOrNull { it.price }
        if (startPrice != null && startPrice > 0) {
            tvBottomPrice.visibility = View.VISIBLE
            tvBottomPrice.text = "From Rs ${startPrice.toInt()}"
        } else {
            tvBottomPrice.visibility = View.GONE
        }

        if (gym.photos.isNotEmpty()) {
            photoAdapter.submitList(gym.photos)
            tvPhotoCount.text = "1 / ${gym.photos.size}"
        }

        // Amenities
        chipGroupAmenities.removeAllViews()
        gym.amenities.forEach { amenity ->
            val chip = Chip(this).apply {
                text = amenity.replaceFirstChar(Char::uppercase)
                isClickable = false
                isCheckable = false
                setChipBackgroundColorResource(R.color.gym_chip_bg)
                setTextColor(ContextCompat.getColor(context, R.color.text_white_crisp))
            }
            chipGroupAmenities.addView(chip)
        }

        // Open/Closed badge from real shifts
        val isOpen = isGymOpenNow(gym)
        if (isOpen) {
            tvTodayOpenStatus.text = "OPEN NOW"
            tvTodayOpenStatus.setTextColor(ContextCompat.getColor(this, R.color.gym_green))
        } else {
            tvTodayOpenStatus.text = "CLOSED"
            tvTodayOpenStatus.setTextColor(ContextCompat.getColor(this, R.color.status_rejected))
        }

        populateOpeningHours(gym)
        populateMembershipPlans(gym)

        // Setup OSM static preview map
        val lat = gym.location?.lat ?: LocationHelper.DEFAULT_LAT
        val lng = gym.location?.lng ?: LocationHelper.DEFAULT_LNG
        OsmMapProvider.setupMap(mapDetailOsm, lat, lng, 15.0, false)
        OsmMapProvider.clearMarkers(mapDetailOsm)
        OsmMapProvider.addMarker(mapDetailOsm, gym.id, lat, lng, gym.name) {
            openDirectionsIntent()
        }
    }

    private fun isGymOpenNow(gym: GymItem): Boolean {
        val cal = Calendar.getInstance()
        val todayDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
        if (gym.holidays.any { it.date.equals(todayDateStr, ignoreCase = true) }) {
            return false
        }

        if (gym.openingHours.isEmpty()) return true
        val dayOfWeek = getTodayDayName()
        val todaySchedule = gym.openingHours.find { it.day.equals(dayOfWeek, ignoreCase = true) }
            ?: return true

        if (todaySchedule.isClosed) return false

        val currentTotalMin = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val shifts = todaySchedule.displayShifts
        if (shifts.isEmpty()) return false

        return shifts.any { shift ->
            val openMin = parseTimeToMinutes(shift.open)
            val closeMin = parseTimeToMinutes(shift.close)
            currentTotalMin in openMin..closeMin
        }
    }

    private fun parseTimeToMinutes(timeStr: String): Int {
        return try {
            val parts = timeStr.trim().split(":")
            val h = parts[0].toInt()
            val m = parts.getOrNull(1)?.toInt() ?: 0
            h * 60 + m
        } catch (_: Exception) {
            0
        }
    }

    private fun populateOpeningHours(gym: GymItem) {
        layoutHours.removeAllViews()
        val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
        val currentDay = getTodayDayName()

        days.forEach { dayName ->
            val schedule = gym.openingHours.find { it.day.equals(dayName, ignoreCase = true) }
            val isToday = dayName.equals(currentDay, ignoreCase = true)

            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 8, 0, 8)
            }

            val tvDay = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                text = if (isToday) "$dayName (Today)" else dayName
                textSize = 13f
                setTextColor(
                    ContextCompat.getColor(
                        this@GymDetailActivity,
                        if (isToday) R.color.gym_accent else R.color.text_white_crisp
                    )
                )
                if (isToday) setTypeface(typeface, android.graphics.Typeface.BOLD)
            }

            val tvTime = TextView(this).apply {
                textSize = 13f
                if (schedule != null) {
                    if (schedule.isClosed) {
                        text = "Closed"
                        setTextColor(ContextCompat.getColor(this@GymDetailActivity, R.color.status_rejected))
                    } else {
                        val shifts = schedule.displayShifts
                        text = if (shifts.isNotEmpty()) {
                            shifts.joinToString(", ") { "${it.open} - ${it.close}" }
                        } else {
                            "${schedule.open} - ${schedule.close}"
                        }
                        setTextColor(
                            ContextCompat.getColor(
                                this@GymDetailActivity,
                                if (isToday) R.color.gym_accent else R.color.text_gray_blue
                            )
                        )
                    }
                } else {
                    text = "06:00 - 22:00"
                    setTextColor(ContextCompat.getColor(this@GymDetailActivity, R.color.text_gray_blue))
                }
                if (isToday) setTypeface(typeface, android.graphics.Typeface.BOLD)
            }

            row.addView(tvDay)
            row.addView(tvTime)
            layoutHours.addView(row)
        }
    }

    private fun populateMembershipPlans(gym: GymItem) {
        layoutDetailPlans.removeAllViews()
        val plans = gym.activePlans
        if (plans.isEmpty()) {
            val emptyTv = TextView(this).apply {
                text = "No plans currently active. Contact gym directly for walk-in passes."
                setTextColor(ContextCompat.getColor(this@GymDetailActivity, R.color.text_muted))
                textSize = 13f
            }
            layoutDetailPlans.addView(emptyTv)
            return
        }

        val inflater = LayoutInflater.from(this)
        plans.forEach { plan ->
            val card = inflater.inflate(R.layout.item_detail_plan_card, layoutDetailPlans, false)

            val tvName = card.findViewById<TextView>(R.id.tvPlanCardName)
            val tvDuration = card.findViewById<TextView>(R.id.tvPlanCardDuration)
            val tvPrice = card.findViewById<TextView>(R.id.tvPlanCardPrice)
            val tvMrp = card.findViewById<TextView>(R.id.tvPlanCardMrp)
            val tvFreeze = card.findViewById<TextView>(R.id.tvPlanCardFreeze)
            val tvInclusions = card.findViewById<TextView>(R.id.tvPlanCardInclusions)
            val btnChoose = card.findViewById<MaterialButton>(R.id.btnChoosePlan)

            tvName.text = plan.name
            tvDuration.text = "${plan.durationDays} days access • ${plan.displayType}"
            tvPrice.text = "₹${plan.price.toInt()}"

            if (plan.mrp != null && plan.mrp > plan.price) {
                tvMrp.visibility = View.VISIBLE
                tvMrp.text = "MRP: ₹${plan.mrp.toInt()}"
                tvMrp.paintFlags = tvMrp.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                tvMrp.visibility = View.GONE
            }

            if (plan.maxFreezeDays > 0) {
                tvFreeze.visibility = View.VISIBLE
                tvFreeze.text = "❄️ Freezable up to ${plan.maxFreezeDays} days"
            } else {
                tvFreeze.visibility = View.GONE
            }

            if (plan.inclusions.isNotEmpty()) {
                tvInclusions.visibility = View.VISIBLE
                tvInclusions.text = "Inclusions: ${plan.inclusions.joinToString(", ")}"
            } else {
                tvInclusions.visibility = View.GONE
            }

            btnChoose.setOnClickListener {
                launchBookingOrMembershipFlow(plan)
            }

            layoutDetailPlans.addView(card)
        }
    }

    private fun launchBookingOrMembershipFlow(plan: GymPlan?) {
        val gym = loadedGym
        val intent = Intent(this, GymBookingActivity::class.java).apply {
            putExtra("GYM_ID", gymId)
            putExtra("GYM_NAME", gym?.name ?: gymNameExtra)
            putExtra("PRICE", plan?.price ?: (gym?.startingPrice ?: 150.0))
            if (plan != null) {
                putExtra("PLAN_ID", plan.planId)
                putExtra("PLAN_NAME", plan.name)
                putExtra("PLAN_TYPE", plan.type)
                putExtra("PLAN_DURATION", plan.durationDays)
                putExtra("MAX_FREEZE_DAYS", plan.maxFreezeDays)
            }
        }
        startActivity(intent)
    }

    private fun submitEnquiry() {
        val name = etEnquiryName.text.toString().trim()
        val phone = etEnquiryPhone.text.toString().trim()
        val email = etEnquiryEmail.text.toString().trim().ifEmpty { null }
        val message = etEnquiryMessage.text.toString().trim()
        val wantsTrial = cbEnquiryTrial.isChecked

        if (name.isEmpty()) {
            etEnquiryName.error = "Name is required"
            etEnquiryName.requestFocus()
            return
        }
        if (phone.isEmpty()) {
            etEnquiryPhone.error = "Phone is required"
            etEnquiryPhone.requestFocus()
            return
        }

        val preferredTime = when (chipGroupPreferredTime.checkedChipId) {
            R.id.chipTimeAfternoon -> "afternoon"
            R.id.chipTimeEvening -> "evening"
            else -> "morning"
        }

        btnSubmitEnquiry.isEnabled = false
        btnSubmitEnquiry.text = "Sending..."

        val req = GymEnquiryRequest(
            name = name,
            phone = phone,
            email = email,
            message = message,
            preferredTime = preferredTime,
            wantsTrial = wantsTrial
        )

        lifecycleScope.launch {
            try {
                val res = RetrofitClient.api.sendGymEnquiry(gymId, req)
                btnSubmitEnquiry.isEnabled = true
                btnSubmitEnquiry.text = "Send Enquiry"

                if (res.isSuccessful && res.body()?.success == true) {
                    Toast.makeText(
                        this@GymDetailActivity,
                        "Enquiry submitted! The gym owner has been notified.",
                        Toast.LENGTH_LONG
                    ).show()
                    etEnquiryMessage.setText("")
                    cbEnquiryTrial.isChecked = false
                } else {
                    val errMsg = res.body()?.message ?: "Submission failed (${res.code()})"
                    Toast.makeText(this@GymDetailActivity, errMsg, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                btnSubmitEnquiry.isEnabled = true
                btnSubmitEnquiry.text = "Send Enquiry"
                Toast.makeText(this@GymDetailActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun getTodayDayName(): String {
        return when (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "Monday"
            Calendar.TUESDAY -> "Tuesday"
            Calendar.WEDNESDAY -> "Wednesday"
            Calendar.THURSDAY -> "Thursday"
            Calendar.FRIDAY -> "Friday"
            Calendar.SATURDAY -> "Saturday"
            Calendar.SUNDAY -> "Sunday"
            else -> "Monday"
        }
    }

    private fun loadReviews() {
        lifecycleScope.launch {
            try {
                val res = RetrofitClient.api.getGymReviews(gymId)
                if (res.isSuccessful && res.body()?.reviews != null) {
                    val reviews = res.body()!!.reviews
                    if (reviews.isNotEmpty()) {
                        tvNoReviews.visibility = View.GONE
                        rvReviews.visibility = View.VISIBLE
                        reviewAdapter.submitList(reviews)
                    } else {
                        tvNoReviews.visibility = View.VISIBLE
                        rvReviews.visibility = View.GONE
                    }
                } else {
                    tvNoReviews.visibility = View.VISIBLE
                }
            } catch (_: Exception) {
                tvNoReviews.visibility = View.VISIBLE
            }
        }
    }

    override fun onResume() {
        super.onResume()
        OsmMapProvider.onResume(mapDetailOsm)
    }

    override fun onPause() {
        super.onPause()
        OsmMapProvider.onPause(mapDetailOsm)
    }

    override fun onDestroy() {
        super.onDestroy()
        OsmMapProvider.onDetach(mapDetailOsm)
    }
}
