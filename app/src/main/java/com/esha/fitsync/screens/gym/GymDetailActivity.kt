package com.esha.fitsync.screens.gym

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.esha.fitsync.R
import com.esha.fitsync.api.RetrofitClient
import com.esha.fitsync.maps.OsmMapProvider
import com.esha.fitsync.models.GymItem
import com.esha.fitsync.screens.gym.adapters.GymPhotoPagerAdapter
import com.esha.fitsync.screens.gym.adapters.GymReviewAdapter
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.launch
import org.osmdroid.views.MapView
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
    private lateinit var rvReviews: RecyclerView
    private lateinit var mapDetailOsm: MapView

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
        rvReviews = findViewById(R.id.rvReviews)
        mapDetailOsm = findViewById(R.id.mapDetailOsm)

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

        // Default map preview centered on default coords
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
            val intent = Intent(this, GymBookingActivity::class.java).apply {
                putExtra("GYM_ID", gymId)
                putExtra("GYM_NAME", gym?.name ?: gymNameExtra)
                putExtra("PRICE", gym?.startingPrice ?: 150.0)
            }
            startActivity(intent)
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

        val price = gym.startingPrice
        tvBottomPrice.text = if (price > 0) "₹${price.toInt()} / day" else "₹150 / day"

        if (gym.photos.isNotEmpty()) {
            photoAdapter.submitList(gym.photos)
            tvPhotoCount.text = "1 / ${gym.photos.size}"
        }

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

        populateOpeningHours(gym)

        // Setup OSM static preview map
        val lat = gym.location?.lat ?: LocationHelper.DEFAULT_LAT
        val lng = gym.location?.lng ?: LocationHelper.DEFAULT_LNG
        OsmMapProvider.setupMap(mapDetailOsm, lat, lng, 15.0, false)
        OsmMapProvider.clearMarkers(mapDetailOsm)
        OsmMapProvider.addMarker(mapDetailOsm, gym.id, lat, lng, gym.name) {
            openDirectionsIntent()
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
                        text = "${schedule.open} - ${schedule.close}"
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
