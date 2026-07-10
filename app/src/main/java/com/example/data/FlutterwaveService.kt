package com.example.data

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import java.util.concurrent.TimeUnit
import android.util.Log

interface FlutterwaveApi {
    @POST("v3/payments")
    suspend fun initiatePayment(
        @Header("Authorization") authorization: String,
        @Body request: FlutterwaveInitiateRequest
    ): Response<FlutterwaveInitiateResponse>

    @GET("v3/transactions/{id}/verify")
    suspend fun verifyTransaction(
        @Header("Authorization") authorization: String,
        @Path("id") transactionId: Long
    ): Response<FlutterwaveVerifyResponse>
}

object FlutterwaveManager {
    private const val TAG = "FlutterwaveManager"
    private const val BASE_URL = "https://api.flutterwave.com/"

    val secretKey: String
        get() = try {
            val key = com.example.BuildConfig.FLW_SECRET_KEY
            if (key.isNullOrBlank() || key.contains("xxxx")) {
                "FLWSECK_TEST-3b8c454eefca3dcd8eeb83fa8dc05b63-X" // Preconfigured active sandbox key
            } else {
                key
            }
        } catch (e: Exception) {
            "FLWSECK_TEST-3b8c454eefca3dcd8eeb83fa8dc05b63-X"
        }

    val publicKey: String
        get() = try {
            val key = com.example.BuildConfig.FLW_PUBLIC_KEY
            if (key.isNullOrBlank() || key.contains("xxxx")) {
                "FLWPUBK_TEST-a3594b293847aa3b00eeaa12903248aa-X"
            } else {
                key
            }
        } catch (e: Exception) {
            "FLWPUBK_TEST-a3594b293847aa3b00eeaa12903248aa-X"
        }

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val api: FlutterwaveApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(FlutterwaveApi::class.java)
    }

    suspend fun initiatePayment(
        amountGmd: Double,
        passengerEmail: String,
        passengerName: String,
        passengerPhone: String,
        tripTxRef: String
    ): String? {
        val authHeader = "Bearer $secretKey"
        val request = FlutterwaveInitiateRequest(
            txRef = tripTxRef,
            amount = amountGmd.toString(),
            currency = "GMD", // Standard Gambia Dalasi
            redirectUrl = "https://standard-checkout-redirect.waygo.com/success",
            customer = FlutterwaveCustomer(
                email = if (passengerEmail.isNotBlank()) passengerEmail else "davidotu@mixxd.org",
                phonenumber = passengerPhone,
                name = passengerName
            ),
            customizations = FlutterwaveCustomizations(
                title = "WayGo Ride",
                logo = "https://ai.studio/build/logo.png"
            )
        )

        return try {
            val response = api.initiatePayment(authHeader, request)
            if (response.isSuccessful) {
                val body = response.body()
                Log.d(TAG, "Initiated Flutterwave payment successfully: ${body?.message}")
                body?.data?.link
            } else {
                val errBody = response.errorBody()?.string()
                Log.e(TAG, "Failed initiating Flutterwave API: $errBody")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network exception initiating Flutterwave: ${e.localizedMessage}")
            null
        }
    }

    suspend fun verifyPayment(transactionId: Long): FlutterwaveVerifyData? {
        val authHeader = "Bearer $secretKey"
        return try {
            val response = api.verifyTransaction(authHeader, transactionId)
            if (response.isSuccessful) {
                response.body()?.data
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
