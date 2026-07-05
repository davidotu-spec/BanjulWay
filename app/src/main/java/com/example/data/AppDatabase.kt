package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        UserProfileEntity::class,
        DriverEntity::class,
        TripEntity::class,
        SupportMessageEntity::class,
        ScheduledRideEntity::class,
        SavedPlaceEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class BanjulWayDatabase : RoomDatabase() {
    abstract fun dao(): BanjulWayDao

    companion object {
        @Volatile
        private var INSTANCE: BanjulWayDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): BanjulWayDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BanjulWayDatabase::class.java,
                    "banjulway_database"
                )
                .fallbackToDestructiveMigration()
                .addCallback(DatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    preseedData(database.dao())
                }
            }
        }

        private suspend fun preseedData(dao: BanjulWayDao) {
            // Seed passenger profile
            dao.insertOrUpdateProfile(
                UserProfileEntity(
                    id = "current_passenger",
                    name = "David Otu",
                    phone = "+220 771 2345",
                    email = "davidotu@mixxd.org",
                    gender = "Male",
                    mobileMoneyNumber = "+220 384 5678",
                    savedHome = "Kairaba Avenue, Serrekunda",
                    savedWork = "Albert Market, Banjul",
                    avatarIndex = 1
                )
            )

            // Seed initial Saved Places
            dao.insertSavedPlace(
                SavedPlaceEntity(
                    name = "Kairaba Avenue, Serrekunda",
                    label = "Home",
                    lat = 13.4471,
                    lng = -16.6791,
                    iconType = "HOME"
                )
            )
            dao.insertSavedPlace(
                SavedPlaceEntity(
                    name = "Albert Market, Banjul",
                    label = "Work",
                    lat = 13.4533,
                    lng = -16.5746,
                    iconType = "WORK"
                )
            )

            // Seed initial drivers
            val initialDrivers = listOf(
                DriverEntity(
                    id = "drv_alieu",
                    name = "Alieu Ceesay",
                    phone = "+220 992 4831",
                    vehicleType = "CAR",
                    vehiclePlate = "BJL 4821 C",
                    rating = 4.8f,
                    approvalStatus = "APPROVED",
                    isOnline = true,
                    currentLat = 13.4470,
                    currentLng = -16.6790,
                    driverLicense = "DL-2024-9981"
                ),
                DriverEntity(
                    id = "drv_mariama",
                    name = "Mariama Jallow",
                    phone = "+220 312 0451",
                    vehicleType = "TRICYCLE",
                    vehiclePlate = "KM 9312 T",
                    rating = 4.9f,
                    approvalStatus = "APPROVED",
                    isOnline = true,
                    currentLat = 13.4460,
                    currentLng = -16.6720,
                    driverLicense = "DL-2025-1029"
                ),
                DriverEntity(
                    id = "drv_bakary",
                    name = "Bakary Touray",
                    phone = "+220 754 1121",
                    vehicleType = "CAR",
                    vehiclePlate = "WCR 7431 B",
                    rating = 4.7f,
                    approvalStatus = "APPROVED",
                    isOnline = true,
                    currentLat = 13.4410,
                    currentLng = -16.7100,
                    driverLicense = "DL-2023-4552"
                ),
                DriverEntity(
                    id = "drv_ebrima",
                    name = "Ebrima Sanneh",
                    phone = "+220 221 0098",
                    vehicleType = "TRICYCLE",
                    vehiclePlate = "BJL 1102 T",
                    rating = 4.6f,
                    approvalStatus = "APPROVED",
                    isOnline = true,
                    currentLat = 13.4530,
                    currentLng = -16.5760,
                    driverLicense = "DL-2025-0551"
                ),
                DriverEntity(
                    id = "drv_fatoumata",
                    name = "Fatoumata Barrow",
                    phone = "+220 541 7765",
                    vehicleType = "CAR",
                    vehiclePlate = "KM 5612 A",
                    rating = 5.0f,
                    approvalStatus = "PENDING", // Pending Admin Approval
                    isOnline = false,
                    currentLat = 13.4750,
                    currentLng = -16.6800,
                    driverLicense = "DL-2026-6612"
                ),
                DriverEntity(
                    id = "drv_lamin",
                    name = "Lamin Camara",
                    phone = "+220 663 2291",
                    vehicleType = "TRICYCLE",
                    vehiclePlate = "KM 3099 T",
                    rating = 4.3f,
                    approvalStatus = "APPROVED",
                    isOnline = false, // Offline
                    currentLat = 13.4380,
                    currentLng = -16.6820,
                    driverLicense = "DL-2024-3011"
                )
            )
            dao.insertDrivers(initialDrivers)

            // Seed initial completed trips
            dao.insertOrUpdateTrip(
                TripEntity(
                    id = "trip_local_1",
                    passengerName = "David Otu",
                    driverId = "drv_alieu",
                    driverName = "Alieu Ceesay",
                    vehicleType = "CAR",
                    vehiclePlate = "BJL 4821 C",
                    pickupName = "Kairaba Avenue, Serrekunda",
                    dropoffName = "Albert Market, Banjul",
                    pickupLat = 13.4470,
                    pickupLng = -16.6790,
                    dropoffLat = 13.4530,
                    dropoffLng = -16.5760,
                    fareGmd = 350,
                    paymentMethod = "WAVE",
                    status = "COMPLETED",
                    rating = 5,
                    reviewComment = "Smooth ride from Serrekunda to Albert Market!",
                    reviewTags = "Safe, Polite, Quick",
                    timestamp = System.currentTimeMillis() - 24 * 60 * 60 * 1000
                )
            )
            dao.insertOrUpdateTrip(
                TripEntity(
                    id = "trip_local_2",
                    passengerName = "David Otu",
                    driverId = "drv_mariama",
                    driverName = "Mariama Jallow",
                    vehicleType = "TRICYCLE",
                    vehiclePlate = "KM 9312 T",
                    pickupName = "Senegambia Strip, Kololi",
                    dropoffName = "Kairaba Avenue, Serrekunda",
                    pickupLat = 13.4380,
                    pickupLng = -16.7120,
                    dropoffLat = 13.4470,
                    dropoffLng = -16.6790,
                    fareGmd = 150,
                    paymentMethod = "CASH",
                    status = "COMPLETED",
                    rating = 4,
                    reviewComment = "Fun tricycle ride!",
                    reviewTags = "Cheerful",
                    timestamp = System.currentTimeMillis() - 3 * 24 * 60 * 60 * 1000
                )
            )

            // Seed initial support messages
            dao.insertSupportMessage(
                SupportMessageEntity(
                    senderRole = "PASSENGER",
                    message = "Hello! Welcome to BanjulWay. How can we help you with your journey in Gambia today?",
                    status = "RESOLVED",
                    issueCategory = "Welcome"
                )
            )
        }
    }
}
