package com.esha.fitsync.screens.gym

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.esha.fitsync.R
import com.esha.fitsync.api.RetrofitClient
import com.esha.fitsync.models.GeoLocation
import com.esha.fitsync.models.GymOpeningHour
import com.esha.fitsync.models.GymSessionType
import com.esha.fitsync.models.RegisterGymRequest
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.Locale

class RegisterGymActivity : AppCompatActivity() {

    private var editingGymId: String = ""
    private var selectedLat: Double = LocationHelper.DEFAULT_LAT
    private var selectedLng: Double = LocationHelper.DEFAULT_LNG
    private var uploadedPhotoUrl: String = ""

    private lateinit var etName: EditText
    private lateinit var etDesc: EditText
    private lateinit var etPhone: EditText
    private lateinit var etAddress: EditText
    private lateinit var etCity: EditText
    private lateinit var etPincode: EditText
    private lateinit var etCapacity: EditText
    private lateinit var etDayPassPrice: EditText
    private lateinit var tvCoordsDisplay: TextView
    private lateinit var tvPhotoStatus: TextView
    private lateinit var ivPhotoPreview: ImageView
    private lateinit var btnSubmit: Button
    private lateinit var chipGroupAmenities: ChipGroup

    private val mapPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            selectedLat = result.data!!.getDoubleExtra("LAT", LocationHelper.DEFAULT_LAT)
            selectedLng = result.data!!.getDoubleExtra("LNG", LocationHelper.DEFAULT_LNG)
            val addr = result.data!!.getStringExtra("ADDRESS") ?: ""
            if (addr.isNotEmpty() && etAddress.text.isEmpty()) {
                etAddress.setText(addr)
            }
            updateCoordsText()
        }
    }

    private val photoPickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            uploadSelectedPhoto(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register_gym)

        editingGymId = intent.getStringExtra("GYM_ID") ?: ""
        val rejectionReason = intent.getStringExtra("REASON")

        initViews()
        setupListeners()

        if (rejectionReason != null && rejectionReason.isNotEmpty()) {
            val tvRejection = findViewById<TextView>(R.id.tvRejectionBanner)
            tvRejection.visibility = View.VISIBLE
            tvRejection.text = "⚠️ Previous application needs update: $rejectionReason"
        }

        if (editingGymId.isNotEmpty()) {
            findViewById<TextView>(R.id.tvRegisterTitle).text = "Update Gym Details"
            btnSubmit.text = "Save & Resubmit Gym"
            loadExistingGymData(editingGymId)
        }
    }

    private fun initViews() {
        etName = findViewById(R.id.etGymName)
        etDesc = findViewById(R.id.etGymDescription)
        etPhone = findViewById(R.id.etGymPhone)
        etAddress = findViewById(R.id.etGymAddress)
        etCity = findViewById(R.id.etGymCity)
        etPincode = findViewById(R.id.etGymPincode)
        etCapacity = findViewById(R.id.etCapacityPerSlot)
        etDayPassPrice = findViewById(R.id.etDayPassPrice)
        tvCoordsDisplay = findViewById(R.id.tvCoordinatesDisplay)
        tvPhotoStatus = findViewById(R.id.tvPhotoStatus)
        ivPhotoPreview = findViewById(R.id.ivGymPhotoPreview)
        btnSubmit = findViewById(R.id.btnSubmitRegistration)
        chipGroupAmenities = findViewById(R.id.chipGroupRegisterAmenities)

        updateCoordsText()
    }

    private fun setupListeners() {
        findViewById<ImageButton>(R.id.btnRegisterBack).setOnClickListener { finish() }

        findViewById<MaterialButton>(R.id.btnUseGps).setOnClickListener {
            lifecycleScope.launch {
                val loc = LocationHelper.getDeviceLocation(this@RegisterGymActivity)
                if (loc != null) {
                    selectedLat = loc.latitude
                    selectedLng = loc.longitude
                    updateCoordsText()
                    Toast.makeText(this@RegisterGymActivity, "GPS location applied", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@RegisterGymActivity, "Unable to get GPS lock", Toast.LENGTH_SHORT).show()
                }
            }
        }

        findViewById<MaterialButton>(R.id.btnPickOnMap).setOnClickListener {
            val intent = Intent(this, LocationPickerActivity::class.java).apply {
                putExtra("LAT", selectedLat)
                putExtra("LNG", selectedLng)
            }
            mapPickerLauncher.launch(intent)
        }

        findViewById<Button>(R.id.btnUploadPhoto).setOnClickListener {
            photoPickerLauncher.launch("image/*")
        }

        btnSubmit.setOnClickListener {
            validateAndSubmit()
        }
    }

    private fun updateCoordsText() {
        tvCoordsDisplay.text = String.format(Locale.US, "Selected Coordinates: Lat %.4f, Lng %.4f", selectedLat, selectedLng)
    }

    private fun uploadSelectedPhoto(uri: Uri) {
        tvPhotoStatus.text = "Uploading photo..."
        lifecycleScope.launch {
            try {
                val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
                if (bytes == null || bytes.isEmpty()) {
                    tvPhotoStatus.text = "Failed to read image"
                    return@launch
                }

                val reqBody = bytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
                val part = MultipartBody.Part.createFormData("photo", "gym_upload_${System.currentTimeMillis()}.jpg", reqBody)

                val res = RetrofitClient.api.uploadGymPhoto(part)
                if (res.isSuccessful && res.body()?.url != null) {
                    uploadedPhotoUrl = res.body()!!.url
                    tvPhotoStatus.text = "Uploaded successfully ✓"
                    Glide.with(this@RegisterGymActivity)
                        .load(uri)
                        .transform(CenterCrop(), RoundedCorners(16))
                        .into(ivPhotoPreview)
                } else {
                    tvPhotoStatus.text = "Upload failed (${res.code()})"
                }
            } catch (e: Exception) {
                tvPhotoStatus.text = "Upload error: ${e.message}"
            }
        }
    }

    private fun loadExistingGymData(id: String) {
        lifecycleScope.launch {
            try {
                val res = RetrofitClient.api.getGymDetails(id)
                if (res.isSuccessful && res.body()?.gym != null) {
                    val gym = res.body()!!.gym!!
                    etName.setText(gym.name)
                    etDesc.setText(gym.description ?: "")
                    etPhone.setText(gym.phone ?: "")
                    etAddress.setText(gym.address)
                    etCity.setText(gym.city)
                    etPincode.setText(gym.pincode ?: "")
                    etCapacity.setText(gym.capacityPerSlot.toString())
                    etDayPassPrice.setText(gym.startingPrice.toInt().toString())

                    if (gym.location != null) {
                        selectedLat = gym.location.lat
                        selectedLng = gym.location.lng
                        updateCoordsText()
                    }

                    if (gym.photos.isNotEmpty()) {
                        uploadedPhotoUrl = gym.photos.first()
                        tvPhotoStatus.text = "Photo loaded"
                    }
                }
            } catch (_: Exception) {}
        }
    }

    private fun validateAndSubmit() {
        val name = etName.text.toString().trim()
        val desc = etDesc.text.toString().trim()
        val phone = etPhone.text.toString().trim()
        val address = etAddress.text.toString().trim()
        val city = etCity.text.toString().trim()
        val pincode = etPincode.text.toString().trim()
        val capacity = etCapacity.text.toString().toIntOrNull() ?: 20
        val dayPassPrice = etDayPassPrice.text.toString().toDoubleOrNull() ?: 150.0

        if (name.isEmpty()) {
            etName.error = "Gym name is required"
            etName.requestFocus()
            return
        }
        if (address.isEmpty()) {
            etAddress.error = "Address is required"
            etAddress.requestFocus()
            return
        }
        if (city.isEmpty()) {
            etCity.error = "City is required"
            etCity.requestFocus()
            return
        }

        val selectedAmenities = mutableListOf<String>()
        val amenitiesMap = mapOf(
            R.id.chipRegAc to "ac",
            R.id.chipRegCardio to "cardio",
            R.id.chipRegWeights to "weights",
            R.id.chipRegLockers to "lockers",
            R.id.chipRegParking to "parking",
            R.id.chipRegWifi to "wifi",
            R.id.chipRegShower to "shower"
        )
        amenitiesMap.forEach { (id, key) ->
            if (findViewById<Chip>(id)?.isChecked == true) {
                selectedAmenities.add(key)
            }
        }

        val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
        val openingHours = days.map { day ->
            GymOpeningHour(day = day, open = "06:00", close = "22:00", isClosed = false)
        }

        val sessionTypes = listOf(
            GymSessionType(type = "dayPass", name = "Day Pass", price = dayPassPrice, description = "1-day gym access")
        )

        val photosList = if (uploadedPhotoUrl.isNotEmpty()) listOf(uploadedPhotoUrl) else emptyList()

        val req = RegisterGymRequest(
            name = name,
            description = desc.ifEmpty { null },
            address = address,
            city = city,
            pincode = pincode.ifEmpty { null },
            phone = phone.ifEmpty { null },
            location = GeoLocation(coordinates = listOf(selectedLng, selectedLat)),
            photos = photosList,
            amenities = selectedAmenities,
            openingHours = openingHours,
            sessionTypes = sessionTypes,
            capacityPerSlot = capacity
        )

        btnSubmit.isEnabled = false
        btnSubmit.text = "Submitting..."

        lifecycleScope.launch {
            try {
                val res = if (editingGymId.isNotEmpty()) {
                    RetrofitClient.api.updateGym(editingGymId, req)
                } else {
                    RetrofitClient.api.registerGym(req)
                }

                btnSubmit.isEnabled = true
                btnSubmit.text = "Submit Gym Application"

                if (res.isSuccessful) {
                    Toast.makeText(
                        this@RegisterGymActivity,
                        "Gym registered successfully! Application submitted for admin approval.",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                } else {
                    val errMsg = res.body()?.message ?: "Registration failed (${res.code()})"
                    Toast.makeText(this@RegisterGymActivity, errMsg, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                btnSubmit.isEnabled = true
                btnSubmit.text = "Submit Gym Application"
                Toast.makeText(this@RegisterGymActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
