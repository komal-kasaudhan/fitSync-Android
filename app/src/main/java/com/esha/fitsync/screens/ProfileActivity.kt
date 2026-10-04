package com.esha.fitsync.screens

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.esha.fitsync.BuildConfig
import com.esha.fitsync.R
import com.esha.fitsync.api.RetrofitClient
import com.esha.fitsync.data.AppCacheManager
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.snackbar.Snackbar
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class ProfileActivity : AppCompatActivity() {

    private lateinit var mainProfileLayout: View
    private lateinit var btnProfileBack: ImageView
    private lateinit var llProfileError: LinearLayout
    private lateinit var tvProfileErrorMessage: TextView
    private lateinit var btnRetryProfile: MaterialButton
    private lateinit var shimmerProfile: ShimmerFrameLayout
    private lateinit var nsvProfileContent: View

    private lateinit var ivProfilePhoto: ShapeableImageView
    private lateinit var pbUploadPhoto: ProgressBar
    private lateinit var btnChangePhoto: MaterialButton

    private lateinit var etProfileName: EditText
    private lateinit var etProfileEmail: EditText
    private lateinit var tvPhonePrompt: TextView
    private lateinit var cardProfilePhone: MaterialCardView
    private lateinit var etProfilePhone: EditText
    private lateinit var tvPhoneError: TextView

    private lateinit var btnSaveProfile: MaterialButton
    private lateinit var btnLogoutProfile: MaterialButton

    private var currentAvatarUrl: String? = null
    private var lastLoadedName: String = ""

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { handleSelectedPhoto(it) }
    }

    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: Bitmap? ->
        bitmap?.let { handleCapturedBitmap(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        initViews()
        setupListeners()
        loadCachedData()
        fetchProfileFromApi()
    }

    private fun initViews() {
        mainProfileLayout = findViewById(R.id.mainProfileLayout)
        btnProfileBack = findViewById(R.id.btnProfileBack)
        llProfileError = findViewById(R.id.llProfileError)
        tvProfileErrorMessage = findViewById(R.id.tvProfileErrorMessage)
        btnRetryProfile = findViewById(R.id.btnRetryProfile)
        shimmerProfile = findViewById(R.id.shimmerProfile)
        nsvProfileContent = findViewById(R.id.nsvProfileContent)

        ivProfilePhoto = findViewById(R.id.ivProfilePhoto)
        pbUploadPhoto = findViewById(R.id.pbUploadPhoto)
        btnChangePhoto = findViewById(R.id.btnChangePhoto)

        etProfileName = findViewById(R.id.etProfileName)
        etProfileEmail = findViewById(R.id.etProfileEmail)
        tvPhonePrompt = findViewById(R.id.tvPhonePrompt)
        cardProfilePhone = findViewById(R.id.cardProfilePhone)
        etProfilePhone = findViewById(R.id.etProfilePhone)
        tvPhoneError = findViewById(R.id.tvPhoneError)

        btnSaveProfile = findViewById(R.id.btnSaveProfile)
        btnLogoutProfile = findViewById(R.id.btnLogoutProfile)
    }

    private fun setupListeners() {
        btnProfileBack.setOnClickListener {
            finish()
        }

        btnRetryProfile.setOnClickListener {
            llProfileError.visibility = View.GONE
            fetchProfileFromApi()
        }

        btnChangePhoto.setOnClickListener {
            showPhotoSourceDialog()
        }

        etProfilePhone.doAfterTextChanged { text ->
            val value = text?.toString()?.trim() ?: ""
            if (value.isEmpty()) {
                tvPhonePrompt.visibility = View.VISIBLE
            } else {
                tvPhonePrompt.visibility = View.GONE
            }
            tvPhoneError.visibility = View.GONE
            cardProfilePhone.strokeColor = Color.parseColor("#1F2E3D")
        }

        btnSaveProfile.setOnClickListener {
            saveProfile()
        }

        btnLogoutProfile.setOnClickListener {
            showLogoutConfirmationDialog()
        }
    }

    private fun loadCachedData() {
        val prefs = getSharedPreferences("FitSync", Context.MODE_PRIVATE)
        val name = prefs.getString("USER_NAME", "") ?: ""
        var email = prefs.getString("USER_EMAIL", "") ?: ""
        val phone = prefs.getString("USER_PHONE", "") ?: ""
        val avatar = prefs.getString("USER_AVATAR", null)

        // If email was not explicitly saved in prefs, attempt extraction from JWT token payload
        if (email.isBlank()) {
            val token = prefs.getString("TOKEN", null)
            val extracted = extractEmailFromToken(token)
            if (!extracted.isNullOrBlank()) {
                email = extracted
                prefs.edit().putString("USER_EMAIL", email).apply()
            }
        }

        lastLoadedName = name
        currentAvatarUrl = avatar

        if (name.isNotBlank()) etProfileName.setText(name)
        if (email.isNotBlank()) etProfileEmail.setText(email)

        val clean10 = sanitize10DigitPhone(phone)
        if (clean10.isNotEmpty()) {
            etProfilePhone.setText(clean10)
            tvPhonePrompt.visibility = View.GONE
        } else {
            tvPhonePrompt.visibility = View.VISIBLE
        }

        displayAvatar(avatar, name)
    }

    private fun fetchProfileFromApi() {
        shimmerProfile.visibility = View.VISIBLE
        shimmerProfile.startShimmer()

        lifecycleScope.launch {
            try {
                // Try PUT/GET api/user/profile first, fallback to api/auth/profile
                val response = try {
                    val resUser = RetrofitClient.api.getUserProfileAlt()
                    if (resUser.isSuccessful && resUser.body() != null) {
                        resUser
                    } else {
                        RetrofitClient.api.getUserProfile()
                    }
                } catch (_: Exception) {
                    RetrofitClient.api.getUserProfile()
                }

                if (response.isSuccessful && response.body() != null) {
                    val root = response.body()!!
                    val userObj = if (root.has("user") && root.get("user").isJsonObject) {
                        root.getAsJsonObject("user")
                    } else {
                        root
                    }

                    bindUserProfile(userObj)
                    llProfileError.visibility = View.GONE
                } else {
                    val code = response.code()
                    showErrorMessage("Server returned $code. Showing cached profile.")
                }
            } catch (e: Exception) {
                showErrorMessage("Cannot connect to server. Showing cached profile.")
            } finally {
                shimmerProfile.stopShimmer()
                shimmerProfile.visibility = View.GONE
            }
        }
    }

    private fun bindUserProfile(user: JsonObject) {
        val name = user.get("name")?.asString?.ifBlank { null }
            ?: user.get("fullName")?.asString?.ifBlank { null }
            ?: etProfileName.text.toString()

        val email = user.get("email")?.asString?.ifBlank { null }
            ?: etProfileEmail.text.toString()

        val phone = user.get("phone")?.asString?.ifBlank { null }
            ?: user.get("phoneNumber")?.asString?.ifBlank { null }
            ?: etProfilePhone.text.toString()

        val avatar = user.get("avatar")?.asString?.ifBlank { null }
            ?: user.get("photo")?.asString?.ifBlank { null }
            ?: user.get("profilePicture")?.asString?.ifBlank { null }
            ?: currentAvatarUrl

        lastLoadedName = name
        currentAvatarUrl = avatar

        etProfileName.setText(name)
        etProfileEmail.setText(email)

        val clean10 = sanitize10DigitPhone(phone)
        if (clean10.isNotEmpty()) {
            etProfilePhone.setText(clean10)
            tvPhonePrompt.visibility = View.GONE
        } else {
            tvPhonePrompt.visibility = View.VISIBLE
        }

        // Cache loaded data
        val prefs = getSharedPreferences("FitSync", Context.MODE_PRIVATE)
        prefs.edit().apply {
            if (name.isNotBlank()) putString("USER_NAME", name)
            if (email.isNotBlank()) putString("USER_EMAIL", email)
            if (clean10.isNotBlank()) putString("USER_PHONE", "+91$clean10")
            if (!avatar.isNullOrBlank()) putString("USER_AVATAR", avatar)
            apply()
        }

        displayAvatar(avatar, name)
    }

    private fun sanitize10DigitPhone(rawPhone: String): String {
        val digits = rawPhone.replace("[^0-9]".toRegex(), "")
        return if (digits.length > 10 && digits.startsWith("91")) {
            digits.substring(2)
        } else if (digits.length >= 10) {
            digits.takeLast(10)
        } else {
            digits
        }
    }

    private fun extractEmailFromToken(token: String?): String? {
        if (token.isNullOrBlank()) return null
        return try {
            val parts = token.split(".")
            if (parts.size >= 2) {
                val decodedBytes = Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
                val payloadJson = String(decodedBytes, Charsets.UTF_8)
                val obj = JsonParser.parseString(payloadJson).asJsonObject
                obj.get("email")?.asString
            } else null
        } catch (_: Exception) {
            null
        }
    }

    private fun displayAvatar(avatarUrl: String?, name: String) {
        val initialsBitmap = createInitialsBitmap(name.ifBlank { "User" })
        val initialsDrawable = android.graphics.drawable.BitmapDrawable(resources, initialsBitmap)

        if (!avatarUrl.isNullOrBlank()) {
            val fullUrl = if (avatarUrl.startsWith("http")) {
                avatarUrl
            } else {
                "${BuildConfig.BASE_URL.trimEnd('/')}/${avatarUrl.trimStart('/')}"
            }

            Glide.with(this)
                .load(fullUrl)
                .transform(CircleCrop())
                .placeholder(initialsDrawable)
                .error(initialsDrawable)
                .into(ivProfilePhoto)
        } else {
            ivProfilePhoto.setImageDrawable(initialsDrawable)
        }
    }

    private fun showPhotoSourceDialog() {
        val options = arrayOf("Choose from Gallery", "Take Photo")
        MaterialAlertDialogBuilder(this)
            .setTitle("Change photo")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> pickImageLauncher.launch("image/*")
                    1 -> takePictureLauncher.launch(null)
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun handleSelectedPhoto(uri: Uri) {
        lifecycleScope.launch {
            try {
                pbUploadPhoto.visibility = View.VISIBLE
                val compressedBytes = compressImageUri(uri, maxDimension = 800)
                val localBitmap = BitmapFactory.decodeByteArray(compressedBytes, 0, compressedBytes.size)
                ivProfilePhoto.setImageBitmap(localBitmap)

                // Save locally to cache dir
                val localFile = File(cacheDir, "profile_avatar.jpg")
                FileOutputStream(localFile).use { it.write(compressedBytes) }
                currentAvatarUrl = localFile.absolutePath

                val prefs = getSharedPreferences("FitSync", Context.MODE_PRIVATE)
                prefs.edit().putString("USER_AVATAR", currentAvatarUrl).apply()

                // Attempt API upload
                uploadPhotoBytes(compressedBytes)
            } catch (e: Exception) {
                Toast.makeText(this@ProfileActivity, "Failed to load photo: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                pbUploadPhoto.visibility = View.GONE
            }
        }
    }

    private fun handleCapturedBitmap(bitmap: Bitmap) {
        lifecycleScope.launch {
            try {
                pbUploadPhoto.visibility = View.VISIBLE
                val compressedBytes = compressBitmap(bitmap, maxDimension = 800)
                val localBitmap = BitmapFactory.decodeByteArray(compressedBytes, 0, compressedBytes.size)
                ivProfilePhoto.setImageBitmap(localBitmap)

                val localFile = File(cacheDir, "profile_avatar.jpg")
                FileOutputStream(localFile).use { it.write(compressedBytes) }
                currentAvatarUrl = localFile.absolutePath

                val prefs = getSharedPreferences("FitSync", Context.MODE_PRIVATE)
                prefs.edit().putString("USER_AVATAR", currentAvatarUrl).apply()

                uploadPhotoBytes(compressedBytes)
            } catch (e: Exception) {
                Toast.makeText(this@ProfileActivity, "Failed to process photo: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                pbUploadPhoto.visibility = View.GONE
            }
        }
    }

    private suspend fun uploadPhotoBytes(bytes: ByteArray) {
        try {
            val reqBody = bytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
            val part = MultipartBody.Part.createFormData("photo", "profile_${System.currentTimeMillis()}.jpg", reqBody)

            var uploadedUrl: String? = null
            try {
                val resUser = RetrofitClient.api.uploadProfilePhotoUserRoute(part)
                if (resUser.isSuccessful && resUser.body() != null) {
                    uploadedUrl = resUser.body()?.get("url")?.asString
                        ?: resUser.body()?.get("avatar")?.asString
                        ?: resUser.body()?.get("photo")?.asString
                }
            } catch (_: Exception) {}

            if (uploadedUrl == null) {
                try {
                    val resAuth = RetrofitClient.api.uploadProfilePhoto(part)
                    if (resAuth.isSuccessful && resAuth.body() != null) {
                        uploadedUrl = resAuth.body()?.get("url")?.asString
                            ?: resAuth.body()?.get("avatar")?.asString
                            ?: resAuth.body()?.get("photo")?.asString
                    }
                } catch (_: Exception) {}
            }

            if (uploadedUrl == null) {
                try {
                    val resAvatar = RetrofitClient.api.uploadUserAvatar(part)
                    if (resAvatar.isSuccessful && resAvatar.body() != null) {
                        uploadedUrl = resAvatar.body()?.get("url")?.asString
                            ?: resAvatar.body()?.get("avatar")?.asString
                            ?: resAvatar.body()?.get("photo")?.asString
                    }
                } catch (_: Exception) {}
            }

            if (uploadedUrl == null) {
                try {
                    val gymRes = RetrofitClient.api.uploadGymPhoto(part)
                    if (gymRes.isSuccessful && gymRes.body() != null) {
                        uploadedUrl = gymRes.body()?.url
                    }
                } catch (_: Exception) {}
            }

            if (!uploadedUrl.isNullOrBlank()) {
                currentAvatarUrl = uploadedUrl
                getSharedPreferences("FitSync", Context.MODE_PRIVATE).edit()
                    .putString("USER_AVATAR", uploadedUrl).apply()
                Snackbar.make(mainProfileLayout, "Photo uploaded successfully", Snackbar.LENGTH_SHORT).show()
            }
        } catch (_: Exception) {
            // Keep local cached photo
        }
    }

    private fun compressImageUri(uri: Uri, maxDimension: Int): ByteArray {
        val inputStream = contentResolver.openInputStream(uri)
        val original = BitmapFactory.decodeStream(inputStream)
        inputStream?.close() ?: return ByteArray(0)
        return compressBitmap(original, maxDimension)
    }

    private fun compressBitmap(original: Bitmap, maxDimension: Int): ByteArray {
        val width = original.width
        val height = original.height
        val scale = if (width > height) {
            if (width > maxDimension) maxDimension.toFloat() / width else 1f
        } else {
            if (height > maxDimension) maxDimension.toFloat() / height else 1f
        }

        val scaled = if (scale < 1f) {
            Bitmap.createScaledBitmap(original, (width * scale).toInt(), (height * scale).toInt(), true)
        } else {
            original
        }

        val stream = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 85, stream)
        return stream.toByteArray()
    }

    private fun createInitialsBitmap(name: String, sizeDp: Int = 100): Bitmap {
        val px = (sizeDp * Resources.getSystem().displayMetrics.density).toInt()
        val bitmap = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val paintBg = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#1F2E3D") // Workout/Nutrition border tone
            style = Paint.Style.FILL
        }
        canvas.drawCircle(px / 2f, px / 2f, px / 2f, paintBg)

        val initials = name.trim().split("\\s+".toRegex())
            .filter { it.isNotEmpty() }
            .map { it.first().uppercaseChar() }
            .take(2)
            .joinToString("")
            .ifEmpty { "FS" }

        val paintText = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#FFFFFF") // Crisp white text
            textSize = px * 0.38f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        val yPos = (canvas.height / 2f) - ((paintText.descent() + paintText.ascent()) / 2f)
        canvas.drawText(initials, canvas.width / 2f, yPos, paintText)
        return bitmap
    }

    private fun saveProfile() {
        val name = etProfileName.text.toString().trim()
        val phoneInput = etProfilePhone.text.toString().trim()

        if (name.isBlank()) {
            etProfileName.error = "Name cannot be empty"
            etProfileName.requestFocus()
            return
        }

        // Validate 10-digit Phone Number (country code +91 default)
        val digitsOnly = phoneInput.replace("[^0-9]".toRegex(), "")
        if (phoneInput.isNotEmpty() && digitsOnly.length != 10) {
            tvPhoneError.text = "Please enter a valid 10-digit phone number"
            tvPhoneError.visibility = View.VISIBLE
            cardProfilePhone.strokeColor = Color.parseColor("#EF4444")
            etProfilePhone.requestFocus()
            return
        }

        tvPhoneError.visibility = View.GONE
        cardProfilePhone.strokeColor = Color.parseColor("#1F2E3D")

        val formattedPhone = if (digitsOnly.length == 10) "+91$digitsOnly" else ""

        // 1. Immediately persist to SharedPreferences
        val prefs = getSharedPreferences("FitSync", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString("USER_NAME", name)
            putString("USER_PHONE", formattedPhone)
            if (!currentAvatarUrl.isNullOrBlank()) {
                putString("USER_AVATAR", currentAvatarUrl)
            }
            apply()
        }

        // Keep 10-digit number in the field and hide empty prompt if filled
        if (digitsOnly.length == 10) {
            etProfilePhone.setText(digitsOnly)
            tvPhonePrompt.visibility = View.GONE
        } else {
            tvPhonePrompt.visibility = View.VISIBLE
        }

        // 2. Notify cache and triggers
        AppCacheManager.notifyDataChanged()

        // 3. Asynchronously call update endpoint (PUT api/user/profile with fallback to api/auth/profile)
        lifecycleScope.launch {
            try {
                val body = mutableMapOf(
                    "name" to name,
                    "phone" to formattedPhone
                )
                currentAvatarUrl?.let { body["avatar"] = it }

                try {
                    val resUser = RetrofitClient.api.updateUserProfileAlt(body)
                    if (!resUser.isSuccessful) {
                        RetrofitClient.api.updateUserProfile(body)
                    }
                } catch (_: Exception) {
                    RetrofitClient.api.updateUserProfile(body)
                }
            } catch (_: Exception) {
                // Silent fallback: local state is preserved
            }
        }

        // 4. Show Snackbar and keep the value
        Snackbar.make(mainProfileLayout, "Profile saved successfully", Snackbar.LENGTH_SHORT).show()
        setResult(RESULT_OK)
    }

    private fun showLogoutConfirmationDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Log Out")
            .setMessage("Are you sure you want to log out of FitSync?")
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .setPositiveButton("Log Out") { dialog, _ ->
                dialog.dismiss()
                performLogout()
            }
            .show()
    }

    private fun performLogout() {
        // 1. Clear user token and session data
        val prefs = getSharedPreferences("FitSync", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()

        // 2. Clear app data cache and reset in-memory state
        AppCacheManager.clearAll()
        com.esha.fitsync.screens.fragments.WorkoutFragment.hasAutoOpenedBottomSheetThisSession = false

        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()

        // 3. Navigate to Login screen
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finishAffinity()
    }

    private fun showErrorMessage(message: String) {
        tvProfileErrorMessage.text = message
        llProfileError.visibility = View.VISIBLE
    }
}
