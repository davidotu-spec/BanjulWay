package com.example.data

import kotlin.math.*

/**
 * Result data class containing complete breakdown of an estimated trip fare.
 */
data class FareEstimateResult(
    val baseFare: Double,
    val distanceKm: Double,
    val perKmRate: Double,
    val distanceCharge: Double,
    val estimatedDurationMin: Int,
    val perMinRate: Double,
    val timeCharge: Double,
    val subtotal: Double,
    val surchargeMultiplier: Double,
    val surchargeTier: String, // "OFF_PEAK", "STANDARD", "RUSH_HOUR"
    val finalFareGmd: Int,
    val vehicleType: String, // "CAR" or "TRICYCLE"
    val vehicleDisplayName: String,
    val fareGuaranteeMessage: String = "Guaranteed local fare calculated before booking confirmation"
)

/**
 * Service that estimates trip fares based on estimated distance (or coordinates)
 * and vehicle type (CAR vs TRICYCLE) to display before a user confirms a booking.
 */
object TripFareEstimationService {

    // Pricing Constants for Gambia Local Ride Market
    const val CAR_BASE_FARE = 60.0
    const val CAR_PER_KM_RATE = 22.0
    const val CAR_PER_MIN_RATE = 3.0
    const val CAR_MIN_FARE = 100

    const val TRICYCLE_BASE_FARE = 30.0
    const val TRICYCLE_PER_KM_RATE = 11.0
    const val TRICYCLE_PER_MIN_RATE = 1.5
    const val TRICYCLE_MIN_FARE = 50

    // Average speed for duration estimation (25 km/h in urban Gambia traffic)
    private const val AVERAGE_SPEED_KMH = 25.0

    /**
     * Calculates Haversine distance in kilometers between two GPS coordinates (origin and destination).
     */
    fun calculateDistanceKm(
        pLat: Double, pLng: Double,
        dLat: Double, dLng: Double
    ): Double {
        val r = 6371.0 // Earth radius in km
        val dLatRad = Math.toRadians(dLat - pLat)
        val dLonRad = Math.toRadians(dLng - pLng)
        val a = sin(dLatRad / 2) * sin(dLatRad / 2) +
                cos(Math.toRadians(pLat)) * cos(Math.toRadians(dLat)) *
                sin(dLonRad / 2) * sin(dLonRad / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        val raw = r * c
        return if (raw < 0.2) 2.4 else (round(raw * 10) / 10.0)
    }

    /**
     * Estimates trip duration in minutes given distance in km.
     */
    fun estimateDurationMinutes(distanceKm: Double): Int {
        val calculated = (distanceKm / AVERAGE_SPEED_KMH * 60).toInt() + 2
        return if (calculated < 3) 5 else calculated
    }

    /**
     * Primary function to calculate an estimated fare based on the distance between
     * origin (pickup) and destination (drop-off) coordinates.
     */
    fun calculateEstimatedFare(
        originLat: Double,
        originLng: Double,
        destLat: Double,
        destLng: Double,
        vehicleType: String = "CAR",
        surchargeTier: String = "STANDARD"
    ): FareEstimateResult {
        val distanceKm = calculateDistanceKm(originLat, originLng, destLat, destLng)
        return calculateEstimatedFareByDistance(distanceKm, vehicleType, surchargeTier)
    }

    /**
     * Calculates an estimated fare based directly on distance in kilometers and vehicle type.
     */
    fun calculateEstimatedFareByDistance(
        distanceKm: Double,
        vehicleType: String = "CAR",
        surchargeTier: String = "STANDARD"
    ): FareEstimateResult {
        val isCar = vehicleType.uppercase().contains("CAR")
        
        val baseFare = if (isCar) CAR_BASE_FARE else TRICYCLE_BASE_FARE
        val perKmRate = if (isCar) CAR_PER_KM_RATE else TRICYCLE_PER_KM_RATE
        val perMinRate = if (isCar) CAR_PER_MIN_RATE else TRICYCLE_PER_MIN_RATE
        val minFare = if (isCar) CAR_MIN_FARE else TRICYCLE_MIN_FARE
        val displayName = if (isCar) "Taxi / Car" else "Tricycle (Keke)"

        val durationMinutes = estimateDurationMinutes(distanceKm)

        val distanceCharge = distanceKm * perKmRate
        val timeCharge = durationMinutes * perMinRate
        val subtotal = baseFare + distanceCharge + timeCharge

        val multiplier = when (surchargeTier.uppercase()) {
            "OFF_PEAK" -> 0.85
            "RUSH_HOUR" -> 1.30
            else -> 1.00
        }

        val rawGrandTotal = subtotal * multiplier

        // Round off to nearest 5 GMD to match local currency market standard
        val roundedFare = (round(rawGrandTotal / 5.0) * 5).toInt()
        val finalFare = max(roundedFare, minFare)

        return FareEstimateResult(
            baseFare = baseFare,
            distanceKm = distanceKm,
            perKmRate = perKmRate,
            distanceCharge = distanceCharge,
            estimatedDurationMin = durationMinutes,
            perMinRate = perMinRate,
            timeCharge = timeCharge,
            subtotal = subtotal,
            surchargeMultiplier = multiplier,
            surchargeTier = surchargeTier,
            finalFareGmd = finalFare,
            vehicleType = if (isCar) "CAR" else "TRICYCLE",
            vehicleDisplayName = displayName
        )
    }

    /**
     * Estimates fare given pickup and dropoff coordinates and vehicle type (compatibility alias).
     */
    fun estimateFare(
        pLat: Double, pLng: Double,
        dLat: Double, dLng: Double,
        vehicleType: String,
        surchargeTier: String = "STANDARD"
    ): FareEstimateResult {
        return calculateEstimatedFare(pLat, pLng, dLat, dLng, vehicleType, surchargeTier)
    }

    /**
     * Estimates fare given distance in kilometers and vehicle type (compatibility alias).
     */
    fun estimateFareByDistance(
        distanceKm: Double,
        vehicleType: String,
        surchargeTier: String = "STANDARD"
    ): FareEstimateResult {
        return calculateEstimatedFareByDistance(distanceKm, vehicleType, surchargeTier)
    }

    /**
     * Compares fares for both Car and Tricycle side-by-side for a trip.
     */
    fun compareVehicleFares(
        pLat: Double, pLng: Double,
        dLat: Double, dLng: Double,
        surchargeTier: String = "STANDARD"
    ): Pair<FareEstimateResult, FareEstimateResult> {
        val carEstimate = estimateFare(pLat, pLng, dLat, dLng, "CAR", surchargeTier)
        val tricycleEstimate = estimateFare(pLat, pLng, dLat, dLng, "TRICYCLE", surchargeTier)
        return Pair(carEstimate, tricycleEstimate)
    }
}
