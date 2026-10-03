package com.esha.fitsync.models

import com.google.gson.annotations.SerializedName

data class TrainerPackage(
    @SerializedName("id") val id: String? = null,
    @SerializedName("_id") val mongoId: String? = null,
    @SerializedName("name") val name: String = "",
    @SerializedName("sessions") val sessions: Int = 1,
    @SerializedName("price") val price: Double = 0.0,
    @SerializedName("validityDays") val validityDays: Int = 30
)

data class TrainerPricing(
    @SerializedName("online") val online: Double = 0.0,
    @SerializedName("offline") val offline: Double = 0.0,
    @SerializedName("packages") val packages: List<TrainerPackage> = emptyList()
)

data class TrainerShift(
    @SerializedName("start") val start: String = "09:00",
    @SerializedName("end") val end: String = "17:00"
)

data class TrainerDayAvailability(
    @SerializedName("day") val day: String = "Monday",
    @SerializedName("isAvailable") val isAvailable: Boolean = true,
    @SerializedName("slotLengthMin") val slotLengthMin: Int = 60,
    @SerializedName("shifts") val shifts: List<TrainerShift> = emptyList()
)

data class TrainerProfile(
    @SerializedName("_id") val mongoId: String = "",
    @SerializedName("id") val publicId: String? = null,
    @SerializedName("userId") val userId: String = "",
    @SerializedName("user") val user: GymOwnerInfo? = null,
    @SerializedName("type") val type: String = "trainer", // "trainer" or "nutritionist"
    @SerializedName("bio") val bio: String = "",
    @SerializedName("specialties") val specialties: List<String> = emptyList(),
    @SerializedName("certifications") val certifications: List<String> = emptyList(),
    @SerializedName("experienceYears") val experienceYears: Int = 1,
    @SerializedName("languages") val languages: List<String> = listOf("English"),
    @SerializedName("mode") val mode: String = "online", // "online", "offline", "both"
    @SerializedName("city") val city: String = "",
    @SerializedName("location") val location: GeoLocation? = null,
    @SerializedName("pricing") val pricing: TrainerPricing? = null,
    @SerializedName("weeklyAvailability") val weeklyAvailability: List<TrainerDayAvailability> = emptyList(),
    @SerializedName("currency") val currency: String = "INR",
    @SerializedName("timezone") val timezone: String = "Asia/Kolkata",
    @SerializedName("status") val status: String = "pending", // pending, approved, rejected, suspended
    @SerializedName("rejectionReason") val rejectionReason: String? = null,
    @SerializedName("ratingAvg") val ratingAvg: Double = 0.0,
    @SerializedName("ratingCount") val ratingCount: Int = 0,
    @SerializedName("disclaimer") val disclaimer: String? = null,
    @SerializedName("name") val profileName: String? = null
) {
    val id: String get() = mongoId.ifEmpty { publicId ?: "" }
    val displayName: String get() = profileName ?: user?.name ?: "FitSync Expert"
    val isNutritionist: Boolean get() = type.equals("nutritionist", ignoreCase = true)
    val mandatoryDisclaimer: String
        get() = disclaimer?.ifEmpty { "Not a medical diagnosis" } ?: "Not a medical diagnosis"
}

data class TrainerListResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("count") val count: Int = 0,
    @SerializedName("trainers") val trainers: List<TrainerProfile> = emptyList(),
    @SerializedName("disclaimer") val disclaimer: String? = null,
    @SerializedName("message") val message: String? = null
)

data class SingleTrainerResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("profile") val profile: TrainerProfile? = null,
    @SerializedName("trainer") val trainer: TrainerProfile? = null,
    @SerializedName("disclaimer") val disclaimer: String? = null,
    @SerializedName("message") val message: String? = null
) {
    val activeProfile: TrainerProfile? get() = profile ?: trainer
}

data class TrainerSlot(
    @SerializedName("slot") val slot: String = "",
    @SerializedName("start") val start: String = "",
    @SerializedName("end") val end: String = "",
    @SerializedName("isAvailable") val isAvailable: Boolean = true
)

data class TrainerSlotsResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("date") val date: String = "",
    @SerializedName("slots") val slots: List<TrainerSlot> = emptyList(),
    @SerializedName("message") val message: String? = null
)

data class TrainerBooking(
    @SerializedName("_id") val mongoId: String = "",
    @SerializedName("id") val publicId: String? = null,
    @SerializedName("userId") val userId: String = "",
    @SerializedName("trainerProfileId") val trainerProfileId: String = "",
    @SerializedName("trainerUserId") val trainerUserId: String = "",
    @SerializedName("trainer") val trainer: TrainerProfile? = null,
    @SerializedName("trainerName") val trainerName: String? = null,
    @SerializedName("clientName") val clientName: String? = null,
    @SerializedName("date") val date: String = "",
    @SerializedName("slot") val slot: String = "",
    @SerializedName("slotStart") val slotStart: String = "",
    @SerializedName("durationMin") val durationMin: Int = 60,
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("mode") val mode: String = "online",
    @SerializedName("price") val price: Double = 0.0,
    @SerializedName("meetingLink") val meetingLink: String? = null,
    @SerializedName("status") val status: String = "confirmed", // confirmed, completed, cancelled, rescheduled
    @SerializedName("checkInCode") val checkInCode: String? = null,
    @SerializedName("rescheduleCount") val rescheduleCount: Int = 0,
    @SerializedName("paymentStatus") val paymentStatus: String? = null,
    @SerializedName("createdAt") val createdAt: String? = null
) {
    val id: String get() = mongoId.ifEmpty { publicId ?: "" }
}

data class CreateTrainerBookingRequest(
    @SerializedName("trainerId") val trainerId: String,
    @SerializedName("mode") val mode: String = "online",
    @SerializedName("slotStart") val slotStart: String,
    @SerializedName("durationMin") val durationMin: Int = 60,
    @SerializedName("notes") val notes: String = "",
    @SerializedName("packageId") val packageId: String? = null
)

data class CreateTrainerBookingResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("message") val message: String = "",
    @SerializedName("booking") val booking: TrainerBooking? = null,
    @SerializedName("paymentsEnabled") val paymentsEnabled: Boolean = false
)

data class TrainerBookingsListResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("bookings") val bookings: List<TrainerBooking> = emptyList(),
    @SerializedName("message") val message: String? = null
)

data class RescheduleTrainerBookingRequest(
    @SerializedName("slotStart") val slotStart: String,
    @SerializedName("slot") val slot: String? = null
)

data class CompleteTrainerBookingRequest(
    @SerializedName("notes") val notes: String? = null
)

data class TrainerBookingActionResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("message") val message: String = "",
    @SerializedName("booking") val booking: TrainerBooking? = null
)

data class RegisterTrainerRequest(
    @SerializedName("type") val type: String,
    @SerializedName("bio") val bio: String,
    @SerializedName("specialties") val specialties: List<String>,
    @SerializedName("certifications") val certifications: List<String>,
    @SerializedName("experienceYears") val experienceYears: Int,
    @SerializedName("languages") val languages: List<String>,
    @SerializedName("mode") val mode: String,
    @SerializedName("city") val city: String,
    @SerializedName("location") val location: GeoLocation? = null,
    @SerializedName("pricing") val pricing: TrainerPricing,
    @SerializedName("weeklyAvailability") val weeklyAvailability: List<TrainerDayAvailability>
)

data class RegisterTrainerResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("message") val message: String = "",
    @SerializedName("profile") val profile: TrainerProfile? = null
)

data class TrainerReviewRequest(
    @SerializedName("rating") val rating: Int = 5,
    @SerializedName("comment") val comment: String = ""
)

