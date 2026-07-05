package com.example.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class FlutterwaveInitiateRequest(
    @Json(name = "tx_ref") val txRef: String,
    @Json(name = "amount") val amount: String,
    @Json(name = "currency") val currency: String,
    @Json(name = "redirect_url") val redirectUrl: String,
    @Json(name = "customer") val customer: FlutterwaveCustomer,
    @Json(name = "customizations") val customizations: FlutterwaveCustomizations? = null,
    @Json(name = "meta") val meta: Map<String, String>? = null
)

@JsonClass(generateAdapter = true)
data class FlutterwaveCustomer(
    @Json(name = "email") val email: String,
    @Json(name = "phonenumber") val phonenumber: String,
    @Json(name = "name") val name: String
)

@JsonClass(generateAdapter = true)
data class FlutterwaveCustomizations(
    @Json(name = "title") val title: String,
    @Json(name = "logo") val logo: String? = null
)

@JsonClass(generateAdapter = true)
data class FlutterwaveInitiateResponse(
    @Json(name = "status") val status: String,
    @Json(name = "message") val message: String,
    @Json(name = "data") val data: FlutterwaveInitiateData? = null
)

@JsonClass(generateAdapter = true)
data class FlutterwaveInitiateData(
    @Json(name = "link") val link: String
)

@JsonClass(generateAdapter = true)
data class FlutterwaveVerifyResponse(
    @Json(name = "status") val status: String,
    @Json(name = "message") val message: String,
    @Json(name = "data") val data: FlutterwaveVerifyData? = null
)

@JsonClass(generateAdapter = true)
data class FlutterwaveVerifyData(
    @Json(name = "id") val id: Long,
    @Json(name = "tx_ref") val txRef: String,
    @Json(name = "flw_ref") val flwRef: String? = null,
    @Json(name = "amount") val amount: Double,
    @Json(name = "currency") val currency: String,
    @Json(name = "status") val status: String,
    @Json(name = "payment_type") val paymentType: String? = null,
    @Json(name = "created_at") val createdAt: String? = null
)
