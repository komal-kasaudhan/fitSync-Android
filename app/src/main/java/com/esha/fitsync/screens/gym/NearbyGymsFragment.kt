package com.esha.fitsync.screens.gym

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.esha.fitsync.BuildConfig
import com.esha.fitsync.R
import com.esha.fitsync.api.RetrofitClient
import com.esha.fitsync.data.AppCacheManager
import com.esha.fitsync.maps.OsmMapProvider
import com.esha.fitsync.models.GeocodeRequest
import com.esha.fitsync.models.GymItem
import com.esha.fitsync.models.NearbyGymsResponse
import com.esha.fitsync.screens.gym.adapters.NearbyGymAdapter
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.launch
import org.osmdroid.views.MapView
import java.util.Locale

class NearbyGymsFragment : Fragment() {

    private var currentLat = LocationHelper.DEFAULT_LAT
    private var currentLng = LocationHelper.DEFAULT_LNG
    private var currentCityName = LocationHelper.DEFAULT_CITY_NAME
    private var currentRadiusKm = 5.0
    private var currentSort = "distance"
    private var currentOpenNow = false
    private var currentMaxPrice: Double? = null
    private var currentAmenities = listOf<String>()

    private var isMapMode = false
    private var currentGyms: List<GymItem> = emptyList()

    private lateinit var adapter: NearbyGymAdapter
    private lateinit var rvGyms: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var shimmer: ShimmerFrameLayout
    private lateinit var layoutMapContainer: FrameLayout
    private lateinit var osmMapView: MapView
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var layoutError: LinearLayout
    private lateinit var mapMiniCard: View
    private lateinit var tvLocationName: TextView
    private lateinit var etSearchCity: EditText
    private lateinit var btnToggleMap: ImageButton

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            fetchGpsLocation()
        } else {
            Toast.makeText(requireContext(), "Location permission denied. Using default location.", Toast.LENGTH_SHORT).show()
            loadGyms()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_nearby_gyms, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupRecyclerView()
        setupListeners(view)

        // Init OSM Map
        OsmMapProvider.setupMap(osmMapView, currentLat, currentLng, 13.0, true)

        // Try getting real device location if permitted, else load default
        if (LocationHelper.hasLocationPermission(requireContext())) {
            fetchGpsLocation()
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
            loadGyms()
        }
    }

    private fun initViews(view: View) {
        rvGyms = view.findViewById(R.id.rvGyms)
        swipeRefresh = view.findViewById(R.id.swipeRefreshGyms)
        shimmer = view.findViewById(R.id.shimmerNearbyGyms)
        layoutMapContainer = view.findViewById(R.id.layoutMapContainer)
        osmMapView = view.findViewById(R.id.gymsOsmMapView)
        layoutEmpty = view.findViewById(R.id.layoutEmpty)
        layoutError = view.findViewById(R.id.layoutError)
        mapMiniCard = view.findViewById(R.id.mapMiniCard)
        tvLocationName = view.findViewById(R.id.tvLocationName)
        etSearchCity = view.findViewById(R.id.etSearchCity)
        btnToggleMap = view.findViewById(R.id.btnToggleMap)

        tvLocationName.text = currentCityName
    }

    private fun setupRecyclerView() {
        adapter = NearbyGymAdapter { gym ->
            openGymDetails(gym)
        }
        rvGyms.layoutManager = LinearLayoutManager(requireContext())
        rvGyms.adapter = adapter
    }

    private fun setupListeners(view: View) {
        view.findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        btnToggleMap.setOnClickListener {
            toggleMapListMode()
        }

        view.findViewById<ImageButton>(R.id.btnFilter).setOnClickListener {
            showFilterBottomSheet()
        }

        view.findViewById<ImageButton>(R.id.btnGpsLocation).setOnClickListener {
            if (LocationHelper.hasLocationPermission(requireContext())) {
                fetchGpsLocation()
            } else {
                LocationHelper.showPermissionRationaleDialog(
                    requireContext(),
                    onPositive = {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    },
                    onNegative = { loadGyms() }
                )
            }
        }

        etSearchCity.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                searchCity(etSearchCity.text.toString().trim())
                true
            } else false
        }

        // Radius Chips
        val chipGroupRadius = view.findViewById<ChipGroup>(R.id.chipGroupRadius)
        chipGroupRadius.setOnCheckedStateChangeListener { _, checkedIds ->
            currentRadiusKm = when (checkedIds.firstOrNull()) {
                R.id.chipRadius1 -> 1.0
                R.id.chipRadius3 -> 3.0
                R.id.chipRadius10 -> 10.0
                else -> 5.0
            }
            loadGyms()
        }

        swipeRefresh.setOnRefreshListener {
            loadGyms()
        }

        view.findViewById<Button>(R.id.btnExpandRadius).setOnClickListener {
            view.findViewById<Chip>(R.id.chipRadius10)?.isChecked = true
            currentRadiusKm = 10.0
            loadGyms()
        }

        view.findViewById<Button>(R.id.btnRetry).setOnClickListener {
            loadGyms()
        }
    }

    private fun toggleMapListMode() {
        isMapMode = !isMapMode
        if (isMapMode) {
            swipeRefresh.visibility = View.GONE
            layoutMapContainer.visibility = View.VISIBLE
            btnToggleMap.setImageResource(R.drawable.ic_list)
            updateMapPins()
        } else {
            layoutMapContainer.visibility = View.GONE
            swipeRefresh.visibility = View.VISIBLE
            btnToggleMap.setImageResource(R.drawable.ic_map)
        }
    }

    private fun fetchGpsLocation() {
        if (!LocationHelper.isGpsEnabled(requireContext())) {
            LocationHelper.showGpsDisabledDialog(requireContext()) {
                loadGyms()
            }
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val loc = LocationHelper.getDeviceLocation(requireContext())
            if (loc != null) {
                currentLat = loc.latitude
                currentLng = loc.longitude
                currentCityName = String.format(Locale.US, "GPS (%.3f, %.3f)", currentLat, currentLng)
                tvLocationName.text = currentCityName
                OsmMapProvider.setCenter(osmMapView, currentLat, currentLng, 13.0)
            }
            loadGyms()
        }
    }

    private fun searchCity(query: String) {
        if (query.isEmpty()) return
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val res = RetrofitClient.api.geocodeAddress(GeocodeRequest(query))
                if (res.isSuccessful && res.body()?.location != null) {
                    val loc = res.body()!!.location!!
                    currentLat = loc.lat
                    currentLng = loc.lng
                    currentCityName = res.body()!!.formattedAddress ?: query
                    tvLocationName.text = currentCityName
                    OsmMapProvider.setCenter(osmMapView, currentLat, currentLng, 13.0)
                    loadGyms()
                } else {
                    val msg = res.body()?.message ?: "Address not found"
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Geocoding error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadGyms() {
        val cacheKey = "NEARBY_GYMS_${currentRadiusKm}_${currentSort}"
        showLoading(true)

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val amenitiesParam = if (currentAmenities.isNotEmpty()) currentAmenities.joinToString(",") else null
                val openNowParam = if (currentOpenNow) true else null

                // Verify lat and lng are not swapped in request
                Log.d("FitSyncNearby", "Requesting nearby gyms: lat=$currentLat, lng=$currentLng, radiusKm=$currentRadiusKm, sort=$currentSort")

                val response = RetrofitClient.api.getNearbyGyms(
                    lat = currentLat,
                    lng = currentLng,
                    radiusKm = currentRadiusKm,
                    amenities = amenitiesParam,
                    maxPrice = currentMaxPrice,
                    openNow = openNowParam,
                    sort = currentSort
                )

                showLoading(false)

                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    val gyms = body.gyms
                    currentGyms = gyms
                    Log.d("FitSyncNearby", "Received ${gyms.size} gyms from server")
                    AppCacheManager.put(cacheKey, body)

                    if (gyms.isEmpty()) {
                        showEmpty(true)
                    } else {
                        showEmpty(false)
                        showError(false)
                        adapter.submitList(gyms)
                        updateMapPins()
                    }
                } else {
                    handleFetchFailure(cacheKey, "Server error (${response.code()})")
                }
            } catch (e: Exception) {
                showLoading(false)
                handleFetchFailure(cacheKey, e.localizedMessage ?: "Network error")
            }
        }
    }

    private fun handleFetchFailure(cacheKey: String, errorMsg: String) {
        val cached = AppCacheManager.get<NearbyGymsResponse>(cacheKey)
        if (cached != null && cached.gyms.isNotEmpty()) {
            currentGyms = cached.gyms
            adapter.submitList(cached.gyms)
            updateMapPins()
            showEmpty(false)
            showError(false)
            Toast.makeText(requireContext(), "Offline: Showing cached gyms", Toast.LENGTH_SHORT).show()
        } else {
            showError(true, errorMsg)
        }
    }

    private fun showFilterBottomSheet() {
        val sheet = GymFiltersBottomSheet(
            currentSort = currentSort,
            currentOpenNow = currentOpenNow,
            currentMaxPrice = currentMaxPrice,
            currentAmenities = currentAmenities
        ) { sort, openNow, maxPrice, amenities ->
            currentSort = sort
            currentOpenNow = openNow
            currentMaxPrice = maxPrice
            currentAmenities = amenities
            loadGyms()
        }
        sheet.show(childFragmentManager, "GymFiltersBottomSheet")
    }

    private fun showLoading(isLoading: Boolean) {
        swipeRefresh.isRefreshing = false
        if (isLoading) {
            shimmer.visibility = View.VISIBLE
            shimmer.startShimmer()
            rvGyms.visibility = View.GONE
            layoutEmpty.visibility = View.GONE
            layoutError.visibility = View.GONE
        } else {
            shimmer.stopShimmer()
            shimmer.visibility = View.GONE
            rvGyms.visibility = View.VISIBLE
        }
    }

    private fun showEmpty(isEmpty: Boolean) {
        layoutEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
        if (isEmpty) {
            rvGyms.visibility = View.GONE
            layoutError.visibility = View.GONE
            view?.findViewById<TextView>(R.id.tvEmptySubtitle)?.text =
                "No approved gyms found within ${currentRadiusKm.toInt()} km. Expand radius or register your gym!"
        }
    }

    private fun showError(isError: Boolean, msg: String = "") {
        layoutError.visibility = if (isError) View.VISIBLE else View.GONE
        if (isError) {
            view?.findViewById<TextView>(R.id.tvErrorMessage)?.text = msg
            rvGyms.visibility = View.GONE
            layoutEmpty.visibility = View.GONE
        }
    }

    private fun updateMapPins() {
        OsmMapProvider.clearMarkers(osmMapView)
        OsmMapProvider.setCenter(osmMapView, currentLat, currentLng, if (currentRadiusKm <= 3) 14.0 else 12.0)

        // Center marker for user location
        OsmMapProvider.addMarker(
            osmMapView,
            "center_marker",
            currentLat,
            currentLng,
            "Search Center: $currentCityName"
        )

        currentGyms.forEach { gym ->
            val loc = gym.location
            if (loc != null) {
                OsmMapProvider.addMarker(
                    osmMapView,
                    gym.id,
                    loc.lat,
                    loc.lng,
                    gym.name,
                    String.format(Locale.US, "%.2f km • ₹%d", gym.distanceKm, gym.startingPrice.toInt())
                ) {
                    showMapMiniCard(gym)
                }
            }
        }
    }

    private fun showMapMiniCard(gym: GymItem) {
        mapMiniCard.visibility = View.VISIBLE
        val tvName = mapMiniCard.findViewById<TextView>(R.id.tvMiniGymName)
        val tvRating = mapMiniCard.findViewById<TextView>(R.id.tvMiniRating)
        val tvDistance = mapMiniCard.findViewById<TextView>(R.id.tvMiniDistance)
        val tvPrice = mapMiniCard.findViewById<TextView>(R.id.tvMiniPrice)
        val ivPhoto = mapMiniCard.findViewById<ImageView>(R.id.ivMiniGymPhoto)
        val btnView = mapMiniCard.findViewById<Button>(R.id.btnMiniView)

        tvName.text = gym.name
        tvRating.text = String.format(Locale.US, "%.1f", gym.ratingAvg)
        tvDistance.text = String.format(Locale.US, "%.2f km away", gym.distanceKm)
        tvPrice.text = "From ₹${gym.startingPrice.toInt()} / day"

        val photoUrl = gym.primaryPhoto
        if (!photoUrl.isNullOrEmpty()) {
            val base = BuildConfig.BASE_URL.trimEnd('/')
            val fullUrl = if (photoUrl.startsWith("http")) photoUrl else "$base/${photoUrl.trimStart('/')}"
            Glide.with(this)
                .load(fullUrl)
                .transform(CenterCrop(), RoundedCorners(16))
                .placeholder(R.drawable.gym)
                .into(ivPhoto)
        } else {
            ivPhoto.setImageResource(R.drawable.gym)
        }

        btnView.setOnClickListener {
            openGymDetails(gym)
        }
        mapMiniCard.setOnClickListener {
            openGymDetails(gym)
        }
    }

    private fun openGymDetails(gym: GymItem) {
        val intent = Intent(requireContext(), GymDetailActivity::class.java).apply {
            putExtra("GYM_ID", gym.id)
            putExtra("GYM_NAME", gym.name)
        }
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        OsmMapProvider.onResume(osmMapView)
    }

    override fun onPause() {
        super.onPause()
        OsmMapProvider.onPause(osmMapView)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        OsmMapProvider.onDetach(osmMapView)
    }
}
