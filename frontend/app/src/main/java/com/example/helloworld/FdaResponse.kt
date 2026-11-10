package com.example.helloworld.model

import com.google.gson.annotations.SerializedName

data class FdaResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("message")
    val message: String,

    @SerializedName("oy_number")
    val oyNumber: String?,

    @SerializedName("detail")
    val detail: FdaDetail?
)

data class FdaDetail(
    @SerializedName("name_th")
    val nameTh: String?,

    @SerializedName("location_name")
    val locationName: String?,

    @SerializedName("product_status")
    val productType: String?,

    @SerializedName("address_name")
    val address: String?
)
