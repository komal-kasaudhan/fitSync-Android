package com.esha.fitsync.screens.gym

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.esha.fitsync.R
import com.esha.fitsync.api.RetrofitClient
import com.esha.fitsync.models.GeoLocation
import com.esha.fitsync.models.GymHoliday
import com.esha.fitsync.models.GymOpeningHour
import com.esha.fitsync.models.GymPlan
import com.esha.fitsync.models.GymSessionType
import com.esha.fitsync.models.GymShift
import com.esha.fitsync.models.RegisterGymRequest
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.text.SimpleDateFormat
import java.util.Calendar
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
    private lateinit var tvCoordsDisplay: TextView
    private lateinit var tvPhotoStatus: TextView
    private lateinit var ivPhotoPreview: ImageView
    private lateinit var btnSubmit: Button
    private lateinit var chipGroupAmenities: ChipGroup

    private lateinit var layoutDaysScheduleContainer: LinearLayout
    private lateinit var layoutHolidaysContainer: LinearLayout
    private lateinit var layoutPlansContainer: LinearLayout
    private lateinit var btnCopyMondayAll: MaterialButton
    private lateinit var btnAddHoliday: MaterialButton
    private lateinit var btnAddPlan: MaterialButton

    private val weekdays = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

    private class DayScheduleState(
        val dayName: String,
        val switchClosed: SwitchMaterial,
        val layoutShifts: LinearLayout,
        val btnAddShift: MaterialButton,
        val shifts: MutableList<GymShift> = mutableListOf()
    )

    private val dayStates = mutableListOf<DayScheduleState>()
    private val holidays = mutableListOf<GymHoliday>()
    private val planViews = mutableListOf<View>()

    private val planTypeKeys = listOf(
        "day_pass",
        "weekly",
        "monthly",
        "quarterly",
        "half_yearly",
        "yearly",
        "custom"
    )

    private val planTypeLabels = listOf(
        "Day pass (1 day)",
        "Weekly (7 days)",
        "Monthly (30 days)",
        "Quarterly (90 days)",
        "Half-yearly (180 days)",
        "Yearly (365 days)",
        "Custom"
    )

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
        setupOpeningHoursSection()

        if (editingGymId.isNotEmpty()) {
            findViewById<TextView>(R.id.tvRegisterTitle).text = "Update Gym Details"
            btnSubmit.text = "Save & Resubmit Gym"
            loadExistingGymData(editingGymId)
        } else {
            // Default initial plans for new gym registration
            addPlanView(
                GymPlan(
                    id = "plan_day_pass",
                    name = "Single Day Pass",
                    type = "day_pass",
                    durationDays = 1,
                    price = 150.0,
                    mrp = 200.0,
                    inclusions = listOf("Cardio", "Weights"),
                    isActive = true,
                    maxFreezeDays = 0
                )
            )
            addPlanView(
                GymPlan(
                    id = "plan_monthly_pro",
                    name = "Monthly Elite",
                    type = "monthly",
                    durationDays = 30,
                    price = 2000.0,
                    mrp = 2500.0,
                    inclusions = listOf("All Equipment", "Lockers", "Shower"),
                    isActive = true,
                    maxFreezeDays = 7
                )
            )
        }

        if (!rejectionReason.isNullOrEmpty()) {
            val tvRejection = findViewById<TextView>(R.id.tvRejectionBanner)
            tvRejection.visibility = View.VISIBLE
            tvRejection.text = "⚠️ Previous application needs update: $rejectionReason"
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
        tvCoordsDisplay = findViewById(R.id.tvCoordinatesDisplay)
        tvPhotoStatus = findViewById(R.id.tvPhotoStatus)
        ivPhotoPreview = findViewById(R.id.ivGymPhotoPreview)
        btnSubmit = findViewById(R.id.btnSubmitRegistration)
        chipGroupAmenities = findViewById(R.id.chipGroupRegisterAmenities)

        layoutDaysScheduleContainer = findViewById(R.id.layoutDaysScheduleContainer)
        layoutHolidaysContainer = findViewById(R.id.layoutHolidaysContainer)
        layoutPlansContainer = findViewById(R.id.layoutPlansContainer)
        btnCopyMondayAll = findViewById(R.id.btnCopyMondayAll)
        btnAddHoliday = findViewById(R.id.btnAddHoliday)
        btnAddPlan = findViewById(R.id.btnAddPlan)

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

        btnCopyMondayAll.setOnClickListener {
            val monday = dayStates.firstOrNull { it.dayName.equals("Monday", ignoreCase = true) }
            if (monday != null) {
                copyDayScheduleToAll(monday)
            }
        }

        btnAddHoliday.setOnClickListener {
            showAddHolidayDialog()
        }

        btnAddPlan.setOnClickListener {
            addPlanView()
        }

        btnSubmit.setOnClickListener {
            validateAndSubmit()
        }
    }

    private fun updateCoordsText() {
        tvCoordsDisplay.text = String.format(Locale.US, "Selected Coordinates: Lat %.4f, Lng %.4f", selectedLat, selectedLng)
    }

    private fun setupOpeningHoursSection() {
        layoutDaysScheduleContainer.removeAllViews()
        dayStates.clear()

        val inflater = LayoutInflater.from(this)

        weekdays.forEach { dayName ->
            val dayCard = inflater.inflate(R.layout.item_register_day_schedule, layoutDaysScheduleContainer, false)
            val tvDay = dayCard.findViewById<TextView>(R.id.tvDayName)
            val switchClosed = dayCard.findViewById<SwitchMaterial>(R.id.switchDayClosed)
            val layoutShifts = dayCard.findViewById<LinearLayout>(R.id.layoutShiftsContainer)
            val btnAddShift = dayCard.findViewById<MaterialButton>(R.id.btnAddShift)
            val btnCopy = dayCard.findViewById<MaterialButton>(R.id.btnCopyAllDays)

            tvDay.text = dayName

            val state = DayScheduleState(dayName, switchClosed, layoutShifts, btnAddShift)
            dayStates.add(state)

            // Sunday defaults to closed, other days default to open with 06:00-11:00 and 16:00-22:00
            val isSunday = dayName.equals("Sunday", ignoreCase = true)
            switchClosed.isChecked = isSunday

            if (isSunday) {
                layoutShifts.visibility = View.GONE
                btnAddShift.visibility = View.GONE
            } else {
                layoutShifts.visibility = View.VISIBLE
                btnAddShift.visibility = View.VISIBLE
                addShiftRow(state, "06:00", "11:00")
                addShiftRow(state, "16:00", "22:00")
            }

            switchClosed.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    layoutShifts.visibility = View.GONE
                    btnAddShift.visibility = View.GONE
                } else {
                    layoutShifts.visibility = View.VISIBLE
                    btnAddShift.visibility = View.VISIBLE
                    if (state.shifts.isEmpty()) {
                        addShiftRow(state, "06:00", "22:00")
                    }
                }
            }

            btnAddShift.setOnClickListener {
                val nextOpen = if (state.shifts.isEmpty()) "06:00" else "17:00"
                val nextClose = if (state.shifts.isEmpty()) "12:00" else "22:00"
                addShiftRow(state, nextOpen, nextClose)
            }

            btnCopy.setOnClickListener {
                copyDayScheduleToAll(state)
            }

            layoutDaysScheduleContainer.addView(dayCard)
        }
    }

    private fun addShiftRow(state: DayScheduleState, initialOpen: String, initialClose: String) {
        val shift = GymShift(initialOpen, initialClose)
        state.shifts.add(shift)

        val shiftIndex = state.shifts.size - 1
        val shiftView = LayoutInflater.from(this).inflate(R.layout.item_register_shift, state.layoutShifts, false)

        val btnOpen = shiftView.findViewById<MaterialButton>(R.id.btnShiftOpen)
        val btnClose = shiftView.findViewById<MaterialButton>(R.id.btnShiftClose)
        val btnDelete = shiftView.findViewById<ImageButton>(R.id.btnDeleteShift)

        btnOpen.text = initialOpen
        btnClose.text = initialClose

        btnOpen.setOnClickListener {
            showTimePicker(btnOpen.text.toString()) { selected ->
                btnOpen.text = selected
                val idx = state.layoutShifts.indexOfChild(shiftView)
                if (idx in state.shifts.indices) {
                    state.shifts[idx] = state.shifts[idx].copy(open = selected)
                }
            }
        }

        btnClose.setOnClickListener {
            showTimePicker(btnClose.text.toString()) { selected ->
                btnClose.text = selected
                val idx = state.layoutShifts.indexOfChild(shiftView)
                if (idx in state.shifts.indices) {
                    state.shifts[idx] = state.shifts[idx].copy(close = selected)
                }
            }
        }

        btnDelete.setOnClickListener {
            val idx = state.layoutShifts.indexOfChild(shiftView)
            if (idx in state.shifts.indices) {
                state.shifts.removeAt(idx)
            }
            state.layoutShifts.removeView(shiftView)
        }

        state.layoutShifts.addView(shiftView)
    }

    private fun showTimePicker(currentTime: String, onTimeSelected: (String) -> Unit) {
        val parts = currentTime.split(":")
        val defaultHour = parts.getOrNull(0)?.toIntOrNull() ?: 6
        val defaultMin = parts.getOrNull(1)?.toIntOrNull() ?: 0

        TimePickerDialog(this, { _, hour, minute ->
            val formatted = String.format(Locale.US, "%02d:%02d", hour, minute)
            onTimeSelected(formatted)
        }, defaultHour, defaultMin, true).show()
    }

    private fun copyDayScheduleToAll(source: DayScheduleState) {
        val isClosed = source.switchClosed.isChecked
        val sourceShifts = source.shifts.map { GymShift(it.open, it.close) }

        dayStates.forEach { target ->
            if (target !== source) {
                target.switchClosed.isChecked = isClosed
                target.layoutShifts.removeAllViews()
                target.shifts.clear()

                if (!isClosed) {
                    target.layoutShifts.visibility = View.VISIBLE
                    target.btnAddShift.visibility = View.VISIBLE
                    sourceShifts.forEach { s ->
                        addShiftRow(target, s.open, s.close)
                    }
                } else {
                    target.layoutShifts.visibility = View.GONE
                    target.btnAddShift.visibility = View.GONE
                }
            }
        }
        Toast.makeText(this, "Schedule copied from ${source.dayName} to all days", Toast.LENGTH_SHORT).show()
    }

    private fun showAddHolidayDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_holiday, null, false)
        val btnPickDate = dialogView.findViewById<MaterialButton>(R.id.btnHolidayPickDate)
        val etReason = dialogView.findViewById<EditText>(R.id.etHolidayReason)

        val cal = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        btnPickDate.text = sdf.format(cal.time)

        btnPickDate.setOnClickListener {
            DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    val chosen = Calendar.getInstance()
                    chosen.set(year, month, dayOfMonth)
                    btnPickDate.text = sdf.format(chosen.time)
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        AlertDialog.Builder(this)
            .setTitle("Add Holiday / Closure")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val dateStr = btnPickDate.text.toString()
                val reasonStr = etReason.text.toString().trim().ifEmpty { "Holiday" }
                val item = GymHoliday(dateStr, reasonStr)
                holidays.add(item)
                renderHolidayItem(item)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun renderHolidayItem(item: GymHoliday) {
        val view = LayoutInflater.from(this).inflate(R.layout.item_register_holiday, layoutHolidaysContainer, false)
        val tvDate = view.findViewById<TextView>(R.id.tvHolidayDate)
        val tvReason = view.findViewById<TextView>(R.id.tvHolidayReason)
        val btnDelete = view.findViewById<ImageButton>(R.id.btnDeleteHoliday)

        tvDate.text = item.date
        tvReason.text = item.reason

        btnDelete.setOnClickListener {
            holidays.remove(item)
            layoutHolidaysContainer.removeView(view)
        }

        layoutHolidaysContainer.addView(view)
    }

    private fun addPlanView(existing: GymPlan? = null) {
        val view = LayoutInflater.from(this).inflate(R.layout.item_register_plan, layoutPlansContainer, false)

        val tvHeader = view.findViewById<TextView>(R.id.tvPlanHeaderTitle)
        val switchActive = view.findViewById<SwitchMaterial>(R.id.switchPlanActive)
        val btnDelete = view.findViewById<ImageButton>(R.id.btnDeletePlan)
        val etName = view.findViewById<EditText>(R.id.etPlanName)
        val spType = view.findViewById<Spinner>(R.id.spPlanType)
        val etDuration = view.findViewById<EditText>(R.id.etPlanDuration)
        val etPrice = view.findViewById<EditText>(R.id.etPlanPrice)
        val etMrp = view.findViewById<EditText>(R.id.etPlanMrp)
        val etInclusions = view.findViewById<EditText>(R.id.etPlanInclusions)
        val etFreezeDays = view.findViewById<EditText>(R.id.etPlanFreezeDays)

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, planTypeLabels)
        spType.adapter = adapter

        if (existing != null) {
            etName.setText(existing.name)
            val typeIdx = planTypeKeys.indexOf(existing.type.lowercase()).coerceAtLeast(0)
            spType.setSelection(typeIdx)
            etDuration.setText(existing.durationDays.toString())
            etPrice.setText(existing.price.toInt().toString())
            if (existing.mrp != null && existing.mrp > 0) {
                etMrp.setText(existing.mrp.toInt().toString())
            }
            etInclusions.setText(existing.inclusions.joinToString(", "))
            etFreezeDays.setText(existing.maxFreezeDays.toString())
            switchActive.isChecked = existing.isActive
            tvHeader.text = existing.name.ifEmpty { "Membership Plan" }
        } else {
            spType.setSelection(2) // Default Monthly
            etDuration.setText("30")
            etFreezeDays.setText("0")
        }

        spType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, v: View?, position: Int, id: Long) {
                val defaultDuration = when (position) {
                    0 -> 1
                    1 -> 7
                    2 -> 30
                    3 -> 90
                    4 -> 180
                    5 -> 365
                    else -> null
                }
                if (defaultDuration != null && (etDuration.text.isEmpty() || existing == null)) {
                    etDuration.setText(defaultDuration.toString())
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        btnDelete.setOnClickListener {
            planViews.remove(view)
            layoutPlansContainer.removeView(view)
        }

        planViews.add(view)
        layoutPlansContainer.addView(view)
    }

    private fun collectPlans(): List<GymPlan> {
        val result = mutableListOf<GymPlan>()
        planViews.forEachIndexed { index, view ->
            val etName = view.findViewById<EditText>(R.id.etPlanName)
            val spType = view.findViewById<Spinner>(R.id.spPlanType)
            val etDuration = view.findViewById<EditText>(R.id.etPlanDuration)
            val etPrice = view.findViewById<EditText>(R.id.etPlanPrice)
            val etMrp = view.findViewById<EditText>(R.id.etPlanMrp)
            val etInclusions = view.findViewById<EditText>(R.id.etPlanInclusions)
            val etFreezeDays = view.findViewById<EditText>(R.id.etPlanFreezeDays)
            val switchActive = view.findViewById<SwitchMaterial>(R.id.switchPlanActive)

            val name = etName.text.toString().trim()
            val typeKey = planTypeKeys.getOrElse(spType.selectedItemPosition) { "monthly" }
            val duration = etDuration.text.toString().toIntOrNull() ?: 30
            val price = etPrice.text.toString().toDoubleOrNull() ?: 0.0
            val mrp = etMrp.text.toString().toDoubleOrNull()
            val freezeDays = etFreezeDays.text.toString().toIntOrNull() ?: 0
            val inclusionsStr = etInclusions.text.toString().trim()
            val inclusions = if (inclusionsStr.isNotEmpty()) {
                inclusionsStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            } else emptyList()

            if (name.isNotEmpty()) {
                val plan = GymPlan(
                    id = "plan_${typeKey}_${index + 1}",
                    name = name,
                    type = typeKey,
                    durationDays = duration,
                    price = price,
                    mrp = mrp,
                    inclusions = inclusions,
                    isActive = switchActive.isChecked,
                    maxFreezeDays = freezeDays
                )
                result.add(plan)
            }
        }
        return result
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

                    if (gym.location != null) {
                        selectedLat = gym.location.lat
                        selectedLng = gym.location.lng
                        updateCoordsText()
                    }

                    if (gym.photos.isNotEmpty()) {
                        uploadedPhotoUrl = gym.photos.first()
                        tvPhotoStatus.text = "Photo loaded"
                    }

                    // Populate opening hours
                    if (gym.openingHours.isNotEmpty()) {
                        dayStates.forEach { state ->
                            val match = gym.openingHours.find { it.day.equals(state.dayName, ignoreCase = true) }
                            if (match != null) {
                                state.switchClosed.isChecked = match.isClosed
                                state.layoutShifts.removeAllViews()
                                state.shifts.clear()
                                if (!match.isClosed) {
                                    state.layoutShifts.visibility = View.VISIBLE
                                    state.btnAddShift.visibility = View.VISIBLE
                                    val shiftsToLoad = match.displayShifts
                                    if (shiftsToLoad.isNotEmpty()) {
                                        shiftsToLoad.forEach { s ->
                                            addShiftRow(state, s.open, s.close)
                                        }
                                    } else {
                                        addShiftRow(state, "06:00", "22:00")
                                    }
                                } else {
                                    state.layoutShifts.visibility = View.GONE
                                    state.btnAddShift.visibility = View.GONE
                                }
                            }
                        }
                    }

                    // Populate holidays
                    holidays.clear()
                    layoutHolidaysContainer.removeAllViews()
                    gym.holidays.forEach { h ->
                        holidays.add(h)
                        renderHolidayItem(h)
                    }

                    // Populate plans
                    if (gym.plans.isNotEmpty()) {
                        layoutPlansContainer.removeAllViews()
                        planViews.clear()
                        gym.plans.forEach { p ->
                            addPlanView(p)
                        }
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

        // Amenities
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

        // Opening Hours Validation: at least one open day with hours
        val openingHours = dayStates.map { state ->
            val isClosed = state.switchClosed.isChecked
            val validShifts = if (isClosed) emptyList() else state.shifts.toList()
            GymOpeningHour(
                day = state.dayName,
                open = validShifts.firstOrNull()?.open ?: "06:00",
                close = validShifts.lastOrNull()?.close ?: "22:00",
                isClosed = isClosed,
                shifts = validShifts
            )
        }

        val hasAtLeastOneOpenDay = openingHours.any { !it.isClosed && it.shifts.isNotEmpty() }
        if (!hasAtLeastOneOpenDay) {
            Toast.makeText(this, "Please set at least one open day with valid shifts", Toast.LENGTH_LONG).show()
            return
        }

        // Plans Validation: at least one active plan
        val plans = collectPlans()
        val hasActivePlan = plans.any { it.isActive && it.price > 0 && it.name.isNotEmpty() }
        if (!hasActivePlan) {
            Toast.makeText(this, "Please configure at least one active membership plan with a price", Toast.LENGTH_LONG).show()
            return
        }

        // Compatibility sessionTypes
        val dayPassPlan = plans.find { it.type.equals("day_pass", ignoreCase = true) } ?: plans.first()
        val sessionTypes = listOf(
            GymSessionType(
                type = "dayPass",
                name = dayPassPlan.name,
                price = dayPassPlan.price,
                description = "${dayPassPlan.durationDays}-day gym pass"
            )
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
            plans = plans,
            holidays = holidays.toList(),
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
