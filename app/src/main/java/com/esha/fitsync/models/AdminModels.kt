package com.esha.fitsync.models

import com.google.gson.annotations.SerializedName

data class AppConfigResponse(
    @SerializedName("success") val success: Boolean = true,
    @SerializedName("paymentsEnabled") val paymentsEnabled: Boolean = false
)

data class AdminStatsCounts(
    @SerializedName("total") val total: Int = 0,
    @SerializedName("pending") val pending: Int = 0,
    @SerializedName("approved") val approved: Int = 0,
    @SerializedName("sellers") val sellers: Int = 0,
    @SerializedName("products") val products: Int = 0,
    @SerializedName("orders") val orders: Int = 0
)

data class AdminRevenueStats(
    @SerializedName("totalRevenue") val totalRevenue: Double = 0.0,
    @SerializedName("totalPlatformFee") val totalPlatformFee: Double = 0.0,
    @SerializedName("totalPartnerAmount") val totalPartnerAmount: Double = 0.0
)

data class AdminStatsData(
    @SerializedName("usersCount") val usersCount: Int = 0,
    @SerializedName("gyms") val gyms: AdminStatsCounts? = null,
    @SerializedName("trainers") val trainers: AdminStatsCounts? = null,
    @SerializedName("marketplace") val marketplace: AdminStatsCounts? = null,
    @SerializedName("revenue") val revenue: AdminRevenueStats? = null
)

data class AdminStatsResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("stats") val stats: AdminStatsData? = null,
    @SerializedName("message") val message: String? = null
)

data class AdminMeResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName(value = "admin", alternate = ["user"]) val admin: AdminUserInfo? = null,
    @SerializedName("message") val message: String? = null
) {
    val user: AdminUserInfo? get() = admin
}

data class AdminUserInfo(
    @SerializedName(value = "_id", alternate = ["id"]) val id: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("email") val email: String = "",
    @SerializedName("roles") val roles: List<String> = emptyList()
)

data class AdminUpdateStatusRequest(
    @SerializedName("status") val status: String,
    @SerializedName("rejectionReason") val rejectionReason: String? = null
)

data class AdminGenericActionResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("message") val message: String = ""
)

data class AdminGymsListResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("count") val count: Int = 0,
    @SerializedName("gyms") val gyms: List<GymItem> = emptyList()
)

data class AdminEntityItem(
    @SerializedName("_id") val id: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("title") val title: String? = null,
    @SerializedName("businessName") val businessName: String? = null,
    @SerializedName("type") val type: String? = null,
    @SerializedName("category") val category: String? = null,
    @SerializedName("status") val status: String = "pending",
    @SerializedName("rejectionReason") val rejectionReason: String? = null,
    @SerializedName("flagReason") val flagReason: String? = null,
    @SerializedName("flaggedForAdminReview") val flaggedForAdminReview: Boolean = false,
    @SerializedName("price") val price: Double = 0.0,
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("address") val address: String? = null,
    @SerializedName("city") val city: String? = null,
    @SerializedName("photos") val photos: List<String> = emptyList(),
    @SerializedName("images") val images: List<String> = emptyList(),
    @SerializedName("owner") val owner: GymOwnerInfo? = null,
    @SerializedName("seller") val seller: GymOwnerInfo? = null,
    @SerializedName("location") val location: GeoLocation? = null,
    @SerializedName("fssaiNumber") val fssaiNumber: String? = null,
    @SerializedName("bio") val bio: String? = null,
    @SerializedName("description") val description: String? = null
) {
    val displayName: String
        get() = when {
            name.isNotEmpty() -> name
            !title.isNullOrEmpty() -> title
            !businessName.isNullOrEmpty() -> businessName
            else -> "Item"
        }

    val primaryImage: String?
        get() = photos.firstOrNull() ?: images.firstOrNull()
}

data class AdminEntityListResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("count") val count: Int = 0,
    @SerializedName("trainers") val trainers: List<AdminEntityItem> = emptyList(),
    @SerializedName("sellers") val sellers: List<AdminEntityItem> = emptyList(),
    @SerializedName("products") val products: List<AdminEntityItem> = emptyList(),
    @SerializedName("gyms") val gyms: List<AdminEntityItem> = emptyList()
)
