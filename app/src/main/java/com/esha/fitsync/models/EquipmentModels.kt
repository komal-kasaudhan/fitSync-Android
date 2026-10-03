package com.esha.fitsync.models

import com.google.gson.annotations.SerializedName

data class MasterEquipment(
    @SerializedName("id") val id: String = "",
    @SerializedName(value = "label", alternate = ["name"]) val label: String = "",
    @SerializedName("category") val category: String? = null,
    @SerializedName("icon") val icon: String? = null
)

data class MasterEquipmentResponse(
    @SerializedName("equipment") val equipment: List<MasterEquipment> = emptyList()
)

data class UserEquipmentResponse(
    @SerializedName("equipment") val equipment: List<String> = emptyList(),
    @SerializedName("planStale") val planStale: Boolean = false
)

data class UpdateEquipmentRequest(
    @SerializedName("equipment") val equipment: List<String> = emptyList()
)

data class UpdateEquipmentResponse(
    @SerializedName("message") val message: String? = null,
    @SerializedName("equipment") val equipment: List<String> = emptyList(),
    @SerializedName("planStale") val planStale: Boolean = false
)

data class RegeneratePlanResponse(
    @SerializedName("message") val message: String? = null,
    @SerializedName("planStale") val planStale: Boolean = false,
    @SerializedName("plan") val plan: EnginePlanWrapper? = null
)

