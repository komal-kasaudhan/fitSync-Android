package com.esha.fitsync.screens.gym

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.esha.fitsync.R
import com.esha.fitsync.api.RetrofitClient
import com.esha.fitsync.maps.OsmMapProvider
import com.esha.fitsync.models.GeocodeRequest
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch
import org.osmdroid.views.MapView
import java.util.Locale

class LocationPickerActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private var currentLat: Double = LocationHelper.DEFAULT_LAT
    private var currentLng: Double = LocationHelper.DEFAULT_LNG
    private var currentAddress: String = LocationHelper.DEFAULT_CITY_NAME

    private lateinit var tvSelectedCoords: TextView
    private lateinit var tvSelectedAddress: TextView
    private lateinit var etSearchAddress: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location_picker)

        currentLat = intent.getDoubleExtra("LAT", LocationHelper.DEFAULT_LAT)
        currentLng = intent.getDoubleExtra("LNG", LocationHelper.DEFAULT_LNG)

        mapView = findViewById(R.id.osmMapView)
        tvSelectedCoords = findViewById(R.id.tvSelectedCoords)
        tvSelectedAddress = findViewById(R.id.tvSelectedAddress)
        etSearchAddress = findViewById(R.id.etSearchAddress)

        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val btnSearch = findViewById<ImageButton>(R.id.btnSearch)
        val btnConfirm = findViewById<Button>(R.id.btnConfirmLocation)
        val btnMyLocation = findViewById<FloatingActionButton>(R.id.btnMyLocation)

        // Setup OSM map with zoom 15
        OsmMapProvider.setupMap(mapView, currentLat, currentLng, 15.0, true)
        updateCoordText(currentLat, currentLng)

        // Listen for scroll stop to read center pin position
        OsmMapProvider.setOnCenterChangedListener(mapView) { lat, lng ->
            currentLat = lat
            currentLng = lng
            updateCoordText(lat, lng)
        }

        btnBack.setOnClickListener { finish() }

        btnSearch.setOnClickListener { performSearch() }
        etSearchAddress.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch()
                true
            } else false
        }

        btnMyLocation.setOnClickListener {
            lifecycleScope.launch {
                val loc = LocationHelper.getDeviceLocation(this@LocationPickerActivity)
                if (loc != null) {
                    currentLat = loc.latitude
                    currentLng = loc.longitude
                    OsmMapProvider.setCenter(mapView, currentLat, currentLng, 15.0)
                    updateCoordText(currentLat, currentLng)
                    Toast.makeText(this@LocationPickerActivity, "Location updated to GPS", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@LocationPickerActivity, "Unable to get GPS location. Please check GPS settings.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        btnConfirm.setOnClickListener {
            val resultIntent = Intent().apply {
                putExtra("LAT", currentLat)
                putExtra("LNG", currentLng)
                putExtra("ADDRESS", currentAddress)
            }
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }
    }

    private fun updateCoordText(lat: Double, lng: Double) {
        val formatted = String.format(Locale.US, "Lat: %.4f, Lng: %.4f", lat, lng)
        tvSelectedCoords.text = formatted
    }

    private fun performSearch() {
        val query = etSearchAddress.text.toString().trim()
        if (query.isEmpty()) {
            Toast.makeText(this, "Enter an address, city, or pincode", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val res = RetrofitClient.api.geocodeAddress(GeocodeRequest(query))
                if (res.isSuccessful && res.body()?.location != null) {
                    val loc = res.body()!!.location!!
                    currentLat = loc.lat
                    currentLng = loc.lng
                    currentAddress = res.body()!!.formattedAddress ?: query
                    tvSelectedAddress.text = currentAddress
                    OsmMapProvider.setCenter(mapView, currentLat, currentLng, 15.0)
                } else {
                    val msg = res.body()?.message ?: "Location not found"
                    Toast.makeText(this@LocationPickerActivity, msg, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@LocationPickerActivity, "Geocoding error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        OsmMapProvider.onResume(mapView)
    }

    override fun onPause() {
        super.onPause()
        OsmMapProvider.onPause(mapView)
    }

    override fun onDestroy() {
        super.onDestroy()
        OsmMapProvider.onDetach(mapView)
    }
}
