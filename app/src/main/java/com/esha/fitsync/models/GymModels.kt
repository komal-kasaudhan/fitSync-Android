package com.esha.fitsync.models

import com.google.gson.annotations.SerializedName

data class GeoLocation(
    @SerializedName("type") val type: String = "Point",
    @SerializedName("coordinates") val coordinates: List<Double> = emptyList()
) {
    val lng: Double get() = coordinates.getOrNull(0) ?: 0.0
    val lat: Double get() = coordinates.getOrNull(1) ?: 0.0
}

data class GymShift(
    @SerializedName("open") val open: String = "06:00",
    @SerializedName("close") val close: String = "22:00"
)

data class GymOpeningHour(
    @SerializedName("day") val day: String = "",
    @SerializedName("open") val open: String = "06:00",
    @SerializedName("close") val close: String = "22:00",
    @SerializedName("isClosed") val isClosed: Boolean = false,
    @SerializedName("shifts") val shifts: List<GymShift> = emptyList()
) {
    val displayShifts: List<GymShift>
        get() = if (shifts.isNotEmpty()) shifts else if (!isClosed) listOf(GymShift(open, close)) else emptyList()
}

data class WomenOnlyHours(
    @SerializedName("enabled") val enabled: Boolean = false,
    @SerializedName("shifts") val shifts: List<GymShift> = emptyList()
)

data class GymPlan(
    @SerializedName("id") val id: String = "",
    @SerializedName("_id") val mongoId: String? = null,
    @SerializedName("name") val name: String = "",
    @SerializedName("type") val type: String = "monthly", // day_pass, weekly, monthly, quarterly, half_yearly, yearly, custom
    @SerializedName("durationDays") val durationDays: Int = 30,
    @SerializedName("price") val price: Double = 0.0,
    @SerializedName("mrp") val mrp: Double? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("inclusions") val inclusions: List<String> = emptyList(),
    @SerializedName("isActive") val isActive: Boolean = true,
    @SerializedName("maxFreezeDays") val maxFreezeDays: Int = 0
) {
    val planId: String get() = id.ifEmpty { mongoId ?: "" }
    val displayType: String get() = type.replace("_", " ").replaceFirstChar { it.uppercase() }
}

data class GymSessionType(
    @SerializedName("type") val type: String = "dayPass",
    @SerializedName("name") val name: String = "Day Pass",
    @SerializedName("price") val price: Double = 0.0,
    @SerializedName("description") val description: String = ""
)

data class GymOwnerInfo(
    @SerializedName("id") val id: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("email") val email: String = ""
)

data class GymItem(
    @SerializedName("_id") val id: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("description") val description: String? = null,
    @SerializedName("address") val address: String = "",
    @SerializedName("city") val city: String = "",
    @SerializedName("pincode") val pincode: String? = null,
    @SerializedName("location") val location: GeoLocation? = null,
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("photos") val photos: List<String> = emptyList(),
    @SerializedName("amenities") val amenities: List<String> = emptyList(),
    @SerializedName("openingHours") val openingHours: List<GymOpeningHour> = emptyList(),
    @SerializedName("sessionTypes") val sessionTypes: List<GymSessionType> = emptyList(),
    @SerializedName("plans") val plans: List<GymPlan> = emptyList(),
    @SerializedName("holidays") val holidays: List<String> = emptyList(),
    @SerializedName("womenOnlyHours") val womenOnlyHours: WomenOnlyHours? = null,
    @SerializedName("startingPrice") val startingPriceRaw: Double? = null,
    @SerializedName("startingPlanType") val startingPlanType: String? = null,
    @SerializedName("capacityPerSlot") val capacityPerSlot: Int = 20,
    @SerializedName("status") val status: String = "approved",
    @SerializedName("rejectionReason") val rejectionReason: String? = null,
    @SerializedName("ratingAvg") val ratingAvg: Double = 0.0,
    @SerializedName("ratingCount") val ratingCount: Int = 0,
    @SerializedName("isFeatured") val isFeatured: Boolean = false,
    @SerializedName("distanceKm") val distanceKm: Double = 0.0,
    @SerializedName("owner") val owner: GymOwnerInfo? = null
) {
    val startingPrice: Double
        get() = startingPriceRaw ?: plans.filter { it.isActive }.minOfOrNull { it.price }
            ?: sessionTypes.minOfOrNull { it.price } ?: 0.0

    val primaryPhoto: String?
        get() = photos.firstOrNull()

    val activePlans: List<GymPlan>
        get() = if (plans.isNotEmpty()) plans.filter { it.isActive } else sessionTypes.map {
            GymPlan(
                id = it.type,
                name = it.name,
                type = it.type,
                durationDays = if (it.type.equals("dayPass", ignoreCase = true) || it.type.equals("day_pass", ignoreCase = true)) 1 else 30,
                price = it.price,
                description = it.description
            )
        }
}

data class NearbyGymsResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("count") val count: Int = 0,
    @SerializedName("total") val total: Int = 0,
    @SerializedName("page") val page: Int = 1,
    @SerializedName("totalPages") val totalPages: Int = 1,
    @SerializedName("radiusKm") val radiusKm: Double = 5.0,
    @SerializedName("gyms") val gyms: List<GymItem> = emptyList(),
    @SerializedName("message") val message: String? = null
)

data class SingleGymResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("gym") val gym: GymItem? = null,
    @SerializedName("message") val message: String? = null
)

data class GymSlot(
    @SerializedName("slot") val slot: String = "",
    @SerializedName("capacity") val capacity: Int = 20,
    @SerializedName("booked") val booked: Int = 0,
    @SerializedName("available") val available: Int = 20,
    @SerializedName("isAvailable") val isAvailable: Boolean = true,
    @SerializedName("isPast") val isPast: Boolean = false
)

data class GymSlotsResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("date") val date: String = "",
    @SerializedName("day") val day: String = "",
    @SerializedName("isClosed") val isClosed: Boolean = false,
    @SerializedName("capacityPerSlot") val capacityPerSlot: Int = 20,
    @SerializedName("slots") val slots: List<GymSlot> = emptyList(),
    @SerializedName("message") val message: String? = null
)

data class CreateBookingRequest(
    @SerializedName("gymId") val gymId: String,
    @SerializedName("sessionType") val sessionType: String,
    @SerializedName("date") val date: String,
    @SerializedName("slot") val slot: String
)

data class RazorpayOrderInfo(
    @SerializedName("orderId") val orderId: String = "",
    @SerializedName("amount") val amount: Long = 0L,
    @SerializedName("currency") val currency: String = "INR",
    @SerializedName("keyId") val keyId: String = ""
)

data class GymBookingDetails(
    @SerializedName("_id") val id: String = "",
    @SerializedName("gymId") val gymId: String = "",
    @SerializedName("gymName") val gymName: String = "",
    @SerializedName("userName") val userName: String? = null,
    @SerializedName("userEmail") val userEmail: String? = null,
    @SerializedName("sessionType") val sessionType: String = "",
    @SerializedName("date") val date: String = "",
    @SerializedName("slot") val slot: String = "",
    @SerializedName("price") val price: Double = 0.0,
    @SerializedName("checkInCode") val checkInCode: String = "",
    @SerializedName("status") val status: String = "pending_payment",
    @SerializedName("razorpayOrderId") val razorpayOrderId: String? = null,
    @SerializedName("attendedAt") val attendedAt: String? = null,
    @SerializedName("cancelledAt") val cancelledAt: String? = null,
    @SerializedName("refundPercent") val refundPercent: Double? = null,
    @SerializedName("refundAmount") val refundAmount: Double? = null,
    @SerializedName("createdAt") val createdAt: String? = null
)

data class CreateBookingResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("message") val message: String = "",
    @SerializedName("booking") val booking: GymBookingDetails? = null,
    @SerializedName("razorpayOrder") val razorpayOrder: RazorpayOrderInfo? = null
)

data class VerifyPaymentRequest(
    @SerializedName("razorpay_order_id") val razorpayOrderId: String,
    @SerializedName("razorpay_payment_id") val razorpayPaymentId: String,
    @SerializedName("razorpay_signature") val razorpaySignature: String
)

data class PaymentRecord(
    @SerializedName("orderId") val orderId: String = "",
    @SerializedName("paymentId") val paymentId: String = "",
    @SerializedName("amount") val amount: Double = 0.0,
    @SerializedName("platformFee") val platformFee: Double = 0.0,
    @SerializedName("partnerAmount") val partnerAmount: Double = 0.0,
    @SerializedName("status") val status: String = ""
)

data class VerifyPaymentResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("message") val message: String = "",
    @SerializedName("booking") val booking: GymBookingDetails? = null,
    @SerializedName("payment") val payment: PaymentRecord? = null
)

data class CancellationInfo(
    @SerializedName("bookingId") val bookingId: String = "",
    @SerializedName("status") val status: String = "cancelled",
    @SerializedName("cancelledAt") val cancelledAt: String = "",
    @SerializedName("hoursBeforeSlot") val hoursBeforeSlot: Double = 0.0,
    @SerializedName("refundPercent") val refundPercent: Double = 0.0,
    @SerializedName("refundAmount") val refundAmount: Double = 0.0,
    @SerializedName("refundStatus") val refundStatus: String = ""
)

data class CancelBookingResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("message") val message: String = "",
    @SerializedName("cancellation") val cancellation: CancellationInfo? = null
)

data class GymBookingsListResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("count") val count: Int = 0,
    @SerializedName("bookings") val bookings: List<GymBookingDetails> = emptyList(),
    @SerializedName("message") val message: String? = null
)

data class ReviewItem(
    @SerializedName("_id") val id: String = "",
    @SerializedName("targetId") val targetId: String = "",
    @SerializedName("userId") val userId: String? = null,
    @SerializedName("userName") val userName: String? = null,
    @SerializedName("rating") val rating: Int = 5,
    @SerializedName("comment") val comment: String = "",
    @SerializedName("createdAt") val createdAt: String = ""
)

data class GymReviewRequest(
    @SerializedName("rating") val rating: Int,
    @SerializedName("comment") val comment: String
)

data class GymRatingSummary(
    @SerializedName("ratingAvg") val ratingAvg: Double = 0.0,
    @SerializedName("ratingCount") val ratingCount: Int = 0
)

data class GymReviewResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("message") val message: String = "",
    @SerializedName("review") val review: ReviewItem? = null,
    @SerializedName("gymRating") val gymRating: GymRatingSummary? = null
)

data class GymReviewsListResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("reviews") val reviews: List<ReviewItem> = emptyList()
)

data class CheckInRequest(
    @SerializedName("checkInCode") val checkInCode: String
)

data class CheckInResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("message") val message: String = "",
    @SerializedName("booking") val booking: GymBookingDetails? = null
)

data class GymEarningsSummary(
    @SerializedName("totalBookings") val totalBookings: Int = 0,
    @SerializedName("confirmedBookings") val confirmedBookings: Int = 0,
    @SerializedName("attendedBookings") val attendedBookings: Int = 0,
    @SerializedName("cancelledBookings") val cancelledBookings: Int = 0,
    @SerializedName("totalRevenue") val totalRevenue: Double = 0.0,
    @SerializedName("totalPartnerEarnings") val totalPartnerEarnings: Double = 0.0,
    @SerializedName("totalPlatformFee") val totalPlatformFee: Double = 0.0
)

data class GymTransactionItem(
    @SerializedName("id") val id: String = "",
    @SerializedName("orderId") val orderId: String = "",
    @SerializedName("paymentId") val paymentId: String = "",
    @SerializedName("user") val user: HomeUser? = null,
    @SerializedName("amount") val amount: Double = 0.0,
    @SerializedName("partnerAmount") val partnerAmount: Double = 0.0,
    @SerializedName("platformFee") val platformFee: Double = 0.0,
    @SerializedName("createdAt") val createdAt: String = ""
)

data class GymEarningsGymInfo(
    @SerializedName("id") val id: String = "",
    @SerializedName("name") val name: String = ""
)

data class GymEarningsResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("gym") val gym: GymEarningsGymInfo? = null,
    @SerializedName("summary") val summary: GymEarningsSummary? = null,
    @SerializedName("recentTransactions") val recentTransactions: List<GymTransactionItem> = emptyList(),
    @SerializedName("message") val message: String? = null
)

data class ImageUploadResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("message") val message: String = "",
    @SerializedName("url") val url: String = "",
    @SerializedName("filename") val filename: String = ""
)

data class GeocodeRequest(
    @SerializedName("address") val address: String
)

data class GeocodeLocation(
    @SerializedName("lat") val lat: Double = 0.0,
    @SerializedName("lng") val lng: Double = 0.0
)

data class GeocodeResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("configured") val configured: Boolean = true,
    @SerializedName("message") val message: String? = null,
    @SerializedName("location") val location: GeocodeLocation? = null,
    @SerializedName("formattedAddress") val formattedAddress: String? = null
)

data class RegisterGymRequest(
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("address") val address: String,
    @SerializedName("city") val city: String,
    @SerializedName("pincode") val pincode: String? = null,
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("location") val location: GeoLocation,
    @SerializedName("photos") val photos: List<String> = emptyList(),
    @SerializedName("amenities") val amenities: List<String> = emptyList(),
    @SerializedName("openingHours") val openingHours: List<GymOpeningHour> = emptyList(),
    @SerializedName("plans") val plans: List<GymPlan> = emptyList(),
    @SerializedName("holidays") val holidays: List<String> = emptyList(),
    @SerializedName("womenOnlyHours") val womenOnlyHours: WomenOnlyHours? = null,
    @SerializedName("sessionTypes") val sessionTypes: List<GymSessionType> = emptyList(),
    @SerializedName("capacityPerSlot") val capacityPerSlot: Int = 20
)

data class RegisterGymResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("message") val message: String = "",
    @SerializedName("gym") val gym: GymItem? = null
)

// --- Part 1 Membership & Enquiry Models ---

data class GymMembership(
    @SerializedName("_id") val mongoId: String = "",
    @SerializedName("id") val publicId: String? = null,
    @SerializedName("userId") val userId: String = "",
    @SerializedName("gymId") val gymId: String = "",
    @SerializedName("gymName") val gymName: String? = null,
    @SerializedName("planId") val planId: String = "",
    @SerializedName("planName") val planName: String = "",
    @SerializedName("planType") val planType: String = "",
    @SerializedName("durationDays") val durationDays: Int = 30,
    @SerializedName("price") val price: Double = 0.0,
    @SerializedName("startDate") val startDate: String = "",
    @SerializedName("endDate") val endDate: String = "",
    @SerializedName("status") val status: String = "active", // active, upcoming, expired, cancelled
    @SerializedName("memberCode") val memberCode: String = "",
    @SerializedName("isFrozen") val isFrozen: Boolean = false,
    @SerializedName("freezeCount") val freezeCount: Int = 0,
    @SerializedName("frozenDays") val frozenDays: Int = 0,
    @SerializedName("maxFreezeDays") val maxFreezeDays: Int = 0,
    @SerializedName("qrCode") val qrCode: String = "",
    @SerializedName("paymentId") val paymentId: String? = null,
    @SerializedName("paymentStatus") val paymentStatus: String? = null,
    @SerializedName("gym") val gym: GymItem? = null,
    @SerializedName("refundAmount") val refundAmount: Double? = null,
    @SerializedName("createdAt") val createdAt: String? = null
) {
    val id: String get() = mongoId.ifEmpty { publicId ?: "" }
}

data class CreateMembershipRequest(
    @SerializedName("gymId") val gymId: String,
    @SerializedName("planId") val planId: String,
    @SerializedName("startDate") val startDate: String
)

data class CreateMembershipResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("message") val message: String = "",
    @SerializedName("membership") val membership: GymMembership? = null,
    @SerializedName("payment") val payment: PaymentRecord? = null
)

data class MyGymMembershipsResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("active") val active: List<GymMembership> = emptyList(),
    @SerializedName("upcoming") val upcoming: List<GymMembership> = emptyList(),
    @SerializedName("past") val past: List<GymMembership> = emptyList(),
    @SerializedName("memberships") val memberships: List<GymMembership> = emptyList(),
    @SerializedName("message") val message: String? = null
)

data class SingleMembershipResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("membership") val membership: GymMembership? = null,
    @SerializedName("message") val message: String? = null
)

data class FreezeMembershipRequest(
    @SerializedName("freezeDays") val freezeDays: Int
)

data class GymMembershipActionResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("message") val message: String = "",
    @SerializedName("membership") val membership: GymMembership? = null,
    @SerializedName("refundAmount") val refundAmount: Double? = null,
    @SerializedName("refundPercent") val refundPercent: Double? = null
)

data class GymEnquiryRequest(
    @SerializedName("name") val name: String,
    @SerializedName("phone") val phone: String,
    @SerializedName("message") val message: String = "",
    @SerializedName("preferredTime") val preferredTime: String? = null,
    @SerializedName("wantsTrial") val wantsTrial: Boolean = false
)

data class GymEnquiryItem(
    @SerializedName("_id") val mongoId: String = "",
    @SerializedName("id") val publicId: String? = null,
    @SerializedName("gymId") val gymId: String = "",
    @SerializedName("userId") val userId: String? = null,
    @SerializedName("name") val name: String = "",
    @SerializedName("phone") val phone: String = "",
    @SerializedName("message") val message: String = "",
    @SerializedName("preferredTime") val preferredTime: String? = null,
    @SerializedName("wantsTrial") val wantsTrial: Boolean = false,
    @SerializedName("createdAt") val createdAt: String = ""
) {
    val id: String get() = mongoId.ifEmpty { publicId ?: "" }
}

data class GymEnquiriesResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("enquiries") val enquiries: List<GymEnquiryItem> = emptyList(),
    @SerializedName("message") val message: String? = null
)

data class GymMemberInfo(
    @SerializedName("_id") val mongoId: String = "",
    @SerializedName("id") val publicId: String? = null,
    @SerializedName("userId") val userId: String? = null,
    @SerializedName("userName") val userName: String? = null,
    @SerializedName("userEmail") val userEmail: String? = null,
    @SerializedName("userPhone") val userPhone: String? = null,
    @SerializedName("planName") val planName: String = "",
    @SerializedName("planType") val planType: String = "",
    @SerializedName("startDate") val startDate: String = "",
    @SerializedName("endDate") val endDate: String = "",
    @SerializedName("status") val status: String = "active",
    @SerializedName("memberCode") val memberCode: String = "",
    @SerializedName("isFrozen") val isFrozen: Boolean = false,
    @SerializedName("price") val price: Double = 0.0
) {
    val id: String get() = mongoId.ifEmpty { publicId ?: "" }
}

data class GymMembersListResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("count") val count: Int = 0,
    @SerializedName("members") val members: List<GymMemberInfo> = emptyList(),
    @SerializedName("message") val message: String? = null
)

data class GymPlanEarningsItem(
    @SerializedName("planId") val planId: String = "",
    @SerializedName("planName") val planName: String = "",
    @SerializedName("planType") val planType: String = "",
    @SerializedName("totalSold") val totalSold: Int = 0,
    @SerializedName("revenue") val revenue: Double = 0.0,
    @SerializedName("platformFee") val platformFee: Double = 0.0,
    @SerializedName("partnerAmount") val partnerAmount: Double = 0.0
)

data class GymEarningsByPlanResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("gymId") val gymId: String = "",
    @SerializedName("gymName") val gymName: String = "",
    @SerializedName("totalRevenue") val totalRevenue: Double = 0.0,
    @SerializedName("totalPartnerEarnings") val totalPartnerEarnings: Double = 0.0,
    @SerializedName("totalPlatformFee") val totalPlatformFee: Double = 0.0,
    @SerializedName("plans") val plans: List<GymPlanEarningsItem> = emptyList(),
    @SerializedName("message") val message: String? = null
)

data class OwnerMemberCodeCheckInRequest(
    @SerializedName("memberCode") val memberCode: String
)

