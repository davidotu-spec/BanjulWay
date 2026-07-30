package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey val id: String = "current_passenger",
    val name: String,
    val phone: String,
    val email: String = "johndoe@example.com",
    val gender: String = "Not Specified",
    val mobileMoneyNumber: String = "",
    val savedHome: String = "Kairaba Avenue, Serrekunda",
    val savedWork: String = "Albert Market, Banjul",
    val avatarIndex: Int = 0,
    val photoUri: String? = null,
    val isPaymentLinked: Boolean = false,
    val linkedCardLast4: String = "",
    val linkedPaymentEmail: String = ""
)

@Entity(tableName = "drivers")
data class DriverEntity(
    @PrimaryKey val id: String,
    val name: String,
    val phone: String,
    val vehicleType: String, // "CAR" or "TRICYCLE"
    val vehiclePlate: String,
    val rating: Float,
    val approvalStatus: String, // "PENDING", "APPROVED", "REJECTED"
    val isOnline: Boolean,
    val currentLat: Double,
    val currentLng: Double,
    val driverLicense: String,
    val isVerified: Boolean = true
)

@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey val id: String,
    val passengerName: String,
    val driverId: String?,
    val driverName: String?,
    val vehicleType: String, // "CAR" or "TRICYCLE"
    val vehiclePlate: String?,
    val pickupName: String,
    val dropoffName: String,
    val pickupLat: Double,
    val pickupLng: Double,
    val dropoffLat: Double,
    val dropoffLng: Double,
    val fareGmd: Int,
    val paymentMethod: String, // "CASH", "WAVE", "AFRICELL", "QCELL"
    val status: String, // "REQUESTED", "ACCEPTED", "EN_ROUTE", "ARRIVED", "COMPLETED", "CANCELLED"
    val rating: Int = 0, // 0 means not rated yet
    val reviewComment: String = "",
    val reviewTags: String = "", // comma-separated tags e.g. "Safe, Polite"
    val tipGmd: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val commissionGmd: Int = 0,
    val verificationPin: String = "4829",
    val preferences: String = ""
)

@Entity(tableName = "support_messages")
data class SupportMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val senderRole: String, // "PASSENGER" or "DRIVER"
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "PENDING", // "PENDING", "RESOLVED"
    val issueCategory: String = "General"
)

@Entity(tableName = "scheduled_rides")
data class ScheduledRideEntity(
    @PrimaryKey val id: String,
    val passengerName: String,
    val vehicleType: String, // "CAR" or "TRICYCLE"
    val pickupName: String,
    val dropoffName: String,
    val pickupLat: Double,
    val pickupLng: Double,
    val dropoffLat: Double,
    val dropoffLng: Double,
    val fareGmd: Int,
    val paymentMethod: String,
    val scheduledTime: String, // e.g. "Tomorrow, 14:00"
    val scheduledEpochMs: Long,
    val status: String = "SCHEDULED", // "SCHEDULED", "ACCEPTED", "CANCELLED"
    val isNotified: Boolean = false,
    val driverId: String? = null,
    val driverName: String? = null
)

@Entity(tableName = "saved_places")
data class SavedPlaceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val label: String, // e.g. "Home", "Work", "Gym", "Market"
    val lat: Double,
    val lng: Double,
    val iconType: String // "HOME", "WORK", "GYM", "MARKET", "PLACE"
)

@Entity(tableName = "vehicle_mileage")
data class VehicleMileageEntity(
    @PrimaryKey val driverId: String,
    val currentMileage: Double = 12450.0,
    val lastOilChangeMileage: Double = 10000.0,
    val lastTireCheckMileage: Double = 8000.0,
    val isSimulatingMileage: Boolean = false,
    val oilChangeInterval: Double = 5000.0,
    val tireCheckInterval: Double = 10000.0,
    val lastNotifiedOilChange: Double = 10000.0,
    val lastNotifiedTireCheck: Double = 8000.0
)


